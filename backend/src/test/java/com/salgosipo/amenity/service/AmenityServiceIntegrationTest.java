package com.salgosipo.amenity.service;

import com.salgosipo.amenity.dto.AmenityFilter;
import com.salgosipo.amenity.dto.AmenityRequestDTO;
import com.salgosipo.amenity.dto.AmenityResponseDTO;
import com.salgosipo.global.config.RootConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
class AmenityServiceIntegrationTest {

    @Autowired
    private AmenityService amenityService;

    @Test
    void callsTmapAndStoresAmenityForExistingProperty() {
        AmenityFilter n1 = new AmenityFilter();
        n1.setAmenityType(2);
        n1.setWalkTimeMinutes(10);

        AmenityFilter n2 = new AmenityFilter();
        n2.setAmenityType(3);
        n2.setWalkTimeMinutes(15);

        AmenityFilter n3 = new AmenityFilter();
        n3.setAmenityType(5);
        n3.setWalkTimeMinutes(20);


        AmenityRequestDTO request = AmenityRequestDTO.builder()
                .propertyId(3)
                .amenities(List.of(n1, n2, n3))
                .build();

        List<AmenityResponseDTO> result = amenityService.getAmenitiesByFilter(request);

        assertFalse(result.isEmpty(), "TMAP 조회 결과가 없거나 저장에 실패했습니다.");
        assertNotNull(result.get(0).getDistanceMeters());
        assertNotNull(result.get(0).getWalkTimeMinutes());
    }
}
