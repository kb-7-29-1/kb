package com.salgosipo.amenity.mapper;

import com.salgosipo.amenity.dto.AmenityPropertyCoordinateDTO;
import com.salgosipo.amenity.dto.AmenityRequestDTO;
import com.salgosipo.amenity.dto.AmenityResponseDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AmenityMapper {

    // 단일 매물에 캐시된 편의시설을 조회
    List<AmenityResponseDTO> getAmenitiesByFilter(AmenityRequestDTO request);

    // 목록 필터에 필요한 여러 매물의 편의시설 캐시를 한 번에 조회
    List<AmenityResponseDTO> getAmenitiesByPropertyIds(@Param("propertyIds") List<Integer> propertyIds);

    // 캐시에 없는 편의시설의 예상 시간을 계산하기 위한 매물 좌표를 조회
    AmenityPropertyCoordinateDTO getPropertyCoordinates(Integer propertyId);

    // 매물과 편의시설 유형 조합이 아직 없을 때만 계산 결과를 캐시에 저장
    int insertAmenityIfAbsent(AmenityResponseDTO amenity);
}
