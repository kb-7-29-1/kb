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
          <div
              v-for="item in selectedAmenities"
              :key="'slider-' + item.id"
              class="slider-item"
          >
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
              />
            </div>
            <div class="slider-value">
              {{ item.timeLimit }}분
            </div>
          </div>
        </div>
      </div>
    </div>

    <button
        type="button"
        class="apply-button"
        @click="applyFilters"
    >
      적용하기
    </button>
  </div>
</template>

<script setup>
import {ref, computed} from 'vue';
import FilterBottomBar from './FilterBottomBar.vue';

const emit = defineEmits(['close', 'apply']);

const activeTab = ref('amenity');

// 편의시설 데이터 상태 관리
const amenities = ref([
  {id: 'convenience', amenityType: 1, name: '편의점', icon: '🏪', selected: false, timeLimit: 15},
  {id: 'cafe', amenityType: 2, name: '카페', icon: '☕', selected: false, timeLimit: 15},
  {id: 'laundry', amenityType: 3, name: '코인세탁소', icon: '🧺', selected: false, timeLimit: 15},
  {id: 'fastfood', amenityType: 4, name: '패스트푸드', icon: '🍔', selected: false, timeLimit: 15},
  {id: 'daiso', amenityType: 5, name: '다이소', icon: '🛒', selected: false, timeLimit: 15},
  {id: 'oliveyoung', amenityType: 6, name: '올리브영', icon: '💄', selected: false, timeLimit: 15},
  {id: 'mart', amenityType: 7, name: '대형마트', icon: '🏢', selected: false, timeLimit: 15},
]);

// 선택된 편의시설만 필터링
const selectedAmenities = computed(() => {
  return amenities.value.filter(item => item.selected);
});

// 편의시설 선택 토글 함수
const toggleAmenity = (item) => {
  item.selected = !item.selected;
};

// 초기화 버튼 함수
const resetFilters = () => {
  amenities.value.forEach(item => {
    item.selected = false;
    item.timeLimit = 15;
  });
};

// 적용완료 버튼 함수
const getFilters = () => {
  return selectedAmenities.value.map(item => ({
    amenityType: item.amenityType,
    walkTimeMinutes: Number(item.timeLimit)
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
  display: flex;
  flex-direction: column;
  height: 100%;
}

.filter-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px;
}

.apply-button {
  width: calc(100% - 40px);
  margin: 16px 20px 20px;
  padding: 12px;
  border: 0;
  border-radius: 10px;
  background: #3d55f6;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

/* 공통 섹션 스타일 */
.section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #333;
  margin-bottom: 16px;
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
  background-color: #edf2ff;
  border-color: #edf2ff;
}

.amenity-btn.selected .name {
  color: #3b5bdb;
  font-weight: 600;
}

/* 도보 시간 제한 슬라이더 */
.slider-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
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
  color: #555;
  font-weight: 500;
}

.slider-control {
  flex: 1;
  margin: 0 16px;
}

.styled-slider {
  width: 100%;
  accent-color: #3b5bdb;
  cursor: pointer;
}

.slider-value {
  width: 40px;
  text-align: right;
  font-size: 13px;
  font-weight: 600;
  color: #3b5bdb;
}
</style>
