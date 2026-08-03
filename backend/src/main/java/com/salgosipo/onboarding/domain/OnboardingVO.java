package com.salgosipo.onboarding.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingVO {
    private Integer userId;
    private Integer destinationId;
    private String destinationType;
    private String transportMode;
    private Integer maxTravelTime;
    private Integer budgetDeposit;
    private Integer budgetRent;
    private Integer minSafetyScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
