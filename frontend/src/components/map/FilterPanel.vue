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
  appliedAmenityFilters: {
    type: Array,
    default: () => [],
  },
});

const activeTab = ref('all');
const amenityFilterRef = ref(null);
const onboardingFilterRef = ref(null);
const onboarding = ref(null);
const applyError = ref('');

const emit = defineEmits(['close', 'apply-onboarding', 'apply-amenities', 'reset']);

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
  emit('reset');

  await loadOnboardingSummary();

  if (onboardingFilterRef.value) {
    onboardingFilterRef.value.resetFilters();
  }

  if (amenityFilterRef.value) {
    amenityFilterRef.value.resetFilters();
  }
};

const handleApply = async () => {
  applyError.value = '';
  const selectedAmenities = amenityFilterRef.value?.getFilters?.();
  const amenities = Array.isArray(selectedAmenities) ? selectedAmenities : [];

  if (activeTab.value === 'amenity') {
    emit('apply-amenities', amenities);
    emit('close');
    return;
  }

  const filters = onboardingFilterRef.value ? onboardingFilterRef.value.getFilters() : {};
  const { selectedDestination, ...onboardingFilters } = filters;

  try {
    if (selectedDestination) {
      await onboardingApi.saveDestination(selectedDestination);
    }

    emit('apply-onboarding', onboardingFilters);
    emit('close');
  } catch (error) {
    applyError.value = '목적지를 저장하지 못했어요. 잠시 후 다시 시도해 주세요.';
    console.error('FILTER DESTINATION SAVE ERROR: ', error);
  }
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
        <AmenityFilter
          v-show="activeTab === 'amenity'"
          ref="amenityFilterRef"
          :applied-filters="props.appliedAmenityFilters"
        />
      </div>

      <p v-if="applyError" class="apply-error">{{ applyError }}</p>

      <FilterBottomBar @reset="handleReset" @apply="handleApply" />
    </div>
  </section>
</template>

<style scoped>
.filter-overlay {
  --map-header-height: max(100px, calc(env(safe-area-inset-top) + 62px));
  position: fixed;
  top: var(--map-header-height);
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 60;
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
.apply-error {
  margin: 0;
  padding: 8px 20px;
  background: #fff1f2;
  color: #dc2626;
  font-size: 12px;
  text-align: center;
}

@media (min-width: 768px) {
  .filter-overlay {
    --map-header-height: 56px;
  }
}

.filter-content {
  flex: 1;
  min-height: 0;
  padding: 12px 20px 24px;
  overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
}
</style>
