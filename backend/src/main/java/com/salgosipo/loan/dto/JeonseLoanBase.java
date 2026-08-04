package com.salgosipo.loan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JeonseLoanBase {
    @JsonProperty("fin_co_no")
    private String finCoNo;

    @JsonProperty("fin_prdt_cd")
    private String finPrdtCd;

    @JsonProperty("kor_co_nm")
    private String companyName;

    @JsonProperty("fin_prdt_nm")
    private String productName;

    @JsonProperty("loan_lmt")
    private String loanLimit;

    @JsonProperty("join_way")
    private String joinWay;
}
