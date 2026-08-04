package com.salgosipo.loan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanRecommendationDto {
    private String productName;
    private String companyName;
    private String rateInfo;
    private String loanLimit;
    private String target;
    private double loanRatio;
    private int expectedLoanAmount;
    private int maxSearchAmount;
}
