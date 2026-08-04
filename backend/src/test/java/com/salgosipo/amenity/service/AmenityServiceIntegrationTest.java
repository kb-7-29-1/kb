package com.salgosipo.amenity.service;

import com.salgosipo.amenity.dto.AmenityFilter;
import com.salgosipo.amenity.dto.AmenityRequestDTO;
import com.salgosipo.amenity.dto.AmenityResponseDTO;
import com.salgosipo.global.config.RootConfig;
import com.salgosipo.global.security.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class, SecurityConfig.class})
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

    @Test
    void getsAmenitiesForMultipleProperties() {
        AmenityFilter filter = new AmenityFilter();
        filter.setAmenityType(1);
        filter.setWalkTimeMinutes(30);

        List<AmenityRequestDTO> requests = List.of(
                AmenityRequestDTO.builder().propertyId(1).amenities(List.of(filter)).build(),
                AmenityRequestDTO.builder().propertyId(2).amenities(List.of(filter)).build()
        );

        Map<Integer, List<AmenityResponseDTO>> result =
                amenityService.getAmenitiesByProperties(requests);

        assertNotNull(result.get(1));
        assertNotNull(result.get(2));
    }
}
