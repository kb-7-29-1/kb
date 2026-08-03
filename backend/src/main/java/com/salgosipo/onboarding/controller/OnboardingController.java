package com.salgosipo.onboarding.controller;

import com.salgosipo.onboarding.dto.OnboardingDTO;
import com.salgosipo.onboarding.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {
    private final OnboardingService onboardingService;

    @PostMapping
    public ResponseEntity<Void> saveOnboarding(@RequestBody OnboardingDTO onboardingDTO) {
        onboardingService.saveOnboarding(1, onboardingDTO); // 임시 userId
        return ResponseEntity.ok().build();
    }
}
