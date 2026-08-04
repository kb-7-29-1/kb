package com.salgosipo.loan.client;

import com.salgosipo.loan.dto.JeonseLoanApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class LoanApiClient {

    private final RestTemplate restTemplate;

    @Value("${loan.jeonse.api.key}")
    private String jeonseApiKey;

    private static final String JEONSE_END_POINT =
            "https://finlife.fss.or.kr/finlifeapi/rentHouseLoanProductsSearch.json";

    // 금감원 (전세용)
    public JeonseLoanApiResponse fetchJeonseLoans(String topFinGrpNo){
        String uri = UriComponentsBuilder.fromHttpUrl(JEONSE_END_POINT)
                .queryParam("auth",jeonseApiKey)
                .queryParam("topFinGrpNo",topFinGrpNo)
                .queryParam("pageNo",1)
                .toUriString();

        return restTemplate.getForObject(uri, JeonseLoanApiResponse.class);
    }
}