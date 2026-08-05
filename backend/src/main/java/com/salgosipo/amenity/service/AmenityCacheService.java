package com.salgosipo.amenity.service;

import com.salgosipo.amenity.dto.AmenityResponseDTO;
import com.salgosipo.amenity.mapper.AmenityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AmenityCacheService {

    private final AmenityMapper amenityMapper;

    @Transactional
    public void saveIfAbsent(AmenityResponseDTO amenity) {
        amenityMapper.insertAmenityIfAbsent(amenity);
    }
}
