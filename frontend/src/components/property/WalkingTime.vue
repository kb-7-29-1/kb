<template>
  <section class="walking-time">
    <!-- 헤더 -->
    <div class="section-header" @click="isOpen = !isOpen">
      <div class="title">
        <span class="title-icon">📍</span>
        <span>주변 편의시설 (도보 소요 시간)</span>
      </div>

      <button
        type="button"
        class="toggle-button"
        :aria-expanded="isOpen"
        @click.stop="isOpen = !isOpen"
      >
        <svg class="toggle-icon" viewBox="0 0 24 24" aria-hidden="true">
          <path :d="isOpen ? 'M6 15l6-6 6 6' : 'M6 9l6 6 6-6'" />
        </svg>
      </button>
    </div>

    <!-- 편의시설 목록 -->
    <div v-if="isOpen" class="amenity-list">
      <div
        v-for="amenity in displayAmenities"
        :key="`${amenity.type}-${amenity.name}`"
        class="amenity-item"
      >
        <!-- 편의시설 이름 -->
        <div class="amenity-name">
          <span class="amenity-icon">
            {{ amenity.icon }}
          </span>

          <span>{{ amenity.name }}</span>
        </div>

        <!-- 진행 바 (최대 20분 기준) -->
        <div class="progress-area">
          <div class="progress-bar">
            <div
              class="progress-fill"
              :style="{ width: getProgressWidth(amenity.walkingTime) }"
            ></div>
          </div>

          <!-- 시간 -->
          <div class="walking-minute">
            <span class="clock-icon">◷</span>
            <span>{{ amenity.walkingTime }}분</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue';

const isOpen = ref(false);

const props = defineProps({
  amenities: {
    type: Array,
    default: () => [],
  },
});

watch(
  () => props.amenities,
  (amenities) => {
    console.log('WalkingTime 받은 amenities:', amenities);
    console.table(amenities);
  },
  { immediate: true, deep: true },
);

const amenityIcons = {
  1: '🏪',
  2: '☕',
  3: '🧺',
  4: '🍔',
  5: '🛒',
  6: '💄',
  7: '🏢',
};

const displayAmenities = computed(() =>
  props.amenities
    .filter((amenity) => amenity.walkTimeMinutes != null || amenity.walkingTime != null)
    .map((amenity) => ({
      type: amenity.amenityType ?? amenity.type,
      name: amenity.amenityName ?? amenity.name,
      icon: amenityIcons[amenity.amenityType ?? amenity.type] ?? amenity.icon ?? '📍',
      walkingTime: amenity.walkTimeMinutes ?? amenity.walkingTime,
    })),
);

// 도보 소요 시간 게이지 최대 스케일 (20분 간격/범위 기준)
const MAX_WALKING_TIME = 20;

const getProgressWidth = (walkingTime) => {
  const minutes = Number(walkingTime) || 0;
  const clampedMinutes = Math.min(Math.max(minutes, 0), MAX_WALKING_TIME);
  return `${(clampedMinutes / MAX_WALKING_TIME) * 100}%`;
};
</script>

<style scoped>
.walking-time {
  width: 100%;
  box-sizing: border-box;
  padding: 0 18px 20px;
  background: #fff;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  min-height: 48px;

  cursor: pointer;
}

.title {
  display: flex;
  align-items: center;
  gap: 6px;

  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.title-icon {
  font-size: 16px;
}

.toggle-button {
  display: flex;
  align-items: center;
  justify-content: center;

  width: 28px;
  height: 28px;

  padding: 0;
  border: none;
  background: transparent;

  font-size: 22px;
  font-weight: 700;
  color: #222;

  cursor: pointer;
}

.toggle-icon {
  display: block;
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2.5;
}

.amenity-list {
  display: flex;
  flex-direction: column;
  gap: 13px;

  padding: 7px 0 4px;
}

.amenity-item {
  display: flex;
  align-items: center;

  width: 100%;
  min-height: 25px;
}

.amenity-name {
  display: flex;
  align-items: center;

  width: 105px;
  flex-shrink: 0;

  gap: 7px;

  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.amenity-icon {
  display: flex;
  align-items: center;
  justify-content: center;

  width: 20px;
  font-size: 16px;
}

.progress-area {
  display: flex;
  align-items: center;

  flex: 1;
  gap: 9px;
}

.progress-bar {
  position: relative;

  flex: 1;
  height: 8px;

  overflow: hidden;

  background: #f0f2f5;
  border-radius: 999px;
}

.progress-fill {
  height: 100%;

  border-radius: 999px;

  background: #26b86f;

  transition: width 0.3s ease;
}

.amenity-item:nth-child(3) .progress-fill {
  background: #3968f5;
}

.walking-minute {
  display: flex;
  align-items: center;

  width: 45px;
  flex-shrink: 0;

  gap: 2px;

  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
}

.clock-icon {
  font-size: 14px;
  color: #999;
}

@media (max-width: 480px) {
  .walking-time {
    padding: 0 16px 18px;
  }

  .title {
    font-size: 16px;
  }

  .amenity-name {
    width: 95px;
    font-size: 13px;
  }

  .walking-minute {
    width: 42px;
    font-size: 12px;
  }
}
</style>
