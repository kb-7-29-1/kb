<script setup>
import { ref, computed } from 'vue';
import api from '@/api/api.js';
import NaverMap from '@/components/map/NaverMap.vue';
import PropertyCard from '@/components/property/PropertyCard.vue';
import SlidingDoorPanel from '@/components/detail/SlidingDoorPanel.vue';
import MapQuickFilterBar from '@/components/map/MapQuickFilterBar.vue';

// 5종 정렬 필터 옵션 (스펙: 추천순, 가격 낮은순, 가격 높은순, 안전점수 높은순, 면적 넓은순)
const currentSort = ref('RECOMMENDED');
const sortOptions = [
  { key: 'RECOMMENDED', label: '추천순' },
  { key: 'PRICE_ASC', label: '가격 낮은순' },
  { key: 'PRICE_DESC', label: '가격 높은순' },
  { key: 'SAFETY_DESC', label: '안전점수 높은순' },
  { key: 'AREA_DESC', label: '면적 넓은순' },
];

// 퀵 필터 바 반응형 상태
const filterState = ref({
  destination: '세종대학교',
  tradeType: 'MONTHLY',
  maxDeposit: 5000,
  maxRent: 100,
  minSafetyScore: 0,
  transportMode: 'WALK',
  showIsochrone: true,
});

// 선택된 매물 & 우측 상세 패널 열림 상태
const selectedProperty = ref(null);
const isPanelOpen = ref(false);

// 더미/샘플 매물 데이터 목록 (지도 마커 및 사이드바 카드 렌더링용)
const properties = ref([
  {
    propertyId: 1,
    title: '역삼역 도보 3분 풀옵션 안심 원룸',
    buildingType: 1, // 빌라
    roomType: 1, // 원룸
    deposit: 1000,
    monthlyRent: 65,
    area: 24.5,
    floor: 3,
    builtYear: '2022년',
    address: '서울시 강남구 역삼동 642-10',
    latitude: 37.5002,
    longitude: 127.0358,
    thumbnailUrl:
      'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=600&q=80',
    safetyScore: 92,
    safetyGrade: 'SAFE',
    cctvCount: 18,
    streetlightCount: 42,
    hasPoliceStation: true,
    isIllegalBuilding: false,
    isBookmarked: true,
  },
  {
    propertyId: 2,
    title: '강남역 신축 아늑한 오피스텔',
    buildingType: 3, // 오피스텔
    roomType: 1, // 원룸
    deposit: 2000,
    monthlyRent: 85,
    area: 29.8,
    floor: 7,
    builtYear: '2023년',
    address: '서울시 서초구 서초동 1303-6',
    latitude: 37.4985,
    longitude: 127.025,
    thumbnailUrl:
      'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=600&q=80',
    safetyScore: 88,
    safetyGrade: 'SAFE',
    cctvCount: 22,
    streetlightCount: 50,
    hasPoliceStation: true,
    isIllegalBuilding: false,
    isBookmarked: false,
  },
  {
    propertyId: 3,
    title: '선릉역 인접 가성비 만점 투룸',
    buildingType: 1, // 빌라
    roomType: 2, // 투룸
    deposit: 3000,
    monthlyRent: 70,
    area: 42.1,
    floor: 2,
    builtYear: '2020년',
    address: '서울시 강남구 삼성동 140-12',
    latitude: 37.5048,
    longitude: 127.0488,
    thumbnailUrl:
      'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=600&q=80',
    safetyScore: 78,
    safetyGrade: 'WARNING',
    cctvCount: 12,
    streetlightCount: 28,
    hasPoliceStation: false,
    isIllegalBuilding: false,
    isBookmarked: false,
  },
  {
    propertyId: 4,
    title: '양재역 햇살 밝은 깔끔 원룸',
    buildingType: 2, // 다가구
    roomType: 1, // 원룸
    deposit: 500,
    monthlyRent: 55,
    area: 21.0,
    floor: 4,
    builtYear: '2021년',
    address: '서울시 서초구 양재동 12-5',
    latitude: 37.4842,
    longitude: 127.0341,
    thumbnailUrl:
      'https://images.unsplash.com/photo-1554995207-c18c203602cb?auto=format&fit=crop&w=600&q=80',
    safetyScore: 84,
    safetyGrade: 'SAFE',
    cctvCount: 15,
    streetlightCount: 35,
    hasPoliceStation: true,
    isIllegalBuilding: false,
    isBookmarked: false,
  },
]);

// 퀵버튼 필터 + 5종 정렬 연동 로직
const sortedProperties = computed(() => {
  let list = properties.value.filter((p) => {
    // 보증금 필터
    if (p.deposit > filterState.value.maxDeposit) return false;
    // 월세 필터
    if (filterState.value.tradeType !== 'JEONSE' && p.monthlyRent > filterState.value.maxRent) return false;
    // 안전 점수 필터
    if (p.safetyScore < filterState.value.minSafetyScore) return false;
    return true;
  });

  // 정렬 적용
  if (currentSort.value === 'PRICE_ASC') {
    return list.sort(
      (a, b) =>
        a.deposit + a.monthlyRent * 100 - (b.deposit + b.monthlyRent * 100),
    );
  }
  if (currentSort.value === 'PRICE_DESC') {
    return list.sort(
      (a, b) =>
        b.deposit + b.monthlyRent * 100 - (a.deposit + a.monthlyRent * 100),
    );
  }
  if (currentSort.value === 'SAFETY_DESC') {
    return list.sort((a, b) => (b.safetyScore || 0) - (a.safetyScore || 0));
  }
  if (currentSort.value === 'AREA_DESC') {
    return list.sort((a, b) => (b.area || 0) - (a.area || 0));
  }
  return list; // RECOMMENDED
});

// 매물 선택 처리 (사이드바 카드 또는 지도 핀 클릭 시)
const handleSelectProperty = (property) => {
  selectedProperty.value = property;
  isPanelOpen.value = true;
};

// 찜 토글
const handleToggleBookmark = async (id) => {
  const item = properties.value.find((p) => p.propertyId === id);
  if (!item) return;

  try {
    if (item.isBookmarked) {
      await api.delete(`/bookmark/${id}`);
    } else {
      await api.post('/bookmark', { propertyId: id });
    }
    item.isBookmarked = !item.isBookmarked;
  } catch (error) {
    console.error('BOOKMARK TOGGLE ERROR: ', error);
  }
};

</script>

<template>
  <div class="relative w-full h-screen overflow-hidden flex flex-col-reverse md:flex-row bg-slate-100">
    <!-- 1. 매물 탐색 사이드바 (모바일: flex-col-reverse 하단배치 / PC: md:flex-row 좌측배치) -->
    <aside
      class="w-full md:w-[380px] h-1/3 md:h-full bg-white border-t md:border-t-0 md:border-r border-slate-200 z-20 flex flex-col shrink-0 shadow-lg"
    >
      <!-- 사이드바 상단 헤더 및 5종 정렬 탭 -->
      <div class="p-4 border-b border-slate-200 bg-white space-y-3">
        <div class="flex items-center justify-between">
          <h1 class="font-black text-slate-900 text-lg flex items-center gap-2">
            <span>🛡️</span>
            <span>살고싶오 매물 탐색</span>
          </h1>
          <span
            class="text-xs font-bold text-blue-600 bg-blue-50 px-2.5 py-1 rounded-full"
          >
            총 {{ sortedProperties.length }}개 매물
          </span>
        </div>

        <!-- 5종 정렬 선택 탭 -->
        <div
          class="flex items-center gap-1 overflow-x-auto pb-1 scrollbar-none"
        >
          <button
            v-for="opt in sortOptions"
            :key="opt.key"
            type="button"
            class="px-2.5 py-1.5 rounded-lg text-xs font-bold transition-all shrink-0"
            :class="[
              currentSort === opt.key
                ? 'bg-slate-900 text-white shadow-sm'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200',
            ]"
            @click="currentSort = opt.key"
          >
            {{ opt.label }}
          </button>
        </div>
      </div>

      <!-- 사이드바 매물 카드리스트 (스크롤) -->
      <div class="flex-1 overflow-y-auto p-3 space-y-2.5">
        <PropertyCard
          v-for="prop in sortedProperties"
          :key="prop.propertyId"
          :property="prop"
          :is-selected="
            selectedProperty && selectedProperty.propertyId === prop.propertyId
          "
          @select="handleSelectProperty"
          @toggle-bookmark="handleToggleBookmark"
        />
      </div>
    </aside>

    <!-- 2. 중앙 메인 지도 캔버스 (Full-bleed) -->
    <main class="flex-1 h-full relative z-10">
      <!-- 🗺️ 지도 상단 부유형(Floating) 퀵버튼 바 (요소 크기 맞춤 w-fit) -->
      <div class="absolute top-4 left-4 z-30 pointer-events-none">
        <MapQuickFilterBar
          v-model="filterState"
          :total-count="sortedProperties.length"
          class="pointer-events-auto"
        />
      </div>

      <NaverMap
        :properties="sortedProperties"
        :selected-property="selectedProperty"
        :destination="{ name: filterState.destination + ' (주 목적지)', lat: 37.5502, lng: 127.0731 }"
        :show-isochrone="filterState.showIsochrone"
        :transport-mode="filterState.transportMode || 'WALK'"
        :travel-time="filterState.travelTime || 15"
        :walk-pace="filterState.walkPace || 'NORMAL'"
        :flex-time="filterState.flexTime || 10"
        @select-property="handleSelectProperty"
      />
    </main>

    <!-- 3. 우측 560px Slide-Over 매물 상세 패널 -->
    <SlidingDoorPanel
      :is-open="isPanelOpen"
      :property="selectedProperty"
      @close="isPanelOpen = false"
      @toggle-bookmark="handleToggleBookmark"
    />
  </div>
</template>
