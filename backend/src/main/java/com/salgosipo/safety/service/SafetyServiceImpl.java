package com.salgosipo.safety.service;

import com.salgosipo.safety.client.SafetyRouteClient;
import com.salgosipo.safety.domain.PedestrianRoute;
import com.salgosipo.safety.domain.PropertySafetyVO;
import com.salgosipo.safety.domain.SafetyDestinationVO;
import com.salgosipo.safety.domain.SafetyFacilityVO;
import com.salgosipo.safety.domain.SafetyPropertyCoordinateVO;
import com.salgosipo.safety.dto.RoutePointDTO;
import com.salgosipo.safety.dto.SafetyRouteCandidateDTO;
import com.salgosipo.safety.dto.SafetyRouteRequestDTO;
import com.salgosipo.safety.dto.SafetyRouteResponseDTO;
import com.salgosipo.safety.mapper.SafetyMapper;
import com.salgosipo.safety.repository.SafetyFacilityRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class SafetyServiceImpl implements SafetyService {

    private static final Logger log =
            LogManager.getLogger(SafetyServiceImpl.class);

    private static final double FACILITY_QUERY_MARGIN_METERS = 320.0;

    private final SafetyMapper safetyMapper;
    private final SafetyRouteClient safetyRouteClient;
    private final SafetyFacilityRepository safetyFacilityRepository;
    private final SafetyScoreCalculator safetyScoreCalculator;

    /*
     * Spring이 실제 서비스 Bean을 생성할 때 사용하는 생성자입니다.
     *
     * 생성자가 두 개이므로 @Autowired를 명시하여
     * Spring이 이 생성자를 선택하도록 합니다.
     */
    @Autowired
    public SafetyServiceImpl(
            SafetyMapper safetyMapper,
            @Value("${TMAP_API_KEY:}")
            String tmapApiKey,
            @Value(
                    "${SAFETY_FACILITY_RESOURCE:"
                            + "public_data/safety_facility_normalized.csv}"
            )
            String facilityResource
    ) {
        this(
                safetyMapper,
                new SafetyRouteClient(tmapApiKey),
                new SafetyFacilityRepository(facilityResource),
                new SafetyScoreCalculator()
        );
    }

    /*
     * 단위 테스트에서 가짜 Client와 Repository를 전달하기 위한 생성자입니다.
     *
     * Spring이 아닌 같은 패키지의 테스트 코드에서 사용합니다.
     */
    SafetyServiceImpl(
            SafetyMapper safetyMapper,
            SafetyRouteClient safetyRouteClient,
            SafetyFacilityRepository safetyFacilityRepository,
            SafetyScoreCalculator safetyScoreCalculator
    ) {
        this.safetyMapper = safetyMapper;
        this.safetyRouteClient = safetyRouteClient;
        this.safetyFacilityRepository = safetyFacilityRepository;
        this.safetyScoreCalculator = safetyScoreCalculator;
    }

    @Override
    @Transactional
    public SafetyRouteResponseDTO getOrCalculateSafety(
            SafetyRouteRequestDTO request
    ) {
        validateRequest(request);

        SafetyPropertyCoordinateVO property =
                resolveProperty(request);

        SafetyDestinationVO destination =
                resolveDestination(request);

        PropertySafetyVO cached =
                safetyMapper.selectPropertySafety(
                        request.getPropertyId(),
                        destination.getDestinationId()
                );

        if (cached != null) {
            return createCachedResponse(cached);
        }

        List<PedestrianRoute> routes =
                safetyRouteClient.findCandidateRoutes(
                        property.getLatitude(),
                        property.getLongitude(),
                        defaultName(
                                request.getPropertyName(),
                                property.getAddress()
                        ),
                        destination.getLatitude().doubleValue(),
                        destination.getLongitude().doubleValue(),
                        defaultName(
                                destination.getName(),
                                "선택 목적지"
                        )
                );

        BoundingBox boundingBox =
                calculateBoundingBox(
                        routes,
                        FACILITY_QUERY_MARGIN_METERS
                );

        List<SafetyFacilityVO> facilities =
                safetyFacilityRepository.findInBounds(
                        boundingBox.minLatitude(),
                        boundingBox.maxLatitude(),
                        boundingBox.minLongitude(),
                        boundingBox.maxLongitude()
                );

        List<SafetyRouteCandidateDTO> candidates =
                new ArrayList<>();

        for (PedestrianRoute route : routes) {
            candidates.add(
                    safetyScoreCalculator.calculate(
                            route,
                            facilities
                    )
            );
        }

        SafetyRouteCandidateDTO selectedRoute =
                candidates.stream()
                        .min(
                                Comparator
                                        .comparing(
                                                SafetyRouteCandidateDTO
                                                        ::getSafetyScore,
                                                Comparator.reverseOrder()
                                        )
                                        .thenComparing(
                                                SafetyRouteCandidateDTO
                                                        ::getTotalTimeSeconds
                                        )
                                        .thenComparing(
                                                SafetyRouteCandidateDTO
                                                        ::getDistanceMeters
                                        )
                                        .thenComparing(
                                                candidate ->
                                                        routeOptionRank(
                                                                candidate
                                                                        .getSearchOption()
                                                        )
                                        )
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "평가 가능한 보행자 경로가 없습니다."
                                )
                        );

        selectedRoute.setSelected(true);

        PropertySafetyVO calculated =
                new PropertySafetyVO();

        calculated.setPropertyId(
                request.getPropertyId()
        );

        calculated.setDestinationId(
                destination.getDestinationId()
        );

        calculated.setSafetyScore(
                selectedRoute.getSafetyScore()
        );

        calculated.setCctvCount(
                selectedRoute
                        .getBreakdown()
                        .getCctvCount()
        );

        calculated.setStreetLampCount(
                selectedRoute
                        .getBreakdown()
                        .getStreetLightCount()
        );

        calculated.setHasPoliceStation(
                selectedRoute
                        .getBreakdown()
                        .getHasPoliceStation()
        );

        int insertedRows =
                safetyMapper.insertPropertySafetyIfAbsent(
                        calculated
                );

        /*
         * 동시에 들어온 다른 요청이 먼저 저장했을 수 있으므로
         * 최종 반환값은 DB에서 다시 읽습니다.
         */
        PropertySafetyVO stored =
                safetyMapper.selectPropertySafety(
                        request.getPropertyId(),
                        destination.getDestinationId()
                );

        if (stored == null) {
            throw new IllegalStateException(
                    "안전점수 계산은 완료했지만 "
                            + "property_safety 저장 결과를 읽지 못했습니다."
            );
        }

        SafetyRouteResponseDTO response =
                new SafetyRouteResponseDTO();

        response.setPropertyId(
                stored.getPropertyId()
        );

        response.setDestinationId(
                stored.getDestinationId()
        );

        response.setCacheHit(false);

        response.setPersisted(
                insertedRows > 0
        );

        response.setMessage(
                insertedRows > 0
                        ? "DB에 기존 값이 없어 TMAP 경로를 계산하고 "
                        + "property_safety에 저장했습니다."
                        : "동시 요청이 먼저 저장한 "
                        + "property_safety 값을 반환합니다."
        );

        response.setSafetyScore(
                stored.getSafetyScore()
        );

        response.setSafetyGrade(
                toGrade(stored.getSafetyScore())
        );

        response.setCctvCount(
                stored.getCctvCount()
        );

        response.setStreetLampCount(
                stored.getStreetLampCount()
        );

        response.setHasPoliceStation(
                stored.getHasPoliceStation()
        );

        response.setSelectedRoute(
                selectedRoute
        );

        response.setCandidateRoutes(
                candidates
        );

        return response;
    }

    private SafetyRouteResponseDTO createCachedResponse(
            PropertySafetyVO cached
    ) {
        SafetyRouteResponseDTO response =
                new SafetyRouteResponseDTO();

        response.setPropertyId(
                cached.getPropertyId()
        );

        response.setDestinationId(
                cached.getDestinationId()
        );

        response.setCacheHit(true);
        response.setPersisted(true);

        response.setMessage(
                "property_safety에 저장된 값을 반환했습니다. "
                        + "TMAP API는 호출하지 않았습니다."
        );

        response.setSafetyScore(
                cached.getSafetyScore()
        );

        response.setSafetyGrade(
                toGrade(cached.getSafetyScore())
        );

        response.setCctvCount(
                cached.getCctvCount()
        );

        response.setStreetLampCount(
                cached.getStreetLampCount()
        );

        response.setHasPoliceStation(
                cached.getHasPoliceStation()
        );

        response.setSelectedRoute(null);
        response.setCandidateRoutes(
                Collections.emptyList()
        );

        return response;
    }

    private SafetyPropertyCoordinateVO resolveProperty(
            SafetyRouteRequestDTO request
    ) {
        SafetyPropertyCoordinateVO property =
                safetyMapper.selectPropertyCoordinate(
                        request.getPropertyId()
                );

        if (property == null) {
            throw new IllegalArgumentException(
                    "properties 테이블에 존재하는 "
                            + "propertyId가 필요합니다: "
                            + request.getPropertyId()
            );
        }

        validateCoordinate(
                property.getLatitude(),
                property.getLongitude(),
                "매물"
        );

        return property;
    }

    private SafetyDestinationVO resolveDestination(
            SafetyRouteRequestDTO request
    ) {
        if (request.getDestinationId() != null
                && request.getDestinationId() > 0) {

            SafetyDestinationVO stored =
                    safetyMapper.selectDestinationById(
                            request.getDestinationId()
                    );

            if (stored == null) {
                throw new IllegalArgumentException(
                        "destinations 테이블에 존재하지 않는 "
                                + "destinationId입니다: "
                                + request.getDestinationId()
                );
            }

            validateCoordinate(
                    stored.getLatitude().doubleValue(),
                    stored.getLongitude().doubleValue(),
                    "목적지"
            );

            return stored;
        }

        validateCoordinate(
                request.getDestinationLatitude(),
                request.getDestinationLongitude(),
                "목적지"
        );

        SafetyDestinationVO destination =
                new SafetyDestinationVO();

        destination.setLatitude(
                toDatabaseCoordinate(
                        request.getDestinationLatitude()
                )
        );

        destination.setLongitude(
                toDatabaseCoordinate(
                        request.getDestinationLongitude()
                )
        );

        destination.setName(
                defaultName(
                        request.getDestinationName(),
                        "선택 목적지"
                )
        );

        destination.setAddress(
                request.getDestinationAddress()
        );

        safetyMapper.upsertDestination(
                destination
        );

        if (destination.getDestinationId() == null
                || destination.getDestinationId() <= 0) {
            throw new IllegalStateException(
                    "목적지 ID를 생성하거나 조회하지 못했습니다."
            );
        }

        return destination;
    }

    private BoundingBox calculateBoundingBox(
            List<PedestrianRoute> routes,
            double marginMeters
    ) {
        double minLatitude =
                Double.POSITIVE_INFINITY;

        double maxLatitude =
                Double.NEGATIVE_INFINITY;

        double minLongitude =
                Double.POSITIVE_INFINITY;

        double maxLongitude =
                Double.NEGATIVE_INFINITY;

        for (PedestrianRoute route : routes) {
            for (RoutePointDTO point : route.getRoutePoints()) {
                minLatitude = Math.min(
                        minLatitude,
                        point.getLatitude()
                );

                maxLatitude = Math.max(
                        maxLatitude,
                        point.getLatitude()
                );

                minLongitude = Math.min(
                        minLongitude,
                        point.getLongitude()
                );

                maxLongitude = Math.max(
                        maxLongitude,
                        point.getLongitude()
                );
            }
        }

        if (!Double.isFinite(minLatitude)
                || !Double.isFinite(maxLatitude)
                || !Double.isFinite(minLongitude)
                || !Double.isFinite(maxLongitude)) {

            throw new IllegalStateException(
                    "경로의 bounding box를 계산할 수 없습니다."
            );
        }

        double centerLatitude =
                (minLatitude + maxLatitude) / 2.0;

        double latitudeMargin =
                marginMeters / 111_320.0;

        double longitudeMargin =
                marginMeters
                        / (
                        111_320.0
                                * Math.max(
                                0.2,
                                Math.cos(
                                        Math.toRadians(
                                                centerLatitude
                                        )
                                )
                        )
                );

        return new BoundingBox(
                minLatitude - latitudeMargin,
                maxLatitude + latitudeMargin,
                minLongitude - longitudeMargin,
                maxLongitude + longitudeMargin
        );
    }

    private void validateRequest(
            SafetyRouteRequestDTO request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "요청 본문이 필요합니다."
            );
        }

        if (request.getPropertyId() == null
                || request.getPropertyId() <= 0) {
            throw new IllegalArgumentException(
                    "propertyId가 필요합니다."
            );
        }

        boolean hasDestinationId =
                request.getDestinationId() != null
                        && request.getDestinationId() > 0;

        boolean hasDestinationCoordinate =
                request.getDestinationLatitude() != null
                        && request.getDestinationLongitude() != null;

        if (!hasDestinationId
                && !hasDestinationCoordinate) {
            throw new IllegalArgumentException(
                    "destinationId 또는 목적지 위도·경도가 필요합니다."
            );
        }
    }

    private void validateCoordinate(
            Double latitude,
            Double longitude,
            String label
    ) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException(
                    label + " 위도와 경도가 필요합니다."
            );
        }

        if (latitude < -90.0
                || latitude > 90.0) {
            throw new IllegalArgumentException(
                    label + " 위도 범위가 올바르지 않습니다."
            );
        }

        if (longitude < -180.0
                || longitude > 180.0) {
            throw new IllegalArgumentException(
                    label + " 경도 범위가 올바르지 않습니다."
            );
        }
    }

    private BigDecimal toDatabaseCoordinate(
            double value
    ) {
        return BigDecimal
                .valueOf(value)
                .setScale(
                        8,
                        RoundingMode.HALF_UP
                );
    }

    private int routeOptionRank(
            String searchOption
    ) {
        if ("0".equals(searchOption)) {
            return 0;
        }

        if ("4".equals(searchOption)) {
            return 1;
        }

        if ("10".equals(searchOption)) {
            return 2;
        }

        return 3;
    }

    private String defaultName(
            String value,
            String fallback
    ) {
        return value == null || value.isBlank()
                ? fallback
                : value;
    }

    private String toGrade(
            Integer safetyScore
    ) {
        int score =
                safetyScore == null
                        ? 0
                        : safetyScore;

        if (score >= 80) {
            return "SAFE";
        }

        if (score >= 60) {
            return "WARNING";
        }

        return "DANGER";
    }

    private record BoundingBox(
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude
    ) {
    }
}