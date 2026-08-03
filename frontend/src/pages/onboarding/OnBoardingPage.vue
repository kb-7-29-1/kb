<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import BudgetStep from '@/components/onboarding/BudgetStep.vue';
import CompleteStep from '@/components/onboarding/CompleteStep.vue';
import DestinationStep from '@/components/onboarding/DestinationStep.vue';
import OnboardingBottom from '@/components/onboarding/OnboardingBottom.vue';
import OnboardingHeader from '@/components/onboarding/OnboardingHeader.vue';
import SafetyStep from '@/components/onboarding/SafetyStep.vue';
import TransportStep from '@/components/onboarding/TransportStep.vue';

const ONBOARDING_DRAFT_KEY = 'salgosipo-onboarding-draft';

const defaultOnboardingData = {
  purpose: 'school',
  destination: null,
  transport: 'walk',
  deposit: 3000,
  monthlyRent: 70,
  safety: 'high',
};

const getSavedOnboardingData = () => {
  try {
    const savedData = localStorage.getItem(ONBOARDING_DRAFT_KEY);
    return savedData ? JSON.parse(savedData) : {};
  } catch (error) {
    console.warn('ONBOARDING DRAFT LOAD ERROR: ', error);
    return {};
  }
};

const getSavedStep = () => {
  const savedStep = Number(localStorage.getItem(`${ONBOARDING_DRAFT_KEY}-step`));
  return Number.isInteger(savedStep) && savedStep >= 1 && savedStep <= 5 ? savedStep : 1;
};

const currentStep = ref(getSavedStep());
const router = useRouter();

const onboardingData = reactive({
  ...defaultOnboardingData,
  ...getSavedOnboardingData(),
});

watch(
  onboardingData,
  (value) => {
    localStorage.setItem(ONBOARDING_DRAFT_KEY, JSON.stringify(value));
  },
  { deep: true },
);

watch(currentStep, (value) => {
  localStorage.setItem(`${ONBOARDING_DRAFT_KEY}-step`, String(value));
});

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

const setDestination = (destination) => {
  onboardingData.destination = destination;
};
</script>

<template>
  <main class="onboarding-page">
    <OnboardingHeader :current-step="currentStep" @back="goPrevious" />
    <section class="onboarding-content">
      <component
        :is="currentComponent"
        :selected-destination="onboardingData.destination"
        :purpose="onboardingData.purpose"
        :transport="onboardingData.transport"
        :deposit="onboardingData.deposit"
        :monthly-rent="onboardingData.monthlyRent"
        :safety="onboardingData.safety"
        @select-destination="setDestination"
        @update:purpose="onboardingData.purpose = $event"
        @update:transport="onboardingData.transport = $event"
        @update:deposit="onboardingData.deposit = $event"
        @update:monthly-rent="onboardingData.monthlyRent = $event"
        @update:safety="onboardingData.safety = $event"
      />
    </section>
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

.onboarding-content {
  flex: 1;
  padding: 24px 20px 110px;
}
</style>
