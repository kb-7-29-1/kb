package com.salgosipo.onboarding.service;

import com.salgosipo.onboarding.dto.OnboardingDTO;

public interface OnboardingService {
    // 온보딩 저장
    void saveOnboarding(Integer userId, OnboardingDTO onboarding);

    // 사용자 온보딩 조회
    OnboardingDTO getOnboarding(Integer userId);
}
