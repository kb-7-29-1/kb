<script setup>
import { ref } from 'vue';
import MapView from './MapView.vue';
import FilterPanel from '@/components/map/FilterPanel.vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/useAuthStore.js';

const router = useRouter();
const authStore = useAuthStore();

const isFilterOpen = ref(false);
const appliedOnboardingFilters = ref(null);
const appliedAmenityFilters = ref([]);
const filterResetVersion = ref(0);
const isMovingToMyPage = ref(false);

const openFilter = () => {
  isFilterOpen.value = true;
};

const closeFilter = () => {
  isFilterOpen.value = false;
};

const getDestinationKey = (destination) => {
  if (!destination) return '';
  if (typeof destination === 'string') return destination.trim();

  return JSON.stringify({
    name: destination.destName ?? destination.destinationName ?? destination.name ?? '',
    address: destination.destAddress ?? destination.address ?? '',
    lat: destination.destLatitude ?? destination.latitude ?? destination.lat ?? '',
    lng: destination.destLongitude ?? destination.longitude ?? destination.lng ?? '',
  });
};

const applyOnboardingFilters = (onboarding) => {
  const prevDestinationKey = getDestinationKey(appliedOnboardingFilters.value?.destination);
  const nextDestinationKey = getDestinationKey(onboarding?.destination);

  appliedOnboardingFilters.value = onboarding;

  if (prevDestinationKey !== nextDestinationKey) {
    appliedAmenityFilters.value = [];
  }
};

const applyAmenityFilters = (amenities) => {
  appliedAmenityFilters.value = Array.isArray(amenities) ? amenities : [];
};

const resetFilters = () => {
  appliedOnboardingFilters.value = null;
  appliedAmenityFilters.value = [];
  filterResetVersion.value += 1;
};

const goMyPage = () => {
  if (isMovingToMyPage.value) return;

  isMovingToMyPage.value = true;
  setTimeout(() => {
    router.push({ name: 'mypage' });
  }, 160);
};
</script>

<template>
  <main class="home-page" :class="{ 'home-page--leaving': isMovingToMyPage }">
    <header class="app-header">
      <span class="logo">
        <i class="fa-solid fa-shield-halved" aria-hidden="true"></i>
        살고싶오
      </span>
      <button class="profile-button" type="button" aria-label="마이페이지로 이동" @click="goMyPage">
        {{ authStore.user?.name?.charAt(0) || '나' }}
      </button>
    </header>
    <MapView
      :applied-onboarding-filters="appliedOnboardingFilters"
      :applied-amenity-filters="appliedAmenityFilters"
      :filter-reset-version="filterResetVersion"
      @open-filter="openFilter"
      @apply-amenity-filters="applyAmenityFilters"
    />

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
        :applied-amenity-filters="appliedAmenityFilters"
        @close="closeFilter"
        @apply-onboarding="applyOnboardingFilters"
        @apply-amenities="applyAmenityFilters"
        @reset="resetFilters"
      />
    </Transition>
  </main>
</template>

<style scoped>
.home-page {
  --app-header-height: max(100px, calc(env(safe-area-inset-top) + 62px));
  position: relative;
  width: 100%;
  height: 100dvh;
  max-height: 100dvh;
  overflow: hidden;
  padding-top: var(--app-header-height);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  animation: home-page-enter 0.2s ease-out;
}

.home-page--leaving {
  pointer-events: none;
  animation: home-page-leave 0.16s ease-in forwards;
}

@keyframes home-page-enter {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes home-page-leave {
  to {
    opacity: 0;
    transform: translateY(-6px);
  }
}

.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 50;
  box-sizing: border-box;
  height: var(--app-header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: max(60px, calc(env(safe-area-inset-top) + 22px)) 20px 0;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 10px;
}

.logo {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: #4058f5;
  font-weight: 700;
  font-size: 14px;
}

.logo i {
  font-size: 12px;
}

.profile-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: #4058f5;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.profile-button:active {
  transform: scale(0.96);
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

@media (min-width: 768px) {
  .home-page {
    --app-header-height: 56px;
  }

  .app-header {
    padding: 0 20px;
  }
}
</style>
