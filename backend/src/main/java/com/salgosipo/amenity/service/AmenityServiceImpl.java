package com.salgosipo.amenity.service;

import com.salgosipo.amenity.client.WalkingApiClient;
import com.salgosipo.amenity.dto.AmenityRequestDTO;
import com.salgosipo.amenity.dto.AmenityResponseDTO;
import com.salgosipo.amenity.mapper.AmenityMapper;
import com.salgosipo.property.mapper.PropertyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AmenityServiceImpl implements AmenityService {

    private final AmenityMapper amenityMapper;
    private final WalkingApiClient walkingApiClient;

    private final PropertyMapper propertyMapper;

    @Override
    public List<AmenityResponseDTO> getAmenitiesByFilter(AmenityRequestDTO request) {

        List<AmenityResponseDTO> existingAmenities = amenityMapper.getAmenitiesByFilter(request);

        // DB에 데이터가 있음
        if (existingAmenities != null && !existingAmenities.isEmpty()) {
            return existingAmenities;
        }


//          1. 매물의 위경도 구하기
//          Double startLat = propertyMapper.getLatitude(request.getPropertyId());
//          Double startLng = propertyMapper.getLongitude(request.getPropertyId());
//
//          2. 주변 편의시설 위경도 구하기
//          API(카카오 로컬 등)를 쓰거나 DB 다른 테이블에서 매물 주변의 편의점, 카페 목록(위경도 포함)을 가져옵니다.
//          List<AmenityLocation> nearbyAmenities = ...;
//
//          3. TMAP API로 도보 시간 계산 및 DB 저장
//          List<AmenityResponseDTO> newAmenities = new ArrayList<>();
//
//          for (AmenityLocation amenity : nearbyAmenities) {
//              Double endLat = amenity.getLatitude();
//              Double endLng = amenity.getLongitude();
//
//              Integer walkTime = walkingApiClient.calculateWalkingTime(startLat, startLng, endLat, endLng);
//
//              if (walkTime != null && walkTime <= request.요청한_최대_도보시간) {
//                  // VO 객체 만들어서 DB에 INSERT
//                  PropertyAmenityVO vo = PropertyAmenityVO.builder()
//                          .propertyId(request.getPropertyId())
//                          .amenityType(amenity.getType())
//                          .walkTimeMinutes(walkTime)
//                          // ... 나머지 필드 채우기
//                          .build();
//
//                  amenityMapper.insertAmenity(vo);
//
//                  // 응답 리스트에도 추가
//                  newAmenities.add(변환로직);
//              }
//          }
//          return newAmenities;

        return new ArrayList<>();
    }
}
