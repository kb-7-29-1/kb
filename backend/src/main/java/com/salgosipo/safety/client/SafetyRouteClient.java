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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SafetyRouteClient {

    private static final Logger log = LogManager.getLogger(SafetyRouteClient.class);

    private static final String PEDESTRIAN_ROUTE_URL =
            "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1";

    /**
     * 모든 매물에 동일한 기준을 적용하기 위해 TMAP의 대로 우선 경로만 사용합니다.
     * 기존처럼 여러 옵션을 호출해 후보를 비교하지 않으므로 캐시 미스 한 건당
     * TMAP 호출도 정확히 한 번만 발생합니다.
     */
    static final RouteOption PREFERRED_ROUTE_OPTION =
            new RouteOption("4", "대로 우선");

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

    public PedestrianRoute findPreferredRoute(
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

        PedestrianRoute route = requestRoute(
                startLatitude,
                startLongitude,
                startName,
                endLatitude,
                endLongitude,
                endName,
                PREFERRED_ROUTE_OPTION
        );

        if (route == null
                || route.getRoutePoints() == null
                || route.getRoutePoints().size() < 2) {
            throw new IllegalStateException(
                    "TMAP에서 대로 우선 보행자 경로를 찾지 못했습니다."
            );
        }

        return route;
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
        } catch (HttpStatusCodeException exception) {
            String responseBody = exception.getResponseBodyAsString();
            String message = "TMAP 보행자 경로 호출 실패 (HTTP "
                    + exception.getRawStatusCode() + ")";
            if (responseBody != null && !responseBody.isBlank()) {
                message += ": " + abbreviate(responseBody, 300);
            }
            log.warn("{}", message);
            throw new IllegalStateException(message, exception);
        } catch (Exception exception) {
            String message = "TMAP 보행자 경로 호출 실패: "
                    + defaultName(exception.getMessage(), "알 수 없는 오류");
            log.warn("{}", message);
            throw new IllegalStateException(message, exception);
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
            if ((totalDistance == null || totalDistance <= 0)
                    && properties.has("totalDistance")) {
                totalDistance = properties.path("totalDistance").asInt();
            }
            if ((totalTime == null || totalTime <= 0)
                    && properties.has("totalTime")) {
                totalTime = properties.path("totalTime").asInt();
            }

            JsonNode geometry = feature.path("geometry");
            String geometryType = geometry.path("type").asText();
            JsonNode coordinates = geometry.path("coordinates");

            if ("LineString".equals(geometryType)) {
                appendLineString(routePoints, coordinates);
            } else if ("MultiLineString".equals(geometryType)
                    && coordinates.isArray()) {
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
            if (routePoints.isEmpty()
                    || !samePoint(routePoints.get(routePoints.size() - 1), point)) {
                routePoints.add(point);
            }
        }
    }

    private boolean samePoint(RoutePointDTO first, RoutePointDTO second) {
        return Math.abs(first.getLatitude() - second.getLatitude()) < 1e-8
                && Math.abs(first.getLongitude() - second.getLongitude()) < 1e-8;
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

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    static record RouteOption(String code, String label) {
    }
}
