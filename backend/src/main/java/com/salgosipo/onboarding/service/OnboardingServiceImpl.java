package com.salgosipo.onboarding.service;

import com.salgosipo.destination.domain.DestinationVO;
import com.salgosipo.destination.dto.DestinationDTO;
import com.salgosipo.destination.mapper.DestinationMapper;
import com.salgosipo.onboarding.dto.OnboardingDTO;
import com.salgosipo.onboarding.mapper.OnboardingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class OnboardingServiceImpl implements OnboardingService {
    private final DestinationMapper destinationMapper;
    private final OnboardingMapper onboardingMapper;

    @Override
    @Transactional
    public void saveOnboarding(Integer userId, OnboardingDTO onboarding) {
        validateDestination(onboarding.getDestination());

        DestinationVO destination = onboarding.getDestination().toVO();
        destinationMapper.insertDestination(destination);

        onboarding.setUserId(userId);
        onboarding.setDestinationId(destination.getDestinationId());

        onboardingMapper.insertOnboarding(onboarding.toVO());
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingDTO getOnboarding(Integer userId) {
        return onboardingMapper.findByUserId(userId);
    }

    private void validateDestination(DestinationDTO destination) {
        if (destination == null
                || destination.getDestLatitude() == null
                || destination.getDestLongitude() == null
                || destination.getDestName() == null
                || destination.getDestName().isBlank()) {
            throw new IllegalArgumentException("목적지 정보가 올바르지 않습니다.");
        }
    }
}
