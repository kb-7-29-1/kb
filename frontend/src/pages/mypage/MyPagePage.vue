<script setup>
import { computed, onMounted, ref } from 'vue';
import onboardingApi from '@/api/onboardingApi';
import OnboardingPanel from '@/components/mypage/OnboardingPanel.vue';

const onboarding = ref(null);

const formatAmount = (amount) => {
  const value = Number(amount);

  if (!Number.isFinite(value)) return '설정 정보 없음';
  if (value >= 10000)
    return value % 10000 === 0 ? `${value / 10000}억원 이하` : `${value.toLocaleString()}만원 이하`;

  return `${value.toLocaleString()}만원 이하`;
};

const destination = computed(() => onboarding.value?.destination?.destName ?? '설정 정보 없음');
const transport = computed(() => {
  if (!onboarding.value) return '설정 정보 없음';

  const label = onboarding.value.transportMode === 'WALK' ? '도보' : '대중교통';
  return `${label} (최대 ${onboarding.value.maxTravelTime}분)`;
});
const deposit = computed(() => formatAmount(onboarding.value?.budgetDeposit));
const rent = computed(() => {
  const value = Number(onboarding.value?.budgetRent);
  if (!Number.isFinite(value)) return '설정 정보 없음';
  return value === 0 ? '전세' : `${value.toLocaleString()}만원 이하`;
});
const safety = computed(() => {
  const score = onboarding.value?.minSafetyScore;
  return Number.isFinite(Number(score)) ? `${score}점 이상` : '설정 정보 없음';
});

const loadOnboarding = async () => {
  try {
    onboarding.value = await onboardingApi.getOnboarding();
  } catch (error) {
    if (error.response?.status !== 404) {
      console.error('ONBOARDING GET ERROR: ', error);
    }
  }
};

onMounted(loadOnboarding);
</script>

<template>
  <main>
    <h1>마이페이지</h1>
    <RouterLink to="/map"> 지도로 돌아가기 </RouterLink>

    <OnboardingPanel
      :destination="destination"
      :transport="transport"
      :deposit="deposit"
      :rent="rent"
      :safety="safety"
    />
  </main>
</template>
