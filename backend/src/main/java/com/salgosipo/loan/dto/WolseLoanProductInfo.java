package com.salgosipo.loan.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WolseLoanProductInfo {
    private String type;
    private String companyName;
    private String productName;
    private String rateInfo;
    private String target;
    private Integer monthlyLimitAmount;
    private String totalLimitText;
    private String applyMethod;
}