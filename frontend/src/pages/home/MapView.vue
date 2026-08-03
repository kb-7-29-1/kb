<script setup>
import { ref, computed } from 'vue';
import api from '@/api/api.js';
import NaverMap from '@/components/map/NaverMap.vue';
import PropertyCard from '@/components/property/PropertyCard.vue';
import SlidingDoorPanel from '@/components/detail/SlidingDoorPanel.vue';
import MapQuickFilterBar from '@/components/map/MapQuickFilterBar.vue';
import AmenityFilter from '@/components/map/AmenityFilter.vue';

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
  selectedAmenities: [],
});

// 좌측 아코디언/패널 편의시설 필터 열림 상태
const showAmenityFilter = ref(false);

// 선택된 매물 & 우측 상세 패널 열림 상태
const selectedProperty = ref(null);
const isPanelOpen = ref(false);

// 하버사인 공식 (목적지 세종대 ~ 매물 간 실제 거리 km 계산)
const getHaversineDistance = (lat1, lon1, lat2, lon2) => {
  const R = 6371; // 지구 반지름 (km)
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * (Math.PI / 180)) *
      Math.cos(lat2 * (Math.PI / 180)) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c; // 거리 (km)
};

// 광진구 세종대 인근 실제 공공데이터 실거래 월세 매물 5개
const properties = ref([
  {
    propertyId: 101,
    title: '세종대 화양동 프리미엄 오피스텔',
    buildingType: 3, // 오피스텔
    roomType: 1, // 원룸
    deposit: 1000,
    monthlyRent: 65,
    area: 24.5,
    floor: 5,
    builtYear: '2023년',
    address: '서울특별시 광진구 화양동 111-23',
    latitude: 37.5485,
    longitude: 127.072,
    thumbnailUrl:
      'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=600&q=80',
    safetyScore: 92,
    safetyGrade: 'SAFE',
    cctvCount: 18,
    streetlightCount: 42,
    hasPoliceStation: true,
    isIllegalBuilding: false,
    isBookmarked: true,
    tags: ['풀옵션', '역세권', 'CCTV가득'],
  },
  {
    propertyId: 102,
    title: '어린이대공원역 역세권 신축 원룸',
    buildingType: 1, // 빌라
    roomType: 1, // 원룸
    deposit: 500,
    monthlyRent: 55,
    area: 22.0,
    floor: 3,
    builtYear: '2022년',
    address: '서울특별시 광진구 군자동 361-15',
    latitude: 37.5528,
    longitude: 127.0745,
    thumbnailUrl:
      'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=600&q=80',
    safetyScore: 88,
    safetyGrade: 'SAFE',
    cctvCount: 22,
    streetlightCount: 50,
    hasPoliceStation: true,
    isIllegalBuilding: false,
    isBookmarked: false,
    tags: ['초역세권', '안심길', '보호구역'],
  },
  {
    propertyId: 103,
    title: '건대입구역 가성비 밝은 원룸',
    buildingType: 2, // 다가구
    roomType: 1, // 원룸
    deposit: 2000,
    monthlyRent: 60,
    area: 26.8,
    floor: 2,
    builtYear: '2020년',
    address: '서울특별시 광진구 화양동 48-12',
    latitude: 37.5442,
    longitude: 127.0685,
    thumbnailUrl:
      'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=600&q=80',
    safetyScore: 78,
    safetyGrade: 'WARNING',
    cctvCount: 12,
    streetlightCount: 28,
    hasPoliceStation: false,
    isIllegalBuilding: false,
    isBookmarked: false,
    tags: ['가성비', '남향', '번화가가까움'],
  },
  {
    propertyId: 104,
    title: '세종대 후문 풀옵션 다가구 원룸',
    buildingType: 2, // 다가구
    roomType: 1, // 원룸
    deposit: 1000,
    monthlyRent: 50,
    area: 21.0,
    floor: 4,
    builtYear: '2021년',
    address: '서울특별시 광진구 군자동 102-4',
    latitude: 37.5545,
    longitude: 127.0782,
    thumbnailUrl:
      'https://images.unsplash.com/photo-1554995207-c18c203602cb?auto=format&fit=crop&w=600&q=80',
    safetyScore: 95,
    safetyGrade: 'SAFE',
    cctvCount: 25,
    streetlightCount: 55,
    hasPoliceStation: true,
    isIllegalBuilding: false,
    isBookmarked: false,
    tags: ['세종대도보3분', '최고안전점수', '조용한주택가'],
  },
  {
    propertyId: 105,
    title: '자양동 신양초 인근 안심 투룸',
    buildingType: 1, // 빌라
    roomType: 2, // 투룸
    deposit: 3000,
    monthlyRent: 80,
    area: 45.2,
    floor: 3,
    builtYear: '2023년',
    address: '서울특별시 광진구 자양동 224-8',
    latitude: 37.5385,
    longitude: 127.066,
    thumbnailUrl:
      'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=600&q=80',
    safetyScore: 82,
    safetyGrade: 'SAFE',
    cctvCount: 16,
    streetlightCount: 38,
    hasPoliceStation: true,
    isIllegalBuilding: false,
    isBookmarked: true,
    tags: ['투룸', '넓은면적', '경찰서인근'],
  },
]);

// 퀵버튼 필터 + 도보/대중교통 도달 범위(Reach) + 5종 정렬 연동 로직
const sortedProperties = computed(() => {
  // 세종대 주 목적지 기준 좌표 (37.5502, 127.0731)
  const destLat = 37.5502;
  const destLng = 127.0731;

  // 이동 수단별 최대 도달 가능 거리 (km) 계산
  let maxReachKm = 1.2; // 기본 15분 도보 약 1.2km
  const minutes = filterState.value.travelTime || 15;

  if (filterState.value.transportMode === 'WALK') {
    // 도보 속도: SLOW(3.6km/h), NORMAL(4.8km/h), FAST(6.0km/h)
    let speedKmH = 4.8;
    if (filterState.value.walkPace === 'SLOW') speedKmH = 3.6;
    if (filterState.value.walkPace === 'FAST') speedKmH = 6.0;
    maxReachKm = speedKmH * (minutes / 60);
  } else {
    // 대중교통 평균 도심 속도 (약 18.0km/h)
    maxReachKm = 18.0 * (minutes / 60);
  }

  let list = properties.value.filter((p) => {
    // 1. 거래 유형 (월세만 처리)
    if (filterState.value.tradeType === 'JEONSE') return false;

    // 2. 보증금 필터
    if (p.deposit > filterState.value.maxDeposit) return false;
    // 월세 필터
    if (
      filterState.value.tradeType !== 'JEONSE' &&
      p.monthlyRent > filterState.value.maxRent
    )
      return false;
    // 안전 점수 필터
    if (p.safetyScore < filterState.value.minSafetyScore) return false;

    // 5. 도보 / 대중교통 도달 범위 (Reach Distance) 필터
    if (filterState.value.showIsochrone) {
      const distKm = getHaversineDistance(
        destLat,
        destLng,
        p.latitude,
        p.longitude,
      );
      if (distKm > maxReachKm) return false;
    }

    return true;
  });

  // 5종 정렬 적용
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

// 편의시설 적용 핸들러
const handleApplyAmenities = (selectedList) => {
  filterState.value.selectedAmenities = selectedList.map((a) => a.amenityType);
};
</script>

<template>
  <div
    class="relative w-full h-screen overflow-hidden flex flex-col-reverse md:flex-row bg-slate-100"
  >
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

        <!-- 좌측 사이드바 최상단 편의시설 필터 아코디언 토글 버튼 -->
        <button
          type="button"
          class="w-full py-2 px-3 rounded-xl border text-xs font-bold flex items-center justify-between transition-all"
          :class="
            showAmenityFilter
              ? 'bg-blue-50 border-blue-300 text-blue-700 font-black'
              : 'bg-slate-50 border-slate-200 text-slate-700 hover:bg-slate-100'
          "
          @click="showAmenityFilter = !showAmenityFilter"
        >
          <span class="flex items-center gap-1.5">
            <span>🛍️</span>
            <span>주변 편의시설 필터 연동</span>
          </span>
          <span>{{ showAmenityFilter ? '▲ 접기' : '▼ 펼치기' }}</span>
        </button>

        <!-- 편의시설 필터 컴포넌트 마운트 -->
        <div
          v-if="showAmenityFilter"
          class="pt-2 border-t border-slate-100 max-h-60 overflow-y-auto"
        >
          <AmenityFilter @apply="handleApplyAmenities" />
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
        :destination="{
          name: filterState.destination + ' (주 목적지)',
          lat: 37.5502,
          lng: 127.0731,
        }"
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
