package com.salgosipo.global.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salgosipo.safety.domain.PedestrianRoute;
import com.salgosipo.safety.dto.RoutePointDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [로컬 발할라(Valhalla) 도보 라우팅 전용 클라이언트]
 * - 기존 TMAP 보행자 API를 100% 대체 가능
 * - 도보 최단 경로 탐색 (PedestrianRoute 및 RoutePointDTO 호환)
 * - 대량 매물(1:N) 도보 매트릭스 초고속 연산 지원
 */
@Component
public class ValhallaPedestrianClient {

    private static final Logger log = LogManager.getLogger(ValhallaPedestrianClient.class);
    private static final String DEFAULT_VALHALLA_URL = "http://localhost:8000";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public ValhallaPedestrianClient() {
        this(resolveBaseUrl(null));
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ValhallaPedestrianClient(
            @org.springframework.beans.factory.annotation.Value("${routing.valhalla.url:#{null}}") String configuredUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(5_000);

        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
        this.baseUrl = resolveBaseUrl(configuredUrl);
        log.info("[ValhallaClient] 활성화된 엔드포인트 URL: {}", this.baseUrl);
    }

    private static String resolveBaseUrl(String inputUrl) {
        if (inputUrl != null && !inputUrl.trim().isEmpty()) {
            return inputUrl.trim();
        }
        String env = System.getenv("ROUTING_VALHALLA_URL");
        if (env != null && !env.trim().isEmpty()) {
            return env.trim();
        }
        String sysProp = System.getProperty("routing.valhalla.url");
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            return sysProp.trim();
        }
        return DEFAULT_VALHALLA_URL;
    }

    /**
     * 출발지 -> 목적지 도보 경로 계산 (기본 보행 속도 4.5 km/h)
     */
    public PedestrianRoute findPedestrianRoute(double startLat, double startLon, double endLat, double endLon) {
        return findPedestrianRoute(startLat, startLon, endLat, endLon, 4.5);
    }

    /**
     * 프론트엔드의 걸음 속도 옵션(MapQuickFilterBar.vue: SLOW / NORMAL / FAST)을 100% 일치 반영한 도보 경로 계산
     * - SLOW: 3.5 km/h (천천히)
     * - NORMAL: 4.5 km/h (보통 걸음)
     * - FAST: 5.5 km/h (빠른 걸음)
     */
    public PedestrianRoute findPedestrianRouteWithPace(double startLat, double startLon, double endLat, double endLon, String walkPace) {
        double speed = 4.5;
        if ("SLOW".equalsIgnoreCase(walkPace)) {
            speed = 3.5;
        } else if ("FAST".equalsIgnoreCase(walkPace)) {
            speed = 5.5;
        }
        return findPedestrianRoute(startLat, startLon, endLat, endLon, speed);
    }

    /**
     * 출발지 -> 목적지 도보 경로 계산 (사용자 지정 맞춤 보행 속도 km/h 지원)
     */
    public PedestrianRoute findPedestrianRoute(double startLat, double startLon, double endLat, double endLon, Double customWalkingSpeed) {
        try {
            String url = baseUrl + "/route";

            Map<String, Object> body = new HashMap<>();
            List<Map<String, Object>> locations = new ArrayList<>();

            Map<String, Object> startLoc = new HashMap<>();
            startLoc.put("lat", startLat);
            startLoc.put("lon", startLon);
            locations.add(startLoc);

            Map<String, Object> endLoc = new HashMap<>();
            endLoc.put("lat", endLat);
            endLoc.put("lon", endLon);
            locations.add(endLoc);

            body.put("locations", locations);
            body.put("costing", "pedestrian");

            double effectiveSpeed = (customWalkingSpeed != null && customWalkingSpeed > 0) ? customWalkingSpeed : 4.5;

            // 🚶 발할라 보행자 전용 고도화 옵션: 골목길·생활도로·횡단보도·육교 계단 지름길 1순위 관통 (큰 도로 램프 우회 차단)
            Map<String, Object> costingOptions = new HashMap<>();
            Map<String, Object> pedOptions = new HashMap<>();
            pedOptions.put("use_sidewalk", 1.0);           // 인도/보도 우선순위 최대화 (0.0 ~ 1.0)
            pedOptions.put("use_crossing", 1.0);           // 횡단보도 이용 우선순위 최대화 (0.0 ~ 1.0)
            pedOptions.put("use_living_streets", 1.0);     // 주택가 생활도로/골목길 적극 이용
            pedOptions.put("use_tracks", 1.0);             // 보행 전용 소로 이용
            pedOptions.put("walkway_factor", 0.1);         // 보행자 전용도로/샛길 지름길 최우선 가중치
            pedOptions.put("alley_factor", 0.1);           // 골목길 지름길 최우선 가중치
            pedOptions.put("service_factor", 1.0);         // 단지/주택가 내 보행 통로 허용
            pedOptions.put("step_penalty", 0);             // 육교/지하도/골목 계단 감점 제거 (최단거리 관통)
            pedOptions.put("use_hills", 0.5);              // 경사로/계단 지름길 적극 수용
            pedOptions.put("walking_speed", effectiveSpeed); // ⏱️ 프론트 연동 맞춤 보행 속도 (km/h)
            costingOptions.put("pedestrian", pedOptions);
            body.put("costing_options", costingOptions);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("[Valhalla] 도보 경로 조회 응답 실패: code={}", response.getStatusCode());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode summary = root.path("trip").path("summary");
            if (summary.isMissingNode()) {
                return null;
            }

            double distanceKm = summary.path("length").asDouble(0.0);
            int timeSeconds = summary.path("time").asInt(0);

            // Polyline 좌표 디코딩
            List<RoutePointDTO> routePoints = new ArrayList<>();
            JsonNode legs = root.path("trip").path("legs");
            if (legs.isArray() && legs.size() > 0) {
                String shape = legs.get(0).path("shape").asText("");
                if (!shape.isEmpty()) {
                    routePoints = decodePolyline6(shape);
                }
            }

            PedestrianRoute route = new PedestrianRoute();
            route.setRouteId("VALHALLA_PEDESTRIAN");
            route.setSearchOption("보행자 최적 경로");
            route.setRouteType("PEDESTRIAN");
            route.setDistanceMeters((int) Math.round(distanceKm * 1000));
            route.setTotalTimeSeconds(timeSeconds);
            route.setRoutePoints(routePoints);

            return route;

        } catch (Exception e) {
            log.error("[Valhalla] 도보 경로 연산 중 예외 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Valhalla 6자리 정밀도 Polyline 디코더
     */
    private List<RoutePointDTO> decodePolyline6(String encoded) {
        List<RoutePointDTO> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            RoutePointDTO p = new RoutePointDTO(lat / 1e6, lng / 1e6);
            poly.add(p);
        }
        return poly;
    }
}
