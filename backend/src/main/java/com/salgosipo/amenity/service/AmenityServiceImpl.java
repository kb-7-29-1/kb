package com.salgosipo.amenity.service;

import com.salgosipo.amenity.client.WalkingApiClient;
import com.salgosipo.amenity.dto.AmenityFilter;
import com.salgosipo.amenity.dto.AmenityPropertyCoordinateDTO;
import com.salgosipo.amenity.dto.AmenityRequestDTO;
import com.salgosipo.amenity.dto.AmenityResponseDTO;
import com.salgosipo.amenity.mapper.AmenityMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

@Log4j2
@Service
public class AmenityServiceImpl implements AmenityService {

    // 지구 반지름을 미터 단위로 둔 값, 위도·경도 두 점의 직선거리를 계산할 때 사용
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    // 직선거리에 1.3배를 곱하는 보정값
    private static final double WALKING_DISTANCE_FACTOR = 1.3;

    // 분당 도보 속도 75m 기준
    private static final double WALKING_SPEED_METERS_PER_MINUTE = 75;

    private final AmenityMapper amenityMapper;
    private final WalkingApiClient walkingApiClient;
    private final AmenityCacheService amenityCacheService;

    public AmenityServiceImpl(
            AmenityMapper amenityMapper,
            WalkingApiClient walkingApiClient,
            AmenityCacheService amenityCacheService
    ) {
        this.amenityMapper = amenityMapper;
        this.walkingApiClient = walkingApiClient;
        this.amenityCacheService = amenityCacheService;
    }

    @Override
    public List<AmenityResponseDTO> getAmenitiesByFilter(AmenityRequestDTO request) {
        // 요청 검증 및 기존 편의시설 조회
        if (!isValidRequest(request)) {
            return Collections.emptyList();
        }

        List<AmenityResponseDTO> storedAmenities = new ArrayList<>(
                amenityMapper.getAmenitiesByFilter(request)
        );
        Set<Integer> storedTypes = new HashSet<>();
        storedAmenities.forEach(amenity -> storedTypes.add(amenity.getAmenityType()));

        boolean hasMissingType = request.getAmenities().stream()
                .anyMatch(filter -> !storedTypes.contains(filter.getAmenityType()));

        if (!hasMissingType) {
            return filterByRequest(storedAmenities, request.getAmenities());
        }

        // DB에 없는 편의시설만 TMAP으로 계산 및 저장
        AmenityPropertyCoordinateDTO property =
                amenityMapper.getPropertyCoordinates(request.getPropertyId());
        Double startLat = property == null ? null : property.getLatitude();
        Double startLng = property == null ? null : property.getLongitude();

        if (startLat == null || startLng == null) {
            log.warn("Property coordinates are missing. PropertyId: {}", request.getPropertyId());
            return filterByRequest(storedAmenities, request.getAmenities());
        }

        for (AmenityFilter filter : request.getAmenities()) {
            Integer type = filter.getAmenityType();
            if (storedTypes.contains(type)) {
                continue;
            }

            String keyword = getKeywordByType(type);
            if (keyword == null) {
                continue;
            }

            double[] nearestPlaceCoords = walkingApiClient.findNearestPlace(startLat, startLng, keyword);
            if (nearestPlaceCoords == null) {
                continue;
            }

            int straightDistanceMeters = calculateStraightDistanceMeters(
                    startLat,
                    startLng,
                    nearestPlaceCoords[0],
                    nearestPlaceCoords[1]
            );
            int estimatedWalkingDistanceMeters = (int) Math.ceil(
                    straightDistanceMeters * WALKING_DISTANCE_FACTOR
            );
            int estimatedWalkingMinutes = Math.max(
                    1,
                    (int) Math.ceil(estimatedWalkingDistanceMeters / WALKING_SPEED_METERS_PER_MINUTE)
            );

            AmenityResponseDTO amenity = AmenityResponseDTO.builder()
                    .propertyId(request.getPropertyId())
                    .amenityType(type)
                    .amenityName(keyword)
                    .amenityLatitude(nearestPlaceCoords[0])
                    .amenityLongitude(nearestPlaceCoords[1])
                    .distanceMeters(estimatedWalkingDistanceMeters)
                    .walkTimeMinutes(estimatedWalkingMinutes)
                    .build();

            // 동시 요청으로 이미 저장된 경우에도 예외 없이 기존 행을 유지한다.
            amenityCacheService.saveIfAbsent(amenity);
            storedTypes.add(type);
        }

        // 동시 요청이 먼저 저장했을 수 있으므로, 최종 결과는 DB에서 다시 읽어 반환한다.
        List<AmenityResponseDTO> finalAmenities = amenityMapper.getAmenitiesByFilter(request);
        return filterByRequest(finalAmenities, request.getAmenities());
    }

    @Override
    public Map<Integer, List<AmenityResponseDTO>> getAmenitiesByProperties(List<AmenityRequestDTO> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyMap();
        }

        List<AmenityRequestDTO> validRequests = requests.stream()
                .filter(this::isValidRequest)
                .toList();
        if (validRequests.isEmpty()) {
            return Collections.emptyMap();
        }

        // 목록에 표시할 매물의 캐시를 한 번에 가져와, 캐시가 채워진 경우 N번의 DB 조회를 피한다.
        Set<Integer> propertyIds = new HashSet<>();
        validRequests.forEach(request -> propertyIds.add(request.getPropertyId()));
        Map<Integer, List<AmenityResponseDTO>> cachedAmenitiesByProperty = new HashMap<>();
        for (AmenityResponseDTO amenity : amenityMapper.getAmenitiesByPropertyIds(new ArrayList<>(propertyIds))) {
            cachedAmenitiesByProperty
                    .computeIfAbsent(amenity.getPropertyId(), ignored -> new ArrayList<>())
                    .add(amenity);
        }

        Map<Integer, List<AmenityResponseDTO>> amenitiesByProperty = new LinkedHashMap<>();
        for (AmenityRequestDTO request : validRequests) {
            try {
                List<AmenityResponseDTO> cachedAmenities = cachedAmenitiesByProperty
                        .getOrDefault(request.getPropertyId(), Collections.emptyList());
                Set<Integer> cachedTypes = new HashSet<>();
                cachedAmenities.forEach(amenity -> cachedTypes.add(amenity.getAmenityType()));

                boolean hasAllRequestedTypes = request.getAmenities().stream()
                        .allMatch(filter -> cachedTypes.contains(filter.getAmenityType()));

                amenitiesByProperty.put(
                        request.getPropertyId(),
                        hasAllRequestedTypes
                                ? filterByRequest(cachedAmenities, request.getAmenities())
                                : getAmenitiesByFilter(request)
                );
            } catch (RuntimeException e) {
                // 한 매물의 조회 실패가 전체 목록 필터링을 중단시키지 않도록 빈 결과로 처리한다.
                log.warn("Amenity calculation failed. PropertyId: {}", request.getPropertyId(), e);
                amenitiesByProperty.put(request.getPropertyId(), Collections.emptyList());
            }
        }
        return amenitiesByProperty;
    }

    private boolean isValidRequest(AmenityRequestDTO request) {
        return request != null
                && request.getPropertyId() != null
                && request.getAmenities() != null
                && !request.getAmenities().isEmpty()
                && request.getAmenities().stream().allMatch(filter ->
                filter != null
                        && filter.getAmenityType() != null
                        && filter.getWalkTimeMinutes() != null
        );
    }

    private List<AmenityResponseDTO> filterByRequest(
            List<AmenityResponseDTO> amenities,
            List<AmenityFilter> filters
    ) {
        return amenities.stream()
                .filter(amenity -> filters.stream().anyMatch(filter ->
                        amenity.getAmenityType().equals(filter.getAmenityType())
                                && amenity.getWalkTimeMinutes() <= filter.getWalkTimeMinutes()
                ))
                .toList();
    }

    private String getKeywordByType(Integer amenityType) {
        return switch (amenityType) {
            case 1 -> "편의점";
            case 2 -> "카페";
            case 3 -> "코인세탁소";
            case 4 -> "패스트푸드";
            case 5 -> "다이소";
            case 6 -> "올리브영";
            case 7 -> "대형마트";
            default -> null;
        };
    }

    // 직선거리 계산
    private int calculateStraightDistanceMeters(
            double startLat,
            double startLng,
            double endLat,
            double endLng
    ) {
        double latDistance = Math.toRadians(endLat - startLat);
        double lngDistance = Math.toRadians(endLng - startLng);
        double startLatitude = Math.toRadians(startLat);
        double endLatitude = Math.toRadians(endLat);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(startLatitude) * Math.cos(endLatitude)
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (int) Math.round(EARTH_RADIUS_METERS * c);
    }
}
