package com.salgosipo.destination.dto;

import com.salgosipo.destination.domain.DestinationVO;
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

    public DestinationVO toVO() {
        return DestinationVO.builder()
                .destinationId(destinationId)
                .destLatitude(destLatitude)
                .destLongitude(destLongitude)
                .destName(destName)
                .destAddress(destAddress)
                .build();
    }
}
