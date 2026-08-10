package com.salgosipo.safety.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salgosipo.safety.domain.PedestrianRoute;
import com.salgosipo.safety.domain.SafetyDestinationVO;
import com.salgosipo.safety.domain.SafetyFacilityVO;
import com.salgosipo.safety.domain.TestLineStringVO;
import com.salgosipo.safety.dto.RoutePointDTO;
import com.salgosipo.safety.dto.SafetyRouteCandidateDTO;
import com.salgosipo.safety.dto.TestFacilityMapResultDTO;
import com.salgosipo.safety.dto.TestLineStringDetailDTO;
import com.salgosipo.safety.dto.TestLineStringPageDTO;
import com.salgosipo.safety.mapper.SafetyMapper;
import com.salgosipo.safety.mapper.TestLineStringMapper;
import com.salgosipo.safety.repository.SafetyFacilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * [임시/테스트 전용] test_line_string에 저장된 경로를 지도 시각화용으로
 * 다시 채점(SafetyScoreCalculator)하고, 목적지 주변 CCTV/가로등/파출소 원본
 * 좌표를 조회하는 읽기 전용 서비스입니다. TMAP은 절대 호출하지 않습니다.
 *
 * 검증이 끝나면 SafetyDebugMapPage.vue와 함께 삭제해도 됩니다.
 */
@Service
public class TestLineStringDebugService {

    private static final double FACILITY_QUERY_MARGIN_METERS = 1_850.0;

    private final TestLineStringMapper testLineStringMapper;
    private final SafetyMapper safetyMapper;
    private final SafetyFacilityRepository safetyFacilityRepository;
    private final SafetyScoreCalculator safetyScoreCalculator = new SafetyScoreCalculator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public TestLineStringDebugService(
            TestLineStringMapper testLineStringMapper,
            SafetyMapper safetyMapper,
            @Value(
                    "${SAFETY_FACILITY_RESOURCE:"
                            + "public_data/safety_facility_normalized.csv}"
            ) String facilityResource
    ) {
        this.testLineStringMapper = testLineStringMapper;
        this.safetyMapper = safetyMapper;
        this.safetyFacilityRepository = new SafetyFacilityRepository(facilityResource);
    }

    public TestLineStringPageDTO getLineStringPage(Integer destinationId, int offset, int limit) {
        int totalCount = testLineStringMapper.countTestLineString(destinationId);
        List<TestLineStringVO> rows =
                testLineStringMapper.selectTestLineStringPage(destinationId, offset, limit);

        List<TestLineStringDetailDTO> items = new ArrayList<>();
        for (TestLineStringVO row : rows) {
            items.add(toDetail(row));
        }

        TestLineStringPageDTO page = new TestLineStringPageDTO();
        page.setTotalCount(totalCount);
        page.setOffset(offset);
        page.setLimit(limit);
        page.setItems(items);
        return page;
    }

    public TestFacilityMapResultDTO getFacilities(Integer destinationId, Double marginMeters) {
        SafetyDestinationVO destination = safetyMapper.selectDestinationById(destinationId);
        if (destination == null) {
            throw new IllegalArgumentException("존재하지 않는 destinationId입니다: " + destinationId);
        }

        double destLat = destination.getLatitude().doubleValue();
        double destLng = destination.getLongitude().doubleValue();
        double margin = marginMeters != null ? marginMeters : FACILITY_QUERY_MARGIN_METERS;
        double latMargin = margin / 111_320.0;
        double lngMargin = margin / (111_320.0 * Math.max(0.2, Math.cos(Math.toRadians(destLat))));

        List<SafetyFacilityVO> facilities = safetyFacilityRepository.findInBounds(
                destLat - latMargin,
                destLat + latMargin,
                destLng - lngMargin,
                destLng + lngMargin
        );

        TestFacilityMapResultDTO result = new TestFacilityMapResultDTO();
        result.setDestinationLatitude(destLat);
        result.setDestinationLongitude(destLng);
        result.setDestinationName(destination.getName());
        result.setFacilities(facilities);
        return result;
    }

    private TestLineStringDetailDTO toDetail(TestLineStringVO row) {
        List<RoutePointDTO> routePoints = deserializeRoutePoints(row.getLineStringJson());

        PedestrianRoute route = new PedestrianRoute();
        route.setRoutePoints(routePoints);

        BoundingBox box = calculateBoundingBox(routePoints);
        List<SafetyFacilityVO> facilities = safetyFacilityRepository.findInBounds(
                box.minLatitude(),
                box.maxLatitude(),
                box.minLongitude(),
                box.maxLongitude()
        );

        SafetyRouteCandidateDTO candidate = safetyScoreCalculator.calculate(route, facilities);

        TestLineStringDetailDTO detail = new TestLineStringDetailDTO();
        detail.setPropertyId(row.getPropertyId());
        detail.setDestinationId(row.getDestinationId());
        detail.setRoutePoints(routePoints);
        detail.setSafetyScore(candidate.getSafetyScore());
        detail.setSafetyGrade(candidate.getSafetyGrade());
        detail.setBreakdown(candidate.getBreakdown());
        return detail;
    }

    private List<RoutePointDTO> deserializeRoutePoints(String routePointsJson) {
        try {
            return objectMapper.readValue(routePointsJson, new TypeReference<List<RoutePointDTO>>() { });
        } catch (Exception exception) {
            throw new IllegalStateException("저장된 경로 좌표를 읽지 못했습니다.", exception);
        }
    }

    private BoundingBox calculateBoundingBox(List<RoutePointDTO> routePoints) {
        double minLatitude = Double.POSITIVE_INFINITY;
        double maxLatitude = Double.NEGATIVE_INFINITY;
        double minLongitude = Double.POSITIVE_INFINITY;
        double maxLongitude = Double.NEGATIVE_INFINITY;

        for (RoutePointDTO point : routePoints) {
            minLatitude = Math.min(minLatitude, point.getLatitude());
            maxLatitude = Math.max(maxLatitude, point.getLatitude());
            minLongitude = Math.min(minLongitude, point.getLongitude());
            maxLongitude = Math.max(maxLongitude, point.getLongitude());
        }

        double centerLatitude = (minLatitude + maxLatitude) / 2.0;
        double latitudeMargin = FACILITY_QUERY_MARGIN_METERS / 111_320.0;
        double longitudeMargin = FACILITY_QUERY_MARGIN_METERS
                / (111_320.0 * Math.max(0.2, Math.cos(Math.toRadians(centerLatitude))));

        return new BoundingBox(
                minLatitude - latitudeMargin,
                maxLatitude + latitudeMargin,
                minLongitude - longitudeMargin,
                maxLongitude + longitudeMargin
        );
    }

    private record BoundingBox(
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude
    ) {
    }
}