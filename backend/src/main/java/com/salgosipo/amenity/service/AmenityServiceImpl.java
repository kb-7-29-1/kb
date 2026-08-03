package com.salgosipo.amenity.service;

import com.salgosipo.amenity.client.WalkingApiClient;
import com.salgosipo.amenity.dto.AmenityFilter;
import com.salgosipo.amenity.dto.AmenityRequestDTO;
import com.salgosipo.amenity.dto.AmenityResponseDTO;
import com.salgosipo.amenity.mapper.AmenityMapper;
import com.salgosipo.property.mapper.PropertyMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
    public List<AmenityResponseDTO> getAmenitiesByFilter(AmenityRequestDTO request) {

        // 1. 기존 DB에 저장된 데이터 확인 (캐싱)
        List<AmenityResponseDTO> existingAmenities = amenityMapper.getAmenitiesByFilter(request);
        if (existingAmenities != null && !existingAmenities.isEmpty()) {
            return existingAmenities;
        }

        // 2. 매물의 위경도 조회
        // Double startLat = propertyMapper.getLatitude(request.getPropertyId());
        // Double startLng = propertyMapper.getLongitude(request.getPropertyId());

        // 예시: 강남역 10번 출구 근처 좌표로 임시 설정
        Double startLat = 37.497952;
        Double startLng = 127.027619;

        List<AmenityResponseDTO> newAmenities = new ArrayList<>();

        // if (startLat == null || startLng == null) {
        //    log.warn("매물 좌표가 존재하지 않습니다. PropertyId: {}", request.getPropertyId());
        //    return newAmenities;
        // }

        // 클라이언트가 보낸 필터 목록이 없으면 빈 리스트 반환
        if (request.getAmenities() == null || request.getAmenities().isEmpty()) {
            return newAmenities;
        }

        // 3. 사용자가 요청한 필터(AmenityFilter) 목록 순회
        for (AmenityFilter filter : request.getAmenities()) {
            Integer type = filter.getAmenityType();
            Integer maxWalkTime = filter.getWalkTimeMinutes(); // 유저가 설정한 최대 도보 시간

            // 3-1. Integer 타입을 실제 검색어(String)로 변환
            String keyword = getKeywordByType(type);
            if (keyword == null) continue;

            // 3-2. 해당 키워드로 반경 2km 내 가장 가까운 장소 좌표 찾기
            double[] nearestPlaceCoords = walkingApiClient.findNearestPlace(startLat, startLng, keyword);

            if (nearestPlaceCoords != null) {
                Double endLat = nearestPlaceCoords[0];
                Double endLng = nearestPlaceCoords[1];

                // 3-3. 출발지(매물)에서 도착지(편의시설)까지의 도보 시간 계산
                WalkingApiClient.WalkingRoute route = walkingApiClient.calculateWalkingRoute(
                        startLat, startLng, endLat, endLng
                );
                Integer walkTime = route == null ? null : route.walkTimeMinutes();

                // 3-4. 계산된 도보 시간이 유저가 설정한 최대 시간(maxWalkTime) 이하인 경우에만 추가
                if (route != null && walkTime != null && route.distanceMeters() != null
                        && walkTime <= maxWalkTime) {

                    // 3-5. 응답 DTO 생성 (DB 저장을 위해 먼저 생성해야 합니다)
                    AmenityResponseDTO responseDTO = AmenityResponseDTO.builder()
                            .propertyId(request.getPropertyId())
                            .amenityType(type)
                            .amenityName(keyword)
                            .amenityLatitude(endLat)
                            .amenityLongitude(endLng)
                            .distanceMeters(route.distanceMeters())
                            .walkTimeMinutes(walkTime)
                            .build();

                    // 3-6. DB에 캐싱용 데이터 INSERT
                    amenityMapper.insertAmenity(responseDTO);

                    // 3-7. 반환할 리스트에 추가
                    newAmenities.add(responseDTO);
                }
            } else {
                log.info("반경 2km 내에 조건에 맞는 편의시설이 없습니다: {}", keyword);
            }
        }

        return newAmenities;
    }

    /**
     * Integer 형태의 amenityType을 TMAP 검색을 위한 문자열 키워드로 변환
     * (프론트엔드의 amenities.ref 매핑 기준 적용)
     */
    private String getKeywordByType(Integer amenityType) {
        if (amenityType == null) return null;

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
