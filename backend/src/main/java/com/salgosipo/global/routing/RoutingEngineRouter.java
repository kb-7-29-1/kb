package com.salgosipo.global.routing;

import com.salgosipo.safety.client.SafetyRouteClient;
import com.salgosipo.safety.domain.PedestrianRoute;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * [참사 방지용 라우팅 엔진 스위처 (Feature Flag & Auto-Fallback)]
 * - DOCKER 모드: 로컬 발할라 도보 엔진으로 1순위 초고속 계산 (무료)
 * - TMAP 모드: 기존 TMAP 상용 API로 계산
 * - 자동 복구(Auto-Fallback): 도커가 꺼져있거나 오류 발생 시 자동으로 TMAP으로 우회하여 무장애 서비스 보장
 */
@Component
public class RoutingEngineRouter {

    private static final Logger log = LogManager.getLogger(RoutingEngineRouter.class);

    private final ValhallaPedestrianClient valhallaClient;
    private final GraphHopperTransitClient graphHopperClient;
    private final SafetyRouteClient tmapClient;
    private final String engineMode;

    @Autowired
    public RoutingEngineRouter(
            ValhallaPedestrianClient valhallaClient,
            GraphHopperTransitClient graphHopperClient,
            @Value("${TMAP_API_KEY:}") String tmapApiKey,
            @Value("${routing.engine.mode:DOCKER}") String engineMode
    ) {
        this.valhallaClient = valhallaClient;
        this.graphHopperClient = graphHopperClient;
        this.tmapClient = new SafetyRouteClient(tmapApiKey);
        this.engineMode = (engineMode != null && !engineMode.trim().isEmpty()) ? engineMode.trim().toUpperCase() : "DOCKER";
        log.info("[RoutingEngineRouter] 현재 활성화된 라우팅 모드: {}", this.engineMode);
    }

    /**
     * [도보 경로 통합 라우팅 - 스위치 & 자동 백업 적용]
     */
    public PedestrianRoute findPedestrianRoute(
            double startLat, double startLon, String startName,
            double endLat, double endLon, String endName
    ) {
        // 1. DOCKER 모드일 때
        if ("DOCKER".equalsIgnoreCase(engineMode)) {
            try {
                PedestrianRoute dockerRoute = valhallaClient.findPedestrianRoute(startLat, startLon, endLat, endLon);
                if (dockerRoute != null && dockerRoute.getRoutePoints() != null && !dockerRoute.getRoutePoints().isEmpty()) {
                    log.debug("[RoutingEngineRouter] 발할라 도보 경로 연산 성공");
                    return dockerRoute;
                }
                log.warn("[RoutingEngineRouter] 발할라 결과 없음. TMAP으로 자동 백업(Fallback) 전환합니다.");
            } catch (Exception e) {
                log.warn("[RoutingEngineRouter] 발할라 호출 실패({}). TMAP으로 자동 백업(Fallback) 전환합니다.", e.getMessage());
            }
        }

        // 2. TMAP 모드이거나 발할라 장애 시 -> 기존 TMAP 호출 (안전 보장)
        return tmapClient.findPreferredRoute(startLat, startLon, startName, endLat, endLon, endName);
    }

    public String getEngineMode() {
        return engineMode;
    }

    public ValhallaPedestrianClient getValhallaClient() {
        return valhallaClient;
    }

    public GraphHopperTransitClient getGraphHopperClient() {
        return graphHopperClient;
    }
}
