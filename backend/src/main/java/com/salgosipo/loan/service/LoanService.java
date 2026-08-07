package com.salgosipo.loan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salgosipo.loan.client.LoanApiClient;
import com.salgosipo.loan.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
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

    // 월세 대출 데이터 (금감원/기금e든든 Open API 미제공으로 국토교통부 고시 기준 수동 정리,
    // resources/wolse_data/wolse_products.json 참고. 자격판정·한도 계산에 쓰는 숫자 기준은
    // 정책 로직이라 코드에 유지하고, 회사명/상품명/금리안내/대상/신청방법 텍스트만 데이터로 분리)
    private static final List<WolseLoanProductInfo> WOLSE_LOAN_PRODUCTS = loadWolseLoanProducts();

    private static List<WolseLoanProductInfo> loadWolseLoanProducts() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = LoanService.class.getResourceAsStream("/wolse_data/wolse_products.json")) {
            return mapper.readValue(is, mapper.getTypeFactory().constructCollectionType(List.class, WolseLoanProductInfo.class));
        } catch (IOException e) {
            throw new IllegalStateException("월세 대출 데이터를 불러오지 못했습니다.", e);
        }
    }

    private static WolseLoanProductInfo findWolseLoanProduct(String type) {
        return WOLSE_LOAN_PRODUCTS.stream()
                .filter(info -> type.equals(info.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("월세 대출 데이터 누락: " + type));
    }

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
                    List<JeonseLoanOption> matchedOptions = optionList == null ? List.of() : optionList.stream()
                            .filter(opt -> opt.getFinCoNo().equals(base.getFinCoNo())
                                           && opt.getFinPrdtCd().equals(base.getFinPrdtCd()))
                            .collect(Collectors.toList());

                    Double minRate = matchedOptions.stream()
                            .map(JeonseLoanOption::getRateMin)
                            .filter(Objects::nonNull)
                            .min(Double::compareTo)
                            .orElse(null);

                    Double avgRate = matchedOptions.stream()
                            .map(JeonseLoanOption::getRateAvg)
                            .filter(Objects::nonNull)
                            .min(Double::compareTo)
                            .orElse(null);

                    String rateInfo;
                    if (minRate != null && avgRate != null) {
                        rateInfo = "최저 " + minRate + "% · 평균 " + avgRate + "%";
                    } else if (minRate != null) {
                        rateInfo = minRate + "%";
                    } else {
                        rateInfo = "정보 없음";
                    }

                    LoanProductDto dto = new LoanProductDto(
                            base.getCompanyName(),
                            base.getProductName(),
                            normalizeLoanLimitText(base.getLoanLimit()),
                            rateInfo,
                            "자세한 자격요건은 해당 은행에서 확인해주세요",
                            base.getJoinWay()
                    );
                    return new LoanProductDtoWithRate(dto, avgRate != null ? avgRate : 999.0);
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
            WolseLoanProductInfo info = findWolseLoanProduct("청년");
            int monthlyCap = info.getMonthlyLimitAmount();

            String depositLimitInfo = (deposit != null && deposit <= 4500)
                    ? "보증금 전액(" + deposit + "만원) "
                    : "보증금 최대 4,500만원" + (deposit != null ? " (초과분 " + (deposit - 4500) + "만원은 본인 부담)" : "");

            String monthlyLimitInfo = (monthlyRent != null && monthlyRent <= monthlyCap)
                    ? "월세 전액(" + monthlyRent + "만원)"
                    : "월 최대 " + monthlyCap + "만원" + (monthlyRent != null ? " (초과분 " + (monthlyRent - monthlyCap) + "만원은 본인 부담)" : "");

            return List.of(new LoanProductDto(
                    info.getCompanyName(),
                    info.getProductName(),
                    depositLimitInfo + "/" + monthlyLimitInfo,
                    info.getRateInfo(),
                    info.getTarget(),
                    info.getApplyMethod()
            ));
        }

        if (isSenior) {
            WolseLoanProductInfo info = findWolseLoanProduct("시니어");
            return List.of(new LoanProductDto(
                    info.getCompanyName(),
                    info.getProductName(),
                    info.getTotalLimitText(),
                    info.getRateInfo(),
                    info.getTarget(),
                    info.getApplyMethod()
            ));
        }

        return WOLSE_LOAN_PRODUCTS.stream()
                .filter(info -> "일반".equals(info.getType()))
                .map(info -> {
                    int monthlyLimit = info.getMonthlyLimitAmount();
                    String limitInfo = (monthlyRent != null && monthlyRent <= monthlyLimit)
                            ? "월세 전액(" + monthlyRent + "만원) "
                            : "월 최대 " + monthlyLimit + "만원" + (monthlyRent != null ? " (초과분 " + (monthlyRent - monthlyLimit) + "만원은 본인 부담)" : "");

                    return new LoanProductDto(
                            info.getCompanyName(),
                            info.getProductName(),
                            limitInfo,
                            info.getRateInfo(),
                            info.getTarget(),
                            info.getApplyMethod()
                    );
                })
                .collect(Collectors.toList());
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

        // "4억 44백만원" 같은 합산 표기 먼저 처리
        Matcher combined = Pattern.compile("(\\d+)\\s*억\\s*(\\d+)\\s*백만원").matcher(loanLimit);
        if(combined.find()){
            double eok = Integer.parseInt(combined.group(1)) + Integer.parseInt(combined.group(2)) / 100.0;
            String eokText = (eok == Math.floor(eok)) ? String.valueOf((int) eok) : String.valueOf(eok);
            return combined.replaceFirst(eokText + "억원");
        }

        Matcher matcher = Pattern.compile("([\\d.]+)\\s*백만원").matcher(loanLimit);
        if(!matcher.find()) return loanLimit;

        double eok = Double.parseDouble(matcher.group(1)) / 100.0;
        String eokText = (eok == Math.floor(eok))
                ? String.valueOf((int) eok)
                : String.valueOf(eok);

        return matcher.replaceFirst(eokText + "억원");
    }
}