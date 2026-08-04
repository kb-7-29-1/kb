<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import api from '@/api/api.js';
import NaverMap from '@/components/map/NaverMap.vue';
import PropertyCard from '@/components/property/PropertyCard.vue';
import SlidingDoorPanel from '@/components/detail/SlidingDoorPanel.vue';
import MapQuickFilterBar from '@/components/map/MapQuickFilterBar.vue';
import AmenityFilter from '@/components/map/AmenityFilter.vue';
import { getHaversineDistance } from '@/utils/geo.js';
import { useMobilePanelDrag } from '@/composables/useMobilePanelDrag.js';
import { useOnboardingFilter } from '@/composables/useOnboardingFilter.js';
import { mockProperties } from '@/mock/mockProperties.js';
import amenityService from '@/api/amenityService.js';

const emit = defineEmits(['open-filter', 'apply-amenity-filters']);
const props = defineProps({
  appliedOnboardingFilters: {
    type: Object,
    default: null,
  },
  appliedAmenityFilters: {
    type: Array,
    default: () => [],
  },
  filterResetVersion: {
    type: Number,
    default: 0,
  },
});

// 5종 정렬 필터 옵션
const currentSort = ref('RECOMMENDED');
const sortOptions = [
  { key: 'RECOMMENDED', label: '추천순' },
  { key: 'PRICE_ASC', label: '가격 낮은순' },
  { key: 'PRICE_DESC', label: '가격 높은순' },
  { key: 'SAFETY_DESC', label: '안전점수 높은순' },
  { key: 'AREA_DESC', label: '면적 넓은순' },
];

// 온보딩 디폴트 연동 퀵 필터 상태 Composable
const { filterState, loadOnboardingDefaultFilters } = useOnboardingFilter();
const isQuickFilterReady = ref(false);

// 좌측 아코디언/패널 편의시설 필터 열림 상태
const showAmenityFilter = ref(false);

// 선택된 매물 & 우측 상세 패널 열림 상태
const selectedProperty = ref(null);
const isPanelOpen = ref(false);
const selectedPropertyDetailAmenities = ref([]);

// 매물 목록 데이터 (기본값: mockProperties 더미 데이터 백업)
const amenitiesByProperty = ref({});
const amenityFilterLoading = ref(false);
let amenityRequestSequence = 0;
let amenityRefreshTimer = null;
let amenityRefreshAttempts = 0;

// 매물 목록 데이터 (백엔드 DB 연동)
const properties = ref([]);
const isPropertyApiError = ref(false);

// 데이터 출처 계산 (DB vs PUBLIC_API)
const currentDataSource = computed(() => {
  return properties.value[0]?.dataSource || 'PUBLIC_API';
});

const getSearchRadiusKm = () => {
  const filters = appliedFilterState.value || filterState.value;
  const minutes = Number(filters.travelTime) || 15;

  if (filters.transportMode === 'WALK') {
    const speedKmH = filters.walkPace === 'SLOW'
      ? 3.6
      : filters.walkPace === 'FAST'
        ? 6.0
        : 4.8;
    return speedKmH * (minutes / 60);
  }

  return 18.0 * (minutes / 60);
};

// 백엔드 실제 DB 매물 API 조회 (/api/properties)
const fetchPropertiesFromBackend = async () => {
  try {
    const res = await api.get('/properties', {
      params: {
        lat: destinationConfig.value.lat,
        lng: destinationConfig.value.lng,
        radius: getSearchRadiusKm(),
      },
    });
    if (res.data && Array.isArray(res.data)) {
      properties.value = res.data;
      isPropertyApiError.value = false;
    }
  } catch (error) {
    console.error('Backend DB API connection failed:', error);
    properties.value = [];
    isPropertyApiError.value = true;
  }
};

onMounted(async () => {
  await loadOnboardingDefaultFilters();
  appliedFilterState.value = JSON.parse(JSON.stringify(filterState.value));
  isQuickFilterReady.value = true;
  await fetchPropertiesFromBackend();
});

// 적용 버튼 클릭 시에만 갱신되는 매물 마커 전용 확정 필터 상태
const appliedFilterState = ref({ ...filterState.value });

const handleApplyFilters = () => {
  appliedFilterState.value = JSON.parse(JSON.stringify(filterState.value));
  fetchPropertiesFromBackend();
};

const handleResetFilters = async () => {
  await loadOnboardingDefaultFilters();
  appliedFilterState.value = JSON.parse(JSON.stringify(filterState.value));
  await fetchPropertiesFromBackend();
};

const applyMobileOnboardingFilters = (filters) => {
  if (!filters) return;

  const destination = filters.destination;
  if (destination && typeof destination === 'object') {
    filterState.value.destination =
      destination.destName ||
      destination.destinationName ||
      destination.name ||
      filterState.value.destination;
    filterState.value.destinationAddress = destination.destAddress || destination.address || '';

    const latitude = destination.destLatitude ?? destination.latitude ?? destination.lat;
    const longitude = destination.destLongitude ?? destination.longitude ?? destination.lng;
    if (latitude != null && longitude != null) {
      filterState.value.destinationLat = Number(latitude);
      filterState.value.destinationLng = Number(longitude);
    }
  } else if (typeof destination === 'string' && destination.trim()) {
    filterState.value.destination = destination;
  }

  if (filters.transportMode) filterState.value.transportMode = filters.transportMode;
  if (filters.maxTravelTime != null) filterState.value.travelTime = Number(filters.maxTravelTime);
  if (filters.budgetDeposit != null) filterState.value.maxDeposit = Number(filters.budgetDeposit);
  if (filters.budgetRent != null) filterState.value.maxRent = Number(filters.budgetRent);
  if (filters.minSafetyScore != null) {
    filterState.value.minSafetyScore = Number(filters.minSafetyScore);
  }

  handleApplyFilters();
};

watch(() => props.appliedOnboardingFilters, applyMobileOnboardingFilters, { deep: true });

watch(() => props.filterResetVersion, handleResetFilters);

const loadAmenitiesForProperties = async (isRefresh = false) => {
  // 목록 필터 적용 시 미계산 편의시설은 서버에서 계산·캐시한 뒤 매물별 결과를 받는다.
  const filters = props.appliedAmenityFilters;
  const propertyIds = properties.value
    .map((property) => property.propertyId)
    .filter((propertyId) => propertyId != null);

  if (!filters.length || !propertyIds.length) {
    clearTimeout(amenityRefreshTimer);
    amenityRefreshAttempts = 0;
    amenitiesByProperty.value = {};
    return;
  }

  if (!isRefresh) {
    clearTimeout(amenityRefreshTimer);
    amenityRefreshAttempts = 0;
  }

  const sequence = ++amenityRequestSequence;
  amenityFilterLoading.value = true;
  try {
    const result = await amenityService.filterProperties(propertyIds, filters);
    if (sequence === amenityRequestSequence) {
      amenitiesByProperty.value = result;
    }
  } catch (error) {
    if (sequence === amenityRequestSequence) {
      amenitiesByProperty.value = {};
      console.error('AMENITY FILTER LOAD ERROR:', error);
    }
  } finally {
    if (sequence === amenityRequestSequence) {
      amenityFilterLoading.value = false;

      if (amenityRefreshAttempts < 12) {
        amenityRefreshAttempts += 1;
        amenityRefreshTimer = setTimeout(
          () => loadAmenitiesForProperties(true),
          5_000,
        );
      }
    }
  }
};

watch([() => props.appliedAmenityFilters, properties], loadAmenitiesForProperties, { deep: true });

onUnmounted(() => clearTimeout(amenityRefreshTimer));

// 네이버 Geocoder API를 활용한 실시간 동적 주소/장소 좌표(lat, lng) 자동 변환
watch(
  () => [filterState.value.destination, filterState.value.destinationAddress],
  ([newDest, newAddr]) => {
    if (!newDest && !newAddr) return;

    // 1. 이미 정확한 위경도 좌표가 온보딩이나 필터에서 직접 전달된 경우 (조회 생략)
    if (filterState.value.destinationLat && filterState.value.destinationLng) {
      return;
    }

    // 2. 좌표가 없는 경우 동명이인/유사 장소명 오류 방지를 위해 '풀 도로명/지번 주소' 우선 조회
    const searchQuery = newAddr || newDest;

    if (window.naver && window.naver.maps && window.naver.maps.Service) {
      window.naver.maps.Service.geocode(
        { query: searchQuery },
        (status, response) => {
          if (
            status === window.naver.maps.Service.Status.OK &&
            response.v2 &&
            response.v2.addresses &&
            response.v2.addresses.length > 0
          ) {
            const item = response.v2.addresses[0];
            filterState.value.destinationLat = Number(item.y);
            filterState.value.destinationLng = Number(item.x);
          }
        },
      );
    }
  },
  { immediate: true },
);

// 동적 목적지 명칭 및 실시간 실제 좌표 (lat, lng) 매핑
const destinationConfig = computed(() => {
  const name = filterState.value.destination || '세종대학교';
  const lat = filterState.value.destinationLat || 37.5502;
  const lng = filterState.value.destinationLng || 127.0731;

  return {
    name: `${name} (주 목적지)`,
    lat,
    lng,
  };
});

// 퀵버튼 필터 + 도보/대중교통 도달 범위(Reach) + 5종 정렬 연동 로직 (appliedFilterState 기준 연산)
const sortedProperties = computed(() => {
  // 실시간 주 목적지 좌표
  const destLat = destinationConfig.value.lat;
  const destLng = destinationConfig.value.lng;

  const currentFilters = appliedFilterState.value || filterState.value;

  // 이동 수단별 최대 도달 가능 거리 (km) 계산
  let maxReachKm = 1.2; // 기본 15분 도보 약 1.2km
  const minutes = currentFilters.travelTime || 15;

  if (currentFilters.transportMode === 'WALK') {
    // 도보 속도: SLOW(3.6km/h), NORMAL(4.8km/h), FAST(6.0km/h)
    let speedKmH = 4.8;
    if (currentFilters.walkPace === 'SLOW') speedKmH = 3.6;
    if (currentFilters.walkPace === 'FAST') speedKmH = 6.0;
    maxReachKm = speedKmH * (minutes / 60);
  } else {
    // 대중교통 평균 도심 속도 (약 18.0km/h)
    maxReachKm = 18.0 * (minutes / 60);
  }

  let list = properties.value.filter((p) => {
    // 1. 거래 유형 필터 (전세/월세)
    if (currentFilters.tradeType === 'JEONSE' && p.monthlyRent > 0) return false;

    // 2. 보증금 / 전세금 필터 (maxDeposit 단위: 만원)
    if (currentFilters.maxDeposit < 5000 && p.deposit > currentFilters.maxDeposit) return false;

    // 3. 월세 필터 (maxRent 단위: 만원)
    if (
      currentFilters.tradeType === 'MONTHLY' &&
      currentFilters.maxRent < 100 &&
      p.monthlyRent > currentFilters.maxRent
    )
      return false;

    // 4. 안전 점수 필터
    if (p.safetyScore < currentFilters.minSafetyScore) return false;

    // 5. 도보 / 대중교통 도달 범위 (Reach Distance) 필터
    if (currentFilters.showIsochrone) {
      const distKm = getHaversineDistance(
        destLat,
        destLng,
        p.latitude,
        p.longitude,
      );
      if (distKm > maxReachKm) return false;
    }

    if (props.appliedAmenityFilters.length && !amenityFilterLoading.value) {
      const propertyAmenities = amenitiesByProperty.value[p.propertyId] ?? [];
      const matchedTypes = new Set(
        propertyAmenities.map((amenity) => amenity.amenityType),
      );
      const requiredTypes = new Set(
        props.appliedAmenityFilters.map((filter) => filter.amenityType),
      );
      if (![...requiredTypes].every((type) => matchedTypes.has(type)))
        return false;
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
const handleSelectProperty = async (property) => {
  selectedProperty.value = property;
  isPanelOpen.value = true;

  try {
    const amenities = props.appliedAmenityFilters.length
      ? await amenityService.filterAmenities(
          property.propertyId,
          props.appliedAmenityFilters,
        )
      : await amenityService.getCachedAmenities(property.propertyId);

    if (selectedProperty.value?.propertyId === property.propertyId) {
      selectedPropertyDetailAmenities.value = amenities;
    }
  } catch (error) {
    if (selectedProperty.value?.propertyId === property.propertyId) {
      selectedPropertyDetailAmenities.value = [];
    }
    console.error('SELECTED PROPERTY AMENITY LOAD ERROR:', error);
  }
};

const selectedPropertyAmenities = computed(() => {
  // 상세 패널과 지도 핀은 같은 선택 매물의 편의시설 결과를 사용한다.
  if (!selectedProperty.value) return [];
  const propertyId = selectedProperty.value.propertyId;
  if (props.appliedAmenityFilters.length) {
    return amenitiesByProperty.value[propertyId]
      ?? selectedPropertyDetailAmenities.value;
  }
  return selectedPropertyDetailAmenities.value;
});

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
  emit('apply-amenity-filters', selectedList);
};

// 모바일/데스크톱 하단 사이드바 실시간 마우스 및 터치 드래그 리사이즈 Composable 연결
const {
  mobilePanelHeight,
  isDragging,
  dragPixelHeight,
  toggleMobilePanel,
  startDrag,
} = useMobilePanelDrag();
</script>

<template>
  <div
    class="relative w-full flex-1 min-h-0 h-full overflow-hidden flex flex-col-reverse md:flex-row bg-slate-100"
  >
    <!-- 1. 매물 탐색 사이드바 (마우스 및 터치 실시간 드래그 지원 / PC: md:flex-row 좌측 고정) -->
    <aside
      class="mobile-aside-panel w-full md:w-[380px] bg-white border-t md:border-t-0 md:border-r border-slate-200 z-20 flex flex-col shrink-0 shadow-2xl transition-all ease-out"
      :class="[
        isDragging ? 'duration-0' : 'duration-300',
        mobilePanelHeight === 'EXPANDED'
          ? 'h-[80vh] md:h-full'
          : 'h-1/3 md:h-full',
      ]"
      :style="dragPixelHeight ? { height: `${dragPixelHeight}px` } : {}"
    >
      <!-- 모바일 전용 마우스/터치 실시간 손잡이 드래그 바 (md:hidden) -->
      <div
        class="w-full py-2 bg-white flex flex-col items-center justify-center cursor-row-resize active:cursor-grabbing md:hidden select-none touch-none shrink-0 border-b border-slate-100"
        @click="toggleMobilePanel"
        @mousedown="startDrag"
        @touchstart.prevent="startDrag"
      >
        <span class="w-12 h-1.5 bg-slate-300 rounded-full mb-1"></span>
        <span class="text-[10px] font-bold text-slate-400">
          {{
            mobilePanelHeight === 'EXPANDED'
              ? '▼ 접고 지도 보기'
              : '▲ 올리고 목록 더보기'
          }}
        </span>
      </div>
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
          <AmenityFilter
            :applied-filters="props.appliedAmenityFilters"
            @apply="handleApplyAmenities"
          />
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
        <template v-if="sortedProperties.length > 0">
          <PropertyCard
            v-for="prop in sortedProperties"
            :key="prop.propertyId"
            :property="prop"
            :is-selected="
              selectedProperty &&
              selectedProperty.propertyId === prop.propertyId
            "
            @select="handleSelectProperty"
            @toggle-bookmark="handleToggleBookmark"
          />
        </template>
        <div
          v-else
          class="h-full flex flex-col items-center justify-center p-6 text-center text-slate-400"
        >
          <span class="text-3xl mb-2">🏠</span>
          <p class="text-sm font-bold text-slate-600">
            조건에 맞는 매물이 없습니다.
          </p>
          <p class="text-xs text-slate-400 mt-1">
            필터 조건을 변경하거나 검색어를 재설정해 보세요.
          </p>
        </div>
      </div>
    </aside>

    <!-- 2. 중앙 메인 지도 캔버스 (Full-bleed) -->
    <main class="flex-1 h-full relative z-10">
      <!-- 🗺️ 지도 상단 부유형(Floating) 퀵버튼 바 (요소 크기 맞춤 w-fit) -->
      <div class="absolute top-4 left-4 z-30 pointer-events-none">
        <MapQuickFilterBar
          v-if="isQuickFilterReady"
          v-model="filterState"
          :total-count="sortedProperties.length"
          class="pointer-events-auto"
          @open-filter="emit('open-filter')"
          @apply="handleApplyFilters"
          @update-filters="handleApplyFilters"
          @reset="handleResetFilters"
        />
      </div>

      <NaverMap
        :properties="sortedProperties"
        :selected-property="selectedProperty"
        :amenities="selectedPropertyAmenities"
        :destination="destinationConfig"
        :show-isochrone="filterState.showIsochrone"
        :transport-mode="filterState.transportMode || 'WALK'"
        :travel-time="filterState.travelTime || 15"
        :walk-pace="filterState.walkPace || 'NORMAL'"
        :flex-time="filterState.flexTime || 10"
        @select-property="handleSelectProperty"
      />

      <!-- 백엔드 DB 매물 수신 실패 안내 배너 -->
      <div
        v-if="isPropertyApiError"
        class="absolute bottom-6 left-1/2 transform -translate-x-1/2 z-40 bg-red-600/90 text-white px-4 py-2.5 rounded-2xl text-xs font-bold shadow-2xl backdrop-blur-md flex items-center gap-2 border border-red-500 pointer-events-auto"
      >
        <span>⚠️</span>
        <span>백엔드 매물 데이터를 불러오는 데 실패했습니다.</span>
        <button
          type="button"
          class="underline ml-2 hover:text-red-200"
          @click="fetchPropertiesFromBackend"
        >
          재시도
        </button>
      </div>
    </main>

    <!-- 3. 우측 560px Slide-Over 매물 상세 패널 -->
    <SlidingDoorPanel
      :is-open="isPanelOpen"
      :property="selectedProperty"
      :amenities="selectedPropertyAmenities"
      @close="isPanelOpen = false"
      @toggle-bookmark="handleToggleBookmark"
    />
  </div>
</template>
