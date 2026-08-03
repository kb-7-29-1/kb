<script setup>
import { onMounted, ref } from 'vue';
import AmenityFilter from './AmenityFilter.vue';
import FilterBottomBar from './FilterBottomBar.vue';
import FilterTabs from './FilterTabs.vue';
import OnBoardingFilter from './OnBoardingFilter.vue';
import OnboardingSummary from './OnboardingSummary.vue';
import onboardingApi from '@/api/onboardingApi';

const props = defineProps({
  appliedFilters: {
    type: Object,
    default: null,
  },
});

const activeTab = ref('all');
const amenityFilterRef = ref(null);
const onboardingFilterRef = ref(null);
const onboarding = ref(null);

const emit = defineEmits(['close', 'apply', 'reset']);

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

const handleReset = async () => {
  activeTab.value = 'all';
  emit('reset');

  await loadOnboardingSummary();

  if (onboardingFilterRef.value) {
    onboardingFilterRef.value.resetFilters();
  }

  if (amenityFilterRef.value) {
    amenityFilterRef.value.resetFilters();
  }
};

const handleApply = () => {
  const amenities = amenityFilterRef.value?.getFilters?.() ?? [];
  const onboardingFilters = onboardingFilterRef.value ? onboardingFilterRef.value.getFilters() : {};

  emit('apply', {
    onboarding: onboardingFilters,
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
          :destination="
            props.appliedFilters?.destination?.destName ?? onboarding?.destination?.destName
          "
          :transport-mode="props.appliedFilters?.transportMode ?? onboarding?.transportMode"
          :travel-time="props.appliedFilters?.maxTravelTime ?? onboarding?.maxTravelTime"
          :max-deposit="props.appliedFilters?.budgetDeposit ?? onboarding?.budgetDeposit"
          :max-rent="props.appliedFilters?.budgetRent ?? onboarding?.budgetRent"
          :min-safety-score="props.appliedFilters?.minSafetyScore ?? onboarding?.minSafetyScore"
          @close="$emit('close')"
        />

        <FilterTabs :active-tab="activeTab" @change="activeTab = $event" />

        <OnBoardingFilter
          v-show="activeTab === 'all'"
          ref="onboardingFilterRef"
          :onboarding="onboarding"
          :applied-filters="props.appliedFilters"
        />
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
