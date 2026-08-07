package com.salgosipo.safety.service;

import com.salgosipo.safety.client.SafetyRouteClient;
import com.salgosipo.safety.domain.PedestrianRoute;
import com.salgosipo.safety.domain.PropertySafetyVO;
import com.salgosipo.safety.domain.SafetyDestinationVO;
import com.salgosipo.safety.domain.SafetyFacilityVO;
import com.salgosipo.safety.domain.SafetyPropertyCoordinateVO;
import com.salgosipo.safety.dto.RoutePointDTO;
import com.salgosipo.safety.dto.SafetyBatchItemDTO;
import com.salgosipo.safety.dto.SafetyBatchRequestDTO;
import com.salgosipo.safety.dto.SafetyBatchResponseDTO;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SafetyServiceImpl implements SafetyService {

    private static final Logger log =
            LogManager.getLogger(SafetyServiceImpl.class);

    private static final double FACILITY_QUERY_MARGIN_METERS = 320.0;

    private final SafetyMapper safetyMapper;
    private final SafetyRouteClient safetyRouteClient;
    private final SafetyFacilityRepository safetyFacilityRepository;
    private final SafetyScoreCalculator safetyScoreCalculator;

    @Autowired
    public SafetyServiceImpl(
            SafetyMapper safetyMapper,
            @Value("${TMAP_API_KEY:}") String tmapApiKey,
            @Value(
                    "${SAFETY_FACILITY_RESOURCE:"
                            + "public_data/safety_facility_normalized.csv}"
            ) String facilityResource
    ) {
        this(
                safetyMapper,
                new SafetyRouteClient(tmapApiKey),
                new SafetyFacilityRepository(facilityResource),
                new SafetyScoreCalculator()
        );
    }

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

        SafetyPropertyCoordinateVO property = resolveProperty(
                request.getPropertyId()
        );
        SafetyDestinationVO destination = resolveDestination(
                request.getDestinationId(),
                request.getDestinationName(),
                request.getDestinationAddress(),
                request.getDestinationLatitude(),
                request.getDestinationLongitude()
        );

        PropertySafetyVO cached = safetyMapper.selectPropertySafety(
                request.getPropertyId(),
                destination.getDestinationId()
        );

        if (cached != null) {
            return createCachedResponse(cached);
        }

        CalculationResult calculation = calculateAndPersist(
                property,
                destination,
                defaultName(request.getPropertyName(), property.getAddress())
        );

        return createCalculatedResponse(calculation);
    }

    /**
     * 외부 TMAP 호출 전체를 하나의 DB 트랜잭션으로 묶지 않습니다.
     * 매물이 많아도 DB 커넥션을 장시간 점유하지 않고, 계산이 끝난 조합부터
     * insertPropertySafetyIfAbsent를 통해 개별 캐시로 확정합니다.
     */
    @Override
    public SafetyRouteResponseDTO calculateSafetyDetails(SafetyRouteRequestDTO request) {
        validateRequest(request);

        SafetyPropertyCoordinateVO property = resolveProperty(request.getPropertyId());
        SafetyDestinationVO destination = new SafetyDestinationVO();
        destination.setDestinationId(request.getDestinationId());
        destination.setLatitude(BigDecimal.valueOf(request.getDestinationLatitude()));
        destination.setLongitude(BigDecimal.valueOf(request.getDestinationLongitude()));
        destination.setName(request.getDestinationName());
        destination.setAddress(request.getDestinationAddress());

        PedestrianRoute route = safetyRouteClient.findPreferredRoute(
                property.getLatitude(),
                property.getLongitude(),
                defaultName(request.getPropertyName(), property.getAddress()),
                destination.getLatitude().doubleValue(),
                destination.getLongitude().doubleValue(),
                defaultName(destination.getName(), "Selected destination")
        );
        BoundingBox boundingBox = calculateBoundingBox(route);
        SafetyRouteCandidateDTO selectedRoute = safetyScoreCalculator.calculate(
                route,
                safetyFacilityRepository.findInBounds(
                        boundingBox.minLatitude(),
                        boundingBox.maxLatitude(),
                        boundingBox.minLongitude(),
                        boundingBox.maxLongitude()
                )
        );
        selectedRoute.setSelected(true);

        SafetyRouteResponseDTO response = new SafetyRouteResponseDTO();
        response.setPropertyId(property.getPropertyId());
        response.setDestinationId(destination.getDestinationId());
        response.setCacheHit(false);
        response.setPersisted(false);
        response.setMessage("Safety details calculated without persistence.");
        response.setSafetyScore(selectedRoute.getSafetyScore());
        response.setSafetyGrade(selectedRoute.getSafetyGrade());
        response.setCctvCount(selectedRoute.getBreakdown().getCctvCount());
        response.setStreetLampCount(selectedRoute.getBreakdown().getStreetLightCount());
        response.setHasPoliceStation(selectedRoute.getBreakdown().getHasPoliceStation());
        response.setSelectedRoute(selectedRoute);
        response.setCandidateRoutes(List.of(selectedRoute));
        return response;
    }

    @Override
    public SafetyBatchResponseDTO getOrCalculateSafetyBatch(
            SafetyBatchRequestDTO request
    ) {
        validateBatchRequest(request);

        SafetyDestinationVO destination = resolveDestination(
                request.getDestinationId(),
                request.getDestinationName(),
                request.getDestinationAddress(),
                request.getDestinationLatitude(),
                request.getDestinationLongitude()
        );

        Set<Long> uniquePropertyIds = request.getPropertyIds().stream()
                .filter(propertyId -> propertyId != null && propertyId > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (uniquePropertyIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "유효한 propertyId가 한 개 이상 필요합니다."
            );
        }

        List<Long> propertyIds = new ArrayList<>(uniquePropertyIds);
        if (propertyIds.size() > 50) {
            log.warn(
                    "안전점수 배치 요청이 큽니다. destinationId={}, count={}",
                    destination.getDestinationId(),
                    propertyIds.size()
            );
        }

        Map<Long, PropertySafetyVO> cachedByPropertyId =
                safetyMapper.selectPropertySafetyBatch(
                                propertyIds,
                                destination.getDestinationId()
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                PropertySafetyVO::getPropertyId,
                                Function.identity()
                        ));

        List<Long> missingPropertyIds = propertyIds.stream()
                .filter(propertyId -> !cachedByPropertyId.containsKey(propertyId))
                .toList();

        Map<Long, SafetyPropertyCoordinateVO> propertyById =
                missingPropertyIds.isEmpty()
                        ? Collections.emptyMap()
                        : safetyMapper.selectPropertyCoordinates(missingPropertyIds)
                        .stream()
                        .collect(Collectors.toMap(
                                SafetyPropertyCoordinateVO::getPropertyId,
                                Function.identity()
                        ));

        List<SafetyBatchItemDTO> items = new ArrayList<>();
        int cacheHitCount = 0;
        int calculatedCount = 0;
        int failedCount = 0;

        for (Long propertyId : propertyIds) {
            PropertySafetyVO cached = cachedByPropertyId.get(propertyId);
            if (cached != null) {
                items.add(createBatchItem(
                        cached,
                        "CACHED",
                        true,
                        true,
                        "DB에 저장된 안전점수를 반환했습니다."
                ));
                cacheHitCount++;
                continue;
            }

            SafetyPropertyCoordinateVO property = propertyById.get(propertyId);
            if (property == null) {
                items.add(createFailedBatchItem(
                        propertyId,
                        destination.getDestinationId(),
                        "존재하지 않거나 삭제된 매물입니다."
                ));
                failedCount++;
                continue;
            }

            /*
            // [원래 TMAP 계산 로직 100% 보존 - 실행 안 되게 주석 처리만 적용]
            try {
                CalculationResult calculation = calculateAndPersist(
                        property,
                        destination,
                        property.getAddress()
                );
                items.add(createBatchItem(
                        calculation.stored(),
                        "CALCULATED",
                        false,
                        calculation.insertedRows() > 0,
                        calculation.insertedRows() > 0
                                ? "대로 우선 경로를 계산해 DB에 저장했습니다."
                                : "동시 요청이 먼저 저장한 DB 값을 반환했습니다."
                ));
                calculatedCount++;
            } catch (RuntimeException exception) {
                log.warn(
                        "매물 안전점수 배치 계산 실패. propertyId={}, destinationId={}, message={}",
                        propertyId,
                        destination.getDestinationId(),
                        exception.getMessage()
                );
                items.add(createFailedBatchItem(
                        propertyId,
                        destination.getDestinationId(),
                        exception.getMessage()
                ));
                failedCount++;
            }
            */
            items.add(createFailedBatchItem(
                    propertyId,
                    destination.getDestinationId(),
                    "안전점수 계산 실행 중단 (임시 주석 처리)"
            ));
            failedCount++;
        }

        SafetyBatchResponseDTO response = new SafetyBatchResponseDTO();
        response.setDestinationId(destination.getDestinationId());
        response.setRequestedCount(propertyIds.size());
        response.setCacheHitCount(cacheHitCount);
        response.setCalculatedCount(calculatedCount);
        response.setFailedCount(failedCount);
        response.setSuccessCount(cacheHitCount + calculatedCount);
        response.setItems(items);
        return response;
    }

    private CalculationResult calculateAndPersist(
            SafetyPropertyCoordinateVO property,
            SafetyDestinationVO destination,
            String propertyName
    ) {
        validateCoordinate(
                property.getLatitude(),
                property.getLongitude(),
                "매물"
        );

        PedestrianRoute route = safetyRouteClient.findPreferredRoute(
                property.getLatitude(),
                property.getLongitude(),
                defaultName(propertyName, property.getAddress()),
                destination.getLatitude().doubleValue(),
                destination.getLongitude().doubleValue(),
                defaultName(destination.getName(), "선택 목적지")
        );

        BoundingBox boundingBox = calculateBoundingBox(route);
        List<SafetyFacilityVO> facilities =
                safetyFacilityRepository.findInBounds(
                        boundingBox.minLatitude(),
                        boundingBox.maxLatitude(),
                        boundingBox.minLongitude(),
                        boundingBox.maxLongitude()
                );

        SafetyRouteCandidateDTO selectedRoute =
                safetyScoreCalculator.calculate(route, facilities);
        selectedRoute.setSelected(true);

        PropertySafetyVO calculated = new PropertySafetyVO();
        calculated.setPropertyId(property.getPropertyId());
        calculated.setDestinationId(destination.getDestinationId());
        calculated.setSafetyScore(selectedRoute.getSafetyScore());
        calculated.setCctvCount(
                selectedRoute.getBreakdown().getCctvCount()
        );
        calculated.setStreetLampCount(
                selectedRoute.getBreakdown().getStreetLightCount()
        );
        calculated.setHasPoliceStation(
                selectedRoute.getBreakdown().getHasPoliceStation()
        );

        int insertedRows = safetyMapper.insertPropertySafetyIfAbsent(calculated);
        PropertySafetyVO stored = safetyMapper.selectPropertySafety(
                property.getPropertyId(),
                destination.getDestinationId()
        );

        if (stored == null) {
            throw new IllegalStateException(
                    "안전점수 계산은 완료했지만 property_safety 저장 결과를 읽지 못했습니다."
            );
        }

        return new CalculationResult(stored, insertedRows, selectedRoute);
    }

    private SafetyRouteResponseDTO createCalculatedResponse(
            CalculationResult calculation
    ) {
        PropertySafetyVO stored = calculation.stored();
        SafetyRouteResponseDTO response = new SafetyRouteResponseDTO();
        response.setPropertyId(stored.getPropertyId());
        response.setDestinationId(stored.getDestinationId());
        response.setCacheHit(false);
        response.setPersisted(calculation.insertedRows() > 0);
        response.setMessage(
                calculation.insertedRows() > 0
                        ? "DB에 기존 값이 없어 TMAP 대로 우선 경로 한 건을 계산하고 "
                        + "property_safety에 저장했습니다."
                        : "동시 요청이 먼저 저장한 property_safety 값을 반환합니다."
        );
        applySummary(response, stored);
        response.setSelectedRoute(calculation.selectedRoute());
        response.setCandidateRoutes(List.of(calculation.selectedRoute()));
        return response;
    }

    private SafetyRouteResponseDTO createCachedResponse(
            PropertySafetyVO cached
    ) {
        SafetyRouteResponseDTO response = new SafetyRouteResponseDTO();
        response.setPropertyId(cached.getPropertyId());
        response.setDestinationId(cached.getDestinationId());
        response.setCacheHit(true);
        response.setPersisted(true);
        response.setMessage(
                "property_safety에 저장된 값을 반환했습니다. "
                        + "TMAP API는 호출하지 않았습니다."
        );
        applySummary(response, cached);
        response.setSelectedRoute(null);
        response.setCandidateRoutes(Collections.emptyList());
        return response;
    }

    private void applySummary(
            SafetyRouteResponseDTO response,
            PropertySafetyVO propertySafety
    ) {
        response.setSafetyScore(propertySafety.getSafetyScore());
        response.setSafetyGrade(toGrade(propertySafety.getSafetyScore()));
        response.setCctvCount(propertySafety.getCctvCount());
        response.setStreetLampCount(propertySafety.getStreetLampCount());
        response.setHasPoliceStation(propertySafety.getHasPoliceStation());
    }

    private SafetyBatchItemDTO createBatchItem(
            PropertySafetyVO propertySafety,
            String status,
            boolean cacheHit,
            boolean persisted,
            String message
    ) {
        SafetyBatchItemDTO item = new SafetyBatchItemDTO();
        item.setPropertyId(propertySafety.getPropertyId());
        item.setDestinationId(propertySafety.getDestinationId());
        item.setStatus(status);
        item.setCacheHit(cacheHit);
        item.setPersisted(persisted);
        item.setMessage(message);
        item.setSafetyScore(propertySafety.getSafetyScore());
        item.setSafetyGrade(toGrade(propertySafety.getSafetyScore()));
        item.setCctvCount(propertySafety.getCctvCount());
        item.setStreetLampCount(propertySafety.getStreetLampCount());
        item.setHasPoliceStation(propertySafety.getHasPoliceStation());
        return item;
    }

    private SafetyBatchItemDTO createFailedBatchItem(
            Long propertyId,
            Integer destinationId,
            String message
    ) {
        SafetyBatchItemDTO item = new SafetyBatchItemDTO();
        item.setPropertyId(propertyId);
        item.setDestinationId(destinationId);
        item.setStatus("FAILED");
        item.setCacheHit(false);
        item.setPersisted(false);
        item.setMessage(defaultName(message, "안전점수를 계산하지 못했습니다."));
        return item;
    }

    private SafetyPropertyCoordinateVO resolveProperty(Long propertyId) {
        SafetyPropertyCoordinateVO property =
                safetyMapper.selectPropertyCoordinate(propertyId);

        if (property == null) {
            throw new IllegalArgumentException(
                    "properties 테이블에 존재하는 propertyId가 필요합니다: "
                            + propertyId
            );
        }
        return property;
    }

    private SafetyDestinationVO resolveDestination(
            Integer destinationId,
            String destinationName,
            String destinationAddress,
            Double destinationLatitude,
            Double destinationLongitude
    ) {
        if (destinationId != null && destinationId > 0) {
            SafetyDestinationVO stored =
                    safetyMapper.selectDestinationById(destinationId);

            if (stored == null) {
                throw new IllegalArgumentException(
                        "destinations 테이블에 존재하지 않는 destinationId입니다: "
                                + destinationId
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
                destinationLatitude,
                destinationLongitude,
                "목적지"
        );

        SafetyDestinationVO destination = new SafetyDestinationVO();
        destination.setLatitude(toDatabaseCoordinate(destinationLatitude));
        destination.setLongitude(toDatabaseCoordinate(destinationLongitude));
        destination.setName(defaultName(destinationName, "선택 목적지"));
        destination.setAddress(destinationAddress);
        safetyMapper.upsertDestination(destination);

        if (destination.getDestinationId() == null
                || destination.getDestinationId() <= 0) {
            throw new IllegalStateException(
                    "목적지 ID를 생성하거나 조회하지 못했습니다."
            );
        }
        return destination;
    }

    private BoundingBox calculateBoundingBox(PedestrianRoute route) {
        double minLatitude = Double.POSITIVE_INFINITY;
        double maxLatitude = Double.NEGATIVE_INFINITY;
        double minLongitude = Double.POSITIVE_INFINITY;
        double maxLongitude = Double.NEGATIVE_INFINITY;

        for (RoutePointDTO point : route.getRoutePoints()) {
            minLatitude = Math.min(minLatitude, point.getLatitude());
            maxLatitude = Math.max(maxLatitude, point.getLatitude());
            minLongitude = Math.min(minLongitude, point.getLongitude());
            maxLongitude = Math.max(maxLongitude, point.getLongitude());
        }

        if (!Double.isFinite(minLatitude)
                || !Double.isFinite(maxLatitude)
                || !Double.isFinite(minLongitude)
                || !Double.isFinite(maxLongitude)) {
            throw new IllegalStateException(
                    "경로의 bounding box를 계산할 수 없습니다."
            );
        }

        double centerLatitude = (minLatitude + maxLatitude) / 2.0;
        double latitudeMargin = FACILITY_QUERY_MARGIN_METERS / 111_320.0;
        double longitudeMargin = FACILITY_QUERY_MARGIN_METERS
                / (
                111_320.0
                        * Math.max(
                        0.2,
                        Math.cos(Math.toRadians(centerLatitude))
                )
        );

        return new BoundingBox(
                minLatitude - latitudeMargin,
                maxLatitude + latitudeMargin,
                minLongitude - longitudeMargin,
                maxLongitude + longitudeMargin
        );
    }

    private void validateRequest(SafetyRouteRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("요청 본문이 필요합니다.");
        }
        if (request.getPropertyId() == null || request.getPropertyId() <= 0) {
            throw new IllegalArgumentException("propertyId가 필요합니다.");
        }
        validateDestinationInput(
                request.getDestinationId(),
                request.getDestinationLatitude(),
                request.getDestinationLongitude()
        );
    }

    private void validateBatchRequest(SafetyBatchRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("요청 본문이 필요합니다.");
        }
        if (request.getPropertyIds() == null || request.getPropertyIds().isEmpty()) {
            throw new IllegalArgumentException("propertyIds가 필요합니다.");
        }
        validateDestinationInput(
                request.getDestinationId(),
                request.getDestinationLatitude(),
                request.getDestinationLongitude()
        );
    }

    private void validateDestinationInput(
            Integer destinationId,
            Double destinationLatitude,
            Double destinationLongitude
    ) {
        boolean hasDestinationId = destinationId != null && destinationId > 0;
        boolean hasDestinationCoordinate =
                destinationLatitude != null && destinationLongitude != null;

        if (!hasDestinationId && !hasDestinationCoordinate) {
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
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException(
                    label + " 위도 범위가 올바르지 않습니다."
            );
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException(
                    label + " 경도 범위가 올바르지 않습니다."
            );
        }
    }

    private BigDecimal toDatabaseCoordinate(double value) {
        return BigDecimal.valueOf(value)
                .setScale(8, RoundingMode.HALF_UP);
    }

    private String defaultName(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String toGrade(Integer safetyScore) {
        int score = safetyScore == null ? 0 : safetyScore;
        if (score >= 80) {
            return "SAFE";
        }
        if (score >= 60) {
            return "WARNING";
        }
        return "DANGER";
    }

    private record CalculationResult(
            PropertySafetyVO stored,
            int insertedRows,
            SafetyRouteCandidateDTO selectedRoute
    ) {
    }

    private record BoundingBox(
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude
    ) {
    }
}
