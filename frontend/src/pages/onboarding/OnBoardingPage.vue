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
import { useAuthStore } from '@/stores/useAuthStore';
import { useAppToast } from '@/composables/useAppToast';
import { getOnboardingStorageKeys } from '@/utils/onboardingStorage';
import { useMapUrlSync } from '@/composables/useMapUrlSync';

const authStore = useAuthStore();
const { showToast } = useAppToast();
const { saveQuickFilterToCache, loadQuickFilterFromCache } = useMapUrlSync();
const getStorageKeys = () => getOnboardingStorageKeys(authStore.user);

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
    const storageKeys = getStorageKeys();
    if (!storageKeys) return {};

    const savedData = localStorage.getItem(storageKeys.draft);
    return savedData ? JSON.parse(savedData) : {};
  } catch (error) {
    console.warn('ONBOARDING DRAFT LOAD ERROR: ', error);
    return {};
  }
};

const getSavedStep = () => {
  const storageKeys = getStorageKeys();
  if (!storageKeys) return 1;

  const savedStep = Number(localStorage.getItem(storageKeys.step));
  return Number.isInteger(savedStep) && savedStep >= 1 && savedStep <= 5 ? savedStep : 1;
};

const router = useRouter();
const route = useRoute();
const isEditingFromMyPage = route.query.from === 'mypage';
const currentStep = ref(isEditingFromMyPage ? 1 : getSavedStep());
const isSaving = ref(false);
const stepDirection = ref('forward');

if (isEditingFromMyPage) {
  const storageKeys = getStorageKeys();
  if (storageKeys) localStorage.setItem(storageKeys.step, '1');
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
    const storageKeys = getStorageKeys();
    if (storageKeys) localStorage.setItem(storageKeys.draft, JSON.stringify(value));
  },
  { deep: true },
);

watch(currentStep, (value) => {
  const storageKeys = getStorageKeys();
  if (storageKeys) localStorage.setItem(storageKeys.step, String(value));
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
  const storageKeys = getStorageKeys();
  if (!storageKeys) return;

  localStorage.removeItem(storageKeys.draft);
  localStorage.removeItem(storageKeys.step);
};

const goMap = async () => {
  if (isSaving.value) return;

  if (!onboardingData.destination) {
    showToast('목적지를 선택해 주세요.', { type: 'warning' });
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
    const storageKeys = getStorageKeys();
    if (storageKeys) localStorage.setItem(storageKeys.result, JSON.stringify(requestData));
    await onboardingApi.saveOnboarding(requestData);
    clearOnboardingDraft();

    // 지도 페이지의 로컬 퀵필터 캐시가 목적지보다 우선 적용되므로,
    // 방금 저장한 새 목적지를 캐시에도 반영해 즉시 반영되게 함 (다른 캐시 필드는 유지)
    const dest = onboardingData.destination;
    if (dest) {
      saveQuickFilterToCache({
        ...(loadQuickFilterFromCache() || {}),
        destinationId: dest.destinationId ?? null,
        destination: dest.destName,
        destinationAddress: dest.destAddress || '',
        destinationLat: Number(dest.destLatitude),
        destinationLng: Number(dest.destLongitude),
      });
    }

    router.push('/home');
  } catch (error) {
    console.error('ONBOARDING SAVE ERROR: ', error);
    showToast('설정 저장에 실패했어요. 잠시 후 다시 시도해 주세요.', { type: 'error' });
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
  min-height: 100%;
  flex: 1;
  background: #f8fafc;
  animation: onboarding-page-enter 0.22s ease-out;
}

@keyframes onboarding-page-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.onboarding-workspace,
.onboarding-flow {
  min-width: 0;
}

.onboarding-flow {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 100%;
  flex: 1;
}

.onboarding-content {
  flex: 1;
  min-height: 0;
  padding: 24px 20px 110px;
  overflow-x: clip;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
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
    width: 100%;
    min-height: 100vh;
    margin: 0;
    background: #f8fafc;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow-y: auto;
  }

  .onboarding-workspace {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    min-height: 100vh;
    padding: 32px 28px;
    box-sizing: border-box;
    margin: 0 auto;
  }

  .onboarding-flow {
    width: min(100%, 460px);
    max-width: 460px;
    height: auto;
    min-height: 0;
    margin: 0 auto;
  }

  .onboarding-content {
    flex: 0 0 auto;
    padding: 16px 0 12px;
    margin: 10px 0px;
    overflow-y: visible;
  }

  @media (max-height: 760px) {
    .onboarding-workspace {
      align-items: flex-start;
    }
  }
}
</style>
