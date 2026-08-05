<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import api from '@/api/api.js';
import NaverMap from '@/components/map/NaverMap.vue';
import PropertyCard from '@/components/property/PropertyCard.vue';
import SlidingDoorPanel from '@/components/detail/SlidingDoorPanel.vue';
import MapQuickFilterBar from '@/components/map/MapQuickFilterBar.vue';
import AmenityFilter from '@/components/map/AmenityFilter.vue';
import AmenityDetailFilterPanel from '@/components/map/AmenityDetailFilterPanel.vue';
import { getHaversineDistance } from '@/utils/geo.js';
import { useMobilePanelDrag } from '@/composables/useMobilePanelDrag.js';
import { useOnboardingFilter } from '@/composables/useOnboardingFilter.js';
import { mockProperties } from '@/mock/mockProperties.js';
import amenityService from '@/api/amenityService.js';

const emit = defineEmits(['open-filter', 'apply-amenity-filters']);
const route = useRoute();
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
  { key: 'RECOMMENDED', label: '추천순', icon: 'fa-solid fa-thumbs-up' },
  { key: 'PRICE_ASC', label: '가격 낮은순', icon: 'fa-solid fa-arrow-down-wide-short' },
  { key: 'PRICE_DESC', label: '가격 높은순', icon: 'fa-solid fa-arrow-up-wide-short' },
  { key: 'SAFETY_DESC', label: '안전점수 높은순', icon: 'fa-solid fa-shield-halved' },
  {
    key: 'AREA_DESC',
    label: '면적 넓은순',
    icon: 'fa-solid fa-up-right-and-down-left-from-center',
  },
];

// 온보딩 디폴트 연동 퀵 필터 상태 Composable
const { filterState, loadOnboardingDefaultFilters } = useOnboardingFilter();
const isQuickFilterReady = ref(false);

// 좌측 아코디언/패널 편의시설 필터 열림 상태
const amenityFilterRef = ref(null);
const isAmenityDetailFilterOpen = ref(false);
const amenityDetailFilters = ref([]);
const activeAmenityFilters = ref([]);

watch(
  () => props.appliedAmenityFilters,
  (filters = []) => {
    activeAmenityFilters.value = filters.map((filter) => ({ ...filter }));
  },
  { immediate: true, deep: true },
);

// 선택된 매물 & 우측 상세 패널 열림 상태
const selectedProperty = ref(null);
const isPanelOpen = ref(false);
const selectedPropertyDetailAmenities = ref([]);

// 매물 목록 데이터 (기본값: mockProperties 더미 데이터 백업)
const amenitiesByProperty = ref({});
const amenityFilterLoading = ref(false);
let amenityRequestSequence = 0;

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
    const speedKmH = filters.walkPace === 'SLOW' ? 3.6 : filters.walkPace === 'FAST' ? 6.0 : 4.8;
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

const loadAmenitiesForProperties = async () => {
  // 목록 필터 적용 시 미계산 편의시설은 서버에서 계산·캐시한 뒤 매물별 결과를 받는다.
  const filters = activeAmenityFilters.value;
  const propertyIds = properties.value
    .map((property) => property.propertyId)
    .filter((propertyId) => propertyId != null);
  const sequence = ++amenityRequestSequence;

  if (!filters.length || !propertyIds.length) {
    amenityFilterLoading.value = false;
    amenitiesByProperty.value = {};
    return;
  }

  amenityFilterLoading.value = true;
  try {
    const result = await amenityService.filterProperties(propertyIds, filters);
    if (sequence === amenityRequestSequence) {
      amenitiesByProperty.value = result;
      amenityFilterLoading.value = false;
    }
  } catch (error) {
    if (sequence === amenityRequestSequence) {
      amenitiesByProperty.value = {};
      amenityFilterLoading.value = false;
      console.error('AMENITY FILTER LOAD ERROR:', error);
    }
  }
};

watch([activeAmenityFilters, properties], loadAmenitiesForProperties, { deep: true });

onUnmounted(() => {
  amenityRequestSequence += 1;
});

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
      window.naver.maps.Service.geocode({ query: searchQuery }, (status, response) => {
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
      });
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

    // 5. 도보 / 대중교통 도달 범위 (Reach Distance) 도넛 링 필터
    if (currentFilters.showIsochrone) {
      const distKm = getHaversineDistance(destLat, destLng, p.latitude, p.longitude);
      const distMeters = distKm * 1000;

      if (currentFilters.transportMode === 'WALK') {
        let speedMetersPerMin = 75;
        if (currentFilters.walkPace === 'SLOW') speedMetersPerMin = 58;
        if (currentFilters.walkPace === 'FAST') speedMetersPerMin = 92;
        const maxReachMeters = Math.max(200, currentFilters.travelTime * speedMetersPerMin);
        if (distMeters > maxReachMeters) return false;
      } else {
        // 대중교통 모드 (TRANSIT): 내접원(transitBaseRadius) ~ 외접원(transitMaxRadius) 도넛 링 구역만 허용
        const transitBaseRadius = Math.max(500, currentFilters.travelTime * 180); // 내부원 (10분 이내)
        const flexMins = currentFilters.flexTime != null ? currentFilters.flexTime : 10;
        const transitMaxRadius = Math.max(
          transitBaseRadius + 200,
          (currentFilters.travelTime + flexMins) * 180,
        ); // 외부원 (30분 이내)

        // 10분 이내 내부원 안쪽 및 30분 초과 외부원 바깥 매물 제외
        if (distMeters < transitBaseRadius || distMeters > transitMaxRadius) return false;
      }
    }

    if (activeAmenityFilters.value.length && !amenityFilterLoading.value) {
      const propertyAmenities = amenitiesByProperty.value[p.propertyId] ?? [];
      const matchedTypes = new Set(propertyAmenities.map((amenity) => amenity.amenityType));
      const requiredTypes = new Set(activeAmenityFilters.value.map((filter) => filter.amenityType));
      if (![...requiredTypes].every((type) => matchedTypes.has(type))) return false;
    }

    return true;
  });

  // 5종 정렬 적용
  if (currentSort.value === 'PRICE_ASC') {
    return list.sort((a, b) => a.deposit + a.monthlyRent * 100 - (b.deposit + b.monthlyRent * 100));
  }
  if (currentSort.value === 'PRICE_DESC') {
    return list.sort((a, b) => b.deposit + b.monthlyRent * 100 - (a.deposit + a.monthlyRent * 100));
  }
  if (currentSort.value === 'SAFETY_DESC') {
    return list.sort((a, b) => (b.safetyScore || 0) - (a.safetyScore || 0));
  }
  if (currentSort.value === 'AREA_DESC') {
    return list.sort((a, b) => (b.area || 0) - (a.area || 0));
  }
  return list; // RECOMMENDED
});

const visibleProperties = computed(() =>
  amenityFilterLoading.value ? [] : sortedProperties.value,
);

// 매물 선택 처리 (사이드바 카드 또는 지도 핀 클릭 시)
const handleSelectProperty = async (property) => {
  selectedProperty.value = property;
  isPanelOpen.value = true;

  if (!activeAmenityFilters.value.length) {
    selectedPropertyDetailAmenities.value = [];
    return;
  }

  try {
    const amenities = await amenityService.filterAmenities(
      property.propertyId,
      activeAmenityFilters.value,
    );

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

// 마이페이지 관심 매물 카드에서 전달한 propertyId로 기존 상세 패널을 열기
const openPropertyDetailFromQuery = async (propertyId) => {
  if (!propertyId) return;

  const numericPropertyId = Number(propertyId);
  if (!Number.isFinite(numericPropertyId)) return;

  const savedBookmarkProperty = sessionStorage.getItem('selectedBookmarkProperty');
  const bookmarkedProperty = savedBookmarkProperty ? JSON.parse(savedBookmarkProperty) : null;
  if (Number(bookmarkedProperty?.propertyId) === numericPropertyId) {
    handleSelectProperty(bookmarkedProperty);
    sessionStorage.removeItem('selectedBookmarkProperty');
    return;
  }

  const listedProperty = properties.value.find(
    (property) => Number(property.propertyId) === numericPropertyId,
  );

  if (listedProperty) {
    handleSelectProperty(listedProperty);
    return;
  }

  try {
    const { data } = await api.get(`/properties/${numericPropertyId}`);
    if (data) handleSelectProperty(data);
  } catch (error) {
    console.error('BOOKMARK PROPERTY DETAIL LOAD ERROR: ', error);
  }
};

watch(
  () => route.query.propertyId,
  (propertyId) => openPropertyDetailFromQuery(propertyId),
  { immediate: true },
);

const selectedPropertyAmenities = computed(() => {
  // 상세 패널과 지도 핀은 같은 선택 매물의 편의시설 결과를 사용한다.
  if (!selectedProperty.value) return [];
  if (!activeAmenityFilters.value.length) return [];

  const propertyId = selectedProperty.value.propertyId;
  return amenitiesByProperty.value[propertyId] ?? selectedPropertyDetailAmenities.value;
});

watch(
  () => activeAmenityFilters.value.length,
  (length) => {
    if (!length) selectedPropertyDetailAmenities.value = [];
  },
);

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
  activeAmenityFilters.value = selectedList.map((filter) => ({ ...filter }));
  filterState.value.selectedAmenities = selectedList.map((filter) => filter.amenityType);
  emit('apply-amenity-filters', activeAmenityFilters.value);
};

const openAmenityDetailFilter = (selectedFilters) => {
  const filters = selectedFilters ?? amenityFilterRef.value?.getSelectedAmenities?.() ?? [];
  amenityDetailFilters.value = filters.map((filter) => ({ ...filter }));
  isAmenityDetailFilterOpen.value = true;
};

const syncAmenityDetailFilter = (selectedFilters) => {
  if (!isAmenityDetailFilterOpen.value) return;
  amenityDetailFilters.value = selectedFilters.map((filter) => {
    const existingFilter = amenityDetailFilters.value.find((item) => item.id === filter.id);
    return {
      ...filter,
      timeLimit: existingFilter?.timeLimit ?? filter.timeLimit,
    };
  });
};

// PC 필터에서 이미 적용된 항목을 해제하면, 상세 필터를 다시 열지 않아도 즉시 반영한다.
const handleAmenitySelectionChange = (selectedFilters) => {
  syncAmenityDetailFilter(selectedFilters);

  const selectedTypes = new Set(selectedFilters.map((filter) => Number(filter.amenityType)));
  const hasRemovedAppliedFilter = activeAmenityFilters.value.some(
    (filter) => !selectedTypes.has(Number(filter.amenityType)),
  );

  if (hasRemovedAppliedFilter) {
    handleApplyAmenities(
      selectedFilters.map((filter) => ({
        amenityType: filter.amenityType,
        walkTimeMinutes: Number(filter.timeLimit),
      })),
    );
  }
};

const resetAmenityDetailFilters = () => {
  amenityFilterRef.value?.resetFilters?.();
  amenityDetailFilters.value = [];
  handleApplyAmenities([]);
};

const applyAmenityDetailFilters = () => {
  handleApplyAmenities(
    amenityDetailFilters.value.map((item) => ({
      amenityType: item.amenityType,
      walkTimeMinutes: Number(item.timeLimit),
    })),
  );
  isAmenityDetailFilterOpen.value = false;
};

const updateAmenityDetailTimeLimit = ({ id, timeLimit }) => {
  const item = amenityDetailFilters.value.find((filter) => filter.id === id);
  if (item) item.timeLimit = timeLimit;
};

// 모바일/데스크톱 하단 사이드바 실시간 마우스 및 터치 드래그 리사이즈 Composable 연결
const { mobilePanelHeight, isDragging, dragPixelHeight, toggleMobilePanel, startDrag } =
  useMobilePanelDrag();
</script>

<template>
  <div
    class="relative w-full flex-1 min-h-0 h-full overflow-hidden bg-slate-100 xl:flex xl:flex-row"
  >
    <!-- 1. 매물 탐색 사이드바 (마우스 및 터치 실시간 드래그 지원 / PC: md:flex-row 좌측 고정) -->
    <aside
      class="mobile-aside-panel absolute inset-x-0 bottom-0 z-20 flex w-full flex-col overflow-hidden rounded-t-[22px] border-t border-slate-200 bg-white shadow-2xl transition-all ease-out xl:relative xl:inset-auto xl:w-[380px] xl:shrink-0 xl:rounded-none xl:border-t-0 xl:border-r"
      :class="[
        isDragging ? 'duration-0' : 'duration-300',
        mobilePanelHeight === 'EXPANDED'
          ? 'h-full xl:h-full'
          : mobilePanelHeight === 'COLLAPSED'
            ? 'h-[120px] xl:h-full'
            : 'h-1/3 xl:h-full',
      ]"
      :style="dragPixelHeight ? { height: `${dragPixelHeight}px` } : {}"
    >
      <!-- 모바일 전용 마우스/터치 실시간 손잡이 드래그 바 (md:hidden) -->
      <div
        class="w-full pb-2 pt-4 bg-white flex flex-col items-center justify-center cursor-row-resize active:cursor-grabbing xl:hidden select-none touch-none shrink-0"
        @click="toggleMobilePanel"
        @mousedown="startDrag"
        @touchstart.prevent="startDrag"
      >
        <span class="w-24 h-1.5 bg-slate-300 rounded-full"></span>
      </div>

      <!-- 사이드바 상단 헤더 및 5종 정렬 탭 -->
      <div class="p-4 pt-1 pb-1 border-b-0 xl:border-b xl:border-slate-200 bg-white space-y-3">
        <div class="flex items-center justify-between">
          <h1 class="hidden xl:flex font-black text-slate-900 text-lg items-center gap-2">
            <span>🛡️</span>
            <span>살고싶오 매물 탐색</span>
          </h1>
          <span
            class="text-[13px] font-bold text-slate-500 xl:text-xs xl:text-blue-600 xl:bg-blue-50 xl:px-2.5 xl:py-1 xl:rounded-full"
          >
            총 {{ visibleProperties.length }}개 매물
          </span>
        </div>

        <!-- 항상 노출되는 편의시설 필터와 상세 설정 -->
        <section class="relative !mt-5 hidden border-t border-slate-100 xl:block">
          <div class="flex w-full items-center justify-between mb-1">
            <h2 class="text-[17px] font-black text-slate-700">편의시설 필터</h2>

            <button
              type="button"
              class="flex h-8 shrink-0 items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-2.5 text-xs font-bold text-slate-600 shadow-sm transition-colors hover:border-blue-300 hover:bg-blue-50 hover:text-blue-600"
              @click="openAmenityDetailFilter()"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="15"
                height="15"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
              >
                <path d="M4 5h16l-6.5 7.2V18l-3 1.5v-7.3L4 5z" />
              </svg>

              <span>상세 필터</span>
            </button>
          </div>

          <AmenityFilter
            ref="amenityFilterRef"
            :applied-filters="activeAmenityFilters"
            :show-walking-time="false"
            @apply="handleApplyAmenities"
            @selection-change="handleAmenitySelectionChange"
          />

          <AmenityDetailFilterPanel
            v-if="isAmenityDetailFilterOpen"
            :amenities="amenityDetailFilters"
            @apply="applyAmenityDetailFilters"
            @close="isAmenityDetailFilterOpen = false"
            @reset="resetAmenityDetailFilters"
            @update-time-limit="updateAmenityDetailTimeLimit"
          />
        </section>

        <!-- 5종 정렬 선택 탭 -->
        <div class="mobile-sort-options flex items-center gap-1.5 overflow-x-auto pb-1 xl:gap-1">
          <button
            v-for="opt in sortOptions"
            :key="opt.key"
            type="button"
            class="shrink-0 rounded-full border px-3 py-1.5 text-[11px] font-semibold transition-all xl:rounded-lg xl:border-0 xl:px-2.5 xl:text-xs xl:font-bold"
            :class="[
              currentSort === opt.key
                ? 'border-[#4058f5] bg-[#eef1ff] text-[#4058f5] xl:border-0 xl:bg-slate-900 xl:text-white xl:shadow-sm'
                : 'border-slate-200 bg-white text-slate-500 hover:border-slate-300 hover:bg-slate-50 xl:bg-slate-100 xl:text-slate-600 xl:hover:bg-slate-200',
            ]"
            @click="currentSort = opt.key"
          >
            <i :class="[opt.icon, 'xl:hidden text-[10px]']" aria-hidden="true"></i>
            {{ opt.label }}
          </button>
        </div>
      </div>

      <!-- 사이드바 매물 카드리스트 (스크롤) -->
      <div class="flex-1 overflow-y-auto p-3 space-y-2.5">
        <template v-if="visibleProperties.length > 0">
          <PropertyCard
            v-for="prop in visibleProperties"
            :key="prop.propertyId"
            :property="prop"
            :is-selected="selectedProperty && selectedProperty.propertyId === prop.propertyId"
            @select="handleSelectProperty"
            @toggle-bookmark="handleToggleBookmark"
          />
        </template>
        <div
          v-else-if="!amenityFilterLoading"
          class="h-full flex flex-col items-center justify-center p-6 text-center text-slate-400"
        >
          <span class="text-3xl mb-2">🏠</span>
          <p class="text-sm font-bold text-slate-600">조건에 맞는 매물이 없습니다.</p>
          <p class="text-xs text-slate-400 mt-1">
            필터 조건을 변경하거나 검색어를 재설정해 보세요.
          </p>
        </div>
        <div
          v-else
          class="h-full flex items-center justify-center p-6 text-center text-sm font-medium text-slate-500"
        >
          매물을 조회하고 있어요.
        </div>
      </div>
    </aside>

    <!-- 2. 중앙 메인 지도 캔버스 (Full-bleed) -->
    <main class="absolute inset-0 z-10 xl:relative xl:inset-auto xl:h-full xl:flex-1">
      <!-- 🗺️ 지도 상단 부유형(Floating) 퀵버튼 바 (요소 크기 맞춤 w-fit) -->
      <div class="absolute top-4 left-4 z-30 pointer-events-none">
        <MapQuickFilterBar
          v-if="isQuickFilterReady"
          v-model="filterState"
          :total-count="visibleProperties.length"
          class="pointer-events-auto"
          @open-filter="emit('open-filter')"
          @apply="handleApplyFilters"
          @update-filters="handleApplyFilters"
          @reset="handleResetFilters"
        />
      </div>

      <NaverMap
        :properties="visibleProperties"
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

<style scoped>
.mobile-sort-options {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.mobile-sort-options::-webkit-scrollbar {
  display: none;
}
</style>
