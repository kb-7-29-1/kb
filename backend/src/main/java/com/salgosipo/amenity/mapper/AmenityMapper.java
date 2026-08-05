package com.salgosipo.amenity.mapper;

import com.salgosipo.amenity.dto.AmenityRequestDTO;
import com.salgosipo.amenity.dto.AmenityResponseDTO;
import com.salgosipo.amenity.dto.AmenityPropertyCoordinateDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AmenityMapper {

    List<AmenityResponseDTO> getAmenitiesByFilter(AmenityRequestDTO request);

    // 목록 편의시설 필터용 매물별 편의시설 캐시 일괄 조회
    List<AmenityResponseDTO> getAmenitiesByPropertyIds(@Param("propertyIds") List<Integer> propertyIds);

    // 편의시설 계산에 필요한 매물 좌표만 조회
    AmenityPropertyCoordinateDTO getPropertyCoordinates(Integer propertyId);

    int insertAmenityIfAbsent(AmenityResponseDTO amenity);
}
