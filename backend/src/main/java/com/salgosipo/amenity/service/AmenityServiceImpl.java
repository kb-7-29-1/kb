package com.salgosipo.amenity.service;

import com.salgosipo.amenity.client.WalkingApiClient;
import com.salgosipo.amenity.dto.AmenityFilter;
import com.salgosipo.amenity.dto.AmenityRequestDTO;
import com.salgosipo.amenity.dto.AmenityResponseDTO;
import com.salgosipo.amenity.mapper.AmenityMapper;
import com.salgosipo.property.dto.PropertyDetailDTO;
import com.salgosipo.property.mapper.PropertyMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Log4j2
@Service
public class AmenityServiceImpl implements AmenityService {

    private final AmenityMapper amenityMapper;
    private final WalkingApiClient walkingApiClient;
    private final PropertyMapper propertyMapper;

    public AmenityServiceImpl(
            AmenityMapper amenityMapper,
            PropertyMapper propertyMapper,
            @Value("${TMAP_API_KEY}") String tmapApiKey
    ) {
        this.amenityMapper = amenityMapper;
        this.propertyMapper = propertyMapper;
        this.walkingApiClient = new WalkingApiClient(tmapApiKey);
    }

    @Override
    @Transactional
    public List<AmenityResponseDTO> getAmenitiesByFilter(AmenityRequestDTO request) {
        // 요청값이 완전하지 않으면 조회하지 않는다.
        if (!isValidRequest(request)) {
            return Collections.emptyList();
        }

        // 해당 매물에 이미 저장된 편의시설을 모두 조회한다.
        List<AmenityResponseDTO> storedAmenities = new ArrayList<>(
                amenityMapper.getAmenitiesByFilter(request)
        );
        // 저장된 편의시설 유형을 집합으로 관리한다.
        Set<Integer> storedTypes = new HashSet<>();
        storedAmenities.forEach(amenity -> storedTypes.add(amenity.getAmenityType()));

        // 요청한 유형 중 아직 계산되지 않은 유형이 있는지 확인한다.
        boolean hasMissingType = request.getAmenities().stream()
                .anyMatch(filter -> !storedTypes.contains(filter.getAmenityType()));

        // 모든 유형이 저장되어 있으면 시간 조건만 적용해 반환한다.
        if (!hasMissingType) {
            return filterByRequest(storedAmenities, request.getAmenities());
        }

        // 누락된 유형 계산에 사용할 매물 좌표를 조회한다.
        PropertyDetailDTO property = propertyMapper.selectPropertyDetail(
                request.getPropertyId().longValue(),
                null
        );
        Double startLat = property == null ? null : property.getLatitude();
        Double startLng = property == null ? null : property.getLongitude();

        // 매물 좌표가 없으면 저장된 결과만 반환한다.
        if (startLat == null || startLng == null) {
            log.warn("Property coordinates are missing. PropertyId: {}", request.getPropertyId());
            return filterByRequest(storedAmenities, request.getAmenities());
        }

        // 요청 유형 중 DB에 없는 유형만 외부 API로 계산한다.
        for (AmenityFilter filter : request.getAmenities()) {
            Integer type = filter.getAmenityType();
            // 이미 저장된 유형은 다시 계산하지 않는다.
            if (storedTypes.contains(type)) {
                continue;
            }

            // 유형을 TMAP 검색어로 변환한다.
            String keyword = getKeywordByType(type);
            if (keyword == null) {
                continue;
            }

            // 매물 주변에서 가장 가까운 편의시설 좌표를 찾는다.
            double[] nearestPlaceCoords = walkingApiClient.findNearestPlace(startLat, startLng, keyword);
            if (nearestPlaceCoords == null) {
                continue;
            }

            // 매물에서 편의시설까지의 도보 경로를 계산한다.
            WalkingApiClient.WalkingRoute route = walkingApiClient.calculateWalkingRoute(
                    startLat,
                    startLng,
                    nearestPlaceCoords[0],
                    nearestPlaceCoords[1]
            );
            if (route == null || route.walkTimeMinutes() == null || route.distanceMeters() == null) {
                continue;
            }

            // 계산 결과를 DB에 저장할 응답 객체로 만든다.
            AmenityResponseDTO amenity = AmenityResponseDTO.builder()
                    .propertyId(request.getPropertyId())
                    .amenityType(type)
                    .amenityName(keyword)
                    .amenityLatitude(nearestPlaceCoords[0])
                    .amenityLongitude(nearestPlaceCoords[1])
                    .distanceMeters(route.distanceMeters())
                    .walkTimeMinutes(route.walkTimeMinutes())
                    .build();

            // 다음 요청부터 재사용할 수 있도록 계산 결과를 저장한다.
            amenityMapper.insertAmenity(amenity);
            storedAmenities.add(amenity);
            storedTypes.add(type);
        }

        return filterByRequest(storedAmenities, request.getAmenities());
    }

    private boolean isValidRequest(AmenityRequestDTO request) {
        // 필수 요청값과 편의시설 조건의 존재 여부를 검증한다.
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
        // 요청한 유형과 최대 도보시간을 만족하는 결과만 남긴다.
        return amenities.stream()
                .filter(amenity -> filters.stream().anyMatch(filter ->
                        amenity.getAmenityType().equals(filter.getAmenityType())
                                && amenity.getWalkTimeMinutes() <= filter.getWalkTimeMinutes()
                ))
                .toList();
    }

    private String getKeywordByType(Integer amenityType) {
        // 프론트의 편의시설 유형 번호를 TMAP 검색어로 매핑한다.
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
}
