<script setup>
import { onMounted, ref } from 'vue';
import AmenityFilter from './AmenityFilter.vue';
import FilterBottomBar from './FilterBottomBar.vue';
import FilterTabs from './FilterTabs.vue';
import OnBoardingFilter from './OnBoardingFilter.vue';
import OnboardingSummary from './OnboardingSummary.vue';
import onboardingApi from '@/api/onboardingApi';

const activeTab = ref('all');
const amenityFilterRef = ref(null);
const onboarding = ref(null);

const emit = defineEmits(['close', 'apply']);

const loadOnboardingSummary = async () => {
  try {
    onboarding.value = await onboardingApi.getOnboarding();
  } catch (error) {
    if (error.response?.status !== 404) {
      console.error('ONBOARDING SUMMARY LOAD ERROR: ', error);
    }
  }
};

onMounted(loadOnboardingSummary);

const handleReset = () => {
  activeTab.value = 'all';

  if (amenityFilterRef.value) {
    amenityFilterRef.value.resetFilters();
  }
};

const handleApply = () => {
  const amenities = amenityFilterRef.value ? amenityFilterRef.value.getFilters() : [];

  emit('apply', {
    // TODO: OnBoardingFilter가 필터 값을 expose하면 여기에서 수집해 전달한다.
    onboarding: {},
    amenities,
  });
  emit('close');
};
</script>

<template>
  <section class="filter-overlay">
    <div class="filter-panel">
      <div class="filter-content">
        <OnboardingSummary
          :destination="onboarding?.destination?.destName"
          :transport-mode="onboarding?.transportMode"
          :travel-time="onboarding?.maxTravelTime"
          :max-deposit="onboarding?.budgetDeposit"
          :max-rent="onboarding?.budgetRent"
          :min-safety-score="onboarding?.minSafetyScore"
          @close="$emit('close')"
        />

        <FilterTabs :active-tab="activeTab" @change="activeTab = $event" />

        <OnBoardingFilter v-show="activeTab === 'all'" />
        <AmenityFilter v-show="activeTab === 'amenity'" ref="amenityFilterRef" />
      </div>

      <FilterBottomBar @reset="handleReset" @apply="handleApply" />
    </div>
  </section>
</template>

<style scoped>
.filter-overlay {
  --map-header-height: 64px;
  position: fixed;
  top: var(--map-header-height);
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 50;
  overflow: hidden;
  background: #fff;
}

.filter-panel {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: calc(100dvh - var(--map-header-height));
  min-height: 0;
  overflow: hidden;
  background: #fff;
}

.filter-content {
  flex: 1;
  min-height: 0;
  padding: 24px 20px;
  overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
}
</style>
