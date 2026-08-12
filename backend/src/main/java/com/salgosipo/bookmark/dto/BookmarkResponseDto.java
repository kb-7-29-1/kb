package com.salgosipo.bookmark.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkResponseDto {
    private Long propertyId;
    private String address;
    private Integer deposit;
    private Integer monthlyRent;
    private Double area;
    private boolean isIllegalBuilding;
    private Integer safetyScore;
    private String buildingTypeTag;
    private Date bookmarkedAt;
    private Integer destinationId;
    private String destinationName;
    private String destinationAddress;
    private BigDecimal destinationLat;
    private BigDecimal destinationLng;
}
