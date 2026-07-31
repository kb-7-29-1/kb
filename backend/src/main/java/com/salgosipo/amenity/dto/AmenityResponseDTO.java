package com.salgosipo.amenity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmenityResponseDTO {

    private Integer propertyId;

    private Integer amenityType;

    private String amenityName;

    private Double amenityLatitude;

    private Double amenityLongitude;

    private Integer walkTimeMinutes;
}