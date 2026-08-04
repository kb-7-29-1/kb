package com.salgosipo.loan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoanProductDtoWithRate {
    private LoanProductDto dto;
    private Double rate;
}
