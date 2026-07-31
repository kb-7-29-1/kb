<script setup>
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import BudgetStep from '@/components/onboarding/BudgetStep.vue';
import CompleteStep from '@/components/onboarding/CompleteStep.vue';
import DestinationStep from '@/components/onboarding/DestinationStep.vue';
import OnboardingBottom from '@/components/onboarding/OnboardingBottom.vue';
import OnboardingHeader from '@/components/onboarding/OnboardingHeader.vue';
import SafetyStep from '@/components/onboarding/SafetyStep.vue';
import TransportStep from '@/components/onboarding/TransportStep.vue';

const currentStep = ref(1);
const router = useRouter();

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
  if (currentStep.value > 1) {
    currentStep.value -= 1;
    return;
  }

  router.push('/');
};

const goNext = () => {
  if (currentStep.value < 5) currentStep.value += 1;
};

const goMap = () => {
  router.push('/map');
};
</script>

<template>
  <main class="onboarding-page">
    <OnboardingHeader :current-step="currentStep" @back="goPrevious" />
    <component :is="currentComponent" />
    <OnboardingBottom
      :current-step="currentStep"
      @previous="goPrevious"
      @next="goNext"
      @complete="goMap"
    />
  </main>
</template>

<style scoped>
.onboarding-page {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
  background: #f8fafc;
}
</style>
