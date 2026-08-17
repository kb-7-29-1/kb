package com.salgosipo.global.routing.controller;

import com.salgosipo.global.routing.HybridRoutingService;
import com.salgosipo.global.routing.dto.HybridRouteResultDTO;
import com.salgosipo.global.routing.dto.TransitSummaryDTO;
import com.salgosipo.safety.domain.PedestrianRoute;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * [도커 라우팅 엔진(발할라 + 호퍼) 독립 테스트 컨트롤러]
 * - 기존 코드에 영향 없이 독립적으로 도커 연산 결과를 검증
 * - 브라우저나 Postman에서 바로 호출 가능
 */
@RestController
@RequestMapping("/api/routing")
public class HybridRoutingController {

    private final HybridRoutingService hybridRoutingService;
    private final com.salgosipo.global.routing.RoutingEngineRouter routingEngineRouter;

    public HybridRoutingController(
            HybridRoutingService hybridRoutingService,
            com.salgosipo.global.routing.RoutingEngineRouter routingEngineRouter) {
        this.hybridRoutingService = hybridRoutingService;
        this.routingEngineRouter = routingEngineRouter;
    }

    /**
     * 1. [하이브리드 동시 연산 테스트]
     * 예: /api/routing/hybrid?startLat=37.5575&startLon=126.9244&destLat=37.5258&destLon=126.9284
     */
    @GetMapping("/hybrid")
    public ResponseEntity<HybridRouteResultDTO> testHybrid(
            @RequestParam double startLat,
            @RequestParam double startLon,
            @RequestParam double destLat,
            @RequestParam double destLon) {
        HybridRouteResultDTO result = hybridRoutingService.calculateHybrid(startLat, startLon, destLat, destLon);
        return ResponseEntity.ok(result);
    }

    /**
     * 2. [발할라 전용 도보 경로 테스트] (기존 TMAP 완벽 호환 데이터 구조 + 맞춤 보행 속도/페이스 지원)
     * 예: /api/routing/valhalla/walk?startLat=37.5575&startLon=126.9244&destLat=37.5258&destLon=126.9284&walkPace=FAST
     */
    @GetMapping("/valhalla/walk")
    public ResponseEntity<PedestrianRoute> testValhallaWalk(
            @RequestParam double startLat,
            @RequestParam double startLon,
            @RequestParam double destLat,
            @RequestParam double destLon,
            @RequestParam(required = false) String walkPace,
            @RequestParam(required = false) Double walkingSpeed) {
        PedestrianRoute route;
        if (walkingSpeed != null && walkingSpeed > 0) {
            route = hybridRoutingService.getValhallaClient()
                    .findPedestrianRoute(startLat, startLon, destLat, destLon, walkingSpeed);
        } else if (walkPace != null && !walkPace.trim().isEmpty()) {
            route = hybridRoutingService.getValhallaClient()
                    .findPedestrianRouteWithPace(startLat, startLon, destLat, destLon, walkPace);
        } else {
            route = hybridRoutingService.getValhallaClient()
                    .findPedestrianRoute(startLat, startLon, destLat, destLon);
        }
        return ResponseEntity.ok(route);
    }

    /**
     * 3. [호퍼 전용 대중교통 이동시간 테스트]
     * 예: /api/routing/hopper/transit?startLat=37.5575&startLon=126.9244&destLat=37.5258&destLon=126.9284
     */
    @GetMapping("/hopper/transit")
    public ResponseEntity<TransitSummaryDTO> testHopperTransit(
            @RequestParam double startLat,
            @RequestParam double startLon,
            @RequestParam double destLat,
            @RequestParam double destLon) {
        TransitSummaryDTO transit = hybridRoutingService.getGraphHopperClient()
                .findTransitRoute(startLat, startLon, destLat, destLon);
        return ResponseEntity.ok(transit);
    }

    /**
     * 4. [이소크론 시간 범위(min~max) 판정 테스트]
     * 예: /api/routing/isochrone/check?startLat=37.5575&startLon=126.9244&destLat=37.5258&destLon=126.9284&minMinutes=10&maxMinutes=35
     */
    @GetMapping("/isochrone/check")
    public ResponseEntity<Map<String, Object>> testIsochroneCheck(
            @RequestParam double startLat,
            @RequestParam double startLon,
            @RequestParam double destLat,
            @RequestParam double destLon,
            @RequestParam(required = false) Double minMinutes,
            @RequestParam(required = false) Double maxMinutes) {
        boolean withinRange = hybridRoutingService.isWithinTransitTimeRange(
                startLat, startLon, destLat, destLon, minMinutes, maxMinutes);

        Map<String, Object> response = new HashMap<>();
        response.put("withinRange", withinRange);
        response.put("minMinutes", minMinutes);
        response.put("maxMinutes", maxMinutes);
        return ResponseEntity.ok(response);
    }

    /**
     * 5. [발할라 vs TMAP 1:1 비교 및 스위처 테스트]
     * 예: /api/routing/test/compare?startLat=37.5575&startLon=126.9244&destLat=37.5258&destLon=126.9284
     */
    @GetMapping("/test/compare")
    public ResponseEntity<Map<String, Object>> compareEngines(
            @RequestParam double startLat,
            @RequestParam double startLon,
            @RequestParam double destLat,
            @RequestParam double destLon) {

        // 1. 발할라 도보 연산
        long vStart = System.currentTimeMillis();
        PedestrianRoute valhallaRoute = hybridRoutingService.getValhallaClient()
                .findPedestrianRoute(startLat, startLon, destLat, destLon);
        long vElapsed = System.currentTimeMillis() - vStart;

        // 2. 스위처 자동 라우팅 결과
        PedestrianRoute routedResult = routingEngineRouter.findPedestrianRoute(
                startLat, startLon, "출발지", destLat, destLon, "도착지");

        Map<String, Object> response = new HashMap<>();
        response.put("currentEngineMode", routingEngineRouter.getEngineMode());
        response.put("valhallaResponseTimeMs", vElapsed);
        response.put("valhallaRoute", valhallaRoute);
        response.put("routedResult", routedResult);

        return ResponseEntity.ok(response);
    }
}
