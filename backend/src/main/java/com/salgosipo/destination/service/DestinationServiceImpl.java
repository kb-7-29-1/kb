package com.salgosipo.destination.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salgosipo.destination.domain.DestinationVO;
import com.salgosipo.destination.dto.DestinationDTO;
import com.salgosipo.destination.mapper.DestinationMapper;
import com.salgosipo.property.service.PublicDataApiService;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
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
    private static final double EARTH_RADIUS_METERS = 6_371_000D;
    private static final double DUPLICATE_DISTANCE_METERS = 30D;
    private static final double METERS_PER_LATITUDE_DEGREE = 111_320D;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DestinationMapper destinationMapper;
    private final PublicDataApiService publicDataApiService;

    @Value("${NAVER_SEARCH_CLIENT_ID:}")
    private String clientId;

    @Value("${NAVER_SEARCH_CLIENT_SECRET:}")
    private String clientSecret;

    @Override
    public List<DestinationDTO> searchDestinations(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }

        List<DestinationDTO> resultList = new ArrayList<>();
        String cleanKeyword = keyword.trim();

        // 0. DB (destinations 테이블) 0순위 최우선 조회
        try {
            List<DestinationVO> dbResults = destinationMapper.searchByNameOrAddress(cleanKeyword);
            if (dbResults != null && !dbResults.isEmpty()) {
                for (DestinationVO vo : dbResults) {
                    resultList.add(DestinationDTO.fromVO(vo));
                }
            }
        } catch (Exception e) {
            log.warn("DB destination search failed: {}", e.getMessage());
        }

        // 1. 네이버 장소(POI) 검색 API 호출
        try {
            validateApiKey();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", clientId);
            headers.set("X-Naver-Client-Secret", clientSecret);

            URI requestUri = UriComponentsBuilder.fromHttpUrl(NAVER_LOCAL_SEARCH_URL)
                    .queryParam("query", cleanKeyword)
                    .queryParam("display", DISPLAY_COUNT)
                    .build()
                    .encode()
                    .toUri();

            ResponseEntity<String> response = restTemplate.exchange(
                    requestUri,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);

            resultList.addAll(toDestinationList(response.getBody()));
        } catch (Exception e) {
            log.warn("Naver local POI search failed: {}", e.getMessage());
        }

        // 2. 지번/도로명 주소 검색 (NCP Geocoding API 연동) - 결합 처리
        try {
            double[] coords = publicDataApiService.getRealCoordinatesFromAddress(cleanKeyword);
            if (coords != null && coords[0] != 0 && coords[1] != 0) {
                boolean duplicateExists = resultList.stream()
                        .anyMatch(d -> cleanKeyword.equals(d.getDestName()) || cleanKeyword.equals(d.getDestAddress()));

                if (!duplicateExists) {
                    DestinationDTO addressDTO = DestinationDTO.builder()
                            .destName(cleanKeyword)
                            .destAddress(cleanKeyword)
                            .destLatitude(BigDecimal.valueOf(coords[0]))
                            .destLongitude(BigDecimal.valueOf(coords[1]))
                            .build();

                    // 주소 검색 결과를 최상단에 결합 삽입
                    resultList.add(0, addressDTO);
                }
            }
        } catch (Exception e) {
            log.warn("NCP Geocoding address search failed: {}", e.getMessage());
        }

        return resultList;
    }

    @Override
    @Transactional
    public DestinationDTO saveDestination(DestinationDTO destination) {
        validateDestination(destination);

        normalizeDestination(destination);

        DestinationVO existingByAddress = findByNameAndAddress(destination);
        if (existingByAddress != null) {
            log.info("[Destination] reuse by name and address: id={}, name={}",
                    existingByAddress.getDestinationId(), existingByAddress.getDestName());
            return DestinationDTO.fromVO(existingByAddress);
        }

        DestinationVO existingNearby = findNearbyDestination(destination);
        if (existingNearby != null) {
            log.info("[Destination] reuse within {}m: id={}, name={}",
                    DUPLICATE_DISTANCE_METERS,
                    existingNearby.getDestinationId(),
                    existingNearby.getDestName());
            return DestinationDTO.fromVO(existingNearby);
        }

        // 같은 이름이 이미 존재하면 기존 행을 재사용
        DestinationVO existingByName = destinationMapper.findByName(destination.getDestName());
        if (existingByName != null) {
            log.info("[Destination] reuse by unique name: {} (id={})",
                    destination.getDestName(), existingByName.getDestinationId());
            return DestinationDTO.fromVO(existingByName);
        }

        DestinationVO destinationVO = destination.toVO();
        try {
            destinationMapper.insertDestination(destinationVO);
        } catch (DuplicateKeyException e) {
            // 동시에 같은 목적지를 저장한 경우, 이미 생성된 목적지를 반환
            DestinationVO duplicatedDestination = destinationMapper.findByName(destination.getDestName());
            if (duplicatedDestination != null) {
                log.info("[Destination] reuse after concurrent save: id={}, name={}",
                        duplicatedDestination.getDestinationId(), duplicatedDestination.getDestName());
                return DestinationDTO.fromVO(duplicatedDestination);
            }
            throw e;
        }

        log.info("[Destination] inserted new destination: id={}, name={}",
                destinationVO.getDestinationId(), destinationVO.getDestName());
        return DestinationDTO.fromVO(destinationVO);
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
                            + "NAVER_SEARCH_CLIENT_SECRET을 설정하세요.");
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

    private DestinationVO findByNameAndAddress(DestinationDTO destination) {
        if (!StringUtils.hasText(destination.getDestAddress())) {
            return null;
        }

        return destinationMapper.findByNameAndAddress(
                destination.getDestName(), destination.getDestAddress());
    }

    private DestinationVO findNearbyDestination(DestinationDTO destination) {
        double latitude = destination.getDestLatitude().doubleValue();
        double longitude = destination.getDestLongitude().doubleValue();
        double latitudeDelta = DUPLICATE_DISTANCE_METERS / METERS_PER_LATITUDE_DEGREE;
        double longitudeDelta = DUPLICATE_DISTANCE_METERS
                / (METERS_PER_LATITUDE_DEGREE * Math.cos(Math.toRadians(latitude)));

        List<DestinationVO> candidates = destinationMapper.findByNameInCoordinateRange(
                destination.getDestName(),
                BigDecimal.valueOf(latitude - latitudeDelta),
                BigDecimal.valueOf(latitude + latitudeDelta),
                BigDecimal.valueOf(longitude - longitudeDelta),
                BigDecimal.valueOf(longitude + longitudeDelta));

        return candidates.stream()
                .filter(candidate -> distanceInMeters(destination, candidate) <= DUPLICATE_DISTANCE_METERS)
                .min(Comparator.comparingDouble(candidate -> distanceInMeters(destination, candidate)))
                .orElse(null);
    }

    private double distanceInMeters(DestinationDTO source, DestinationVO target) {
        double latitudeDifference = Math.toRadians(target.getDestLatitude().doubleValue()
                - source.getDestLatitude().doubleValue());
        double longitudeDifference = Math.toRadians(target.getDestLongitude().doubleValue()
                - source.getDestLongitude().doubleValue());
        double sourceLatitude = Math.toRadians(source.getDestLatitude().doubleValue());
        double targetLatitude = Math.toRadians(target.getDestLatitude().doubleValue());

        double haversine = Math.sin(latitudeDifference / 2) * Math.sin(latitudeDifference / 2)
                + Math.cos(sourceLatitude) * Math.cos(targetLatitude)
                * Math.sin(longitudeDifference / 2) * Math.sin(longitudeDifference / 2);
        return 2 * EARTH_RADIUS_METERS * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

    private void normalizeDestination(DestinationDTO destination) {
        destination.setDestName(normalizeText(destination.getDestName()));
        destination.setDestAddress(normalizeText(destination.getDestAddress()));
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim().replaceAll("\\s+", " ");
    }
}
