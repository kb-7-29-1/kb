package com.salgosipo.amenity.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyAmenityVO {

    private Integer propertyId;

    private Integer amenityType;

    private String amenityName;

    private Double amenityLatitude;

    private Double amenityLongitude;

    private Integer walkTimeMinutes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}