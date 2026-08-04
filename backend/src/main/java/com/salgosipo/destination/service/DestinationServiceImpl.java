package com.salgosipo.destination.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salgosipo.destination.domain.DestinationVO;
import com.salgosipo.destination.dto.DestinationDTO;
import com.salgosipo.destination.mapper.DestinationMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Log4j2
public class DestinationServiceImpl implements DestinationService {
    private static final String NAVER_LOCAL_SEARCH_URL = "https://openapi.naver.com/v1/search/local.json";
    private static final int DISPLAY_COUNT = 5;
    private static final BigDecimal NAVER_COORDINATE_SCALE = BigDecimal.valueOf(10_000_000L);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DestinationMapper destinationMapper;

    @Value("${NAVER_SEARCH_CLIENT_ID:}")
    private String clientId;

    @Value("${NAVER_SEARCH_CLIENT_SECRET:}")
    private String clientSecret;

    @Override
    public List<DestinationDTO> searchDestinations(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }

        validateApiKey();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);

        URI requestUri = UriComponentsBuilder.fromHttpUrl(NAVER_LOCAL_SEARCH_URL)
                .queryParam("query", keyword.trim())
                .queryParam("display", DISPLAY_COUNT)
                .build()
                .encode()
                .toUri();

        ResponseEntity<String> response = restTemplate.exchange(
                requestUri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        return toDestinationList(response.getBody());
    }

    @Override
    @Transactional
    public DestinationDTO saveDestination(DestinationDTO destination) {
        validateDestination(destination);

        DestinationVO destinationVO = destination.toVO();
        destinationMapper.insertDestination(destinationVO);

        destination.setDestinationId(destinationVO.getDestinationId());
        return destination;
    }

    private List<DestinationDTO> toDestinationList(String responseBody) {
        try {
            JsonNode items = objectMapper.readTree(responseBody).path("items");
            List<DestinationDTO> destinations = new ArrayList<>();

            for (JsonNode item : items) {
                String name = item.path("title").asText().replaceAll("<[^>]*>", "");
                String roadAddress = item.path("roadAddress").asText();
                String address = StringUtils.hasText(roadAddress)
                        ? roadAddress
                        : item.path("address").asText();

                destinations.add(DestinationDTO.builder()
                        .destName(name)
                        .destAddress(address)
                        .destLongitude(toCoordinate(item.path("mapx").asText()))
                        .destLatitude(toCoordinate(item.path("mapy").asText()))
                        .build());
            }

            return destinations;
        } catch (Exception e) {
            throw new IllegalStateException("검색 결과를 처리하지 못했습니다.", e);
        }
    }

    private BigDecimal toCoordinate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return new BigDecimal(value).divide(NAVER_COORDINATE_SCALE);
    }

    private void validateApiKey() {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new IllegalStateException(
                    "네이버 검색 API 키가 없습니다. 실행 환경 변수 NAVER_SEARCH_CLIENT_ID와 "
                            + "NAVER_SEARCH_CLIENT_SECRET을 설정하세요."
            );
        }
    }

    private void validateDestination(DestinationDTO destination) {
        if (destination == null
                || destination.getDestLatitude() == null
                || destination.getDestLongitude() == null
                || !StringUtils.hasText(destination.getDestName())) {
            throw new IllegalArgumentException("목적지 정보가 올바르지 않습니다.");
        }
    }
}
