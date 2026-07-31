package com.salgosipo.destination.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinationDTO {
    private Integer destinationId;
    private BigDecimal destLatitude;
    private BigDecimal destLongitude;
    private String destName;
    private String destAddress;
}
