package com.salgosipo.destination.domain;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinationVO {
    private Integer destinationId;
    private BigDecimal destLatitude;
    private BigDecimal destLongitude;
    private String destName;
    private String destAddress;
}
