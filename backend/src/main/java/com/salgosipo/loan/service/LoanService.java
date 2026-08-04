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
        List<LoanProductDto> loans = isJeonse ? getJeonseLoans(age) : getWolseLoans(age, deposit, monthlyRent);

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
                    best.getTarget(),
                    0,
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
                best.getTarget(),
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

    // 월세
    private List<LoanProductDto> getWolseLoans(Integer age, Integer deposit, Integer monthlyRent) {
        boolean isYouth = age != null && age <= 34;
        boolean isSenior = age != null && age >= 60;
        boolean fitsYouthLoan = deposit != null && deposit <= 5000;
        boolean fitsSeniorLoan = deposit != null && deposit <= 1000;

        if (isYouth && fitsYouthLoan) {
            return List.of(new LoanProductDto(
                    "주택도시기금 (KB국민은행 등 수탁은행 취급)",
                    "청년전용 보증부 월세대출",
                    "임차보증금 5천만원 이하",
                    "연 1.0%~2.0% (국토교통부 고시 변동금리)",
                    "만 19~34세 청년, 무주택 세대주",
                    "기금e든든(enhuf.molit.go.kr) 또는 KB국민은행 등 방문"
            ));
        }

        if (isSenior && fitsSeniorLoan) {
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
                    ? "월세 전액(" + monthlyRent + "만원) 지원 가능"
                    : "월 최대 60만원까지 지원" + (monthlyRent != null ? " (초과분 " + (monthlyRent - 60) + "만원은 본인 부담)" : "");

        return List.of(new LoanProductDto(
                "주택도시기금 (KB국민은행 등 수탁은행 취급)",
                "주거안정 월세대출",
                limitInfo,
                "연 1.3%~1.8%",
                "저소득 무주택 세대주",
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
}