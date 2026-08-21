package com.salgosipo.safety.service;

import com.salgosipo.safety.domain.PedestrianRoute;
import com.salgosipo.safety.domain.SafetyFacilityVO;
import com.salgosipo.safety.dto.RoutePointDTO;
import com.salgosipo.safety.dto.SafetyRouteCandidateDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SafetyScoreCalculatorTest {

    private final SafetyScoreCalculator calculator = new SafetyScoreCalculator();

    @Test
    void cctvDensityPenaltyIncreasesContinuously() {
        assertEquals(0, calculator.calculateCctvDensityPenalty(50.0));
        assertEquals(5, calculator.calculateCctvDensityPenalty(75.0));
        assertEquals(10, calculator.calculateCctvDensityPenalty(100.0));
        assertEquals(15, calculator.calculateCctvDensityPenalty(125.0));
        assertEquals(20, calculator.calculateCctvDensityPenalty(150.0));
    }

    @Test
    void cctvCoveragePenaltyChangesProportionally() {
        assertEquals(0, calculator.calculateCctvCoveragePenalty(80.0));
        assertEquals(4, calculator.calculateCctvCoveragePenalty(60.0));
        assertEquals(8, calculator.calculateCctvCoveragePenalty(40.0));
        assertEquals(11, calculator.calculateCctvCoveragePenalty(20.0));
        assertEquals(15, calculator.calculateCctvCoveragePenalty(0.0));
    }

    @Test
    void streetLightCoveragePenaltyChangesProportionally() {
        assertEquals(0, calculator.calculateStreetLightCoveragePenalty(80.0));
        assertEquals(14, calculator.calculateStreetLightCoveragePenalty(60.0));
        assertEquals(28, calculator.calculateStreetLightCoveragePenalty(40.0));
        assertEquals(41, calculator.calculateStreetLightCoveragePenalty(20.0));
        assertEquals(55, calculator.calculateStreetLightCoveragePenalty(0.0));
    }

    @Test
    void facilityRadiiUseUpdatedFeedbackValues() {
        assertEquals(20.0, SafetyScoreCalculator.STREET_LIGHT_ROUTE_RADIUS_METERS);
        assertEquals(500.0, SafetyScoreCalculator.POLICE_ROUTE_RADIUS_METERS);
    }

    @Test
    void noFacilitiesUsesNewPenaltyFormula() {
        PedestrianRoute route = new PedestrianRoute();
        route.setRouteId("TEST");
        route.setSearchOption("4");
        route.setRouteType("대로 우선");
        route.setDistanceMeters(1000);
        route.setTotalTimeSeconds(800);
        route.setRoutePoints(List.of(
                new RoutePointDTO(37.5500, 127.0700),
                new RoutePointDTO(37.5510, 127.0710)
        ));

        SafetyRouteCandidateDTO result = calculator.calculate(route, List.of());

        assertEquals(55, result.getBreakdown().getStreetLightCoveragePenalty());
        assertEquals(10, result.getBreakdown().getPoliceStationPenalty());
        assertEquals(100, result.getBreakdown().getTotalPenalty());
        assertEquals(67, result.getSafetyScore());
    }

    @Test
    void cctvCoverageUsesActualCoveredRouteLengthInsteadOfSectionMidpoints() {
        // 약 200m 직선 경로에서 CCTV를 시작점으로부터 약 40m 지점에 둡니다.
        // CCTV 반경 50m가 실제로 덮는 경로는 시작점~약 90m이므로 커버리지는 약 45%입니다.
        // 기존 50m 구간 중점 방식이면 25m/75m 두 구간이 covered가 되어 50%가 나왔습니다.
        double startLatitude = 37.5500;
        double longitude = 127.0700;

        PedestrianRoute route = new PedestrianRoute();
        route.setRouteId("EXACT-COVERAGE");
        route.setSearchOption("4");
        route.setRouteType("대로 우선");
        route.setDistanceMeters(200);
        route.setTotalTimeSeconds(160);
        route.setRoutePoints(List.of(
                new RoutePointDTO(startLatitude, longitude),
                new RoutePointDTO(startLatitude + metersToLatitude(200.0), longitude)
        ));

        SafetyFacilityVO cctv = new SafetyFacilityVO();
        cctv.setFacilityType("CCTV");
        cctv.setFacilityName("TEST CCTV");
        cctv.setLatitude(startLatitude + metersToLatitude(40.0));
        cctv.setLongitude(longitude);
        cctv.setFacilityCount(1);

        SafetyRouteCandidateDTO result = calculator.calculate(route, List.of(cctv));

        assertEquals(45.0, result.getBreakdown().getCctvCoveragePercent(), 0.2);
    }

    private double metersToLatitude(double meters) {
        return meters / 111_320.0;
    }
}