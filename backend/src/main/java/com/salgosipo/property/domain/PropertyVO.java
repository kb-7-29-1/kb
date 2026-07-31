package com.salgosipo.property.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyVO {
    private Long propertyId;
    private Double propertyLatitude;
    private Double propertyLongitude;
    private String address;
    private Integer buildingType; // 1: 빌라, 2: 다가구, 3: 오피스텔
    private Integer roomType; // 1: 원룸, 2: 투룸
    private Integer deposit; // 만원
    private Integer monthlyRent; // 만원 (0: 전세)
    private Double area; // m²
    private Integer floor;
    private String builtYear; // YYYY
    private Boolean isIllegalBuilding;
    private String delYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
