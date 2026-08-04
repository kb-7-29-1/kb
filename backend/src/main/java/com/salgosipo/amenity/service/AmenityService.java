package com.salgosipo.amenity.service;

import com.salgosipo.amenity.dto.AmenityRequestDTO;
import com.salgosipo.amenity.dto.AmenityResponseDTO;

import java.util.List;
import java.util.Map;

public interface AmenityService {

    // 특정 매물 + 편의시설 종류 + 도보시간 조건으로 조회
    List<AmenityResponseDTO> getAmenitiesByFilter(AmenityRequestDTO request);

    // 목록의 매물별 편의시설을 조회하고, 캐시되지 않은 항목은 계산한다.
    Map<Integer, List<AmenityResponseDTO>> getAmenitiesByProperties(List<AmenityRequestDTO> requests);
}
