<script setup>
import { ref } from 'vue';
import MapView from './MapView.vue';
import FilterPanel from '@/components/map/FilterPanel.vue';
import { useRouter } from 'vue-router';

const router = useRouter();

const isFilterOpen = ref(false);
const appliedOnboardingFilters = ref(null);

const openFilter = () => {
  isFilterOpen.value = true;
};

const closeFilter = () => {
  isFilterOpen.value = false;
};

const applyFilters = ({ onboarding }) => {
  appliedOnboardingFilters.value = onboarding;
  closeFilter();
};

const resetFilters = () => {
  appliedOnboardingFilters.value = null;
};

const goMyPage = () => {
  router.push({ name: 'mypage' });
};
</script>

<template>
  <main class="home-page">
    <header class="app-header">
      <span class="logo">🛡️ 살고싶오</span>
      <button class="mypage-btn" @click="goMyPage">마이페이지</button>
    </header>
    <MapView @open-filter="openFilter" />

    <!-- 보관용 중복 필터 버튼 및 패널 주석 처리 (MapQuickFilterBar.vue 내부로 이전 완료) -->
    <!--
    <button
      v-if="!isFilterOpen"
      type="button"
      class="filter-floating-button"
      aria-label="필터 열기"
      title="필터"
      @click="openFilter"
    >
      <svg
        class="filter-icon"
        viewBox="0 0 32 32"
        fill="none"
        aria-hidden="true"
      >
        <path
          d="M5 8H27"
          stroke="currentColor"
          stroke-width="2.8"
          stroke-linecap="round"
        />
        <circle cx="20" cy="8" r="3.2" fill="currentColor" />

        <path
          d="M5 16H27"
          stroke="currentColor"
          stroke-width="2.8"
          stroke-linecap="round"
        />
        <circle cx="11" cy="16" r="3.2" fill="currentColor" />

        <path
          d="M5 24H27"
          stroke="currentColor"
          stroke-width="2.8"
          stroke-linecap="round"
        />
        <circle cx="22" cy="24" r="3.2" fill="currentColor" />
      </svg>
    </button>

    -->

    <Transition name="bottom-sheet">
      <FilterPanel
        v-if="isFilterOpen"
        :applied-filters="appliedOnboardingFilters"
        @close="closeFilter"
        @apply="applyFilters"
        @reset="resetFilters"
      />
    </Transition>
    <footer class="app-footer">
      This footer 바닥 확인용 문구에요 | 세로 스크롤이 생기는지 , 넘치는 현상이
      있는지 확인하고 추후 지워질 예정이에요 | 바닥 확인용 문구에요 | 세로
      스크롤이 생기는지 , 넘치는 현상이 있는지 확인하고 추후 지워질 예정이에요
      footer This
    </footer>
  </main>
</template>

<style scoped>
.home-page {
  position: relative;
  width: 100%;
  height: 100dvh;
  max-height: 100dvh;
  overflow: hidden;
  padding-top: 56px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 50;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.logo {
  font-weight: 700;
  font-size: 15px;
}

.mypage-btn {
  border: 1px solid #e5e7eb;
  background: #f8f9ff;
  color: #3f5bf6;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 20px;
  cursor: pointer;
}

.mypage-btn:hover {
  background: #eef1ff;
}

.filter-floating-button {
  position: fixed;
  top: 76px;
  left: 20px;
  z-index: 40;

  display: flex;
  align-items: center;
  justify-content: center;

  width: 46px;
  height: 46px;
  padding: 0;

  border: 1px solid #e5e7eb;
  border-radius: 50%;
  background: #ffffff;
  box-shadow:
    0 3px 8px rgb(0 0 0 / 14%),
    0 1px 3px rgb(0 0 0 / 8%);

  color: #3f5bf6;
  cursor: pointer;

  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease,
    background-color 0.15s ease;
}

.filter-floating-button:hover {
  background: #f8f9ff;
  box-shadow:
    0 5px 12px rgb(0 0 0 / 16%),
    0 2px 4px rgb(0 0 0 / 8%);
}

.filter-floating-button:active {
  transform: scale(0.94);
}

.filter-floating-button:focus-visible {
  outline: 3px solid rgb(63 91 246 / 25%);
  outline-offset: 2px;
}

.filter-icon {
  width: 29px;
  height: 29px;
}

.bottom-sheet-enter-active,
.bottom-sheet-leave-active {
  transition: transform 0.3s ease;
}

.bottom-sheet-enter-from,
.bottom-sheet-leave-to {
  transform: translateY(100%);
}

.bottom-sheet-enter-to,
.bottom-sheet-leave-from {
  transform: translateY(0);
}

.app-footer {
  height: 3vh;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
  letter-spacing: -0.02em;
  z-index: 30;
}
</style>
