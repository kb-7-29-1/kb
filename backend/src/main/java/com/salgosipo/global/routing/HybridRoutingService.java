package com.salgosipo.global.routing;

import com.salgosipo.global.routing.dto.HybridRouteResultDTO;
import com.salgosipo.global.routing.dto.TransitSummaryDTO;
import com.salgosipo.safety.domain.PedestrianRoute;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

/**
 * [발할라(도보) + 호퍼(대중교통) 통합 하이브리드 라우팅 서비스]
 * - 기존 코드를 일절 수정하지 않고 독립적으로 주입하여 사용할 수 있는 모듈러 서비스
 * - DB 수동 저장 없이 메모리 상에서 실시간으로 도보 및 대중교통 이동시간 연산
 * - 이소크론(등시선) 최소 시간(minMinutes) ~ 최대 시간(maxMinutes) 범위 필터링 지원
 */
@Service
public class HybridRoutingService {

    private static final Logger log = LogManager.getLogger(HybridRoutingService.class);

    private final ValhallaPedestrianClient valhallaClient;
    // private final GraphHopperTransitClient graphHopperClient; // [호퍼 레거시 보존]
    private final MotisTransitClient motisTransitClient;

    public HybridRoutingService(ValhallaPedestrianClient valhallaClient,
                                MotisTransitClient motisTransitClient) {
        this.valhallaClient = valhallaClient;
        // this.graphHopperClient = graphHopperClient;
        this.motisTransitClient = motisTransitClient;
    }

    /**
     * 특정 출발지(매물) -> 목적지(직장/역)에 대해 도보 및 대중교통 경로를 한 번에 계산
     */
    public HybridRouteResultDTO calculateHybrid(double startLat, double startLon, double destLat, double destLon) {
        // 1. 발할라 도보 계산 (0.001초)
        PedestrianRoute walkRoute = valhallaClient.findPedestrianRoute(startLat, startLon, destLat, destLon);
        Double walkTimeMin = null;
        Double walkDistKm = null;

        if (walkRoute != null && walkRoute.getTotalTimeSeconds() != null) {
            walkTimeMin = Math.round((walkRoute.getTotalTimeSeconds() / 60.0) * 10.0) / 10.0;
            walkDistKm = Math.round((walkRoute.getDistanceMeters() / 1000.0) * 100.0) / 100.0;
        }

        // 2. 모티스(MOTIS) 초고속 C++ 대중교통 계산 (0.002초)
        // TransitSummaryDTO transitSummary = graphHopperClient.findTransitRoute(startLat, startLon, destLat, destLon); // [호퍼 레거시 주석 보존]
        TransitSummaryDTO transitSummary = motisTransitClient.findTransitRoute(startLat, startLon, destLat, destLon);

        return HybridRouteResultDTO.builder()
                .walkTimeMinutes(walkTimeMin)
                .walkDistanceKm(walkDistKm)
                .walkDetail(walkRoute)
                .transitTimeMinutes(transitSummary != null ? transitSummary.getTotalTimeMinutes() : null)
                .transitDistanceKm(transitSummary != null ? transitSummary.getTotalDistanceKm() : null)
                .transitDetail(transitSummary)
                .build();
    }

    /**
     * [이소크론 시간 범위 판정]
     * 대중교통 이동 시간이 사용자가 설정한 최소시간(minMinutes) ~ 최대시간(maxMinutes) 범위 내에 들어오는지 검증
     * - 예: 20분 이상 40분 이하 통근 권역 매물 필터링
     */
    public boolean isWithinTransitTimeRange(double startLat, double startLon,
                                           double destLat, double destLon,
                                           Double minMinutes, Double maxMinutes) {
        // 모티스(MOTIS) 대중교통 시간 범위 조회
        TransitSummaryDTO transit = motisTransitClient.findTransitRoute(startLat, startLon, destLat, destLon);
        if (transit == null || transit.getTotalTimeMinutes() == null) {
            return false;
        }

        double time = transit.getTotalTimeMinutes();

        if (minMinutes != null && time < minMinutes) {
            return false;
        }
        if (maxMinutes != null && time > maxMinutes) {
            return false;
        }

        return true;
    }

    /**
     * 도보 전용 발할라 클라이언트 접근자
     */
    public ValhallaPedestrianClient getValhallaClient() {
        return valhallaClient;
    }

    /**
     * 대중교통 전용 모티스 클라이언트 접근자
     */
    public MotisTransitClient getMotisTransitClient() {
        return motisTransitClient;
    }
}
