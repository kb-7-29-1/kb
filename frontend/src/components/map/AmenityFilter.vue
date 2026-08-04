<template>
  <div class="filter-container">
    <div v-show="activeTab === 'amenity'" class="filter-content">
      <!-- 편의시설 필터 섹션 -->
      <div class="section">
        <h3 class="section-title">편의시설 필터</h3>
        <div class="amenity-grid">
          <button
            v-for="item in amenities"
            :key="item.id"
            class="amenity-btn"
            :class="{ selected: item.selected }"
            @click="toggleAmenity(item)"
          >
            <span class="icon">{{ item.icon }}</span>
            <span class="name">{{ item.name }}</span>
          </button>
        </div>
      </div>

      <!-- 편의시설 도보 시간 제한 섹션 -->
      <div class="section" v-if="selectedAmenities.length > 0">
        <h3 class="section-title">편의시설 도보 시간 제한</h3>
        <div class="slider-list">
          <div v-for="item in selectedAmenities" :key="'slider-' + item.id" class="slider-item">
            <div class="slider-label">
              <span class="icon">{{ item.icon }}</span>
              <span class="name">{{ item.name }}</span>
            </div>
            <div class="slider-control">
              <input
                type="range"
                v-model="item.timeLimit"
                min="0"
                max="30"
                step="1"
                class="styled-slider"
                :style="sliderStyle(item.timeLimit)"
              />
            </div>
            <div class="slider-value">{{ item.timeLimit }}분</div>
          </div>
        </div>
      </div>
    </div>

    <button type="button" class="apply-button" @click="applyFilters">적용하기</button>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';

const emit = defineEmits(['close', 'apply']);
const props = defineProps({
  appliedFilters: {
    type: Array,
    default: () => [],
  },
});

const activeTab = ref('amenity');

// 편의시설 데이터 상태 관리
const amenities = ref([
  { id: 'convenience', amenityType: 1, name: '편의점', icon: '🏪', selected: false, timeLimit: 15 },
  { id: 'cafe', amenityType: 2, name: '카페', icon: '☕', selected: false, timeLimit: 15 },
  { id: 'laundry', amenityType: 3, name: '코인세탁소', icon: '🧺', selected: false, timeLimit: 15 },
  {
    id: 'fastfood',
    amenityType: 4,
    name: '패스트푸드',
    icon: '🍔',
    selected: false,
    timeLimit: 15,
  },
  { id: 'daiso', amenityType: 5, name: '다이소', icon: '🛒', selected: false, timeLimit: 15 },
  {
    id: 'oliveyoung',
    amenityType: 6,
    name: '올리브영',
    icon: '💄',
    selected: false,
    timeLimit: 15,
  },
  { id: 'mart', amenityType: 7, name: '대형마트', icon: '🏢', selected: false, timeLimit: 15 },
]);

// 선택된 편의시설만 필터링
const selectedAmenities = computed(() => {
  return amenities.value.filter((item) => item.selected);
});

const sliderStyle = (timeLimit) => {
  const percent = (Number(timeLimit) / 30) * 100;
  return {
    background: `linear-gradient(to right, #3d55f6 0%, #3d55f6 ${percent}%, #e2e8f0 ${percent}%, #e2e8f0 100%)`,
  };
};

// 편의시설 선택 토글 함수
const toggleAmenity = (item) => {
  item.selected = !item.selected;
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

watch(() => props.appliedFilters, restoreAppliedFilters, { immediate: true, deep: true });

// 적용완료 버튼 함수
const getFilters = () => {
  return selectedAmenities.value.map((item) => ({
    amenityType: item.amenityType,
    walkTimeMinutes: Number(item.timeLimit),
  }));
};

const applyFilters = () => {
  emit('apply', getFilters());
};
defineExpose({
  resetFilters,
  applyFilters,
  getFilters,
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

/* 공통 섹션 스타일 */
.section {
  padding: 20px 0;
  border-bottom: 1px solid #edf0f5;
}

.section:first-child {
  padding-top: 10px;
}

.section:last-child {
  border-bottom: 0;
}

.section-title {
  margin: 0 0 14px;
  color: #374151;
  font-size: 13px;
  font-weight: 700;
}

/* 편의시설 버튼 그리드 */
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

.amenity-btn .icon {
  font-size: 20px;
  margin-bottom: 4px;
}

.amenity-btn .name {
  font-size: 11px;
  color: #888;
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

/* 도보 시간 제한 슬라이더 */
.slider-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.slider-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.slider-label {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 83px;
  font-size: 13px;
  color: #374151;
  font-size: 12px;
  font-weight: 700;
}

.slider-control {
  flex: 1;
  margin: 0 14px;
}

.styled-slider {
  width: 100%;
  height: 6px;
  margin: 0;
  appearance: none;
  border-radius: 999px;
  cursor: pointer;
}

.styled-slider::-webkit-slider-thumb {
  width: 17px;
  height: 17px;
  appearance: none;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #3d55f6;
  box-shadow: 0 1px 4px rgb(61 85 246 / 40%);
}

.styled-slider::-moz-range-thumb {
  width: 14px;
  height: 14px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #3d55f6;
  box-shadow: 0 1px 4px rgb(61 85 246 / 40%);
}

.slider-value {
  width: 40px;
  text-align: right;
  font-size: 12px;
  font-weight: 800;
  color: #3d55f6;
}
</style>
