package com.salgosipo.loan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JeonseLoanApiResponse {
    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private int totalCount;
        private List<JeonseLoanBase> baseList;
        private List<JeonseLoanOption> optionList;
    }
}