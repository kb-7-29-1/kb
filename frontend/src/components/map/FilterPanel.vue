<script setup>
import { ref } from 'vue';
import AmenityFilter from './AmenityFilter.vue';
import FilterBottomBar from './FilterBottomBar.vue';
import FilterTabs from './FilterTabs.vue';
import OnBoardingFilter from './OnBoardingFilter.vue';
import OnboardingSummary from './OnboardingSummary.vue';

const activeTab = ref('all');
const amenityFilterRef = ref(null);

defineEmits(['close']);

const handleReset = () => {
  if (amenityFilterRef.value) {
    amenityFilterRef.value.resetFilters();
  }
};

const handleApply = () => {
  if (amenityFilterRef.value) {
    amenityFilterRef.value.applyFilters();
  }
};
</script>

<template>
  <section class="filter-overlay">
    <div class="filter-panel">
      <div class="filter-content">
        <OnboardingSummary @close="$emit('close')" />

        <FilterTabs :active-tab="activeTab" @change="activeTab = $event" />

        <OnBoardingFilter v-if="activeTab === 'all'" />
        <AmenityFilter
            v-else
            ref="amenityFilterRef"
            @apply="handleAmenityResult"
        />
      </div>

      <FilterBottomBar
          @reset="handleReset"
          @apply="handleApply"
      />
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
  z-index: 10;
  background: #fff;
}

.filter-panel {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: calc(100dvh - var(--map-header-height));
  background: #fff;
}

.filter-content {
  flex: 1;
  min-height: 0;
  padding: 24px 20px;
  overflow-y: auto;
}
</style>
