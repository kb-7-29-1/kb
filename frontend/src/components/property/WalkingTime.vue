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
        {{ isOpen ? '⌃' : '⌄' }}
      </button>
    </div>

    <!-- 편의시설 목록 -->
    <div v-if="isOpen" class="amenity-list">
      <div
          v-for="amenity in amenities"
          :key="amenity.type"
          class="amenity-item"
      >
        <!-- 편의시설 이름 -->
        <div class="amenity-name">
          <span class="amenity-icon">
            {{ amenity.icon }}
          </span>

          <span>{{ amenity.name }}</span>
        </div>

        <!-- 진행 바 -->
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
import { ref } from 'vue';

const isOpen = ref(true);

const props = defineProps({
  amenities: {
    type: Array,
    default: () => [
      {
        type: 'CONVENIENCE_STORE',
        name: '편의점',
        icon: '🏪',
        walkingTime: 2,
      },
      {
        type: 'CAFE',
        name: '카페',
        icon: '☕',
        walkingTime: 4,
      },
      {
        type: 'DAISO',
        name: '다이소',
        icon: '🛍️',
        walkingTime: 9,
      },
    ],
  },
});

const getProgressWidth = (walkingTime) => {
  const maxTime = Math.max(
      ...props.amenities.map((amenity) => amenity.walkingTime),
      10
  );

  return `${Math.min((walkingTime / maxTime) * 100, 100)}%`;
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
  gap: 7px;

  font-size: 16px;
  font-weight: 700;
  color: #444;
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
  color: #555;
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

  width: 43px;
  flex-shrink: 0;

  gap: 2px;

  font-size: 13px;
  font-weight: 600;
  color: #555;
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
    font-size: 15px;
  }

  .amenity-name {
    width: 95px;
    font-size: 13px;
  }

  .walking-minute {
    width: 40px;
    font-size: 12px;
  }
}
</style>