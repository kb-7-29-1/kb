<script setup>
import { computed, ref } from 'vue';
import BudgetStep from '@/components/onboarding/BudgetStep.vue';
import CompleteStep from '@/components/onboarding/CompleteStep.vue';
import DestinationStep from '@/components/onboarding/DestinationStep.vue';
import OnboardingBottom from '@/components/onboarding/OnboardingBottom.vue';
import OnboardingHeader from '@/components/onboarding/OnboardingHeader.vue';
import SafetyStep from '@/components/onboarding/SafetyStep.vue';
import TransportStep from '@/components/onboarding/TransportStep.vue';

const currentStep = ref(1);

const currentComponent = computed(() => {
  const steps = {
    1: DestinationStep,
    2: TransportStep,
    3: BudgetStep,
    4: SafetyStep,
    5: CompleteStep,
  };

  return steps[currentStep.value];
});

const goPrevious = () => {
  if (currentStep.value > 1) currentStep.value -= 1;
};

const goNext = () => {
  if (currentStep.value < 5) currentStep.value += 1;
};
</script>

<template>
  <main>
    <OnboardingHeader :current-step="currentStep" />
    <component :is="currentComponent" />
    <OnboardingBottom :current-step="currentStep" @previous="goPrevious" @next="goNext" />
  </main>
</template>
