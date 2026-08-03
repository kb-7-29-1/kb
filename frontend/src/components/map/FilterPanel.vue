<script setup>
import { ref } from 'vue';
import AmenityFilter from './AmenityFilter.vue';
import FilterBottomBar from './FilterBottomBar.vue';
import FilterTabs from './FilterTabs.vue';
import OnBoardingFilter from './OnBoardingFilter.vue';
import OnboardingSummary from './OnboardingSummary.vue';

const activeTab = ref('all');

defineEmits(['close']);
</script>

<template>
  <section class="filter-panel">
    <header>
      <h1>필터</h1>
      <button type="button" @click="$emit('close')">닫기</button>
    </header>

    <OnboardingSummary />

    <FilterTabs :active-tab="activeTab" @change="activeTab = $event" />

    <OnBoardingFilter v-if="activeTab === 'all'" />
    <AmenityFilter v-else />

    <FilterBottomBar />
  </section>
</template>

<style scoped>
.filter-panel {
  position: fixed;
  inset: 0;
  z-index: 10;
  background: #fff;
}
</style>
