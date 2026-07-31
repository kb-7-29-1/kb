<script setup>
import { ref } from 'vue';
import FilterPanel from '@/components/map/FilterPanel.vue';

const isFilterOpen = ref(false);
</script>

<template>
  <main>
    <h1>지도</h1>
    <button type="button" @click="isFilterOpen = true">필터</button>

    <FilterPanel v-if="isFilterOpen" @close="isFilterOpen = false" />
  </main>
</template>
