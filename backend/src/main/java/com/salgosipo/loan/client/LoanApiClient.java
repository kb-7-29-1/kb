package com.salgosipo.loan.client;

import com.salgosipo.loan.dto.JeonseLoanApiResponse;
import com.salgosipo.loan.dto.MinfundLoanApiResponse;
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

    @Value("${loan.minfund.api.key}")
    private String serviceKey;

    @Value("${loan.jeonse.api.key}")
    private String jeonseApiKey;

    private static final String MINFUND_END_POINT =
            "https://apis.data.go.kr/B553701/LoanProductSearchingInfo/LoanProductSearchingInfo/getLoanProductSearchingInfo";

    private static final String JEONSE_END_POINT =
            "https://finlife.fss.or.kr/finlifeapi/rentHouseLoanProductsSearch.json";

    // 서민금융진흥원 (월세용)
    public MinfundLoanApiResponse fetchMinfundLoans() {
         URI uri = UriComponentsBuilder.fromHttpUrl(MINFUND_END_POINT)
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 50)
                .queryParam("type", "xml")
                .build(true) // 이미 인코딩된 값 인코딩 X
                .toUri();

        return restTemplate.getForObject(uri, MinfundLoanApiResponse.class);
    }

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