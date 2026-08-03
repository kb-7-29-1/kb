package com.salgosipo.amenity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    private Integer distanceMeters;

    private Integer walkTimeMinutes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}