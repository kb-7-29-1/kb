package com.salgosipo.amenity.service;

import com.salgosipo.amenity.dto.AmenityRequestDTO;
import com.salgosipo.amenity.dto.AmenityResponseDTO;

import java.util.List;

public interface AmenityService {

    // 특정 매물 + 편의시설 종류 + 도보시간 조건으로 조회
    List<AmenityResponseDTO> getAmenitiesByFilter(AmenityRequestDTO request);
}
