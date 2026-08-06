package com.salgosipo.loan.service;

import com.salgosipo.loan.client.LoanApiClient;
import com.salgosipo.loan.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanApiClient loanApiClient;

    public LoanRecommendationDto getOnboardingRecommendation(Integer deposit, Integer monthlyRent, Integer age){
        boolean isJeonse = (monthlyRent == null || monthlyRent == 0);
        List<LoanProductDto> loans = isJeonse ? getJeonseLoans(age) : getWolseLoans(age, deposit, monthlyRent);

        if(loans.isEmpty() || deposit == null){
            return null;
        }

        return buildRecommendationDto(loans.get(0), deposit);
    }

    // 전체 리스트 반환
    public List<LoanRecommendationDto> getLoanRecommendation(Integer deposit, Integer monthlyRent, Integer age){
        boolean isJeonse = (monthlyRent == null || monthlyRent == 0);
        List<LoanProductDto> loans = isJeonse ? getJeonseLoans(age) : getWolseLoans(age, deposit, monthlyRent);
        if(deposit == null) return List.of();
        return loans.stream()
                .map(product -> buildRecommendationDto(product,deposit))
                .collect(Collectors.toList());
    }

    // 기존 getOnboardingRecommendation 안에 있던 로직
    private LoanRecommendationDto buildRecommendationDto(LoanProductDto product, Integer deposit){
        Double ratio = extractLoanRatio(product.getLoanLimit());

        if(ratio == null){
            // 비율(%) 공시가 없는 상품은 보증금 대비 개인화 계산을 할 근거가 없으므로 원문 정보만 응답
            return new LoanRecommendationDto(
                    product.getProductName(),
                    product.getCompanyName(),
                    product.getRateInfo(),
                    product.getLoanLimit(),
                    product.getTarget(),
                    0,
                    0,
                    0
            );
        }

        int expectedLoanAmount = (int) Math.round(deposit * ratio);
        int maxSearchAmount = deposit + expectedLoanAmount;

        return new LoanRecommendationDto(
                product.getProductName(),
                product.getCompanyName(),
                product.getRateInfo(),
                product.getLoanLimit(),
                product.getTarget(),
                ratio,
                expectedLoanAmount,
                maxSearchAmount
        );
    }

    // 전세 (금감원 API)
    private List<LoanProductDto> getJeonseLoans(Integer age){
        JeonseLoanApiResponse response = loanApiClient.fetchJeonseLoans("020000"); // 은행권
        List<JeonseLoanBase> baseList = response.getResult().getBaseList();
        List<JeonseLoanOption> optionList = response.getResult().getOptionList();

        if(baseList == null) return List.of();

        boolean isYouth = age != null && age <= 34;

        return baseList.stream()
                .filter(base -> isYouth || !base.getProductName().contains("청년"))
                .map(base -> {
                    Double minRate = optionList == null ? null : optionList.stream()
                            .filter(opt -> opt.getFinCoNo().equals(base.getFinCoNo())
                                           && opt.getFinPrdtCd().equals(base.getFinPrdtCd()))
                            .map(JeonseLoanOption::getRateMin)
                            .filter(Objects::nonNull)
                            .min(Double::compareTo)
                            .orElse(null);

                    String rateInfo = minRate != null
                            ? minRate + "%"
                            : "정보 없음";

                    LoanProductDto dto = new LoanProductDto(
                            base.getCompanyName(),
                            base.getProductName(),
                            normalizeLoanLimitText(base.getLoanLimit()),
                            rateInfo,
                            "자세한 자격요건은 해당 은행에서 확인해주세요",
                            base.getJoinWay()
                    );
                    return new LoanProductDtoWithRate(dto,minRate != null ? minRate : 999.0);
                })
                .sorted(Comparator.comparing(LoanProductDtoWithRate::getRate)
                        .thenComparing((LoanProductDtoWithRate p) ->!p.getDto().getCompanyName().contains("국민은행")))
                .map(LoanProductDtoWithRate::getDto)
                // 은행별로 여러 상품이 있으면 이미 금리순 정렬돼 있으니 가장 앞(최저금리) 것만 남김
                .collect(Collectors.toMap(
                        LoanProductDto::getCompanyName,
                        product -> product,
                        (best, other) -> best,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    // 월세 (API 데이터 부족으로 인한 하드코딩)
    private List<LoanProductDto> getWolseLoans(Integer age, Integer deposit, Integer monthlyRent) {
        boolean isYouth = age != null && age <= 34;
        boolean isSenior = age != null && age >= 60;
        boolean fitsYouthLoan = deposit != null && monthlyRent != null && deposit <= 6500 && monthlyRent <=70;

        if (isYouth && fitsYouthLoan) {
            String depositLimitInfo = (deposit != null && deposit <= 4500)
                    ? "보증금 전액(" + deposit + "만원) "
                    : "보증금 최대 4,500만원" + (deposit != null ? " (초과분 " + (deposit - 4500) + "만원은 본인 부담)" : "");

            String monthlyLimitInfo = (monthlyRent != null && monthlyRent <= 50)
                    ? "월세 전액(" + monthlyRent + "만원)"
                    : "월 최대 50만원" + (monthlyRent != null ? " (초과분 " + (monthlyRent - 50) + "만원은 본인 부담)" : "");

            return List.of(new LoanProductDto(
                    "주택도시기금 (KB국민은행 등 수탁은행 취급)",
                    "청년전용 보증부 월세대출",
                    depositLimitInfo + "/" + monthlyLimitInfo,
                    "연 1.0%~2.0% (국토교통부 고시 변동금리)",
                    "만 19~34세 청년, 무주택 세대주 (보증금 6,500만원·월세 70만원 이하)",
                    "기금e든든(enhuf.molit.go.kr) 또는 KB국민은행 등 방문"
            ));
        }

        if (isSenior) {
            return List.of(new LoanProductDto(
                    "국민연금공단",
                    "노후긴급자금대부",
                    "최대 1,000만원 (연간 연금수령액 2배 이내)",
                    "연 2.51% (국고채권 수익률 등 연동 변동금리)",
                    "만 60세 이상 국민연금수급자 (노령·분할·유족·장애1~3급)",
                    "국민연금공단 지사 또는 상담센터 방문"
            ));
        }

        String limitInfo = (monthlyRent != null && monthlyRent <= 60)
                    ? "월세 전액(" + monthlyRent + "만원) "
                    : "월 최대 60만원" + (monthlyRent != null ? " (초과분 " + (monthlyRent - 60) + "만원은 본인 부담)" : "");

        return List.of(new LoanProductDto(
                "주택도시기금 (KB국민은행 등 수탁은행 취급)",
                "주거안정 월세대출 (2년간 총 1440만원 한도)",
                limitInfo,
                "연 1.3%~1.8%",
                "저소득 무주택 세대주 (소득 기준 등 자격 확인 필요)",
                "기금e든든(enhuf.molit.go.kr) 또는 KB국민은행 등 방문"
        ));
    }

    // 대출한도비율(%) 텍스트에서 숫자만 추출
    private Double extractLoanRatio(String loanLimit){
        if(loanLimit == null) return null;

        Matcher matcher = Pattern.compile("(\\d+)%").matcher(loanLimit);
        if(matcher.find()){
            return Integer.parseInt(matcher.group(1)) / 100.0;
        }
        return null;
    }

    // "400백만원" 같은 표기를 "4억원"으로 통일 (100백만원 = 1억원)
    private String normalizeLoanLimitText(String loanLimit){
        if(loanLimit == null) return null;

        Matcher matcher = Pattern.compile("([\\d.]+)\\s*백만원").matcher(loanLimit);
        if(!matcher.find()) return loanLimit;

        double eok = Double.parseDouble(matcher.group(1)) / 100.0;
        String eokText = (eok == Math.floor(eok))
                ? String.valueOf((int) eok)
                : String.valueOf(eok);

        return matcher.replaceFirst(eokText + "억원");
    }
}