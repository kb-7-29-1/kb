package com.salgosipo.amenity.controller;

import com.salgosipo.amenity.dto.AmenityRequestDTO;
import com.salgosipo.amenity.dto.AmenityResponseDTO;
import com.salgosipo.amenity.service.AmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/amenities")
@RequiredArgsConstructor
public class AmenityController {

    private final AmenityService amenityService;

    // 선택한 매물 한 건의 편의시설을 조회
    @PostMapping("/filter")
    public List<AmenityResponseDTO> getAmenities(@RequestBody AmenityRequestDTO request) {
        return amenityService.getAmenitiesByFilter(request);
    }

    // 지도와 목록의 편의시설 필터 적용을 위해 여러 매물의 결과를 일괄 조회
    @PostMapping("/filter/properties")
    public Map<Integer, List<AmenityResponseDTO>> getAmenitiesByProperties(
            @RequestBody List<AmenityRequestDTO> requests
    ) {
        return amenityService.getAmenitiesByProperties(requests);
    }
}
