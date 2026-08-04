package com.salgosipo.amenity.controller;

import com.salgosipo.amenity.dto.AmenityRequestDTO;
import com.salgosipo.amenity.dto.AmenityResponseDTO;
import com.salgosipo.amenity.service.AmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PostMapping("/filter")
    public List<AmenityResponseDTO> getAmenities(@RequestBody AmenityRequestDTO request){
        return amenityService.getAmenitiesByFilter(request);
    }

    @GetMapping("/properties/{propertyId}")
    public List<AmenityResponseDTO> getCachedAmenities(@PathVariable Integer propertyId) {
        return amenityService.getCachedAmenities(propertyId);
    }

    // 목록 필터링에 필요한 매물별 편의시설을 일괄 조회
    @PostMapping("/filter/properties")
    public Map<Integer, List<AmenityResponseDTO>> getAmenitiesByProperties(
            @RequestBody List<AmenityRequestDTO> requests
    ) {
        return amenityService.getAmenitiesByProperties(requests);
    }
}
