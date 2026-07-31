package com.salgosipo.safety.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertySafetyVO {

    private Long propertyId;
    private Long destinationId;

    private Integer safetyScore;
    private Integer cctvCount;
    private Integer streetLampCount;
    private Boolean hasPoliceStation;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}