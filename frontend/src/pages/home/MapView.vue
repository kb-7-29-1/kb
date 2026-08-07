<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '@/api/api.js';
import NaverMap from '@/components/map/NaverMap.vue';
import PropertyCard from '@/components/property/PropertyCard.vue';
import SlidingDoorPanel from '@/components/detail/SlidingDoorPanel.vue';
import MapQuickFilterBar from '@/components/map/MapQuickFilterBar.vue';

import RouteFeedbackCard from '@/components/map/RouteFeedbackCard.vue';
import AmenityFilter from '@/components/map/AmenityFilter.vue';
import AmenityDetailFilterPanel from '@/components/map/AmenityDetailFilterPanel.vue';
import OnboardingSummary from '@/components/map/OnboardingSummary.vue';
import { getHaversineDistance } from '@/utils/geo.js';
import { useMobilePanelDrag } from '@/composables/useMobilePanelDrag.js';
import { useOnboardingFilter } from '@/composables/useOnboardingFilter.js';
import { useMapStore } from '@/stores/useMapStore.js';
import { useAuthStore } from '@/stores/useAuthStore.js';
import { saveRecentDestinationGlobal } from '@/utils/recentDestinations.js';
import { DEFAULT_DEPOSIT, DEFAULT_RENT, LOAN_PRODUCTS } from '@/utils/budget';
import { mockProperties } from '@/mock/mockProperties.js';
import amenityService from '@/api/amenityService.js';
import safetyService from '@/api/safetyService.js';

const emit = defineEmits(['open-filter', 'apply-amenity-filters']);
const route = useRoute();
const router = useRouter();
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
const isDesktopSortExpanded = ref(false);
const sortOptions = [
  { key: 'RECOMMENDED', label: '추천순', icon: 'fa-solid fa-thumbs-up' },
  {
    key: 'PRICE_ASC',
    label: '가격 낮은순',
    icon: 'fa-solid fa-arrow-down-wide-short',
  },
  {
    key: 'PRICE_DESC',
    label: '가격 높은순',
    icon: 'fa-solid fa-arrow-up-wide-short',
  },
  {
    key: 'SAFETY_DESC',
    label: '안전점수 높은순',
    icon: 'fa-solid fa-shield-halved',
  },
  {
    key: 'AREA_DESC',
    label: '면적 넓은순',
    icon: 'fa-solid fa-up-right-and-down-left-from-center',
  },
];

// 온보딩 디폴트 연동 퀵 필터 상태 Composable
const { filterState, loadOnboardingDefaultFilters } = useOnboardingFilter();
const appliedFilterState = ref({ ...filterState.value });
const isQuickFilterReady = ref(true);
const mapStore = useMapStore();

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
let amenityFilterDebounceTimer = null;

// 매물 목록 데이터 (백엔드 DB 연동)
const properties = ref([]);
const isPropertyApiError = ref(false);
const isPropertyLoading = ref(true);
const isFilterAnalysisLoading = ref(false);
let propertyRequestSequence = 0;
let filterAnalysisTimer = null;

const showFilterAnalysisLoading = () => {
  isFilterAnalysisLoading.value = true;
  clearTimeout(filterAnalysisTimer);
  filterAnalysisTimer = setTimeout(() => {
    isFilterAnalysisLoading.value = false;
  }, 450);
};

const isMapAnalysisLoading = computed(
  () =>
    isPropertyLoading.value ||
    amenityFilterLoading.value ||
    isFilterAnalysisLoading.value,
);

// 데이터 출처 계산 (DB vs PUBLIC_API)
const currentDataSource = computed(() => {
  return properties.value[0]?.dataSource || 'PUBLIC_API';
});

const getSearchRadiusKm = () => {
  const filters = appliedFilterState.value;
  const minutes = Number(filters.travelTime) || 15;

  if (filters.transportMode === 'WALK') {
    let speedMetersPerMin = 75;
    if (filters.walkPace === 'SLOW') speedMetersPerMin = 58;
    if (filters.walkPace === 'FAST') speedMetersPerMin = 92;
    const maxMeters = Math.max(200, minutes * speedMetersPerMin);
    return (maxMeters / 1000) * 1.35;
  }

  const transitMaxRadius = Math.max(500, minutes * 180);
  return (transitMaxRadius / 1000) * 1.35;
};

// 편의점 디폴트 도보 시간 (5분)
const DEFAULT_CONVENIENCE_STORE_WALK_TIME = 5;

// 기타 편의시설 디폴트 도보 시간 (15분)
const DEFAULT_AMENITY_WALK_TIME = 15;

// 프론트엔드 인메모리 목적지별 매물 캐시 (네트워크 재요청 0ms 지연 완전 제거)
const propertyCache = new Map();

// 대출 상품을 반영한 실제 보증금 상한을 서버 검색 조건에도 전달합니다.
const getEffectiveMaxDeposit = (filters) => {
  let maxDeposit = Number(filters.maxDeposit) || DEFAULT_DEPOSIT;

  if (filters.selectedLoanId && filters.selectedLoanId !== 'NONE') {
    const loan = LOAN_PRODUCTS.find(
      (item) => item.id === filters.selectedLoanId,
    );
    if (loan?.ratio > 0) {
      maxDeposit = Math.round(maxDeposit * (1 + loan.ratio));
    }
  }

  return maxDeposit;
};

const buildPropertySearchParams = () => {
  const filters = appliedFilterState.value;
  const params = {
    destinationId: filters.destinationId || undefined,
    lat: destinationConfig.value.lat,
    lng: destinationConfig.value.lng,
    radius: getSearchRadiusKm(),
    maxDeposit: getEffectiveMaxDeposit(filters),
  };

  if (filters.tradeType === 'JEONSE') {
    params.maxMonthlyRent = 0;
  } else if (Number(filters.maxRent) < DEFAULT_RENT) {
    params.maxMonthlyRent = Number(filters.maxRent);
  }

  return params;
};

// 검색 조건에 맞는 매물을 조회한 뒤, 화면에 렌더링하기 전에 목적지별 안전점수를 일괄 준비합니다.
const fetchPropertiesFromBackend = async () => {
  const requestId = ++propertyRequestSequence;
  isPropertyLoading.value = true;
  isPropertyApiError.value = false;
  properties.value = [];

  try {
    const filters = appliedFilterState.value;

    const propertyResponse = await api.get('/properties', {
      params: buildPropertySearchParams(),
    });

    if (requestId !== propertyRequestSequence) return;

    const candidates = Array.isArray(propertyResponse.data)
      ? propertyResponse.data
      : [];

    // 매물 데이터 즉시 바인딩 및 로딩 스피너 해제 (무한 로딩 100% 완전 방지)
    properties.value = candidates;
    isPropertyLoading.value = false;

    const targetCandidates = candidates.length > 500 ? candidates.slice(0, 500) : candidates;
    const propertyIds = targetCandidates
      .map((property) => Number(property.propertyId))
      .filter((propertyId) => Number.isFinite(propertyId) && propertyId > 0);

    if (!propertyIds.length) return;

    const safetyBatch = await safetyService.getScoresForProperties({
      propertyIds,
      destinationId: filters.destinationId,
      destinationName: filters.destination,
      destinationAddress: filters.destinationAddress,
      destinationLatitude: destinationConfig.value.lat,
      destinationLongitude: destinationConfig.value.lng,
    });

    if (requestId !== propertyRequestSequence) return;

    if (safetyBatch.destinationId) {
      filterState.value.destinationId = Number(safetyBatch.destinationId);
      appliedFilterState.value.destinationId = Number(
        safetyBatch.destinationId,
      );
    }

    const safetyByPropertyId = new Map(
      (safetyBatch.items || []).map((item) => [Number(item.propertyId), item]),
    );

    properties.value = candidates.map((property) => {
      const safety = safetyByPropertyId.get(Number(property.propertyId));

      return {
        ...property,
        safetyScore: safety?.safetyScore ?? property.safetyScore ?? null,
        safetyGrade: safety?.safetyGrade ?? property.safetyGrade ?? null,
        cctvCount: safety?.cctvCount ?? 0,
        streetLampCount: safety?.streetLampCount ?? 0,
        streetlightCount: safety?.streetLampCount ?? 0,
        hasPoliceStation: safety?.hasPoliceStation ?? false,
        safetyStatus: safety?.status ?? 'FAILED',
        safetyMessage: safety?.message ?? '',
      };
    });

    isPropertyApiError.value = false;
  } catch (error) {
    if (requestId !== propertyRequestSequence) return;

    console.error('PROPERTY/SAFETY BATCH LOAD ERROR:', error);
    properties.value = [];
    isPropertyApiError.value = true;
  } finally {
    if (requestId === propertyRequestSequence) {
      isPropertyLoading.value = false;
    }
  }
};

// URL 브라우저 주소창 Query 파라미터 양방향 실시간 동기화 유틸 (한글 텍스트 dest 제외, 위경도 좌표 및 숫자로만 깔끔 구성)
const syncFiltersToUrlQuery = (filters) => {
  if (!filters) return;
  const query = {
    ...route.query,
    dest: undefined,
    destLat: filters.destinationLat
      ? Number(filters.destinationLat).toFixed(4)
      : undefined,
    destLng: filters.destinationLng
      ? Number(filters.destinationLng).toFixed(4)
      : undefined,
    tradeType: filters.tradeType || undefined,
    minDeposit: filters.minDeposit != null ? filters.minDeposit : undefined,
    maxDeposit: filters.maxDeposit != null ? filters.maxDeposit : undefined,
    minRent: filters.minRent != null ? filters.minRent : undefined,
    maxRent: filters.maxRent != null ? filters.maxRent : undefined,
    mode: filters.transportMode || undefined,
    travelTime: filters.travelTime != null ? filters.travelTime : undefined,
    minTravelTime: filters.minTravelTime != null ? filters.minTravelTime : undefined,
    minSafety:
      filters.minSafetyScore != null ? filters.minSafetyScore : undefined,
  };
  router.replace({ query }).catch(() => {});
};

const parseUrlQueryToFilters = () => {
  const q = route.query;
  if (!q || (!q.destLat && !q.tradeType && !q.maxDeposit)) return false;

  if (q.destLat) filterState.value.destinationLat = Number(q.destLat);
  if (q.destLng) filterState.value.destinationLng = Number(q.destLng);
  if (q.tradeType) filterState.value.tradeType = String(q.tradeType);
  if (q.minDeposit != null) filterState.value.minDeposit = Number(q.minDeposit);
  if (q.maxDeposit != null) filterState.value.maxDeposit = Number(q.maxDeposit);
  if (q.minRent != null) filterState.value.minRent = Number(q.minRent);
  if (q.maxRent != null) filterState.value.maxRent = Number(q.maxRent);
  if (q.mode) filterState.value.transportMode = String(q.mode);
  if (q.travelTime != null) filterState.value.travelTime = Number(q.travelTime);
  if (q.minTravelTime != null) filterState.value.minTravelTime = Number(q.minTravelTime);
  if (q.minSafety != null)
    filterState.value.minSafetyScore = Number(q.minSafety);
  return true;
};

// 유저 ID 기반 퀵필터 로컬 캐시 유틸
const getQuickFilterCacheKey = () => {
  const userId = authStore.user?.userId || authStore.user?.id || 'guest';
  return `kb_quick_filter_state_${userId}`;
};

const saveQuickFilterToCache = (filters) => {
  if (!filters) return;
  try {
    const key = getQuickFilterCacheKey();
    localStorage.setItem(key, JSON.stringify(filters));
  } catch (e) {
    console.error('Failed to save quick filter cache:', e);
  }
};

const loadQuickFilterFromCache = () => {
  try {
    const key = getQuickFilterCacheKey();
    const cached = localStorage.getItem(key);
    if (cached) {
      return JSON.parse(cached);
    }
  } catch (e) {
    console.error('Failed to load quick filter cache:', e);
  }
  return null;
};

onMounted(async () => {
  const hasUrlQuery = parseUrlQueryToFilters();
  const cachedFilter = loadQuickFilterFromCache();

  if (hasUrlQuery) {
    appliedFilterState.value = JSON.parse(JSON.stringify(filterState.value));
  } else if (cachedFilter) {
    filterState.value = JSON.parse(JSON.stringify(cachedFilter));
    appliedFilterState.value = JSON.parse(JSON.stringify(cachedFilter));
  } else if (mapStore.hasSavedFilterState) {
    filterState.value = JSON.parse(JSON.stringify(mapStore.filterState));
    appliedFilterState.value = JSON.parse(
      JSON.stringify(mapStore.appliedFilterState),
    );
  } else {
    await loadOnboardingDefaultFilters();
    appliedFilterState.value = JSON.parse(JSON.stringify(filterState.value));
  }
  syncFiltersToUrlQuery(appliedFilterState.value);
  isQuickFilterReady.value = true;
  await fetchPropertiesFromBackend(false, true);
});

const getDestinationKey = (filters) => {
  const name = filters?.destination ?? '';
  const latitude = Number(filters?.destinationLat);
  const longitude = Number(filters?.destinationLng);

  const latKey = Number.isFinite(latitude) ? latitude : '';
  const lngKey = Number.isFinite(longitude) ? longitude : '';
  return `${name}|${latKey}|${lngKey}`;
};

const clearAmenitiesForDestinationChange = () => {
  amenityFilterRef.value?.resetFilters?.();
  activeAmenityFilters.value = [];
  amenityDetailFilters.value = [];
  amenitiesByProperty.value = {};
  selectedPropertyDetailAmenities.value = [];
  filterState.value.selectedAmenities = [];
  isAmenityDetailFilterOpen.value = false;
  emit('apply-amenity-filters', []);
};

const authStore = useAuthStore();

const handleChangeDestination = ({ name, lat, lng, address }) => {
  if (!name || lat == null || lng == null) return;
  const destName = name || address || '선택한 위치';
  const destAddress = address || '';

  filterState.value.destination = destName;
  filterState.value.destinationAddress = destAddress;
  filterState.value.destinationLat = Number(lat);
  filterState.value.destinationLng = Number(lng);

  // 지도 우측키로 목적지 변경 시에도 유저아이디 기반 최근 검색 기록에 저장
  saveRecentDestinationGlobal(
    {
      destName,
      destAddress,
      destLatitude: Number(lat),
      destLongitude: Number(lng),
    },
    authStore.user?.userId || authStore.user?.id,
  );

  handleApplyFilters(true);
};

const handleApplyFilters = async (showOverlay = false) => {
  if (
    getDestinationKey(appliedFilterState.value) !==
    getDestinationKey(filterState.value)
  ) {
    clearAmenitiesForDestinationChange();
  }

  appliedFilterState.value = JSON.parse(JSON.stringify(filterState.value));
  saveQuickFilterToCache(appliedFilterState.value);
  syncFiltersToUrlQuery(appliedFilterState.value);
  await fetchPropertiesFromBackend();
};

// ⭕ 이소크론 영역 보이기/가리기 버튼 토글 시 백엔드 재조회 없이 0ms 즉시 화면 반작용 동기화
watch(
  () => filterState.value?.showIsochrone,
  (val) => {
    if (appliedFilterState.value && val !== undefined) {
      appliedFilterState.value.showIsochrone = val;
      saveQuickFilterToCache(appliedFilterState.value);
    }
  },
);

const handleResetFilters = async () => {
  const confirmReset = window.confirm(
    '처음 설정한 온보딩 조건으로 필터를 되돌릴까요?',
  );
  if (!confirmReset) return;

  showFilterAnalysisLoading();
  const previousDestinationKey = getDestinationKey(appliedFilterState.value);
  await loadOnboardingDefaultFilters();

  if (previousDestinationKey !== getDestinationKey(filterState.value)) {
    clearAmenitiesForDestinationChange();
  }

  appliedFilterState.value = JSON.parse(JSON.stringify(filterState.value));
  await fetchPropertiesFromBackend();
};

const applyMobileOnboardingFilters = (filters) => {
  if (!filters) return;

  if (filters.destinationId != null) {
    filterState.value.destinationId = Number(filters.destinationId);
  }

  const destination = filters.destination;
  if (destination && typeof destination === 'object') {
    const destinationId = destination.destinationId ?? destination.destId;
    if (destinationId != null) {
      filterState.value.destinationId = Number(destinationId);
    }

    filterState.value.destination =
      destination.destName ||
      destination.destinationName ||
      destination.name ||
      filterState.value.destination;
    filterState.value.destinationAddress =
      destination.destAddress || destination.address || '';

    const latitude =
      destination.destLatitude ?? destination.latitude ?? destination.lat;
    const longitude =
      destination.destLongitude ?? destination.longitude ?? destination.lng;
    if (latitude != null && longitude != null) {
      filterState.value.destinationLat = Number(latitude);
      filterState.value.destinationLng = Number(longitude);
    }
  } else if (typeof destination === 'string' && destination.trim()) {
    filterState.value.destination = destination;
  }

  if (filters.transportMode)
    filterState.value.transportMode = filters.transportMode;
  if (filters.maxTravelTime != null)
    filterState.value.travelTime = Number(filters.maxTravelTime);
  if (filters.travelTime != null)
    filterState.value.travelTime = Number(filters.travelTime);
  if (filters.minTravelTime != null)
    filterState.value.minTravelTime = Number(filters.minTravelTime);
  if (filters.flexTime != null)
    filterState.value.flexTime = Number(filters.flexTime);
  if (filters.budgetDepositMin != null)
    filterState.value.minDeposit = Number(filters.budgetDepositMin);
  if (filters.budgetDeposit != null)
    filterState.value.maxDeposit = Number(filters.budgetDeposit);
  if (filters.budgetRentMin != null)
    filterState.value.minRent = Number(filters.budgetRentMin);
  if (filters.budgetRent != null)
    filterState.value.maxRent = Number(filters.budgetRent);
  if (filters.minSafetyScore != null) {
    filterState.value.minSafetyScore = Number(filters.minSafetyScore);
  }

  handleApplyFilters();
};

watch(() => props.appliedOnboardingFilters, applyMobileOnboardingFilters, {
  deep: true,
});

watch(() => props.filterResetVersion, handleResetFilters);

const loadAmenitiesForProperties = async () => {
  // 목록 필터 적용 시 미계산 편의시설은 서버에서 계산·캐시한 뒤 매물별 결과를 받는다.
  const filters = activeAmenityFilters.value;
  const propertyIds = baseFilteredProperties.value
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

const scheduleAmenityLoad = () => {
  if (!activeAmenityFilters.value.length) {
    amenityFilterLoading.value = false;
    amenitiesByProperty.value = {};
    return;
  }
  showFilterAnalysisLoading();
  if (amenityFilterDebounceTimer) clearTimeout(amenityFilterDebounceTimer);
  amenityFilterDebounceTimer = setTimeout(loadAmenitiesForProperties, 250);
};

onUnmounted(() => {
  mapStore.saveFilterState(filterState.value, appliedFilterState.value);
  propertyRequestSequence += 1;
  amenityRequestSequence += 1;
  if (amenityFilterDebounceTimer) clearTimeout(amenityFilterDebounceTimer);
  clearTimeout(filterAnalysisTimer);
});

// 네이버 Geocoder API를 활용한 실시간 동적 주소/장소 좌표(lat, lng) 자동 변환
// 네이버 Geocoder API를 활용한 실시간 동적 주소/장소 좌표(lat, lng) 자동 변환
watch(
  () => [filterState.value.destination, filterState.value.destinationAddress],
  ([newDest, newAddr], oldVal) => {
    if (!newDest && !newAddr) return;
    const [oldDest, oldAddr] = oldVal || [];

    // 목적지 텍스트가 변경되었을 경우, 이전 송파구 좌표 등에 고정되지 않도록 즉시 최신 주소/장소 Geocoding을 실행
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
            const newLat = Number(item.y);
            const newLng = Number(item.x);
            if (
              filterState.value.destinationLat !== newLat ||
              filterState.value.destinationLng !== newLng
            ) {
              filterState.value.destinationLat = newLat;
              filterState.value.destinationLng = newLng;
              fetchPropertiesFromBackend(true);
            }
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
    id: filterState.value.destinationId || null,
    name,
    address: filterState.value.destinationAddress || '',
    lat,
    lng,
  };
});

// 퀵버튼 필터 + 도보/대중교통 도달 범위(Reach) + 5종 정렬 연동 로직 (appliedFilterState 기준 연산)
const baseFilteredProperties = computed(() => {
  const currentFilters = appliedFilterState.value;
  // 적용된 목적지 위경도 좌표
  const destLat =
    Number(currentFilters.destinationLat) || destinationConfig.value.lat;
  const destLng =
    Number(currentFilters.destinationLng) || destinationConfig.value.lng;

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
    if (currentFilters.tradeType === 'JEONSE' && p.monthlyRent > 0)
      return false;

    // 2. 보증금 / 전세금 필터 (minDeposit ~ maxDeposit 단위: 만원 & 대출 레버리지 한도 증액 반영)
    let effectiveMinDeposit = currentFilters.minDeposit || 0;
    let effectiveMaxDeposit = currentFilters.maxDeposit;
    if (
      currentFilters.selectedLoanId &&
      currentFilters.selectedLoanId !== 'NONE'
    ) {
      const loan = LOAN_PRODUCTS.find(
        (l) => l.id === currentFilters.selectedLoanId,
      );
      if (loan && loan.ratio > 0) {
        effectiveMaxDeposit = Math.round(
          currentFilters.maxDeposit * (1 + loan.ratio),
        );
      }
    }
    if (p.deposit < effectiveMinDeposit || p.deposit > effectiveMaxDeposit)
      return false;

    // 3. 월세 필터 (minRent ~ maxRent 단위: 만원)
    if (currentFilters.tradeType === 'MONTHLY') {
      const minRent = currentFilters.minRent || 0;
      const maxRent = currentFilters.maxRent;
      if (p.monthlyRent < minRent || p.monthlyRent > maxRent) return false;
    }

    // 4. 안전 점수 필터
    if (p.safetyScore < currentFilters.minSafetyScore) return false;

    // 5. 도보 / 대중교통 도달 범위 (Reach Distance) 도넛 링 필터 (최소 ~ 최대 시간)
    const distKm = getHaversineDistance(
      destLat,
      destLng,
      p.latitude,
      p.longitude,
    );
    const distMeters = distKm * 1000;
    const minTravelTime = currentFilters.minTravelTime || 0;

    if (currentFilters.transportMode === 'WALK') {
      let speedMetersPerMin = 75;
      if (currentFilters.walkPace === 'SLOW') speedMetersPerMin = 58;
      if (currentFilters.walkPace === 'FAST') speedMetersPerMin = 92;
      const minReachMeters =
        minTravelTime > 0 ? minTravelTime * speedMetersPerMin : 0;
      const maxReachMeters = Math.max(
        200,
        currentFilters.travelTime * speedMetersPerMin,
      );
      if (distMeters < minReachMeters || distMeters > maxReachMeters)
        return false;
    } else {
      // 대중교통 모드 (TRANSIT): 내접원(transitBaseRadius: minTime) 바깥 ~ 외접원(transitMaxRadius: travelTime) 안쪽 사이 도넛 영역 매물만 노출
      const travelTime = currentFilters.travelTime || 15;
      const flexTime = currentFilters.flexTime != null ? currentFilters.flexTime : 10;

      let minTime = 5;
      if (currentFilters.minTravelTime != null && currentFilters.minTravelTime > 0) {
        minTime = currentFilters.minTravelTime;
      } else if (currentFilters.flexTime != null && currentFilters.flexTime > 0) {
        minTime = currentFilters.flexTime;
      }

      const transitMaxRadius = Math.max(500, travelTime * 180);
      const transitBaseRadius = Math.max(200, minTime * 180);

      // 내접원 안쪽(minTime 미만) 및 외접원 바깥(travelTime 초과) 매물 엄격 제외 (도넛 링 이소크론 구간만 통과)
      if (distMeters < transitBaseRadius || distMeters > transitMaxRadius)
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

// 온보딩으로 후보 매물을 먼저 줄이고, 그 후보들에만 편의시설 필터를 적용
watch([activeAmenityFilters, baseFilteredProperties], scheduleAmenityLoad, {
  deep: true,
});

const amenityFilteredProperties = computed(() => {
  if (!activeAmenityFilters.value.length || amenityFilterLoading.value) {
    return baseFilteredProperties.value;
  }

  const requiredTypes = new Set(
    activeAmenityFilters.value.map((filter) => filter.amenityType),
  );
  return baseFilteredProperties.value.filter((property) => {
    const propertyAmenities =
      amenitiesByProperty.value[property.propertyId] ?? [];
    const matchedTypes = new Set(
      propertyAmenities.map((amenity) => amenity.amenityType),
    );
    return [...requiredTypes].every((type) => matchedTypes.has(type));
  });
});

const visibleProperties = computed(() =>
  activeAmenityFilters.value.length && amenityFilterLoading.value
    ? []
    : amenityFilteredProperties.value,
);

const clearSelectedProperty = () => {
  selectedProperty.value = null;
  selectedPropertyDetailAmenities.value = [];
  isPanelOpen.value = false;
};

watch(
  [visibleProperties, selectedProperty, isMapAnalysisLoading],
  ([nextProperties, currentProperty, isLoading]) => {
    if (!currentProperty || isLoading) return;

    const isStillVisible = nextProperties.some(
      (property) =>
        Number(property.propertyId) === Number(currentProperty.propertyId),
    );

    if (!isStillVisible) clearSelectedProperty();
  },
);

const shouldHideAmenityPins = computed(
  () => activeAmenityFilters.value.length > 0 && amenityFilterLoading.value,
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

  const savedBookmarkProperty = sessionStorage.getItem(
    'selectedBookmarkProperty',
  );
  const bookmarkedProperty = savedBookmarkProperty
    ? JSON.parse(savedBookmarkProperty)
    : null;
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
    const { data } = await api.get(`/properties/${numericPropertyId}`, {
      params: {
        destinationId: appliedFilterState.value.destinationId || undefined,
      },
    });
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
  return (
    amenitiesByProperty.value[propertyId] ??
    selectedPropertyDetailAmenities.value
  );
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
  showFilterAnalysisLoading();
  activeAmenityFilters.value = selectedList.map((filter) => ({ ...filter }));
  filterState.value.selectedAmenities = selectedList.map(
    (filter) => filter.amenityType,
  );
  emit('apply-amenity-filters', activeAmenityFilters.value);
};

// 편의시설 선택 시 기본 디폴트 시간 적용 (편의점: 5분, 기타: 10분)
const normalizeAmenitySelection = (selectedFilters) =>
  selectedFilters.map((filter) => {
    const appliedFilter = activeAmenityFilters.value.find(
      (item) => item.amenityType === filter.amenityType,
    );
    const detailFilter = amenityDetailFilters.value.find(
      (item) => item.id === filter.id,
    );

    // 편의점은 5분, 그 외 기타 편의시설은 15분을 디폴트 기본값으로 설정
    const defaultWalkTime =
      Number(filter.amenityType) === 1
        ? DEFAULT_CONVENIENCE_STORE_WALK_TIME
        : DEFAULT_AMENITY_WALK_TIME;

    const walkTimeMinutes = Number(
      appliedFilter?.walkTimeMinutes ??
        detailFilter?.timeLimit ??
        filter.timeLimit ??
        defaultWalkTime,
    );

    return {
      ...filter,
      timeLimit: walkTimeMinutes,
      walkTimeMinutes,
    };
  });

const openAmenityDetailFilter = (selectedFilters) => {
  if (isAmenityDetailFilterOpen.value) {
    isAmenityDetailFilterOpen.value = false;
    return;
  }

  const filters =
    selectedFilters ?? amenityFilterRef.value?.getSelectedAmenities?.() ?? [];
  amenityDetailFilters.value = normalizeAmenitySelection(filters).map(
    (filter) => ({ ...filter }),
  );
  isAmenityDetailFilterOpen.value = true;
};

const syncAmenityDetailFilter = (selectedFilters) => {
  if (!isAmenityDetailFilterOpen.value) return;
  amenityDetailFilters.value = normalizeAmenitySelection(selectedFilters).map(
    (filter) => {
      const existingFilter = amenityDetailFilters.value.find(
        (item) => item.id === filter.id,
      );
      return {
        ...filter,
        timeLimit: existingFilter?.timeLimit ?? filter.timeLimit,
      };
    },
  );
};

// PC 필터에서 이미 적용된 항목을 해제하면, 상세 필터를 다시 열지 않아도 즉시 반영한다.
const handleAmenitySelectionChange = (selectedFilters) => {
  syncAmenityDetailFilter(selectedFilters);
  const normalizedFilters = normalizeAmenitySelection(selectedFilters);
  handleApplyAmenities(
    normalizedFilters.map((filter) => ({
      amenityType: filter.amenityType,
      walkTimeMinutes: Number(filter.walkTimeMinutes),
    })),
  );
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

const activePopoverName = ref(null);
const handlePopoverChange = (name) => {
  activePopoverName.value = name;
  if (!name && appliedFilterState.value && filterState.value) {
    // 팝오버 모달이 닫힐 때, 미적용 드래프트 변경사항이 있는 경우에만 원래 적용값으로 복구 (불필요한 재탐색/로딩 100% 방지)
    const draftJson = JSON.stringify(filterState.value);
    const appliedJson = JSON.stringify(appliedFilterState.value);
    if (draftJson !== appliedJson) {
      Object.assign(filterState.value, JSON.parse(appliedJson));
    }
  }
};

const isPreviewingIsochrone = computed(() => {
  return activePopoverName.value === 'travel';
});

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
    class="relative w-full flex-1 min-h-0 h-full overflow-hidden bg-slate-100 xl:flex xl:flex-row"
  >
    <!-- 1. 매물 탐색 사이드바 (마우스 및 터치 실시간 드래그 지원 / PC: md:flex-row 좌측 고정) -->
    <aside
      class="mobile-aside-panel absolute inset-x-0 bottom-0 z-20 flex w-full flex-col overflow-hidden rounded-t-[22px] border-t border-slate-200 bg-white shadow-2xl transition-all ease-out xl:relative xl:inset-auto xl:w-[380px] xl:shrink-0 xl:overflow-visible xl:rounded-none xl:border-t-0 xl:border-r"
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
      <div
        class="p-4 pt-1 pb-1 border-b-0 bg-white space-y-3 xl:space-y-0 xl:pt-3"
      >
        <div class="flex items-center justify-between xl:hidden">
          <span
            v-if="isMapAnalysisLoading"
            class="inline-flex shrink-0 items-center gap-1.5 text-[13px] font-bold text-[#5267e8]"
          >
            <i class="fa-solid fa-spinner animate-spin" aria-hidden="true"></i>
            안전 분석 중
          </span>
          <span
            v-else
            class="inline-flex shrink-0 items-center text-[13px] font-bold text-slate-500"
          >
            총 {{ visibleProperties.length }}개 매물
          </span>
        </div>

        <!-- PC: 온보딩에서 설정한 탐색 조건 요약 -->
        <OnboardingSummary
          class="desktop-sidebar-summary hidden xl:block summary--sidebar border-b border-slate-200 pb-3"
          :destination="filterState.destination"
          :transport-mode="filterState.transportMode"
          :travel-time="filterState.travelTime"
          :max-deposit="filterState.maxDeposit"
          :max-rent="filterState.maxRent"
          :min-safety-score="filterState.minSafetyScore"
          :show-close="false"
        />

        <!-- 항상 노출되는 편의시설 필터와 상세 설정 -->
        <section
          class="relative hidden border-b border-slate-200 py-3 xl:block"
        >
          <div class="flex w-full items-center justify-between mb-1">
            <h2 class="text-[16px] font-bold text-[#1e293b]">편의시설 필터</h2>

            <button
              type="button"
              class="flex h-8 shrink-0 items-center gap-1 rounded-full border border-slate-200 bg-white px-3 text-xs font-bold text-[#3e55df] shadow-sm transition-all hover:border-[#b9c5ff] hover:bg-[#f5f7ff] active:scale-[0.98]"
              @click="openAmenityDetailFilter()"
            >
              <i class="fa-solid fa-sliders text-[10px]" aria-hidden="true"></i>
              <span>상세 필터</span>
              <i
                class="fa-solid fa-chevron-right text-[9px]"
                aria-hidden="true"
              ></i>
            </button>
          </div>

          <AmenityFilter
            ref="amenityFilterRef"
            class="desktop-amenity-filter"
            :applied-filters="activeAmenityFilters"
            :show-walking-time="false"
            @apply="handleApplyAmenities"
            @selection-change="handleAmenitySelectionChange"
          />

          <p class="mt-2 text-[12px] leading-5 text-slate-500">
            <span class="notice-icon" aria-hidden="true">⚠</span>
            선택한 조건이 모두 반영되어 검색 결과가 다소 적을 수 있어요
          </p>

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
        <!-- 모바일: 가로 스크롤 정렬 버튼 -->
        <div
          class="mobile-sort-options flex items-center gap-1.5 overflow-x-auto pb-1 xl:hidden"
        >
          <button
            v-for="opt in sortOptions"
            :key="opt.key"
            type="button"
            class="shrink-0 rounded-full border px-3 py-1.5 text-[11px] font-semibold transition-all"
            :class="[
              currentSort === opt.key
                ? 'border-[#4058f5] bg-[#eef1ff] text-[#4058f5]'
                : 'border-slate-200 bg-white text-slate-500 hover:border-slate-300 hover:bg-slate-50',
            ]"
            @click="currentSort = opt.key"
          >
            <i :class="[opt.icon, 'text-[10px]']" aria-hidden="true"></i>
            {{ opt.label }}
          </button>
        </div>

        <!-- PC: 너비 안에서 펼쳐지는 정렬 버튼 -->
        <section class="hidden space-y-2 pt-3 xl:block">
          <p
            v-if="isMapAnalysisLoading"
            class="m-0 flex items-center gap-1.5 text-[13px] font-bold text-[#5267e8]"
          >
            <i class="fa-solid fa-spinner animate-spin" aria-hidden="true"></i>
            안전 분석 중
          </p>
          <p v-else class="m-0 text-[13px] font-bold text-slate-500">
            총 {{ visibleProperties.length }}개 매물
          </p>
          <div class="flex flex-wrap items-center gap-1.5 pb-1">
            <button
              v-for="opt in isDesktopSortExpanded
                ? sortOptions
                : sortOptions.slice(0, 3)"
              :key="opt.key"
              type="button"
              class="shrink-0 rounded-full border px-3 py-1.5 text-[11px] font-semibold transition-all"
              :class="
                currentSort === opt.key
                  ? 'border-[#4058f5] bg-[#eef1ff] text-[#4058f5]'
                  : 'border-slate-200 bg-white text-slate-500 hover:border-slate-300 hover:bg-slate-50'
              "
              @click="currentSort = opt.key"
            >
              <i :class="[opt.icon, 'mr-1 text-[10px]']" aria-hidden="true"></i>
              {{ opt.label }}
            </button>
            <button
              type="button"
              class="inline-flex h-7 w-7 items-center justify-center rounded-full border text-xs transition-colors"
              :class="
                isDesktopSortExpanded ||
                sortOptions.slice(3).some((opt) => opt.key === currentSort)
                  ? 'border-[#4058f5] bg-[#eef1ff] text-[#4058f5]'
                  : 'border-slate-200 bg-white text-slate-500 hover:bg-slate-50'
              "
              :aria-label="
                isDesktopSortExpanded
                  ? '정렬 옵션 접기'
                  : '추가 정렬 옵션 펼치기'
              "
              @click="isDesktopSortExpanded = !isDesktopSortExpanded"
            >
              <i
                :class="
                  isDesktopSortExpanded
                    ? 'fa-solid fa-chevron-up'
                    : 'fa-solid fa-ellipsis'
                "
                aria-hidden="true"
              ></i>
            </button>
          </div>
        </section>
      </div>

      <!-- 사이드바 매물 카드리스트 (스크롤) -->
      <div class="property-list-scroll flex-1 overflow-y-auto p-3 space-y-2.5">
        <template v-if="isPropertyLoading">
          <div
            v-for="index in 3"
            :key="index"
            class="property-card-skeleton animate-pulse"
          >
            <div class="property-card-skeleton__image"></div>
            <div class="property-card-skeleton__content">
              <span class="property-card-skeleton__tag"></span>
              <span class="property-card-skeleton__title"></span>
              <span class="property-card-skeleton__text"></span>
            </div>
          </div>
        </template>
        <template v-else-if="visibleProperties.length > 0">
          <PropertyCard
            v-for="prop in visibleProperties"
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
          v-else-if="!amenityFilterLoading"
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
        <div
          v-else
          class="h-full flex items-center justify-center p-6 text-center text-sm font-medium text-slate-500"
        >
          매물을 조회하고 있어요.
        </div>
      </div>
    </aside>

    <!-- 2. 중앙 메인 지도 캔버스 (Full-bleed) -->
    <main
      class="absolute inset-0 z-10 xl:relative xl:inset-auto xl:h-full xl:flex-1"
    >
      <!-- 🗺️ 지도 상단 부유형(Floating) 퀵버튼 바 (요소 크기 맞춤 w-fit) -->
      <div class="absolute top-4 left-4 z-30 flex flex-col items-start gap-3 pointer-events-none">
        <MapQuickFilterBar
          v-model="filterState"
          :total-count="visibleProperties.length"
          class="pointer-events-auto"
          @open-filter="emit('open-filter')"
          @popover-change="handlePopoverChange"
          @apply="handleApplyFilters"
          @reset="handleResetFilters"
        />
        <RouteFeedbackCard
          v-if="isPanelOpen && selectedProperty"
          :key="selectedProperty.propertyId"
          class="pointer-events-auto"
        />
      </div>

      <NaverMap
        :properties="visibleProperties"
        :selected-property="selectedProperty"
        :amenities="selectedPropertyAmenities"
        :destination="destinationConfig"
        :applied-filter="appliedFilterState"
        :live-filter="filterState"
        :is-preview-mode="isPreviewingIsochrone"
        @select-property="handleSelectProperty"
        @change-destination="handleChangeDestination"
      />

      <Transition name="analysis-loader">
        <div
          v-if="isMapAnalysisLoading"
          class="pointer-events-none absolute inset-0 z-20 flex items-center justify-center"
        >
          <div
            class="flex max-w-[320px] flex-col items-center rounded-[22px] border border-white/70 bg-white/90 px-7 py-6 text-center shadow-xl backdrop-blur-md"
          >
            <span
              class="mb-3.5 flex h-12 w-12 items-center justify-center rounded-2xl bg-[#eef1ff] text-xl text-[#4058f5]"
            >
              <i class="fa-solid fa-shield-halved" aria-hidden="true"></i>
            </span>
            <strong class="text-[15px] font-extrabold text-slate-800">
              안전 귀갓길과 생활 조건을 분석 중이에요
            </strong>
            <span class="mt-1.5 text-[13px] leading-5 text-slate-500">
              잠시만 기다리시면 맞춤 매물을 보여드릴게요
            </span>
            <span
              class="mt-4 h-1.5 w-32 overflow-hidden rounded-full bg-slate-100"
            >
              <span
                class="analysis-loader-bar block h-full rounded-full bg-[#4058f5]"
              ></span>
            </span>
          </div>
        </div>
      </Transition>

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
      :destination="destinationConfig"
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

.property-card-skeleton {
  display: flex;
  min-height: 104px;
  gap: 12px;
  padding: 12px;
  border: 1px solid #e0e7ff;
  border-radius: 16px;
  background: #f8faff;
}

.property-card-skeleton__image {
  width: 88px;
  flex: 0 0 88px;
  border-radius: 12px;
  background: #e8edf8;
}

.property-card-skeleton__content {
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: center;
  gap: 9px;
}

.property-card-skeleton__content span {
  display: block;
  border-radius: 999px;
  background: #e8edf8;
}

.property-card-skeleton__tag {
  width: 52px;
  height: 16px;
}

.property-card-skeleton__title {
  width: 82%;
  height: 18px;
}

.property-card-skeleton__text {
  width: 62%;
  height: 13px;
}

.analysis-loader-bar {
  animation: analysis-loading 1.1s ease-in-out infinite;
}

.analysis-loader-enter-active,
.analysis-loader-leave-active {
  transition:
    opacity 180ms ease,
    transform 180ms ease;
}

.analysis-loader-enter-from,
.analysis-loader-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

@keyframes analysis-loading {
  0% {
    width: 24%;
    margin-left: 0;
  }
  50% {
    width: 58%;
    margin-left: 30%;
  }
  100% {
    width: 24%;
    margin-left: 76%;
  }
}

@media (min-width: 1280px) {
  .desktop-amenity-filter :deep(.filter-content) {
    padding: 4px 0 8px;
  }

  .desktop-amenity-filter :deep(.amenity-type-filter) {
    padding: 4px 0 8px;
    border-bottom: 0;
  }

  .property-list-scroll {
    scrollbar-width: thin;
    scrollbar-color: #d7deea transparent;
  }

  .property-list-scroll::-webkit-scrollbar {
    width: 5px;
  }

  .property-list-scroll::-webkit-scrollbar-thumb {
    border-radius: 999px;
    background: #d7deea;
  }

  .notice-icon {
    flex-shrink: 0;
    font-size: 12px;
  }
}
</style>
