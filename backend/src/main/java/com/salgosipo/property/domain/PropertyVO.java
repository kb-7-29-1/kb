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
    private String useAprDay;           // 사용승인일 (YYYYMMDD 또는 YYYY-MM-DD)
    private String structureName;       // 건물 구조 (철근콘크리트구조 등)
    private String mainPurposeName;     // 주용도 (공동주택, 오피스텔 등)
    private String earthquakeProofYn;   // 내진설계 여부 (1/0)
    private String illegalReason;       // 위반/적법 지정 사유
    private String delYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
