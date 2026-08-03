package com.salgosipo.onboarding.controller;

import com.salgosipo.global.security.account.domain.CustomUser;
import com.salgosipo.onboarding.dto.OnboardingDTO;
import com.salgosipo.onboarding.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ResponseEntity<Void> saveOnboarding(@AuthenticationPrincipal CustomUser customUser, @RequestBody OnboardingDTO onboardingDTO) {
        onboardingService.saveOnboarding(getUserId(customUser), onboardingDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<OnboardingDTO> getOnboarding(@AuthenticationPrincipal CustomUser customUser) {
        OnboardingDTO onboarding = onboardingService.getOnboarding(getUserId(customUser));

        if (onboarding == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(onboarding);
    }

    private Integer getUserId(CustomUser customUser) {
        return customUser.getUser().getUserId().intValue();
    }
}
