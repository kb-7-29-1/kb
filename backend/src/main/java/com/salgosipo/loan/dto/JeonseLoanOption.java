package com.salgosipo.loan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JeonseLoanOption {
    @JsonProperty("fin_co_no")
    private String finCoNo;

    @JsonProperty("fin_prdt_cd")
    private String finPrdtCd;

    @JsonProperty("lend_rate_type_nm")
    private String rateTypeName;

    @JsonProperty("lend_rate_min")
    private Double rateMin;

    @JsonProperty("lend_rate_max")
    private Double rateMax;

    @JsonProperty("lend_rate_avg")
    private Double rateAvg;
}
