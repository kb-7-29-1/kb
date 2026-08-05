package com.salgosipo.safety.client;

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
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SafetyRouteClient {

    private static final Logger log = LogManager.getLogger(SafetyRouteClient.class);

    private static final String PEDESTRIAN_ROUTE_URL =
            "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1";

    /**
     * TMAP 보행자 API는 한 요청에서 탐색 옵션 하나만 받으므로 여러 번 호출합니다.
     * 0(추천), 4(대로 우선), 10(최단)을 우선 사용하고 중복 경로가 있으면
     * 30(계단 제외)을 보조 후보로 사용해 서로 다른 경로를 최대 3개 확보합니다.
     */
    private static final List<RouteOption> ROUTE_OPTIONS = List.of(
            new RouteOption("0", "추천"),
            new RouteOption("4", "대로 우선"),
            new RouteOption("10", "최단"),
            new RouteOption("30", "계단 제외")
    );

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String tmapApiKey;

    public SafetyRouteClient(String tmapApiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(12_000);

        this.restTemplate = new RestTemplate(requestFactory);
        this.objectMapper = new ObjectMapper();
        this.tmapApiKey = tmapApiKey;
    }

    SafetyRouteClient(String tmapApiKey, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.tmapApiKey = tmapApiKey;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<PedestrianRoute> findCandidateRoutes(
            double startLatitude,
            double startLongitude,
            String startName,
            double endLatitude,
            double endLongitude,
            String endName
    ) {
        if (tmapApiKey == null || tmapApiKey.isBlank()) {
            throw new IllegalStateException("TMAP_API_KEY가 설정되어 있지 않습니다.");
        }

        Map<String, PedestrianRoute> uniqueRoutes = new LinkedHashMap<>();
        for (RouteOption option : ROUTE_OPTIONS) {
            if (uniqueRoutes.size() >= 3) {
                break;
            }

            PedestrianRoute route = requestRoute(
                    startLatitude,
                    startLongitude,
                    startName,
                    endLatitude,
                    endLongitude,
                    endName,
                    option
            );
            if (route == null || route.getRoutePoints() == null || route.getRoutePoints().size() < 2) {
                continue;
            }
            uniqueRoutes.putIfAbsent(createGeometryFingerprint(route.getRoutePoints()), route);
        }

        if (uniqueRoutes.isEmpty()) {
            throw new IllegalStateException("TMAP에서 보행자 경로를 찾지 못했습니다.");
        }
        return new ArrayList<>(uniqueRoutes.values());
    }

    private PedestrianRoute requestRoute(
            double startLatitude,
            double startLongitude,
            String startName,
            double endLatitude,
            double endLongitude,
            String endName,
            RouteOption option
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("appKey", tmapApiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("startX", startLongitude);
        body.put("startY", startLatitude);
        body.put("endX", endLongitude);
        body.put("endY", endLatitude);
        body.put("startName", encodeName(defaultName(startName, "출발지")));
        body.put("endName", encodeName(defaultName(endName, "도착지")));
        body.put("reqCoordType", "WGS84GEO");
        body.put("resCoordType", "WGS84GEO");
        body.put("searchOption", option.code());
        body.put("sort", "index");

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    PEDESTRIAN_ROUTE_URL,
                    new HttpEntity<>(body, headers),
                    String.class
            );
            return parseRoute(response.getBody(), option);
        } catch (Exception e) {
            log.warn("TMAP 보행자 경로 호출 실패. option={}, message={}", option.code(), e.getMessage());
            return null;
        }
    }

    PedestrianRoute parseRoute(String responseBody, RouteOption option) throws Exception {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode features = root.path("features");
        if (!features.isArray() || features.size() == 0) {
            return null;
        }

        Integer totalDistance = null;
        Integer totalTime = null;
        List<RoutePointDTO> routePoints = new ArrayList<>();

        for (JsonNode feature : features) {
            JsonNode properties = feature.path("properties");
            if ((totalDistance == null || totalDistance <= 0) && properties.has("totalDistance")) {
                totalDistance = properties.path("totalDistance").asInt();
            }
            if ((totalTime == null || totalTime <= 0) && properties.has("totalTime")) {
                totalTime = properties.path("totalTime").asInt();
            }

            JsonNode geometry = feature.path("geometry");
            String geometryType = geometry.path("type").asText();
            JsonNode coordinates = geometry.path("coordinates");

            if ("LineString".equals(geometryType)) {
                appendLineString(routePoints, coordinates);
            } else if ("MultiLineString".equals(geometryType) && coordinates.isArray()) {
                for (JsonNode line : coordinates) {
                    appendLineString(routePoints, line);
                }
            }
        }

        if (routePoints.size() < 2) {
            return null;
        }
        if (totalDistance == null || totalDistance <= 0) {
            totalDistance = (int) Math.round(calculatePolylineLength(routePoints));
        }
        if (totalTime == null || totalTime <= 0) {
            totalTime = Math.max(1, (int) Math.round(totalDistance / 1.3));
        }

        PedestrianRoute route = new PedestrianRoute();
        route.setRouteId("TMAP-" + option.code());
        route.setSearchOption(option.code());
        route.setRouteType(option.label());
        route.setDistanceMeters(totalDistance);
        route.setTotalTimeSeconds(totalTime);
        route.setRoutePoints(routePoints);
        return route;
    }

    private void appendLineString(List<RoutePointDTO> routePoints, JsonNode coordinates) {
        if (!coordinates.isArray()) {
            return;
        }
        for (JsonNode coordinate : coordinates) {
            if (!coordinate.isArray() || coordinate.size() < 2) {
                continue;
            }
            RoutePointDTO point = new RoutePointDTO(
                    coordinate.get(1).asDouble(),
                    coordinate.get(0).asDouble()
            );
            if (routePoints.isEmpty() || !samePoint(routePoints.get(routePoints.size() - 1), point)) {
                routePoints.add(point);
            }
        }
    }

    private boolean samePoint(RoutePointDTO first, RoutePointDTO second) {
        return Math.abs(first.getLatitude() - second.getLatitude()) < 1e-8
                && Math.abs(first.getLongitude() - second.getLongitude()) < 1e-8;
    }

    private String createGeometryFingerprint(List<RoutePointDTO> points) {
        StringBuilder builder = new StringBuilder();
        int sampleCount = Math.min(24, points.size());
        for (int i = 0; i < sampleCount; i++) {
            int index = sampleCount == 1
                    ? 0
                    : (int) Math.round(i * (points.size() - 1.0) / (sampleCount - 1.0));
            RoutePointDTO point = points.get(index);
            builder.append(String.format(
                    Locale.ROOT,
                    "%.5f,%.5f|",
                    point.getLatitude(),
                    point.getLongitude()
            ));
        }
        return builder.toString();
    }

    private double calculatePolylineLength(List<RoutePointDTO> points) {
        double total = 0.0;
        for (int i = 1; i < points.size(); i++) {
            total += haversineMeters(points.get(i - 1), points.get(i));
        }
        return total;
    }

    private double haversineMeters(RoutePointDTO first, RoutePointDTO second) {
        double earthRadius = 6_371_000.0;
        double lat1 = Math.toRadians(first.getLatitude());
        double lat2 = Math.toRadians(second.getLatitude());
        double deltaLat = lat2 - lat1;
        double deltaLng = Math.toRadians(second.getLongitude() - first.getLongitude());
        double value = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
    }

    private String defaultName(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String encodeName(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static record RouteOption(String code, String label) {
    }
}
