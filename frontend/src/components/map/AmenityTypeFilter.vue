<template>
  <section class="amenity-type-filter">
    <div class="amenity-grid">
      <button
        v-for="item in amenities"
        :key="item.id"
        type="button"
        class="amenity-btn"
        :class="{ selected: item.selected }"
        @click="$emit('toggle', item.id)"
      >
        <span class="icon">{{ item.icon }}</span>
        <span class="name">{{ item.name }}</span>
      </button>

      <button
        type="button"
        class="amenity-btn reset-btn"
        @click="$emit('reset')"
      >
        <span class="icon">🔄</span>
        <span class="name">초기화</span>
      </button>
    </div>
  </section>
</template>

<script setup>
defineProps({
  amenities: {
    type: Array,
    required: true,
  },
});

defineEmits(['toggle', 'reset']);
</script>

<style scoped>
.amenity-type-filter {
  padding: 10px 0 20px;
  border-bottom: 1px solid #edf0f5;
}

.amenity-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.amenity-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px 0;
  background-color: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.icon {
  margin-bottom: 4px;
  font-size: 20px;
}

.name {
  color: #888;
  font-size: 11px;
  font-weight: 500;
}

.amenity-btn.selected {
  background-color: #eef1ff;
  border-color: #3d55f6;
}

.amenity-btn.selected .name {
  color: #3b5bdb;
  font-weight: 600;
}
</style>
