<script setup>
defineProps({
  activeTab: {
    type: String,
    required: true,
  },
});

defineEmits(['change']);
</script>

<template>
  <nav class="filter-tabs" aria-label="필터 종류">
    <button type="button" :class="{ active: activeTab === 'all' }" @click="$emit('change', 'all')">
      전체 필터
    </button>
    <button
      type="button"
      :class="{ active: activeTab === 'amenity' }"
      @click="$emit('change', 'amenity')"
    >
      편의시설 필터
    </button>
  </nav>
</template>

<style scoped>
.filter-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 4px;
  border-radius: 12px;
  background: #f2f4f8;
}

.filter-tabs button {
  min-height: 38px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: #7d8797;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.filter-tabs button.active {
  background: #fff;
  color: #3d55f6;
  box-shadow: 0 1px 4px rgb(15 23 42 / 8%);
}
</style>
