package com.salgosipo.global.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salgosipo.global.routing.dto.TransitSummaryDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * [로컬 그래프호퍼(GraphHopper PT) 대중교통 라우팅 전용 클라이언트]
 * - 지하철 / 버스 GTFS 스케줄 기반 이동 시간 및 거리 연산
 * - 출퇴근 시간 계산 및 이소크론(등시선) 필터링에 활용
 */
@Component
public class GraphHopperTransitClient {

    private static final Logger log = LogManager.getLogger(GraphHopperTransitClient.class);
    private static final String DEFAULT_GRAPHHOPPER_URL = "http://localhost:8001";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public GraphHopperTransitClient() {
        this(resolveBaseUrl(null));
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GraphHopperTransitClient(
            @org.springframework.beans.factory.annotation.Value("${routing.graphhopper.url:#{null}}") String configuredUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(7_000);

        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
        this.baseUrl = resolveBaseUrl(configuredUrl);
        log.info("[GraphHopperClient] 활성화된 엔드포인트 URL: {}", this.baseUrl);
    }

    private static String resolveBaseUrl(String inputUrl) {
        if (inputUrl != null && !inputUrl.trim().isEmpty()) {
            return inputUrl.trim();
        }
        String env = System.getenv("ROUTING_GRAPHHOPPER_URL");
        if (env != null && !env.trim().isEmpty()) {
            return env.trim();
        }
        String sysProp = System.getProperty("routing.graphhopper.url");
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            return sysProp.trim();
        }
        return DEFAULT_GRAPHHOPPER_URL;
    }

    /**
     * 출발지 -> 목적지 대중교통 이동 시간 및 거리 계산
     */
    public TransitSummaryDTO findTransitRoute(double startLat, double startLon, double endLat, double endLon) {
        return findTransitRoute(startLat, startLon, endLat, endLon, null);
    }

    /**
     * 출발지 -> 목적지 대중교통 이동 시간 및 거리 계산 (출발 시각 지정 가능)
     */
    public TransitSummaryDTO findTransitRoute(double startLat, double startLon, double endLat, double endLon, String departureTimeIso) {
        try {
            String departure = (departureTimeIso != null && !departureTimeIso.isEmpty())
                    ? departureTimeIso
                    : DateTimeFormatter.ISO_INSTANT.format(Instant.now());

            String url = String.format(
                    "%s/route?point=%.6f,%.6f&point=%.6f,%.6f&profile=pt&pt.earliest_departure_time=%s",
                    baseUrl, startLat, startLon, endLat, endLon, departure
            );

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("[GraphHopper] 대중교통 경로 조회 실패: status={}", response.getStatusCode());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode paths = root.path("paths");
            if (!paths.isArray() || paths.size() == 0) {
                return null;
            }

            JsonNode primaryPath = paths.get(0);
            long totalTimeMs = primaryPath.path("time").asLong(0L);
            double distanceMeters = primaryPath.path("distance").asDouble(0.0);

            double totalTimeMinutes = Math.round((totalTimeMs / 60000.0) * 10.0) / 10.0;
            double totalDistanceKm = Math.round((distanceMeters / 1000.0) * 100.0) / 100.0;

            double accessWalkMin = 0.0;
            double egressWalkMin = 0.0;
            double pureTransitMin = 0.0;
            int transfers = 0;
            String detectedRouteShortName = "";
            String departureLocation = "";
            String arrivalLocation = "";
            String routeColor = "";
            String transitType = "BUS";

            JsonNode legs = primaryPath.path("legs");
            if (legs.isArray() && legs.size() > 0) {
                int totalLegs = legs.size();
                for (int i = 0; i < totalLegs; i++) {
                    JsonNode leg = legs.get(i);
                    String type = leg.path("type").asText("");
                    if ("pt".equalsIgnoreCase(type)) {
                        pureTransitMin += leg.path("time").asDouble(0) > 0 ? (leg.path("time").asDouble(0) / 60000.0) : 1.0;
                        String shortName = leg.path("route_short_name").asText("");
                        String longName = leg.path("route_long_name").asText("");
                        String headsign = leg.path("trip_headsign").asText("");
                        String depLoc = leg.path("departure_location").asText("");
                        String arrLoc = leg.path("arrival_location").asText("");
                        String color = leg.path("route_color").asText("");
                        int rType = leg.path("route_type").asInt(3);

                        if (!shortName.isEmpty()) detectedRouteShortName = shortName;
                        else if (!longName.isEmpty()) detectedRouteShortName = longName;
                        else if (!headsign.isEmpty()) detectedRouteShortName = headsign;

                        if (!depLoc.isEmpty()) departureLocation = depLoc;
                        if (!arrLoc.isEmpty()) arrivalLocation = arrLoc;
                        if (!color.isEmpty()) routeColor = color.startsWith("#") ? color : ("#" + color);

                        if (rType == 1 || rType == 0 || rType == 2 || shortName.contains("호선") || headsign.contains("호선")) {
                            transitType = "SUBWAY";
                        } else {
                            transitType = "BUS";
                        }
                    } else if ("walk".equalsIgnoreCase(type)) {
                        double walkMin = leg.path("time").asDouble(0) / 60000.0;
                        if (i == 0) {
                            accessWalkMin = Math.round(walkMin * 10.0) / 10.0;
                        } else {
                            egressWalkMin = Math.round(walkMin * 10.0) / 10.0;
                        }
                    }
                }
                transfers = Math.max(0, totalLegs - 1);
            }

            pureTransitMin = Math.round(pureTransitMin * 10.0) / 10.0;

            // GTFS 기반 실제 노선 요약 문구 조립
            StringBuilder summaryBuilder = new StringBuilder();
            if (!detectedRouteShortName.isEmpty()) {
                summaryBuilder.append(detectedRouteShortName);
                if (!departureLocation.isEmpty() && !arrivalLocation.isEmpty()) {
                    summaryBuilder.append(" (").append(departureLocation).append(" ➡️ ").append(arrivalLocation).append(")");
                } else if (!arrivalLocation.isEmpty()) {
                    summaryBuilder.append(" (").append(arrivalLocation).append(" 방면)");
                } else if (!departureLocation.isEmpty()) {
                    summaryBuilder.append(" (").append(departureLocation).append(" 탑승)");
                }
            } else {
                summaryBuilder.append(transitType.equals("SUBWAY") ? "수도권 지하철" : "시내버스");
            }

            return TransitSummaryDTO.builder()
                    .totalTimeMinutes(totalTimeMinutes)
                    .totalDistanceKm(totalDistanceKm)
                    .accessWalkMinutes(accessWalkMin)
                    .hopperTransitMinutes(pureTransitMin > 0 ? pureTransitMin : totalTimeMinutes)
                    .egressWalkMinutes(egressWalkMin)
                    .totalWalkTimeMinutes(accessWalkMin + egressWalkMin)
                    .transferCount(transfers)
                    .routeSummary(summaryBuilder.toString())
                    .transitType(transitType)
                    .routeShortName(detectedRouteShortName)
                    .routeColor(routeColor)
                    .departureLocation(departureLocation)
                    .arrivalLocation(arrivalLocation)
                    .build();

        } catch (Exception e) {
            log.error("[GraphHopper] 대중교통 경로 연산 중 예외 발생: {}", e.getMessage(), e);
            return null;
        }
    }
}
