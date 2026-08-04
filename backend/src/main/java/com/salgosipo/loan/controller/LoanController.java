package com.salgosipo.loan.controller;

import com.salgosipo.loan.dto.LoanProductDto;
import com.salgosipo.loan.dto.LoanRecommendationDto;
import com.salgosipo.loan.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loan")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @GetMapping("/onboarding-recommend")
    public ResponseEntity<LoanRecommendationDto> getOnboardingLoan(
            @RequestParam Integer deposit,
            @RequestParam(required = false) Integer monthlyRent,
            @RequestParam(required = false) Integer age) {
        return ResponseEntity.ok(loanService.getOnboardingRecommendation(deposit, monthlyRent, age));
    }
}