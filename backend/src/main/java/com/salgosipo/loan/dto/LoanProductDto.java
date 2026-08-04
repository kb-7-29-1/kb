package com.salgosipo.loan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanProductDto {
    private String companyName;
    private String productName;
    private String loanLimit;
    private String rateInfo;
    private String target;
    private String applyMethod;
}
