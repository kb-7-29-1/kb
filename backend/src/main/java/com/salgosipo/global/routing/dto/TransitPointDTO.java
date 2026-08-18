package com.salgosipo.global.routing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitPointDTO {
    private Double latitude;
    private Double longitude;
}
