package com.salgosipo.onboarding.mapper;

import com.salgosipo.onboarding.domain.OnboardingVO;

public interface OnboardingMapper {
    // 온보딩 설정 저장
    int insertOnboarding(OnboardingVO onboarding);
}
