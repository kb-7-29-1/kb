<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BudgetStep from '@/components/onboarding/BudgetStep.vue';
import CompleteStep from '@/components/onboarding/CompleteStep.vue';
import DestinationStep from '@/components/onboarding/DestinationStep.vue';
import OnboardingBottom from '@/components/onboarding/OnboardingBottom.vue';
import OnboardingHeader from '@/components/onboarding/OnboardingHeader.vue';
import SafetyStep from '@/components/onboarding/SafetyStep.vue';
import TransportStep from '@/components/onboarding/TransportStep.vue';
import onboardingApi from '@/api/onboardingApi';

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

const router = useRouter();
const route = useRoute();
const isEditingFromMyPage = route.query.from === 'mypage';
const currentStep = ref(isEditingFromMyPage ? 1 : getSavedStep());
const isSaving = ref(false);
const stepDirection = ref('forward');

if (isEditingFromMyPage) {
  localStorage.setItem(`${ONBOARDING_DRAFT_KEY}-step`, '1');
}

const returnPath = computed(() => (route.query.from === 'mypage' ? '/mypage' : '/'));
const returnLabel = computed(() =>
  route.query.from === 'mypage' ? '마이페이지로' : '로그인 페이지로',
);

const onboardingData = reactive({
  ...defaultOnboardingData,
  ...getSavedOnboardingData(),
});

const loadSavedOnboarding = async () => {
  if (!isEditingFromMyPage) return;

  try {
    const savedOnboarding = await onboardingApi.getOnboarding();

    Object.assign(onboardingData, {
      purpose: savedOnboarding.destinationType?.toLowerCase() ?? defaultOnboardingData.purpose,
      destination: savedOnboarding.destination ?? null,
      transport: savedOnboarding.transportMode?.toLowerCase() ?? defaultOnboardingData.transport,
      deposit: Number(savedOnboarding.budgetDeposit),
      monthlyRent: Number(savedOnboarding.budgetRent),
      safety: Number(savedOnboarding.minSafetyScore) >= 80 ? 'high' : 'normal',
    });
  } catch (error) {
    console.error('ONBOARDING LOAD ERROR: ', error);
  }
};

onMounted(loadSavedOnboarding);

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
    stepDirection.value = 'backward';
    currentStep.value -= 1;
    return;
  }

  router.push(returnPath.value);
};

const goReturn = () => {
  router.push(returnPath.value);
};

const goNext = () => {
  if (currentStep.value < 5) {
    stepDirection.value = 'forward';
    currentStep.value += 1;
  }
};

const goLogin = () => {
  router.push(returnPath.value);
};

const clearOnboardingDraft = () => {
  localStorage.removeItem(ONBOARDING_DRAFT_KEY);
  localStorage.removeItem(`${ONBOARDING_DRAFT_KEY}-step`);
};

const goMap = async () => {
  if (isSaving.value) return;

  if (!onboardingData.destination) {
    alert('목적지를 선택해 주세요.');
    currentStep.value = 1;
    return;
  }

  const requestData = {
    destination: onboardingData.destination,
    destinationType: onboardingData.purpose.toUpperCase(),
    transportMode: onboardingData.transport.toUpperCase(),
    maxTravelTime: 15,
    budgetDeposit: onboardingData.deposit,
    budgetRent: onboardingData.monthlyRent,
    minSafetyScore: onboardingData.safety === 'high' ? 80 : 60,
  };

  isSaving.value = true;

  try {
    localStorage.setItem('salgosipo-onboarding-result', JSON.stringify(requestData));
    await onboardingApi.saveOnboarding(requestData);
    clearOnboardingDraft();
    router.push('/home');
  } catch (error) {
    console.error('ONBOARDING SAVE ERROR: ', error);
    alert('설정 저장에 실패했어요. 잠시 후 다시 시도해 주세요.');
  } finally {
    isSaving.value = false;
  }
};

const setDestination = (destination) => {
  onboardingData.destination = destination;
};
</script>

<template>
  <main class="onboarding-page">
    <div class="onboarding-workspace">
      <div class="onboarding-flow">
        <OnboardingHeader
          :current-step="currentStep"
          :return-label="returnLabel"
          @back="goReturn"
          @go-login="goLogin"
        />
        <section class="onboarding-content">
          <Transition :name="`step-${stepDirection}`" mode="out-in">
            <component
              :is="currentComponent"
              :key="currentStep"
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
          </Transition>
        </section>
        <OnboardingBottom
          :current-step="currentStep"
          :is-saving="isSaving"
          @previous="goPrevious"
          @next="goNext"
          @complete="goMap"
        />
      </div>
    </div>
  </main>
</template>

<style scoped>
.onboarding-page {
  min-height: 100dvh;
  background: #f8fafc;
}

.onboarding-workspace,
.onboarding-flow {
  min-width: 0;
}

.onboarding-flow {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
}

.onboarding-content {
  flex: 1;
  padding: 24px 20px 110px;
  overflow-x: clip;
}

.step-forward-enter-active,
.step-forward-leave-active,
.step-backward-enter-active,
.step-backward-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.step-forward-enter-from,
.step-backward-leave-to {
  opacity: 0;
  transform: translateX(18px);
}

.step-forward-leave-to,
.step-backward-enter-from {
  opacity: 0;
  transform: translateX(-18px);
}

@media (min-width: 768px) {
  .onboarding-page {
    min-height: calc(100vh - 32px);
    margin: 16px;
    background: #f8fafc;
  }

  .onboarding-workspace {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: calc(100vh - 32px);
    padding: 32px 28px;
    box-sizing: border-box;
  }

  .onboarding-flow {
    width: min(100%, 460px);
    min-height: 0;
  }

  .onboarding-content {
    flex: 0 0 auto;
    padding: 16px 0 12px;
    margin: 10px 0px;
  }

  @media (max-height: 760px) {
    .onboarding-workspace {
      align-items: flex-start;
    }
  }
}
</style>
