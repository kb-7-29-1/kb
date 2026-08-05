package com.salgosipo.amenity.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Component
public class WalkingApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String tmapApiKey;

    public WalkingApiClient(RestTemplate restTemplate, @Value("${TMAP_API_KEY}") String tmapApiKey) {
        this.restTemplate = restTemplate;
        this.tmapApiKey = tmapApiKey;
    }

    // 기준 좌표에서 가장 가까운 편의시설을 검색해 위도·경도를 반환
    public double[] findNearestPlace(Double centerLat, Double centerLng, String keyword) {
        String url = "https://apis.openapi.sk.com/tmap/pois?version=1&searchKeyword={keyword}&centerLat={lat}&centerLon={lon}&radius=2&count=1";

        HttpHeaders headers = new HttpHeaders();
        headers.set("appKey", tmapApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class,
                    keyword, centerLat, centerLng
            );

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode poiNode = rootNode.path("searchPoiInfo").path("pois").path("poi");
            if (poiNode.isArray() && poiNode.size() > 0) {
                JsonNode nearestPoi = poiNode.get(0);
                double endLat = nearestPoi.path("frontLat").asDouble();
                double endLng = nearestPoi.path("frontLon").asDouble();
                return new double[]{endLat, endLng};
            }
        } catch (Exception e) {
            log.error("TMAP POI 검색 실패. Keyword: {}, message: {}", keyword, e.getMessage());
        }

        return null;
    }
}
