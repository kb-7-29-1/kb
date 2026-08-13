<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '@/api/api.js';
import NaverMap from '@/components/map/NaverMap.vue';
import PropertyCard from '@/components/property/PropertyCard.vue';
import SlidingDoorPanel from '@/components/detail/SlidingDoorPanel.vue';
import MapQuickFilterBar from '@/components/map/MapQuickFilterBar.vue';
import LoadMoreButton from '@/components/map/LoadMoreButton.vue';

import RouteFeedbackCard from '@/components/map/RouteFeedbackCard.vue';
import AmenityFilter from '@/components/map/AmenityFilter.vue';
import AmenityDetailFilterPanel from '@/components/map/AmenityDetailFilterPanel.vue';
import PropertySortBar from '@/components/map/PropertySortBar.vue';
import OnboardingSummary from '@/components/map/OnboardingSummary.vue';
import { getHaversineDistance, calculateDistanceKm } from '@/utils/geo.js';
import { useMobilePanelDrag } from '@/composables/useMobilePanelDrag.js';
import { useOnboardingFilter } from '@/composables/useOnboardingFilter.js';
import { useMapBounds } from '@/composables/useMapBounds.js';
import { useMapUrlSync } from '@/composables/useMapUrlSync.js';
import { usePropertySearch } from '@/composables/usePropertySearch.js';
import { useMapStore } from '@/stores/useMapStore.js';
import { useAuthStore } from '@/stores/useAuthStore.js';
import {
  saveRecentDestinationGlobal,
  getRecentDestinations,
  findMatchingDestination,
} from '@/utils/recentDestinations.js';
import {
  DEFAULT_DEPOSIT,
  DEFAULT_RENT,
  RENT_MAX,
  LOAN_PRODUCTS,
} from '@/utils/budget';
import { mockProperties } from '@/mock/mockProperties.js';
import amenityService from '@/api/amenityService.js';
import safetyService from '@/api/safetyService.js';
import onboardingApi from '@/api/onboardingApi.js';
import aiVoiceSearchService from '@/api/aiVoiceSearchService.js';
import AiVoiceSearchBar from '@/components/map/AiVoiceSearchBar.vue';
import {
  getSearchRadiusKm,
  getMinSearchRadiusKm,
  isWithinReachDistance,
} from '@/utils/isochroneFilter.js';
import DistrictToast from '@/components/common/DistrictToast.vue';
import { isSupportedSafetyDistrict } from '@/utils/districtSupport.js';
import { useDistrictToast } from '@/composables/useDistrictToast.js';

const emit = defineEmits(['open-filter', 'apply-amenity-filters']);
const route = useRoute();
const router = useRouter();

const { showToast } = useDistrictToast();
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
let isAmenityUrlSyncReady = false;

watch(
  () => props.appliedAmenityFilters,
  (filters = []) => {
    activeAmenityFilters.value = filters.map((filter) => ({ ...filter }));
    if (isAmenityDetailFilterOpen.value) {
      const filtersByAmenityType = new Map(
        filters.map((filter) => [Number(filter.amenityType), filter]),
      );
      amenityDetailFilters.value = amenityDetailFilters.value
        .filter((filter) =>
          filtersByAmenityType.has(Number(filter.amenityType)),
        )
        .map((filter) => {
          const applied = filtersByAmenityType.get(Number(filter.amenityType));
          return {
            ...filter,
            timeLimit: Number(
              applied.walkTimeMinutes ?? applied.timeLimit ?? filter.timeLimit,
            ),
            walkTimeMinutes: Number(
              applied.walkTimeMinutes ?? applied.timeLimit ?? filter.timeLimit,
            ),
          };
        });
    }
    filterState.value.selectedAmenities = filters.map(
      (filter) => filter.amenityType,
    );
    if (isAmenityUrlSyncReady) syncFiltersToUrlQuery(filterState.value);
  },
  { immediate: true, deep: true, flush: 'sync' },
);

// 선택된 매물 & 우측 상세 패널 열림 상태
const selectedProperty = ref(null);
const isPanelOpen = ref(false);
const selectedPropertyDetailAmenities = ref([]);
// 북마크/공유링크로 직접 연 매물은 현재 검색 목록에 없어도 자동으로 닫히지 않도록 예외 처리
const externallyOpenedPropertyId = ref(null);

// 선택 매물의 TMAP 안전 경로 상태
// selectedSafetyRoute.routePoints를 NaverMap에 전달해 실제 폴리라인을 그립니다.
const selectedSafetyRoute = ref(null);
const selectedSafetyRouteMeta = ref(null);
const isSafetyRouteLoading = ref(false);
const safetyRouteError = ref('');
let safetyRouteRequestSequence = 0;

// 매물 목록 데이터 (기본값: mockProperties 더미 데이터 백업)
const amenitiesByProperty = ref({});
const amenityFilterLoading = ref(false);
let amenityRequestSequence = 0;
let amenityFilterDebounceTimer = null;

// 매물 목록 데이터 (usePropertySearch 사용)
const {
  saveQuickFilterToCache,
  loadQuickFilterFromCache,
  syncFiltersToUrlQuery: rawSyncFiltersToUrlQuery,
  parseUrlQueryToFilters: rawParseUrlQueryToFilters,
} = useMapUrlSync();

const {
  properties,
  serverTotalCount,
  currentPage,
  isPropertyLoading,
  isPropertyApiError,
  isMoreLoading,
  showAllLoadedToast,
  triggerAllLoadedToast,
  buildPropertySearchParams: rawBuildPropertySearchParams,
  doFetchPropertiesFromBackend: rawDoFetchPropertiesFromBackend,
  loadMoreProperties: executeLoadMoreProperties,
} = usePropertySearch();

const syncFiltersToUrlQuery = (filters) => {
  rawSyncFiltersToUrlQuery(
    {
      ...filters,
      selectedAmenities:
        activeAmenityFilters.value.length > 0
          ? activeAmenityFilters.value
          : filters.selectedAmenities,
    },
    route,
    router,
    selectedProperty.value?.propertyId,
  );
};

const parseUrlQueryToFilters = ({ includeDestination = true } = {}) => {
  return rawParseUrlQueryToFilters(route, filterState, { includeDestination });
};

const isFilterAnalysisLoading = ref(false);
let propertyRequestSequence = 0;
let filterAnalysisTimer = null;

const showFilterAnalysisLoading = () => {
  isFilterAnalysisLoading.value = true;
  clearTimeout(filterAnalysisTimer);
  filterAnalysisTimer = setTimeout(() => {
    isFilterAnalysisLoading.value = false;
  }, 0);
  // 450ms -> 0ms
  // 이미 filterAnalysis 과정에서 로딩 지연있어서 2중 발생
};

let analysisLoadingTimeoutTimer = null;

const isMapAnalysisLoading = computed(
  () =>
    isPropertyLoading.value ||
    amenityFilterLoading.value ||
    isFilterAnalysisLoading.value,
);

watch(isMapAnalysisLoading, (isLoading) => {
  clearTimeout(analysisLoadingTimeoutTimer);
  if (isLoading) {
    analysisLoadingTimeoutTimer = setTimeout(() => {
      // 5초 경과 시 로딩 오버레이 중단 및 현재까지 수집된 매물만 표시
      isPropertyLoading.value = false;
      amenityFilterLoading.value = false;
      isFilterAnalysisLoading.value = false;
    }, 5000);
  }
});

// 데이터 출처 계산 (DB vs PUBLIC_API)
const currentDataSource = computed(() => {
  return properties.value[0]?.dataSource || 'PUBLIC_API';
});

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
  return rawBuildPropertySearchParams(
    appliedFilterState,
    destinationConfig,
    authStore,
  );
};

const {
  isMapMoved,
  pendingBounds,
  lastFetchedCenter,
  handleBoundsChange,
  applyPendingBoundsToFilters,
  clearBoundsFromFilters,
  updateLastFetchedCenter,
} = useMapBounds();

const handleLoadMoreClick = () => {
  if (isMapMoved.value) {
    handleSearchInThisArea();
  } else if (properties.value.length >= serverTotalCount.value) {
    triggerAllLoadedToast();
  } else {
    loadMoreProperties();
  }
};

const handleSearchInThisArea = () => {
  if (applyPendingBoundsToFilters(appliedFilterState.value)) {
    fetchPropertiesFromBackend(true, false);
  }
};

const loadMoreProperties = async () => {
  return executeLoadMoreProperties({
    appliedFilterState,
    destinationConfig,
    authStore,
  });
};

let fetchPropertiesDebounceTimer = null;

// 검색 조건에 맞는 매물을 조회한 뒤, 화면에 렌더링하기 전에 목적지별 안전점수를 일괄 준비합니다.
const doFetchPropertiesFromBackend = async (isAppend = false) => {
  return rawDoFetchPropertiesFromBackend({
    appliedFilterState,
    destinationConfig,
    authStore,
    isMapMoved,
    pendingBounds,
    clearBoundsFromFilters,
    updateLastFetchedCenter,
    isAppend,
  });
};

// 짧은 시간(250ms) 내 연달아 발생하는 중복 API 호출을 단 1회로 통합 실행합니다.
const fetchPropertiesFromBackend = (immediate = false, isAppend = false) => {
  if (fetchPropertiesDebounceTimer) {
    clearTimeout(fetchPropertiesDebounceTimer);
    fetchPropertiesDebounceTimer = null;
  }

  if (immediate === true) {
    return doFetchPropertiesFromBackend(isAppend);
  }

  return new Promise((resolve) => {
    fetchPropertiesDebounceTimer = setTimeout(async () => {
      fetchPropertiesDebounceTimer = null;
      const res = await doFetchPropertiesFromBackend(isAppend);
      resolve(res);
    }, 250);
  });
};

// URL 주소창 & 퀵필터 로컬 캐시 동기화 인터페이스

onMounted(async () => {
  // 0. 비동기 통신(await) 전에 동기적으로 URL 주소창 쿼리를 0.001ms만에 즉시 파싱하여 0.2초 깜빡임 완전 제거!
  const hasUrlQuery = parseUrlQueryToFilters({ includeDestination: true });

  // 1. URL 쿼리가 없는 경우에만 유저 직전 퀵 필터 로컬 캐시 적용
  let hasCachedDestination = false;
  if (!hasUrlQuery) {
    const cachedFilters = loadQuickFilterFromCache();
    if (cachedFilters) {
      Object.assign(filterState.value, cachedFilters);
      hasCachedDestination = !!(
        cachedFilters.destinationLat && cachedFilters.destinationLng
      );
    }
  }

  // 2. 기본 DB 온보딩 값 로드 (URL 또는 캐시에 목적지가 이미 존재하는 경우 덮어쓰기 차단)
  await loadOnboardingDefaultFilters({
    resetDestination: !hasUrlQuery && !hasCachedDestination,
  });

  // 3. URL 주소창 Query 파라미터 100% 최우선 보장
  parseUrlQueryToFilters({ includeDestination: true });

  const restoredAmenityFilters =
    route.query.amenities != null
      ? filterState.value.selectedAmenities
      : props.appliedAmenityFilters.length > 0
        ? props.appliedAmenityFilters
        : filterState.value.selectedAmenities;

  if (Array.isArray(restoredAmenityFilters)) {
    activeAmenityFilters.value = restoredAmenityFilters.map((amenity) => {
      const amenityType = Number(
        typeof amenity === 'object' ? amenity.amenityType : amenity,
      );
      const defaultWalkTime =
        amenityType === 1
          ? DEFAULT_CONVENIENCE_STORE_WALK_TIME
          : DEFAULT_AMENITY_WALK_TIME;

      return {
        amenityType,
        walkTimeMinutes: Number(
          typeof amenity === 'object'
            ? (amenity.walkTimeMinutes ?? defaultWalkTime)
            : defaultWalkTime,
        ),
      };
    });
    filterState.value.selectedAmenities = activeAmenityFilters.value.map(
      (filter) => filter.amenityType,
    );
  }

  appliedFilterState.value = JSON.parse(JSON.stringify(filterState.value));
  syncFiltersToUrlQuery(appliedFilterState.value);
  isAmenityUrlSyncReady = true;
  emit('apply-amenity-filters', activeAmenityFilters.value);
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

  // 목적지가 바뀌면 같은 매물이라도 캐시 키와 실제 TMAP 경로가 달라지므로
  // 이전 목적지의 선택/경로를 즉시 제거합니다.
  safetyRouteRequestSequence += 1;
  selectedProperty.value = null;
  selectedSafetyRoute.value = null;
  selectedSafetyRouteMeta.value = null;
  safetyRouteError.value = '';
  isSafetyRouteLoading.value = false;
  isPanelOpen.value = false;
  externallyOpenedPropertyId.value = null;

  emit('apply-amenity-filters', []);
};

const authStore = useAuthStore();

const handleChangeDestination = ({ name, lat, lng, address }) => {
  if (!name || lat == null || lng == null) return;
  const destAddress = address || '';

  const userId = authStore.user?.userId || authStore.user?.id;
  const recentList = getRecentDestinations(userId) || [];
  const matched = findMatchingDestination(
    name,
    destAddress,
    recentList,
    lat,
    lng,
  );

  const finalDestName =
    matched?.destName || name || destAddress || '선택한 위치';

  filterState.value.destination = finalDestName;
  filterState.value.destinationAddress = destAddress;
  filterState.value.destinationLat = Number(lat);
  filterState.value.destinationLng = Number(lng);
  filterState.value.destinationId = matched?.destinationId || null;

  // 지도 우측키로 목적지 변경 시에도 유저아이디 기반 최근 검색 기록에 저장
  saveRecentDestinationGlobal(
    {
      destName: finalDestName,
      destAddress,
      destLatitude: Number(lat),
      destLongitude: Number(lng),
    },
    userId,
  );

  handleApplyFilters(true);
};

const handleApplyFilters = async (
  showOverlay = false,
  { preserveAmenities = false } = {},
) => {
  if (
    getDestinationKey(appliedFilterState.value) !==
      getDestinationKey(filterState.value) &&
    !preserveAmenities
  ) {
    clearAmenitiesForDestinationChange();
  }

  // 필터 적용 또는 목적지 변경 시 이전 지도 이동 영역(Bounding Box)을 해제하여
  // 새 목적지 중심 원형 검색이 즉시 정상 작동하도록 처리
  filterState.value.swLat = undefined;
  filterState.value.swLng = undefined;
  filterState.value.neLat = undefined;
  filterState.value.neLng = undefined;

  isMapMoved.value = false;
  pendingBounds.value = null;

  appliedFilterState.value = JSON.parse(JSON.stringify(filterState.value));
  saveQuickFilterToCache(appliedFilterState.value);
  syncFiltersToUrlQuery(appliedFilterState.value);
  await fetchPropertiesFromBackend(true, false);
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
  showFilterAnalysisLoading();
  await loadOnboardingDefaultFilters({ resetDestination: true });

  appliedFilterState.value = JSON.parse(JSON.stringify(filterState.value));
  saveQuickFilterToCache(appliedFilterState.value);
  syncFiltersToUrlQuery(appliedFilterState.value);
  mapStore.saveFilterState(filterState.value, appliedFilterState.value);
  await fetchPropertiesFromBackend();
};

// AI 음성검색 상태
// 음성 -> 브라우저 SpeechRecognition -> 텍스트 -> GPT-5 Nano -> 기존 검색필터 적용
const isAiVoiceSearchLoading = ref(false);
const aiVoiceTranscript = ref('');
const aiVoiceMessage = ref('');
const aiVoiceError = ref('');

const AI_RESET_DEFAULTS = {
  tradeType: 'MONTHLY',
  minDeposit: 0,
  maxDeposit: DEFAULT_DEPOSIT,
  minRent: 0,
  maxRent: DEFAULT_RENT,
  minSafetyScore: 0,
  transportMode: 'WALK',
  minTravelTime: 0,
  travelTime: 15,
  walkPace: 'NORMAL',
  flexTime: 10,
  selectedLoanId: 'NONE',
};

const AI_FILTER_FIELDS = Object.keys(AI_RESET_DEFAULTS);

const clearAiVoiceFeedback = () => {
  aiVoiceTranscript.value = '';
  aiVoiceMessage.value = '';
  aiVoiceError.value = '';
};

const snapshotAiMainFilters = () => {
  const snapshot = {};
  AI_FILTER_FIELDS.forEach((field) => {
    snapshot[field] = filterState.value[field] ?? null;
  });
  snapshot.destinationId = filterState.value.destinationId ?? null;
  snapshot.destination = filterState.value.destination ?? null;
  snapshot.destinationAddress = filterState.value.destinationAddress ?? null;
  snapshot.destinationLat = filterState.value.destinationLat ?? null;
  snapshot.destinationLng = filterState.value.destinationLng ?? null;
  return JSON.stringify(snapshot);
};

const applyAiResetFields = (resetFields = []) => {
  if (!Array.isArray(resetFields)) return;

  resetFields.forEach((field) => {
    if (Object.prototype.hasOwnProperty.call(AI_RESET_DEFAULTS, field)) {
      filterState.value[field] = AI_RESET_DEFAULTS[field];
    }
  });
};

const applyAiFilterPatch = (patch = {}) => {
  if (!patch || typeof patch !== 'object') return;

  AI_FILTER_FIELDS.forEach((field) => {
    const value = patch[field];
    if (value === null || value === undefined) return;

    if (
      [
        'minDeposit',
        'maxDeposit',
        'minRent',
        'maxRent',
        'minSafetyScore',
        'minTravelTime',
        'travelTime',
        'flexTime',
      ].includes(field)
    ) {
      const numericValue = Number(value);
      if (Number.isFinite(numericValue)) {
        filterState.value[field] = numericValue;
      }
      return;
    }

    filterState.value[field] = value;
  });
};

const normalizeAiDestinationText = (value) =>
  String(value || '')
    .toLowerCase()
    .replace(/[^0-9a-z가-힣]/g, '');

const scoreAiDestinationCandidate = (candidate, destinationQuery) => {
  const query = normalizeAiDestinationText(destinationQuery);
  const name = normalizeAiDestinationText(candidate?.destName);
  const address = normalizeAiDestinationText(candidate?.destAddress);

  if (!query || !name) return -1;

  let score = 0;

  // 사용자가 말한 목적지 문장 안에 검색 결과의 장소명이 정확히 들어 있으면
  // 첫 번째 검색 결과보다 우선합니다.
  // 예: "세종대학교 서울특별시 광진구 능동로 209" -> "세종대학교"
  if (query === name) score += 1000;
  else if (query.includes(name)) score += 700 + Math.min(name.length, 100);
  else if (name.includes(query)) score += 500;

  // 주소까지 일치하는 검색 결과를 우선합니다.
  if (address) {
    if (query.includes(address)) {
      score += 500;
    } else {
      const addressTokens = String(candidate?.destAddress || '')
        .split(/\s+/)
        .map(normalizeAiDestinationText)
        .filter((token) => token.length >= 2);

      const matchedAddressTokens = addressTokens.filter((token) =>
        query.includes(token),
      ).length;
      score += matchedAddressTokens * 25;
    }
  }

  // 장소명 일부가 공통인 경우를 위한 보조 점수입니다.
  // 완전한 장소명 포함/주소 일치보다 훨씬 낮게 반영합니다.
  const nameTokens = String(candidate?.destName || '')
    .split(/\s+/)
    .map(normalizeAiDestinationText)
    .filter((token) => token.length >= 2);
  score +=
    nameTokens.filter((token) => query.includes(token)).length * 10;

  return score;
};

const selectBestAiDestination = (results, destinationQuery) => {
  if (!Array.isArray(results) || results.length === 0) return null;

  return results.reduce((best, candidate, index) => {
    const score = scoreAiDestinationCandidate(candidate, destinationQuery);
    if (!best || score > best.score) {
      return { candidate, score, index };
    }
    return best;
  }, null)?.candidate;
};

const resolveAiDestination = async (destinationQuery) => {
  const keyword = String(destinationQuery || '').trim();
  if (!keyword) return null;

  const results = await onboardingApi.searchDestinations(keyword);
  if (!Array.isArray(results) || results.length === 0) {
    throw new Error(`“${keyword}” 목적지를 찾지 못했습니다.`);
  }

  const selected = selectBestAiDestination(results, keyword) || results[0];

  console.info('AI DESTINATION RESOLVED:', {
    requested: keyword,
    selectedName: selected?.destName,
    selectedAddress: selected?.destAddress,
  });

  try {
    return await onboardingApi.saveDestination(selected);
  } catch (error) {
    // 저장 실패 시에도 검증된 검색 결과 좌표는 이번 검색에 사용할 수 있습니다.
    console.warn('AI DESTINATION SAVE WARNING:', error);
    return selected;
  }
};

const buildAppliedAiVoiceMessage = (result, resolvedDestination) => {
  const fallbackMessage = result?.message || 'AI가 검색조건을 적용했습니다.';
  if (!resolvedDestination || !result?.destinationQuery) {
    return fallbackMessage;
  }

  const actualDestination = [
    resolvedDestination.destName,
    resolvedDestination.destAddress,
  ]
    .filter(Boolean)
    .join(' ')
    .trim();

  if (!actualDestination) return fallbackMessage;

  const requestedDestination = String(result.destinationQuery).trim();
  if (
    requestedDestination &&
    fallbackMessage.includes(requestedDestination)
  ) {
    return fallbackMessage.replace(requestedDestination, actualDestination);
  }

  return `실제 목적지는 ${actualDestination}(으)로 적용했습니다. ${fallbackMessage}`;
};

const applyResolvedAiDestination = (destination) => {
  if (!destination) return;

  const latitude = Number(destination.destLatitude);
  const longitude = Number(destination.destLongitude);
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
    throw new Error('AI가 찾은 목적지의 좌표가 올바르지 않습니다.');
  }

  filterState.value.destinationId = destination.destinationId ?? null;
  filterState.value.destination = destination.destName || '선택한 위치';
  filterState.value.destinationAddress = destination.destAddress || '';
  filterState.value.destinationLat = latitude;
  filterState.value.destinationLng = longitude;

  const userId = authStore.user?.userId || authStore.user?.id;
  saveRecentDestinationGlobal(
    {
      destinationId: destination.destinationId ?? null,
      destName: destination.destName || '선택한 위치',
      destAddress: destination.destAddress || '',
      destLatitude: latitude,
      destLongitude: longitude,
    },
    userId,
  );
};

const applyAiAmenities = (result) => {
  const mode = result?.amenityMode || 'UNCHANGED';
  const incoming = Array.isArray(result?.amenities)
    ? result.amenities
        .map((item) => ({
          amenityType: Number(item.amenityType),
          walkTimeMinutes: Number(item.walkTimeMinutes),
        }))
        .filter(
          (item) =>
            Number.isInteger(item.amenityType) &&
            item.amenityType >= 1 &&
            item.amenityType <= 7 &&
            Number.isFinite(item.walkTimeMinutes),
        )
    : [];

  if (mode === 'UNCHANGED') return false;

  let nextAmenities = [];
  if (mode === 'CLEAR') {
    nextAmenities = [];
  } else if (mode === 'MERGE') {
    const merged = new Map(
      activeAmenityFilters.value.map((item) => [
        Number(item.amenityType),
        { ...item },
      ]),
    );
    incoming.forEach((item) => merged.set(item.amenityType, item));
    nextAmenities = [...merged.values()];
  } else {
    nextAmenities = incoming;
  }

  // 최신 MapView의 기존 편의시설 적용 함수를 그대로 사용합니다.
  handleApplyAmenities(nextAmenities);
  return true;
};

const handleAiVoiceSearch = async (recognizedText) => {
  if (isAiVoiceSearchLoading.value) return;
  clearAiVoiceFeedback();

  if (!authStore.token) {
    aiVoiceError.value = 'AI 음성검색은 로그인 후 사용할 수 있습니다.';
    return;
  }

  const transcript = String(recognizedText || '').trim();
  if (!transcript) {
    aiVoiceError.value = '음성을 인식하지 못했습니다. 다시 말씀해 주세요.';
    return;
  }

  aiVoiceTranscript.value = transcript;
  isAiVoiceSearchLoading.value = true;
  try {
    const result = await aiVoiceSearchService.searchWithTranscript(
      transcript,
      filterState.value,
      activeAmenityFilters.value,
    );

    if (!result?.success) {
      throw new Error(
        result?.message || 'AI가 검색조건을 분석하지 못했습니다.',
      );
    }

    aiVoiceTranscript.value = result.transcript || transcript;

    if (result.action === 'NO_CHANGE') {
      aiVoiceMessage.value =
        result.message || '변경할 검색조건을 찾지 못했습니다.';
      return;
    }

    if (result.action === 'RESET_ALL') {
      amenityFilterRef.value?.resetFilters?.();
      amenityDetailFilters.value = [];
      handleApplyAmenities([]);
      await handleResetFilters();
      aiVoiceMessage.value = result.message || '검색조건을 초기화했습니다.';
      return;
    }

    // 목적지는 GPT가 좌표를 만들지 않고 기존 목적지 검색 API로 실제 값을 찾습니다.
    const resolvedDestination = result.destinationQuery
      ? await resolveAiDestination(result.destinationQuery)
      : null;

    const beforeSnapshot = snapshotAiMainFilters();
    applyAiResetFields(result.resetFields);
    applyAiFilterPatch(result.filters);
    applyResolvedAiDestination(resolvedDestination);
    const afterSnapshot = snapshotAiMainFilters();
    const mainFiltersChanged = beforeSnapshot !== afterSnapshot;

    showFilterAnalysisLoading();

    if (mainFiltersChanged) {
      await handleApplyFilters(true);
    }

    const amenitiesChanged = applyAiAmenities(result);
    if (!mainFiltersChanged && !amenitiesChanged) {
      aiVoiceMessage.value = result.message || '현재 검색조건과 동일합니다.';
      return;
    }

    aiVoiceMessage.value = buildAppliedAiVoiceMessage(
      result,
      resolvedDestination,
    );
  } catch (error) {
    console.error('AI VOICE SEARCH ERROR:', error);
    aiVoiceError.value =
      error?.response?.data?.message ||
      error?.response?.data?.error ||
      error?.message ||
      'AI 음성검색에 실패했습니다. 잠시 후 다시 시도해 주세요.';
  } finally {
    isAiVoiceSearchLoading.value = false;
  }
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

  handleApplyFilters(false, { preserveAmenities: true });
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
  ([newDest, newAddr]) => {
    if (!newDest && !newAddr) return;

    const currentLatitude = Number(filterState.value.destinationLat);
    const currentLongitude = Number(filterState.value.destinationLng);
    if (Number.isFinite(currentLatitude) && Number.isFinite(currentLongitude))
      return;

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
);

// 동적 목적지 명칭 및 실시간 실제 좌표 (lat, lng) 매핑
const destinationConfig = computed(() => {
  const name = filterState.value.destination || '목적지 미선택';
  const lat =
    filterState.value.destinationLat != null
      ? Number(filterState.value.destinationLat)
      : null;
  const lng =
    filterState.value.destinationLng != null
      ? Number(filterState.value.destinationLng)
      : null;

  return {
    id: filterState.value.destinationId || null,
    name,
    address: filterState.value.destinationAddress || '',
    lat,
    lng,
  };
});

watch(
  () => [
    destinationConfig.value,
    appliedFilterState.value?.destinationAddress,
    appliedFilterState.value?.destination,
  ],
  ([dest, destAddr, destName]) => {
    const fullText = `${destAddr || ''} ${destName || ''} ${dest?.name || ''} ${dest?.address || ''}`;
    if (fullText.trim() && !isSupportedSafetyDistrict(fullText)) {
      showToast();
    }
  },
  { immediate: true, deep: true },
);

// 퀵버튼 필터 + 도보/대중교통 도달 범위(Reach) + 5종 정렬 연동 로직 (appliedFilterState 기준 연산)
const baseFilteredProperties = computed(() => {
  const currentFilters = appliedFilterState.value;
  // 적용된 목적지 위경도 좌표
  const destLat = Number(
    currentFilters.destinationLat ?? destinationConfig.value.lat,
  );
  const destLng = Number(
    currentFilters.destinationLng ?? destinationConfig.value.lng,
  );

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

    // 4. 안전 점수 필터 (null인 경우 미수집 매물이므로 필터 통과 처리)
    if (p.safetyScore != null && p.safetyScore < currentFilters.minSafetyScore)
      return false;

    // 5. 도보 / 대중교통 도달 범위 (Reach Distance) 도넛 링 필터 (최소 ~ 최대 시간)
    if (!isWithinReachDistance(p, currentFilters, destLat, destLng))
      return false;

    return true;
  });

  // 정렬 적용
  if (currentSort.value === 'DEPOSIT_ASC') {
    return list.sort((a, b) => (a.deposit || 0) - (b.deposit || 0));
  }
  if (currentSort.value === 'DEPOSIT_DESC') {
    return list.sort((a, b) => (b.deposit || 0) - (a.deposit || 0));
  }
  if (currentSort.value === 'RENT_ASC') {
    return list.sort((a, b) => (a.monthlyRent || 0) - (b.monthlyRent || 0));
  }
  if (currentSort.value === 'RENT_DESC') {
    return list.sort((a, b) => (b.monthlyRent || 0) - (a.monthlyRent || 0));
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

// test: 화면에 출력되는 최종 매물 목록 기준 ID만 콘솔 출력
watch(visibleProperties, (list) => {
  if (isPropertyLoading.value || amenityFilterLoading.value || !list.length)
    return;

  console.table(
    list.map((property) => ({
      propertyId: property.propertyId,
      address: property.address || property.title,
      deposit: property.deposit,
      monthlyRent: property.monthlyRent,
      safetyScore: property.safetyScore,
    })),
  );
});
// 사이드바 목록 10개씩 무한 동적 스크롤 로딩
const displayLimit = ref(10);

watch(
  () => visibleProperties.value.length,
  () => {
    displayLimit.value = 10;
  },
);

const displayedProperties = computed(() =>
  visibleProperties.value.slice(0, displayLimit.value),
);

const handleListScroll = (e) => {
  const el = e.target;
  if (!el) return;
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 60) {
    if (displayLimit.value < visibleProperties.value.length) {
      displayLimit.value += 10;
    }
  }
};

// 수집된 마지막 매물의 생성 일자 동적 계산 (더보기 날짜 동적 표시용)
const lastLoadedDateString = computed(() => {
  const list = amenityFilteredProperties.value;
  if (!list.length) return '';
  const lastProp = list[list.length - 1];
  const dateStr = lastProp?.createdAt || lastProp?.createdDate;
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return '';
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}.${month}.${day}`;
});

const mobileSidebarTab = ref('list'); // 'list' | 'detail'

const clearSelectedProperty = () => {
  // 진행 중인 이전 매물 경로 요청의 응답이 늦게 도착해도 화면에 반영되지 않게 무효화합니다.
  safetyRouteRequestSequence += 1;
  selectedProperty.value = null;
  selectedPropertyDetailAmenities.value = [];
  selectedSafetyRoute.value = null;
  selectedSafetyRouteMeta.value = null;
  safetyRouteError.value = '';
  isSafetyRouteLoading.value = false;
  isPanelOpen.value = false;
  mobileSidebarTab.value = 'list';
  externallyOpenedPropertyId.value = null;
};

watch(
  [visibleProperties, selectedProperty, isMapAnalysisLoading],
  ([nextProperties, currentProperty, isLoading]) => {
    if (!currentProperty || isLoading) return;

    if (
      externallyOpenedPropertyId.value != null &&
      Number(currentProperty.propertyId) ===
        Number(externallyOpenedPropertyId.value)
    ) {
      return;
    }

    const isStillVisible = nextProperties.some(
      (property) =>
        Number(property.propertyId) === Number(currentProperty.propertyId),
    );

    if (!isStillVisible) clearSelectedProperty();
  },
);

// 매물 선택 시 URL 주소창 실시간 동기화 (URL 공유 지원)
watch(selectedProperty, (prop) => {
  const query = { ...route.query };
  if (prop && prop.propertyId) {
    query.propertyId = String(prop.propertyId);
  } else {
    delete query.propertyId;
  }
  router.replace({ query }).catch(() => {});
});

// 공유 링크로 접속 시 (?propertyId=123) 해당 매물 자동 선택 및 슬라이딩 패널 팝업
watch(
  [properties, () => route.query.propertyId],
  ([list, targetId]) => {
    if (!list || !list.length || !targetId) return;
    if (
      !selectedProperty.value ||
      Number(selectedProperty.value.propertyId) !== Number(targetId)
    ) {
      const targetProp = list.find(
        (p) => Number(p.propertyId) === Number(targetId),
      );
      if (targetProp) {
        handleSelectProperty(targetProp);
      }
    }
  },
  { immediate: true },
);

const shouldHideAmenityPins = computed(
  () => activeAmenityFilters.value.length > 0 && amenityFilterLoading.value,
);

const syncSafetySummaryToProperty = (propertyId, response) => {
  const safetySummary = {
    safetyScore: response?.safetyScore ?? null,
    safetyGrade: response?.safetyGrade ?? null,
    cctvCount: response?.cctvCount ?? 0,
    streetLampCount: response?.streetLampCount ?? 0,
    streetlightCount: response?.streetLampCount ?? 0,
    hasPoliceStation: response?.hasPoliceStation ?? false,
    safetyStatus: response?.cacheHit ? 'CACHED' : 'CALCULATED',
    safetyMessage: response?.message ?? '',
  };

  properties.value = properties.value.map((item) =>
    Number(item.propertyId) === Number(propertyId)
      ? { ...item, ...safetySummary }
      : item,
  );

  if (Number(selectedProperty.value?.propertyId) === Number(propertyId)) {
    selectedProperty.value = {
      ...selectedProperty.value,
      ...safetySummary,
    };
  }
};

/**
 * 매물 클릭 -> DB 캐시 조회 -> 캐시 미스일 때만 TMAP 계산 -> 경로 반환.
 * 백엔드가 cacheHit 여부를 결정하므로 프론트는 항상 같은 API만 호출하면 됩니다.
 */
const loadSafetyRouteForProperty = async (property) => {
  const requestId = ++safetyRouteRequestSequence;
  selectedSafetyRoute.value = null;
  selectedSafetyRouteMeta.value = null;
  safetyRouteError.value = '';
  isSafetyRouteLoading.value = true;

  const destination = destinationConfig.value;

  try {
    const response = await safetyService.getSafetyRoute({
      propertyId: Number(property.propertyId),
      propertyName: property.address || property.propertyName || '선택 매물',
      destinationId:
        Number(appliedFilterState.value.destinationId || destination.id) ||
        null,
      destinationName: destination.name,
      destinationAddress: destination.address,
      destinationLatitude: Number(destination.lat),
      destinationLongitude: Number(destination.lng),
    });

    if (
      requestId !== safetyRouteRequestSequence ||
      Number(selectedProperty.value?.propertyId) !== Number(property.propertyId)
    ) {
      return;
    }

    if (response && response.isSupportedDistrict === false) {
      showToast(
        response.message ||
          '선택하신 자치구는 보안등 공공데이터가 구축되지 않아 안전 점수가 제공되지 않습니다.',
      );
      syncSafetySummaryToProperty(property.propertyId, response);
      return;
    }

    const route = response?.selectedRoute;
    if (!Array.isArray(route?.routePoints) || route.routePoints.length < 2) {
      throw new Error('안전 경로 좌표가 반환되지 않았습니다.');
    }

    selectedSafetyRoute.value = route;
    selectedSafetyRouteMeta.value = response;
    syncSafetySummaryToProperty(property.propertyId, response);
  } catch (error) {
    if (requestId !== safetyRouteRequestSequence) return;

    selectedSafetyRoute.value = null;
    selectedSafetyRouteMeta.value = null;
    safetyRouteError.value =
      error?.response?.data?.message ||
      error?.message ||
      '안전 경로를 불러오지 못했습니다.';
    console.error('SELECTED PROPERTY SAFETY ROUTE LOAD ERROR:', error);
  } finally {
    if (requestId === safetyRouteRequestSequence) {
      isSafetyRouteLoading.value = false;
    }
  }
};

// 매물 선택 처리 (사이드바 카드 또는 지도 핀 클릭 시)
const handleSelectProperty = async (
  property,
  { skipVisibilityCheck = false } = {},
) => {
  selectedProperty.value = property;
  externallyOpenedPropertyId.value = skipVisibilityCheck
    ? property.propertyId
    : null;
  isPanelOpen.value = true;
  mobileSidebarTab.value = 'detail';
  if (mobilePanelHeight.value === 'COLLAPSED') {
    mobilePanelHeight.value = 'HALF';
  }

  // 경로 조회와 상세 편의시설 조회는 서로 독립이므로 동시에 시작합니다.
  void loadSafetyRouteForProperty(property);

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
// 북마크에 저장된 목적지 정보를 현재 필터 상태에 반영 (안전 경로 계산이 이 목적지 기준으로 이뤄지게 함)
const applyBookmarkDestinationContext = (bookmarked) => {
  if (!bookmarked || bookmarked.destinationId == null) return;

  const patch = {
    destinationId: Number(bookmarked.destinationId),
    destination: bookmarked.destinationName || filterState.value.destination,
    destinationAddress:
      bookmarked.destinationAddress || filterState.value.destinationAddress,
    destinationLat:
      bookmarked.destinationLat != null
        ? Number(bookmarked.destinationLat)
        : filterState.value.destinationLat,
    destinationLng:
      bookmarked.destinationLng != null
        ? Number(bookmarked.destinationLng)
        : filterState.value.destinationLng,
  };

  Object.assign(filterState.value, patch);
  Object.assign(appliedFilterState.value, patch);
};

const openPropertyDetailFromQuery = async (propertyId) => {
  if (!propertyId) return;

  const numericPropertyId = Number(propertyId);
  if (!Number.isFinite(numericPropertyId)) return;

  // 상세 조회 API 응답 사용
  const savedBookmarkProperty = sessionStorage.getItem(
    'selectedBookmarkProperty',
  );
  let bookmarkedProperty = null;
  try {
    bookmarkedProperty = savedBookmarkProperty
      ? JSON.parse(savedBookmarkProperty)
      : null;
  } catch (error) {
    console.warn('BOOKMARK PROPERTY SESSION PARSE ERROR:', error);
  }

  const isBookmarkedTarget =
    Number(bookmarkedProperty?.propertyId) === numericPropertyId;

  // onMounted의 목적지 로딩(캐시/온보딩)이 비동기로 filterState를 되돌려놓을 수 있어서,
  // handleSelectProperty 직전에 매번 다시 적용해 항상 마지막에 반영되게 함
  if (isBookmarkedTarget) applyBookmarkDestinationContext(bookmarkedProperty);

  const listedProperty = properties.value.find(
    (property) => Number(property.propertyId) === numericPropertyId,
  );

  if (listedProperty) {
    if (isBookmarkedTarget) applyBookmarkDestinationContext(bookmarkedProperty);
    handleSelectProperty(listedProperty, { skipVisibilityCheck: true });
    sessionStorage.removeItem('selectedBookmarkProperty');
    return;
  }

  try {
    const { data } = await api.get(`/properties/${numericPropertyId}`, {
      params: {
        destinationId: appliedFilterState.value.destinationId || undefined,
        userId: authStore.user?.userId || authStore.user?.id || undefined,
      },
    });
    if (data) {
      if (isBookmarkedTarget)
        applyBookmarkDestinationContext(bookmarkedProperty);
      handleSelectProperty(
        { ...data, isBookmarked: true },
        { skipVisibilityCheck: true },
      );
      sessionStorage.removeItem('selectedBookmarkProperty');
      return;
    }
  } catch (error) {
    console.error('BOOKMARK PROPERTY DETAIL LOAD ERROR: ', error);
  }

  // 네트워크 오류 시 목록용 요약 데이터를 마지막 대안으로 사용
  if (isBookmarkedTarget) {
    applyBookmarkDestinationContext(bookmarkedProperty);
    handleSelectProperty(
      { ...bookmarkedProperty, isBookmarked: true },
      { skipVisibilityCheck: true },
    );
    sessionStorage.removeItem('selectedBookmarkProperty');
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
const pendingBookmarkIds = ref(new Set());

const handleToggleBookmark = async (id) => {
  const item = properties.value.find((p) => p.propertyId === id);
  if (!item) return;
  if (pendingBookmarkIds.value.has(id)) return;

  pendingBookmarkIds.value.add(id);
  try {
    const response = item.isBookmarked
      ? await api.delete(`/bookmark/${id}`)
      : await api.post('/bookmark', {
          propertyId: id,
          destinationId: appliedFilterState.value.destinationId || undefined,
        });

    if (response.data && response.data.success === false) {
      console.error('BOOKMARK TOGGLE ERROR: ', response.data.message);
      return;
    }

    item.isBookmarked = !item.isBookmarked;

    if (Number(selectedProperty.value?.propertyId) === Number(id)) {
      selectedProperty.value = {
        ...selectedProperty.value,
        isBookmarked: item.isBookmarked,
      };
    }
  } catch (error) {
    console.error('BOOKMARK TOGGLE ERROR: ', error);
  } finally {
    pendingBookmarkIds.value.delete(id);
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
  syncFiltersToUrlQuery(filterState.value);
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
  const selectedFilters =
    amenityFilterRef.value?.getSelectedAmenities?.() ?? [];

  // 초기화 시 편의시설 선택은 유지하고, 시간 제한만 종류별 기본값으로 되돌린다.
  amenityDetailFilters.value = selectedFilters.map((filter) => {
    const defaultWalkTime =
      Number(filter.amenityType) === 1
        ? DEFAULT_CONVENIENCE_STORE_WALK_TIME
        : DEFAULT_AMENITY_WALK_TIME;

    return {
      ...filter,
      timeLimit: defaultWalkTime,
      walkTimeMinutes: defaultWalkTime,
    };
  });
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
        class="w-full pb-4 pt-4 bg-white flex flex-col items-center justify-center cursor-row-resize active:cursor-grabbing xl:hidden select-none touch-none shrink-0"
        @click="toggleMobilePanel"
        @mousedown="startDrag"
        @touchstart.prevent="startDrag"
      >
        <span class="w-24 h-1.5 bg-slate-300 rounded-full"></span>
      </div>

      <!-- 모바일 전용 탭 스위처 ([📋 매물 목록] | [🏠 선택 매물 상세]) -->
      <div
        class="flex xl:hidden items-center px-3 py-1.5 bg-slate-50 gap-2 shrink-0 select-none"
      >
        <button
          type="button"
          class="flex-1 py-2.5 px-3 rounded-xl text-xs font-extrabold transition-all flex items-center justify-center gap-1.5"
          :class="[
            mobileSidebarTab === 'list'
              ? 'bg-white text-blue-600 shadow-sm border border-slate-200'
              : 'text-slate-500 hover:text-slate-800',
          ]"
          @click="mobileSidebarTab = 'list'"
        >
          <span>📋</span>
          <span>매물 목록 ({{ visibleProperties.length }})</span>
        </button>
        <button
          type="button"
          class="flex-1 py-2.5 px-3 rounded-xl text-xs font-extrabold transition-all flex items-center justify-center gap-1.5"
          :class="[
            mobileSidebarTab === 'detail'
              ? 'bg-white text-blue-600 shadow-sm border border-slate-200'
              : selectedProperty
                ? 'text-slate-700 hover:text-slate-900'
                : 'text-slate-300 cursor-not-allowed',
          ]"
          :disabled="!selectedProperty"
          @click="selectedProperty && (mobileSidebarTab = 'detail')"
        >
          <span>🏠</span>
          <span>상세 정보</span>
          <span
            v-if="selectedProperty"
            class="w-2 h-2 rounded-full bg-blue-600 animate-pulse"
          ></span>
        </button>
      </div>

      <!-- 모바일 [상세 정보] 탭 열림 시: Inline SlidingDoorPanel 노출 -->
      <div
        v-if="mobileSidebarTab === 'detail' && selectedProperty"
        class="flex-1 overflow-y-auto xl:hidden"
      >
        <SlidingDoorPanel
          :is-open="true"
          :is-inline="true"
          :property="selectedProperty"
          :amenities="selectedPropertyAmenities"
          :destination="destinationConfig"
          :is-bookmark-pending="
            selectedProperty &&
            pendingBookmarkIds.has(selectedProperty.propertyId)
          "
          @close="clearSelectedProperty"
          @toggle-bookmark="handleToggleBookmark"
        />
      </div>

      <!-- 모바일 [매물 목록] 탭 및 PC 화면일 때: 사이드바 리스트 노출 (PC에서는 상시 flex 노출) -->
      <div
        class="flex-1 min-h-0 flex flex-col overflow-hidden xl:overflow-visible"
        :class="[
          mobileSidebarTab === 'detail' && selectedProperty
            ? 'hidden xl:flex'
            : 'flex',
        ]"
      >
        <!-- 사이드바 상단 헤더 및 5종 정렬 탭 -->
        <div
          class="p-4 pt-1 pb-1 border-b-0 bg-white space-y-3 xl:space-y-0 xl:pt-3 shrink-0"
        >
          <div
            v-if="isMapAnalysisLoading"
            class="flex items-center justify-between xl:hidden"
          >
            <span
              class="inline-flex shrink-0 items-center gap-1.5 text-[13px] font-bold text-[#5267e8]"
            >
              <i
                class="fa-solid fa-spinner animate-spin"
                aria-hidden="true"
              ></i>
              안전 분석 중
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
              <h2 class="text-[16px] font-bold text-[#1e293b]">
                편의시설 필터
              </h2>

              <button
                type="button"
                class="flex h-8 shrink-0 items-center gap-1 rounded-full border border-slate-200 bg-white px-3 text-xs font-bold text-[#3e55df] shadow-sm transition-all hover:border-[#b9c5ff] hover:bg-[#f5f7ff] active:scale-[0.98]"
                @click="openAmenityDetailFilter()"
              >
                <i
                  class="fa-solid fa-sliders text-[10px]"
                  aria-hidden="true"
                ></i>
                <span>이동 시간</span>
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

          <!-- 5종 정렬 선택 탭 (컴포넌트 분리 완료) -->
          <PropertySortBar
            v-model="currentSort"
            :total-count="visibleProperties.length"
            :is-loading="isMapAnalysisLoading"
          />
        </div>

        <!-- 사이드바 매물 카드리스트 (10개씩 동적 스크롤) -->
        <div
          class="property-list-scroll flex-1 overflow-y-auto p-3 space-y-2.5"
          @scroll="handleListScroll"
        >
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
          <template v-else-if="displayedProperties.length > 0">
            <PropertyCard
              v-for="prop in displayedProperties"
              :key="prop.propertyId"
              :property="prop"
              :is-selected="
                selectedProperty &&
                selectedProperty.propertyId === prop.propertyId
              "
              :is-bookmark-pending="pendingBookmarkIds.has(prop.propertyId)"
              @select="handleSelectProperty"
              @toggle-bookmark="handleToggleBookmark"
            />
            <!-- 10개씩 동적 스크롤 하단 로딩 바 -->
            <div
              v-if="displayLimit < visibleProperties.length"
              class="py-2.5 text-center text-xs font-bold text-slate-400 bg-slate-50/60 rounded-xl border border-slate-100/80 flex items-center justify-center gap-2 select-none"
            >
              <i
                class="fa-solid fa-spinner animate-spin text-blue-500 text-[11px]"
                aria-hidden="true"
              ></i>
              <span
                >스크롤하여 10개 더 보는 중 ({{ displayedProperties.length }}/{{
                  visibleProperties.length
                }}개)</span
              >
            </div>
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
            class="h-full flex flex-col items-center justify-center p-6 text-center"
          >
            <p class="text-sm font-extrabold text-slate-700">
              편의시설 조건을 적용하고 있어요
            </p>
            <p class="text-xs text-slate-400 mt-1">
              주변 매물을 다시 확인하는 중입니다
            </p>
          </div>
        </div>
      </div>
    </aside>

    <!-- 2. 중앙 메인 지도 캔버스 (Full-bleed) -->
    <main
      class="absolute inset-0 z-10 xl:relative xl:inset-auto xl:h-full xl:flex-1"
    >
      <!-- 🗺️ 지도 상단 부유형(Floating) 퀵버튼 바 (요소 크기 맞춤 w-fit) -->
      <div
        class="absolute top-4 left-4 z-30 flex flex-col items-start gap-3 pointer-events-none"
      >
        <AiVoiceSearchBar
          :loading="isAiVoiceSearchLoading"
          :disabled="!authStore.token"
          :transcript="aiVoiceTranscript"
          :message="aiVoiceMessage"
          :error="aiVoiceError"
          @voice-search="handleAiVoiceSearch"
          @clear-feedback="clearAiVoiceFeedback"
        />
        <MapQuickFilterBar
          v-model="filterState"
          :total-count="visibleProperties.length"
          :base-count="
            serverTotalCount > 200 &&
            serverTotalCount > baseFilteredProperties.length
              ? serverTotalCount
              : baseFilteredProperties.length
          "
          :active-amenity-filters="activeAmenityFilters"
          :is-loading="isMapAnalysisLoading"
          class="pointer-events-auto"
          @open-filter="emit('open-filter')"
          @popover-change="handlePopoverChange"
          @apply="handleApplyFilters"
          @reset="handleResetFilters"
        />
        <!-- 🚗 선택한 매물의 이동 경로 피드백 카드 (상세 패널 열림 및 매물 선택 시 노출) -->
        <RouteFeedbackCard
          v-if="isPanelOpen && selectedProperty && destinationConfig.id"
          :key="selectedProperty.propertyId"
          class="pointer-events-auto"
          :property-id="selectedProperty.propertyId"
          :destination-id="destinationConfig.id"
        />
      </div>

      <NaverMap
        :properties="amenityFilteredProperties"
        :selected-property="selectedProperty"
        :amenities="selectedPropertyAmenities"
        :destination="destinationConfig"
        :applied-filter="appliedFilterState"
        :live-filter="filterState"
        :is-preview-mode="isPreviewingIsochrone"
        :safety-route="selectedSafetyRoute"
        @select-property="handleSelectProperty"
        @change-destination="handleChangeDestination"
        @bounds-change="handleBoundsChange"
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

      <!-- 🗺️ 지도 중앙 하단 부유형 스마트 '매물 더보기 / 이 위치에서 재검색' 캡슐 버튼 (토스트 일체형 픽셀 센터 정렬) -->
      <LoadMoreButton
        v-if="
          !isMapAnalysisLoading && (isMapMoved || visibleProperties.length > 0)
        "
        :is-loading="isMoreLoading"
        :is-map-moved="isMapMoved"
        :visible-count="visibleProperties.length"
        :base-count="baseFilteredProperties.length"
        :total-count="serverTotalCount"
        :last-loaded-date="lastLoadedDateString"
        :show-all-loaded-toast="showAllLoadedToast"
        @click="handleLoadMoreClick"
      />

      <!-- 매물 데이터 수신 실패 안내 배너 -->
      <div
        v-if="isPropertyApiError"
        class="absolute bottom-6 left-1/2 transform -translate-x-1/2 z-40 bg-slate-900/90 text-white px-4 py-2.5 rounded-2xl text-xs font-bold shadow-2xl backdrop-blur-md flex items-center gap-2 border border-slate-700 pointer-events-auto"
      >
        <span>⚠️</span>
        <span>매물 정보를 불러오지 못했어요.</span>
        <button
          type="button"
          class="underline text-blue-400 hover:text-blue-300 ml-1 font-extrabold"
          @click="fetchPropertiesFromBackend"
        >
          다시 시도
        </button>
      </div>

      <!-- ⚠️ 보안등 미구축 자치구 안내 토스트 팝업 -->
      <DistrictToast />
    </main>

    <!-- 3. 우측 560px Slide-Over 매물 상세 패널 -->
    <SlidingDoorPanel
      :is-open="isPanelOpen"
      :property="selectedProperty"
      :amenities="selectedPropertyAmenities"
      :destination="destinationConfig"
      :is-bookmark-pending="
        selectedProperty && pendingBookmarkIds.has(selectedProperty.propertyId)
      "
      @close="clearSelectedProperty"
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

@keyframes loading-bar {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(200%);
  }
}

.animate-loading-bar {
  animation: loading-bar 1.2s ease-in-out infinite;
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
