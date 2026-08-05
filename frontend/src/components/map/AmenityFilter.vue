<template>
  <div class="filter-container">
    <div class="filter-content">
      <AmenityTypeFilter :amenities="amenities" @toggle="toggleAmenity"/>
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
import {ref, computed, watch} from 'vue';
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
});

// 편의시설 데이터 상태 관리
const amenities = ref([
  {id: 'convenience', amenityType: 1, name: '편의점', icon: '🏪', selected: false, timeLimit: 15},
  {id: 'cafe', amenityType: 2, name: '카페', icon: '☕', selected: false, timeLimit: 15},
  {id: 'laundry', amenityType: 3, name: '코인세탁소', icon: '🧺', selected: false, timeLimit: 15},
  {
    id: 'fastfood',
    amenityType: 4,
    name: '패스트푸드',
    icon: '🍔',
    selected: false,
    timeLimit: 15,
  },
  {id: 'daiso', amenityType: 5, name: '다이소', icon: '🛒', selected: false, timeLimit: 15},
  {
    id: 'oliveyoung',
    amenityType: 6,
    name: '올리브영',
    icon: '💄',
    selected: false,
    timeLimit: 15,
  },
  {id: 'mart', amenityType: 7, name: '대형마트', icon: '🏢', selected: false, timeLimit: 15},
]);

// 선택된 편의시설만 필터링
const selectedAmenities = computed(() => {
  return amenities.value.filter((item) => item.selected);
});

// 편의시설 선택 토글 함수
const toggleAmenity = (id) => {
  const amenity = amenities.value.find((item) => item.id === id);
  if (amenity) {
    amenity.selected = !amenity.selected;
    emit('selection-change', getSelectedAmenities());
  }
};

const updateTimeLimit = ({id, timeLimit}) => {
  const amenity = amenities.value.find((item) => item.id === id);
  if (amenity) amenity.timeLimit = timeLimit;
};

// 초기화 버튼 함수
const resetFilters = () => {
  amenities.value.forEach((item) => {
    item.selected = false;
    item.timeLimit = 15;
  });
};

// 필터 패널을 닫았다 다시 열어도 마지막 적용값을 그대로 복원합니다.
const restoreAppliedFilters = (filters = []) => {
  amenities.value.forEach((item) => {
    const applied = filters.find((filter) => Number(filter.amenityType) === item.amenityType);
    item.selected = Boolean(applied);
    item.timeLimit = applied ? Number(applied.walkTimeMinutes ?? 15) : 15;
  });
};

watch(() => props.appliedFilters, restoreAppliedFilters, {immediate: true, deep: true});

// 적용완료 버튼 함수
const getFilters = () => {
  return selectedAmenities.value.map((item) => ({
    amenityType: item.amenityType,
    walkTimeMinutes: Number(item.timeLimit),
  }));
};

const getSelectedAmenities = () => selectedAmenities.value.map((item) => ({...item}));

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
/* 전체 컨테이너 높이 및 레이아웃 설정 */
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

</style>
