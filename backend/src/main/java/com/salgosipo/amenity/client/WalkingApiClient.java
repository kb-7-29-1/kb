package com.salgosipo.amenity.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Log4j2
@Component
public class WalkingApiClient {

    public record WalkingRoute(Integer walkTimeMinutes, Integer distanceMeters) {
    }

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String tmapApiKey;

    public WalkingApiClient(RestTemplate restTemplate, @Value("${TMAP_API_KEY}") String tmapApiKey) {
        this.restTemplate = restTemplate;
        this.tmapApiKey = tmapApiKey;
    }

    // 1. TMAP 보행자 API: 출발지에서 도착지까지의 최소 도보 시간(분) 계산
    public WalkingRoute calculateWalkingRoute(Double startLat, Double startLng, Double endLat, Double endLng) {
        String url = "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1";

        HttpHeaders headers = new HttpHeaders();
        headers.set("appKey", tmapApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("startX", startLng); // 경도
        requestBody.put("startY", startLat); // 위도
        requestBody.put("endX", endLng);
        requestBody.put("endY", endLat);
        requestBody.put("startName", "출발지");
        requestBody.put("endName", "도착지");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode propertiesNode = rootNode.path("features").get(0).path("properties");
            int totalTimeSeconds = propertiesNode.path("totalTime").asInt();

            int totalDistanceMeters = propertiesNode.path("totalDistance").asInt();
            return new WalkingRoute(totalTimeSeconds / 60, totalDistanceMeters);

        } catch (Exception e) {
            log.error("TMAP 보행자 길찾기 API 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    // 2. TMAP 장소(POI) 검색 API: 특정 좌표 기준 가장 가까운 편의시설의 [위도, 경도] 반환
    public double[] findNearestPlace(Double centerLat, Double centerLng, String keyword) {
        // radius=2 (2km 반경), count=1 (가장 가까운 1개만 조회)
        String url = "https://apis.openapi.sk.com/tmap/pois?version=1&searchKeyword={keyword}&centerLat={lat}&centerLon={lon}&radius=2&count=1";

        HttpHeaders headers = new HttpHeaders();
        headers.set("appKey", tmapApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            // URL의 {} 변수에 keyword, centerLat, centerLon 순서대로 자동 매핑 (한글 검색어 자동 인코딩)
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class,
                    keyword, centerLat, centerLng
            );

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode poiNode = rootNode.path("searchPoiInfo").path("pois").path("poi");

            // 검색 결과가 1개라도 존재하는 경우
            if (poiNode.isArray() && poiNode.size() > 0) {
                JsonNode nearestPoi = poiNode.get(0);

                // TMAP POI API는 frontLat(위도), frontLon(경도)로 좌표를 반환합니다.
                double endLat = nearestPoi.path("frontLat").asDouble();
                double endLng = nearestPoi.path("frontLon").asDouble();

                return new double[]{endLat, endLng};
            }
        } catch (Exception e) {
            log.error("TMAP 장소 검색 API 호출 실패 (검색어: {}): {}", keyword, e.getMessage());
        }

        return null;
    }
}
