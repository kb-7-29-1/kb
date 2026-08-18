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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * [모티스(MOTIS) 초고속 C++ 대중교통 라우팅 클라이언트]
 * - C++ 기반 메모리 맵(mmap)으로 자바 JVM 힙 OOM 없이 0.001초 단위로 대중교통 최적 경로 탐색
 * - 서울 지하철 1~9호선, 경의중앙선, 신분당선 및 서울 버스 환승 완벽 지원
 */
@Component
public class MotisTransitClient {

    private static final Logger log = LogManager.getLogger(MotisTransitClient.class);
    private static final String DEFAULT_MOTIS_URL = "http://localhost:8001";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public MotisTransitClient() {
        this(resolveBaseUrl(null));
    }

    @org.springframework.beans.factory.annotation.Autowired
    public MotisTransitClient(
            @org.springframework.beans.factory.annotation.Value("${routing.motis.url:#{null}}") String configuredUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
        this.baseUrl = resolveBaseUrl(configuredUrl);
        log.info("[MotisTransitClient] 활성화된 엔드포인트 URL: {}", this.baseUrl);
    }

    private static String resolveBaseUrl(String inputUrl) {
        if (inputUrl != null && !inputUrl.trim().isEmpty()) {
            return inputUrl.trim();
        }
        String env = System.getenv("ROUTING_MOTIS_URL");
        if (env != null && !env.trim().isEmpty()) {
            return env.trim();
        }
        String sysProp = System.getProperty("routing.motis.url");
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            return sysProp.trim();
        }
        return DEFAULT_MOTIS_URL;
    }

    public TransitSummaryDTO findTransitRoute(double startLat, double startLon, double endLat, double endLon) {
        return findTransitRoute(startLat, startLon, endLat, endLon, null);
    }

    public TransitSummaryDTO findTransitRoute(double startLat, double startLon, double endLat, double endLon,
            String departureTimeIso) {
        try {
            String departure = (departureTimeIso != null && !departureTimeIso.isEmpty())
                    ? departureTimeIso
                    : "2026-08-11T08:00:00+09:00"; // 기본 2026년 GTFS 스케줄 기준 평일 오전 8시

            String urlStr = String.format(
                    "%s/api/v1/plan?fromPlace=%.6f,%.6f&toPlace=%.6f,%.6f&time=%s",
                    baseUrl, startLat, startLon, endLat, endLon, URLEncoder.encode(departure, StandardCharsets.UTF_8));
            java.net.URI uri = java.net.URI.create(urlStr);

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("[MOTIS] 대중교통 경로 조회 실패: status={}", response.getStatusCode());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode itineraries = root.path("itineraries");
            if (!itineraries.isArray() || itineraries.size() == 0) {
                return null;
            }

            JsonNode primaryItinerary = itineraries.get(0);
            double totalDurationSec = primaryItinerary.path("duration").asDouble(0.0);
            double totalTimeMinutes = Math.round((totalDurationSec / 60.0) * 10.0) / 10.0;

            double accessWalkMin = 0.0;
            double egressWalkMin = 0.0;
            double pureTransitMin = 0.0;
            double totalDistanceMeters = 0.0;
            int transitLegCount = 0;

            List<String> transitNames = new ArrayList<>();
            List<com.salgosipo.global.routing.dto.TransitLegDTO> legDTOs = new ArrayList<>();
            String firstDepartureStation = "";
            String lastArrivalStation = "";
            String mainTransitType = "BUS";
            String primaryRouteName = "";
            String routeColor = "";

            JsonNode legs = primaryItinerary.path("legs");
            if (legs.isArray() && legs.size() > 0) {
                int totalLegs = legs.size();
                for (int i = 0; i < totalLegs; i++) {
                    JsonNode leg = legs.get(i);
                    String mode = leg.path("mode").asText("").toUpperCase();
                    double legDurationMin = leg.path("duration").asDouble(0.0) / 60.0;
                    double legDistM = leg.path("distance").asDouble(0.0);
                    totalDistanceMeters += legDistM;

                    String routeShortName = leg.path("routeShortName").asText("");
                    String fromName = leg.path("from").path("name").asText("");
                    String toName = leg.path("to").path("name").asText("");

                    String legColor = "";
                    if ("SUBWAY".equals(mode) || "RAIL".equals(mode) || routeShortName.contains("호선")) {
                        legColor = resolveSubwayColor(routeShortName);
                    } else if ("BUS".equals(mode) || "TRAM".equals(mode)) {
                        legColor = "#2563EB";
                    } else {
                        legColor = "#10B981"; // 도보 녹색
                    }

                    // 🚇 실제 지하철 선로 및 버스 도로 GPS 좌표 폴리라인 디코딩
                    String polylineStr = leg.path("legGeometry").path("points").asText("");
                    int precision = leg.path("legGeometry").path("precision").asInt(7);
                    List<com.salgosipo.global.routing.dto.TransitPointDTO> points = decodePolyline(polylineStr,
                            precision);

                    if (points.isEmpty()) {
                        double fromLat = leg.path("from").path("lat").asDouble(0.0);
                        double fromLon = leg.path("from").path("lon").asDouble(0.0);
                        double toLat = leg.path("to").path("lat").asDouble(0.0);
                        double toLon = leg.path("to").path("lon").asDouble(0.0);
                        if (fromLat != 0.0 && toLat != 0.0) {
                            points.add(new com.salgosipo.global.routing.dto.TransitPointDTO(fromLat, fromLon));
                            points.add(new com.salgosipo.global.routing.dto.TransitPointDTO(toLat, toLon));
                        }
                    }

                    legDTOs.add(com.salgosipo.global.routing.dto.TransitLegDTO.builder()
                            .mode(mode)
                            .routeName(routeShortName)
                            .routeColor(legColor)
                            .durationMinutes(Math.round(legDurationMin * 10.0) / 10.0)
                            .distanceMeters(legDistM)
                            .fromName(fromName)
                            .toName(toName)
                            .routePoints(points)
                            .build());

                    if ("WALK".equals(mode)) {
                        if (i == 0) {
                            accessWalkMin += legDurationMin;
                        } else if (i == totalLegs - 1) {
                            egressWalkMin += legDurationMin;
                        }
                    } else {
                        // 대중교통 탑승 구간 (SUBWAY, BUS, TRAM, RAIL)
                        transitLegCount++;
                        pureTransitMin += legDurationMin;

                        if (firstDepartureStation.isEmpty() && !fromName.isEmpty()) {
                            firstDepartureStation = fromName;
                        }
                        if (!toName.isEmpty()) {
                            lastArrivalStation = toName;
                        }

                        if ("SUBWAY".equals(mode) || "RAIL".equals(mode) || routeShortName.contains("호선")) {
                            mainTransitType = "SUBWAY";
                            if (routeColor.isEmpty()) {
                                routeColor = resolveSubwayColor(routeShortName);
                            }
                        } else {
                            if (routeColor.isEmpty()) {
                                routeColor = "#2563EB"; // 간선버스 파란색
                            }
                        }

                        if (!routeShortName.isEmpty()) {
                            transitNames.add(routeShortName);
                            if (primaryRouteName.isEmpty()) {
                                primaryRouteName = routeShortName;
                            }
                        }
                    }
                }
            }

            int transfers = Math.max(0, transitLegCount - 1);
            double totalDistanceKm = Math.round((totalDistanceMeters / 1000.0) * 100.0) / 100.0;
            accessWalkMin = Math.round(accessWalkMin * 10.0) / 10.0;
            egressWalkMin = Math.round(egressWalkMin * 10.0) / 10.0;
            pureTransitMin = Math.round(pureTransitMin * 10.0) / 10.0;

            // 요약 문구 조립 (예: 2호선 ➡️ 422번 (서울대입구역 ➡️ 건국대학교))
            StringBuilder summaryBuilder = new StringBuilder();
            if (!transitNames.isEmpty()) {
                summaryBuilder.append(String.join(" ➡️ ", transitNames));
                if (!firstDepartureStation.isEmpty() && !lastArrivalStation.isEmpty()) {
                    summaryBuilder.append(" (").append(firstDepartureStation).append(" ➡️ ").append(lastArrivalStation)
                            .append(")");
                }
            } else {
                summaryBuilder.append(mainTransitType.equals("SUBWAY") ? "수도권 지하철" : "버스");
            }

            return TransitSummaryDTO.builder()
                    .totalTimeMinutes(totalTimeMinutes)
                    .totalDistanceKm(totalDistanceKm)
                    .accessWalkMinutes(accessWalkMin)
                    .hopperTransitMinutes(pureTransitMin > 0 ? pureTransitMin : totalTimeMinutes)
                    .egressWalkMinutes(egressWalkMin)
                    .totalWalkTimeMinutes(Math.round((accessWalkMin + egressWalkMin) * 10.0) / 10.0)
                    .transferCount(transfers)
                    .routeSummary(summaryBuilder.toString())
                    .transitType(mainTransitType)
                    .routeShortName(primaryRouteName)
                    .routeColor(routeColor)
                    .departureLocation(firstDepartureStation)
                    .arrivalLocation(lastArrivalStation)
                    .legs(legDTOs)
                    .build();

        } catch (Exception e) {
            log.error("[MOTIS] 대중교통 경로 연산 중 예외 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Google Encoded Polyline (Precision 7/5) 디코더 -> 실제 위경도 GPS 좌표 목록 변환
     */
    public static List<com.salgosipo.global.routing.dto.TransitPointDTO> decodePolyline(String polylineStr, int precision) {
        List<com.salgosipo.global.routing.dto.TransitPointDTO> track = new ArrayList<>();
        if (polylineStr == null || polylineStr.isEmpty()) {
            return track;
        }
        int index = 0, len = polylineStr.length();
        long lat = 0, lng = 0;
        double factor = Math.pow(10, precision > 0 ? precision : 7);

        while (index < len) {
            long result = 0;
            int shift = 0;
            int b;
            do {
                if (index >= len) break;
                b = polylineStr.charAt(index++) - 63;
                result |= ((long) (b & 0x1f)) << shift;
                shift += 5;
            } while (b >= 0x20);
            long dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            result = 0;
            shift = 0;
            do {
                if (index >= len) break;
                b = polylineStr.charAt(index++) - 63;
                result |= ((long) (b & 0x1f)) << shift;
                shift += 5;
            } while (b >= 0x20);
            long dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            track.add(new com.salgosipo.global.routing.dto.TransitPointDTO(
                    Math.round((lat / factor) * 1000000.0) / 1000000.0,
                    Math.round((lng / factor) * 1000000.0) / 1000000.0));
        }
        return track;
    }

    private String resolveSubwayColor(String lineName) {
        if (lineName == null)
            return "#3CB44A";
        if (lineName.contains("1"))
            return "#0052A4";
        if (lineName.contains("2"))
            return "#3CB44A";
        if (lineName.contains("3"))
            return "#EF7C1C";
        if (lineName.contains("4"))
            return "#00A5DE";
        if (lineName.contains("5"))
            return "#996CAC";
        if (lineName.contains("6"))
            return "#CD7C2F";
        if (lineName.contains("7"))
            return "#747F00";
        if (lineName.contains("8"))
            return "#EA545D";
        if (lineName.contains("9"))
            return "#BDB092";
        if (lineName.contains("신분당"))
            return "#D4003B";
        if (lineName.contains("수원") || lineName.contains("수인") || lineName.contains("분당"))
            return "#F5A200";
        if (lineName.contains("경의") || lineName.contains("중앙"))
            return "#77C4A3";
        if (lineName.contains("공항"))
            return "#0090D2";
        return "#3CB44A"; // 기본 2호선 초록색
    }
}
