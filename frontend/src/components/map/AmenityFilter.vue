<template>
  <div class="filter-container">
    <div class="filter-content">
      <AmenityTypeFilter
        :amenities="amenities"
        @toggle="toggleAmenity"
      />
      <p v-if="showGuideText" class="amenity-guide-text">
        ⚠ 선택한 조건이 모두 반영되어 검색 결과가 다소 적을 수 있어요
      </p>
      <AmenityWalkingTimeFilter
        v-if="showWalkingTime"
        :amenities="selectedAmenities"
        @update-time-limit="updateTimeLimit"
      />
    </div>

    <button type="button" class="apply-button" @click="applyFilters">적용하기</button>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import AmenityTypeFilter from './AmenityTypeFilter.vue';
import AmenityWalkingTimeFilter from './AmenityWalkingTimeFilter.vue';

const emit = defineEmits(['close', 'apply', 'selection-change']);
const props = defineProps({
  appliedFilters: {
    type: Array,
    default: () => [],
  },
  showWalkingTime: {
    type: Boolean,
    default: true,
  },
  showGuideText: {
    type: Boolean,
    default: false,
  },
});

const DEFAULT_CONVENIENCE_TIME = 5;
const DEFAULT_AMENITY_TIME = 15;

const getDefaultTimeLimit = (amenityType) =>
  amenityType === 1 ? DEFAULT_CONVENIENCE_TIME : DEFAULT_AMENITY_TIME;

const amenities = ref([
  { id: 'convenience', amenityType: 1, name: '편의점', icon: '🏪', selected: false, timeLimit: 5 },
  { id: 'cafe', amenityType: 2, name: '카페', icon: '☕', selected: false, timeLimit: 15 },
  { id: 'laundry', amenityType: 3, name: '코인세탁소', icon: '🧺', selected: false, timeLimit: 15 },
  { id: 'fastfood', amenityType: 4, name: '패스트푸드', icon: '🍔', selected: false, timeLimit: 15 },
  { id: 'daiso', amenityType: 5, name: '다이소', icon: '🛒', selected: false, timeLimit: 15 },
  { id: 'oliveyoung', amenityType: 6, name: '올리브영', icon: '💄', selected: false, timeLimit: 15 },
  { id: 'mart', amenityType: 7, name: '대형마트', icon: '🏢', selected: false, timeLimit: 15 },
]);

const selectedAmenities = computed(() => amenities.value.filter((item) => item.selected));

const toggleAmenity = (id) => {
  const amenity = amenities.value.find((item) => item.id === id);
  if (amenity) {
    amenity.selected = !amenity.selected;
    emit('selection-change', getSelectedAmenities());
  }
};

const updateTimeLimit = ({ id, timeLimit }) => {
  const amenity = amenities.value.find((item) => item.id === id);
  if (amenity) amenity.timeLimit = timeLimit;
};

const resetFilters = () => {
  amenities.value.forEach((item) => {
    item.selected = false;
    item.timeLimit = getDefaultTimeLimit(item.amenityType);
  });
};

const restoreAppliedFilters = (filters = []) => {
  amenities.value.forEach((item) => {
    const applied = filters.find((filter) => Number(filter.amenityType) === item.amenityType);
    item.selected = Boolean(applied);
    item.timeLimit = applied
      ? Number(applied.walkTimeMinutes ?? getDefaultTimeLimit(item.amenityType))
      : getDefaultTimeLimit(item.amenityType);
  });
};

watch(() => props.appliedFilters, restoreAppliedFilters, { immediate: true, deep: true });

const getFilters = () =>
  selectedAmenities.value.map((item) => ({
    amenityType: item.amenityType,
    walkTimeMinutes: Number(item.timeLimit),
  }));

const getSelectedAmenities = () => selectedAmenities.value.map((item) => ({ ...item }));

const applyFilters = () => {
  emit('apply', getFilters());
};

defineExpose({
  resetFilters,
  applyFilters,
  getFilters,
  getSelectedAmenities,
});
</script>

<style scoped>
.filter-container {
  display: block;
}

.filter-content {
  padding: 10px 0 30px;
  overflow: visible;
}

.apply-button {
  display: none;
}

.amenity-guide-text {
  margin: 12px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}
</style>
