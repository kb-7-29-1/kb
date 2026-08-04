package com.salgosipo.loan.service;

import com.salgosipo.loan.client.LoanApiClient;
import com.salgosipo.loan.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
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
        List<LoanProductDto> loans = isJeonse ? getJeonseLoans(age) : getWolseLoans(age);

        if(loans.isEmpty() || deposit == null){
            return null;
        }

        LoanProductDto best = loans.get(0);
        Double ratio = extractLoanRatio(best.getLoanLimit());
        if(ratio == null){
            // 비율 정보가 없으면 계산 없이, 상품 정보만 응답
            return new LoanRecommendationDto(
                    best.getProductName(),
                    best.getCompanyName(),
                    best.getRateInfo(),
                    best.getLoanLimit(),
                    0,  // 계산 불가 표시
                    0,
                    0
            );
        }

        int expectedLoanAmount = (int) Math.round(deposit * ratio);
        int maxSearchAmount = deposit + expectedLoanAmount;

        return new LoanRecommendationDto(
                best.getProductName(),
                best.getCompanyName(),
                best.getRateInfo(),
                best.getLoanLimit(),
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
                            base.getLoanLimit(),
                            rateInfo,
                            null,
                            base.getJoinWay()
                    );
                    return new LoanProductDtoWithRate(dto,minRate != null ? minRate : 999.0);
                })
                .sorted(Comparator.comparing((LoanProductDtoWithRate p) -> !p.getDto().getCompanyName().contains("국민은행"))
                        .thenComparing(LoanProductDtoWithRate::getRate))
                .map(LoanProductDtoWithRate::getDto)
                .collect(Collectors.toList());
    }

    // 월세 (서민금융진흥원 API)
    private List<LoanProductDto> getWolseLoans(Integer age) {
        MinfundLoanApiResponse response = loanApiClient.fetchMinfundLoans();
        List<MinfundLoanItem> allItems = response.getBody().getItems().getItem();

        if (allItems == null) return List.of();

        long count = allItems.stream()
                .filter(item -> item.getUsage() != null)
                .filter(item -> item.getUsage().contains("보증금") || item.getUsage().contains("전세") || item.getUsage().contains("월세"))
                .count();
        System.out.println("월세 관련 상품 수: " + count);

        List<LoanProductDto> result = allItems.stream()
                .filter(item -> item.getUsage() != null &&
                        (item.getUsage().contains("보증금") || item.getUsage().contains("전세") || item.getUsage().contains("월세")))
                .filter(item -> {
                    if (age == null) return true;

                    boolean matches39 = "1".equals(item.getAge39blw());
                    boolean matches60 = "1".equals(item.getAge60abnml());

                    if(!matches39 && !matches60) return true;

                    if (age <= 39 && matches39) return true;
                    if (age >= 60 && matches60) return true;

                    return false;
                })
                .sorted(Comparator.comparing(item -> extractFirstRate(item.getRate())))
                .map(item -> new LoanProductDto(
                        item.getInstitution(),
                        item.getProductName(),
                        item.getLoanLimit(),
                        item.getRate(),
                        item.getTarget(),
                        item.getApplyMethod()
                ))
                .collect(Collectors.toList());

        if (result.isEmpty()) {
            boolean isYouth = age != null && age <= 34;

            result = List.of(new LoanProductDto(
                    "주택도시기금 (KB국민은행 등 수탁은행 취급)",
                    isYouth ? "청년전용 보증부 월세대출" : "주거안정 월세대출",
                    isYouth ? "임차보증금 5천만원 이하" : "총 1,440만원 (최대 월세 60만원까지 지원)",
                    isYouth ? "연 1.0%~2.0% (국토교통부 고시 변동금리)" : "연 1.3%~1.8%",
                    isYouth ? "만 19~34세 청년, 무주택 세대주" : "저소득 무주택 세대주",
                    "기금e든든(enhuf.molit.go.kr) 또는 KB국민은행 등 방문"
            ));
        }
        System.out.println("필터링 후 result 크기: " + result.size());
        System.out.println("age: " + age);
        return result;

    }

    // 문자열 금리에서 첫 번째 숫자만 추출 (정렬용)
    private double extractFirstRate(String rateText) {
        if (rateText == null) return 999.0;
        Matcher matcher = Pattern.compile("(\\d+(\\.\\d+)?)").matcher(rateText);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 999.0;
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
}