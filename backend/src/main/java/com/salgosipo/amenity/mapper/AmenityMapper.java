package com.salgosipo.amenity.mapper;

import com.salgosipo.amenity.dto.AmenityRequestDTO;
import com.salgosipo.amenity.dto.AmenityResponseDTO;
import com.salgosipo.amenity.dto.AmenityPropertyCoordinateDTO;

import java.util.List;

public interface AmenityMapper {

    List<AmenityResponseDTO> getAmenitiesByFilter(AmenityRequestDTO request);

    List<AmenityResponseDTO> getCachedAmenities(Integer propertyId);

    // 편의시설 계산에 필요한 매물 좌표만 조회
    AmenityPropertyCoordinateDTO getPropertyCoordinates(Integer propertyId);

    int insertAmenityIfAbsent(AmenityResponseDTO amenity);
}
