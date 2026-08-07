<script setup>
import {
  ref,
  computed,
  nextTick,
  watch,
  onMounted,
  onBeforeUnmount,
} from 'vue';
import { useAuthStore } from '@/stores/useAuthStore.js';
import {
  getRecentDestinations,
  saveRecentDestinationGlobal,
  removeRecentDestinationGlobal,
} from '@/utils/recentDestinations.js';
import onboardingApi from '@/api/onboardingApi';
import api from '@/api/api.js';
import {
  DEPOSIT_MAX,
  DEPOSIT_MAX_LABEL,
  DEPOSIT_MIN_LABEL,
  DEPOSIT_OPTIONS,
  RENT_MAX,
  RENT_MAX_LABEL,
  RENT_MIN,
  RENT_MIN_LABEL,
  RENT_STEP,
  formatDepositAmount,
  formatDepositShort,
  LOAN_PRODUCTS,
} from '@/utils/budget';

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
    default: () => ({
      destination: '세종대학교',
      tradeType: 'MONTHLY', // 'MONTHLY' | 'JEONSE'
      minDeposit: 0, // 만원
      maxDeposit: 5000, // 만원
      minRent: 0, // 만원
      maxRent: 100, // 만원
      minSafetyScore: 0, // 0 ~ 100
      transportMode: 'WALK', // 'WALK' | 'TRANSIT'
      minTravelTime: 5, // 분
      travelTime: 15, // 분 (5분 ~ 60분)
      walkPace: 'NORMAL', // 'SLOW' | 'NORMAL' | 'FAST'
      flexTime: 10, // ±5분 ~ ±20분
      showIsochrone: true,
      selectedAmenities: [],
      selectedLoanId: 'NONE',
    }),
  },
  totalCount: {
    type: Number,
    default: 0,
  },
});

const emit = defineEmits([
  'update:modelValue',
  'reset',
  'apply',
  'open-filter',
  'popover-change',
  'update-filters',
]);

// 로컬 반응형 상태
const filters = ref({
  tradeType: 'MONTHLY',
  travelTime: 15,
  walkPace: 'NORMAL',
  flexTime: 10,
  selectedLoanId: 'NONE',
  ...props.modelValue,
});

// 팝오버 안에서 조절 중인 filters와, 실제 적용된 modelValue를 분리한다.
// 퀵버튼 라벨은 적용된 값만 보여 주고, 팝오버 안에서는 임시값을 자유롭게 조절한다.
const appliedQuickFilters = computed(() => props.modelValue);

// 모바일 바텀시트 모달 상태
// PC 드롭다운 열림 상태 (activePopover: null | 'destination' | 'price' | 'safety' | 'travel')
const activePopover = ref(null);
const activeSliderThumb = ref(null);

watch(activePopover, (newVal) => {
  emit('popover-change', newVal);
});
const destinationSearchKeyword = ref('');
const destinationSearchResults = ref([]);
const selectedDestination = ref(null);
const isDestinationSearching = ref(false);
const isDestinationSaving = ref(false);
const destinationSearchError = ref('');
const isDestinationComposing = ref(false);
let destinationSearchTimer;
let destinationSearchRequestId = 0;

const handleDestinationCompositionStart = () => {
  isDestinationComposing.value = true;
};

const handleDestinationCompositionEnd = (event) => {
  isDestinationComposing.value = false;
  destinationSearchKeyword.value = event.target.value;
  scheduleDestinationSearch(destinationSearchKeyword.value);
};

const handleDestinationSearchInput = (event) => {
  destinationSearchKeyword.value = event.target.value;
  scheduleDestinationSearch(event.target.value);
};
const depositOptions = DEPOSIT_OPTIONS;

const PRESET_COORDS = {
  세종대학교: {
    address: '서울특별시 광진구 능동로 209',
    lat: 37.5502,
    lng: 127.0731,
  },
  건국대학교: {
    address: '서울특별시 광진구 능동로 120',
    lat: 37.5408,
    lng: 127.0793,
  },
  강남역: {
    address: '서울특별시 강남구 강남대로 지하396',
    lat: 37.4979,
    lng: 127.0276,
  },
  역삼역: {
    address: '서울특별시 강남구 테헤란로 지하156',
    lat: 37.5006,
    lng: 127.0365,
  },
  성수역: {
    address: '서울특별시 성동구 아차산로 113',
    lat: 37.5445,
    lng: 127.0557,
  },
};

// 기본 5종 목적지 및 온보딩 지정 목적지 통합 옵션 목록
const destinationList = computed(() => {
  const defaults = ['세종대학교', '건국대학교', '강남역', '역삼역', '성수역'];
  const current = filters.value.destination;
  if (current && !defaults.includes(current)) {
    return [current, ...defaults];
  }
  return defaults;
});

const authStore = useAuthStore();
const currentUserId = computed(
  () => authStore.user?.userId || authStore.user?.id || 'guest',
);

const recentDestinations = ref(getRecentDestinations(currentUserId.value));

watch(currentUserId, (newUserId) => {
  recentDestinations.value = getRecentDestinations(newUserId);
});

const saveRecentDestination = (destObj) => {
  recentDestinations.value = saveRecentDestinationGlobal(
    destObj,
    currentUserId.value,
  );
};

const removeRecentDestination = (destName) => {
  recentDestinations.value = removeRecentDestinationGlobal(
    destName,
    currentUserId.value,
  );
};

const selectRecentDestination = (item) => {
  selectDestination(item);
  applyDestination();
};

const selectDestinationOption = (dest) => {
  filters.value.destinationId = null;
  filters.value.destination = dest;
  if (PRESET_COORDS[dest]) {
    filters.value.destinationAddress = PRESET_COORDS[dest].address;
    filters.value.destinationLat = PRESET_COORDS[dest].lat;
    filters.value.destinationLng = PRESET_COORDS[dest].lng;
    saveRecentDestination({
      destName: dest,
      destAddress: PRESET_COORDS[dest].address,
      destLatitude: PRESET_COORDS[dest].lat,
      destLongitude: PRESET_COORDS[dest].lng,
    });
  } else {
    // 유저 지정 새 장소 시 기존 좌표 초기화 (지오코더가 주소 기반 자동 재생성)
    delete filters.value.destinationLat;
    delete filters.value.destinationLng;
  }
  updateFilters();
  activePopover.value = null;
};

const selectDestination = (destination) => {
  selectedDestination.value = destination;
  destinationSearchKeyword.value = destination.destName;
  destinationSearchResults.value = [];
  destinationSearchError.value = '';

  // 적용 전까지는 로컬 임시 필터에만 저장한다.
  filters.value.destinationId =
    destination.destinationId || destination.destId || null;
  filters.value.destination = destination.destName;
  filters.value.destinationAddress = destination.destAddress;
  filters.value.destinationLat = Number(destination.destLatitude);
  filters.value.destinationLng = Number(destination.destLongitude);
};

const clearDestinationSearch = () => {
  destinationSearchKeyword.value = '';
  destinationSearchResults.value = [];
  selectedDestination.value = null;
  destinationSearchError.value = '';
};

const applyDestination = async () => {
  destinationSearchError.value = '';

  if (selectedDestination.value) {
    isDestinationSaving.value = true;
    try {
      const savedDestination = await onboardingApi.saveDestination(
        selectedDestination.value,
      );
      filters.value.destinationId = savedDestination.destinationId;
      filters.value.destination = savedDestination.destName;
      filters.value.destinationAddress = savedDestination.destAddress;
      filters.value.destinationLat = Number(savedDestination.destLatitude);
      filters.value.destinationLng = Number(savedDestination.destLongitude);
      saveRecentDestination(savedDestination);
    } catch (error) {
      destinationSearchError.value =
        '목적지 저장에 실패했어요. 다시 시도해 주세요.';
      console.error('QUICK FILTER DESTINATION SAVE ERROR:', error);
      return;
    } finally {
      isDestinationSaving.value = false;
    }
  }

  updateFilters();
  emit('apply');
  activePopover.value = null;
};

const scheduleDestinationSearch = (value) => {
  clearTimeout(destinationSearchTimer);
  destinationSearchError.value = '';
  const requestId = ++destinationSearchRequestId;

  const keyword = value.trim();
  if (selectedDestination.value?.destName !== value)
    selectedDestination.value = null;

  if (keyword.length < 2 || selectedDestination.value?.destName === value) {
    destinationSearchResults.value = [];
    isDestinationSearching.value = false;
    return;
  }

  destinationSearchResults.value = [];
  isDestinationSearching.value = true;
  destinationSearchTimer = setTimeout(async () => {
    try {
      const results = await onboardingApi.searchDestinations(keyword);
      if (requestId !== destinationSearchRequestId) return;
      destinationSearchResults.value = results;
    } catch (error) {
      if (requestId !== destinationSearchRequestId) return;
      destinationSearchResults.value = [];
      destinationSearchError.value =
        '목적지 검색에 실패했어요. 다시 시도해 주세요.';
      console.error('QUICK FILTER DESTINATION SEARCH ERROR:', error);
    } finally {
      if (requestId === destinationSearchRequestId)
        isDestinationSearching.value = false;
    }
  }, 300);
};

watch(destinationSearchKeyword, scheduleDestinationSearch);

// 백엔드 금융감독원/KB대출 추천 API 연동
const recommendedLoanFromApi = ref(null);
const isLoanLoading = ref(false);

const fetchRecommendedLoan = async () => {
  isLoanLoading.value = true;
  try {
    const res = await api.get('/loan/onboarding-recommend', {
      params: {
        deposit: filters.value.maxDeposit,
        monthlyRent: filters.value.maxRent,
        age: 26,
      },
    });
    if (res.data) {
      recommendedLoanFromApi.value = res.data;
    }
  } catch (err) {
    console.log('백엔드 대출 API 추천 연동:', err);
  } finally {
    isLoanLoading.value = false;
  }
};

watch(
  () => activePopover.value,
  (val) => {
    if (val === 'loan') {
      fetchRecommendedLoan();
    }
  },
);

// props 변경 감지
watch(
  () => props.modelValue,
  (newVal) => {
    filters.value = {
      tradeType: 'MONTHLY',
      travelTime: 15,
      walkPace: 'NORMAL',
      flexTime: 10,
      ...newVal,
    };
    selectedDestination.value = null;
    destinationSearchKeyword.value = '';
    destinationSearchResults.value = [];
  },
  { deep: true },
);

const isSliderDragging = ref(false);
let sliderDragTimer = null;

const handleSliderStart = (thumbName) => {
  if (thumbName) activeSliderThumb.value = thumbName;
  clearTimeout(sliderDragTimer);
  // 300ms 이상 손으로 잡고 꾸욱 누르거나 쥐고 있을 때만 반투명 전환 (단순 클릭 시 0% 깜빡임 완전 방지)
  sliderDragTimer = setTimeout(() => {
    isSliderDragging.value = true;
  }, 300);
};

const handleSliderEnd = () => {
  clearTimeout(sliderDragTimer);
  isSliderDragging.value = false;
};

onMounted(() => {
  window.addEventListener('pointerup', handleSliderEnd);
  window.addEventListener('touchend', handleSliderEnd);
});

onBeforeUnmount(() => {
  window.removeEventListener('pointerup', handleSliderEnd);
  window.removeEventListener('touchend', handleSliderEnd);
  clearTimeout(destinationSearchTimer);
  clearTimeout(sliderDragTimer);
  if (updateFiltersDebounceTimer) clearTimeout(updateFiltersDebounceTimer);
});

let updateFiltersDebounceTimer = null;

// 필터 상태 변경 시 부모로 전달 (단순 클릭 시 모달창 깜빡임 방지 및 300ms 디바운스 적용)
const updateFilters = () => {
  emit('update:modelValue', { ...filters.value });
  if (updateFiltersDebounceTimer) clearTimeout(updateFiltersDebounceTimer);
  updateFiltersDebounceTimer = setTimeout(() => {
    emit('update-filters', { ...filters.value });
  }, 300);
};

// 버튼 클릭 시 즉시 수신
const updateFiltersImmediate = () => {
  if (updateFiltersDebounceTimer) clearTimeout(updateFiltersDebounceTimer);
  emit('update:modelValue', { ...filters.value });
  emit('update-filters', { ...filters.value });
};

const activePopoverName = ref(null);
const togglePopover = (name) => {
  activePopover.value = activePopover.value === name ? null : name;
};

const selectTradeType = (tradeType) => {
  filters.value.tradeType = tradeType;

  // 전세 탭 선택 시 월세 값 0으로 초기화
  if (tradeType === 'JEONSE') {
    filters.value.maxRent = 0;
  }
};

// 이소크론 영역 토글 (ON <-> OFF - 순수 지도 오버레이 보이기/가리기)
const toggleIsochrone = () => {
  filters.value.showIsochrone = !filters.value.showIsochrone;
  emit('update:modelValue', { ...filters.value });
};

// 필터 초기화
const handleReset = () => {
  activePopover.value = null;
  emit('reset');
};

// 적용된 필터 개수 계산
const activeFilterCount = computed(() => {
  let count = 0;
  if (filters.value.tradeType !== 'MONTHLY') count++;
  if (filters.value.maxDeposit < 5000) count++;
  if (filters.value.maxRent < 100) count++;
  if (filters.value.minSafetyScore > 0) count++;
  if (
    filters.value.transportMode !== 'WALK' ||
    filters.value.travelTime !== 15 ||
    filters.value.walkPace !== 'NORMAL'
  )
    count++;
  if (filters.value.selectedAmenities?.length > 0) {
    count += filters.value.selectedAmenities.length;
  }
  return count;
});

// 듀얼 슬라이더 활성 범위 트랙 스타일 계산 함수 (valA, valB 순서 상관없이 Min~Max 트랙 표기)
const getDualRangeTrackStyle = (valA, valB, minLimit, maxLimit) => {
  const minVal = Math.min(Number(valA), Number(valB));
  const maxVal = Math.max(Number(valA), Number(valB));
  const minPercent = Math.max(
    0,
    Math.min(100, ((minVal - minLimit) / (maxLimit - minLimit)) * 100),
  );
  const maxPercent = Math.max(
    0,
    Math.min(100, ((maxVal - minLimit) / (maxLimit - minLimit)) * 100),
  );
  return {
    left: `${minPercent}%`,
    width: `${Math.max(0, maxPercent - minPercent)}%`,
  };
};

// 보증금 핸들 A, B (독립 2개 핸들)
const depositValA = ref(0);
const depositValB = ref(9);

watch(
  [depositValA, depositValB],
  ([a, b]) => {
    const minIdx = Math.min(Number(a), Number(b));
    const maxIdx = Math.max(Number(a), Number(b));
    filters.value.minDeposit = DEPOSIT_OPTIONS[minIdx];
    filters.value.maxDeposit = DEPOSIT_OPTIONS[maxIdx];
  },
  { immediate: true },
);

// 월세 핸들 A, B (독립 2개 핸들)
const rentValA = ref(0);
const rentValB = ref(100);

watch(
  [rentValA, rentValB],
  ([a, b]) => {
    filters.value.minRent = Math.min(Number(a), Number(b));
    filters.value.maxRent = Math.max(Number(a), Number(b));
  },
  { immediate: true },
);

// 대중교통 이동시간 핸들 A, B (독립 2개 핸들)
const travelValA = ref(10);
const travelValB = ref(15);

watch(
  [travelValA, travelValB],
  ([a, b]) => {
    filters.value.minTravelTime = Math.min(Number(a), Number(b));
    filters.value.travelTime = Math.max(Number(a), Number(b));
  },
  { immediate: true },
);

const minTravelTimeVal = computed(() =>
  Math.min(travelValA.value, travelValB.value),
);
const maxTravelTimeVal = computed(() =>
  Math.max(travelValA.value, travelValB.value),
);

// external modelValue 또는 reset 시 핸들 위치 맞춤 동기화
watch(
  () => props.modelValue,
  (newVal) => {
    if (!newVal) return;
    const minDep = newVal.minDeposit ?? 0;
    const maxDep = newVal.maxDeposit ?? DEPOSIT_MAX;
    const minDepIdx = Math.max(
      0,
      DEPOSIT_OPTIONS.findIndex((opt) => opt >= minDep),
    );
    const maxDepIdx = Math.max(0, DEPOSIT_OPTIONS.indexOf(Number(maxDep)));
    depositValA.value = minDepIdx >= 0 ? minDepIdx : 0;
    depositValB.value = maxDepIdx >= 0 ? maxDepIdx : DEPOSIT_OPTIONS.length - 1;

    rentValA.value = newVal.minRent ?? 0;
    rentValB.value = newVal.maxRent ?? RENT_MAX;

    travelValA.value = newVal.minTravelTime ?? 10;
    travelValB.value = newVal.travelTime ?? 15;
  },
  { immediate: true, deep: true },
);

// 보증금 팝오버 표시 라벨
const depositAmountLabel = computed(() => {
  const minVal = filters.value.minDeposit ?? 0;
  const maxVal = filters.value.maxDeposit ?? DEPOSIT_MAX;
  if (minVal === 0 && maxVal >= DEPOSIT_MAX) return '전체';
  if (minVal === 0) return `${formatDepositAmount(maxVal)} 이하`;
  if (maxVal >= DEPOSIT_MAX) return `${formatDepositAmount(minVal)} 이상`;
  return `${formatDepositAmount(minVal)} ~ ${formatDepositAmount(maxVal)}`;
});

// 월세 팝오버 표시 라벨
const rentAmountLabel = computed(() => {
  const minVal = filters.value.minRent ?? 0;
  const maxVal = filters.value.maxRent ?? RENT_MAX;
  if (minVal === 0 && maxVal >= RENT_MAX) return '전체';
  if (minVal === 0) return `${maxVal}만원 이하`;
  if (maxVal >= RENT_MAX) return `${minVal}만원 이상`;
  return `${minVal}만 ~ ${maxVal}만원`;
});

// 가격 퀵버튼 요약 텍스트 (전세 vs 월세 구분)
const priceSummaryText = computed(() => {
  const {
    tradeType,
    minDeposit = 0,
    maxDeposit = DEPOSIT_MAX,
    minRent = 0,
    maxRent = RENT_MAX,
  } = appliedQuickFilters.value;

  const minDepShort = formatDepositShort(minDeposit);
  const maxDepShort = formatDepositShort(maxDeposit);
  const depStr =
    minDeposit === 0 && maxDeposit >= DEPOSIT_MAX
      ? '전체'
      : minDeposit === 0
        ? `${maxDepShort} 이하`
        : maxDeposit >= DEPOSIT_MAX
          ? `${minDepShort} 이상`
          : `${minDepShort}~${maxDepShort}`;

  if (tradeType === 'JEONSE') {
    return `전세: ${depStr}`;
  }

  const rentStr =
    minRent === 0 && maxRent >= RENT_MAX
      ? '전체'
      : minRent === 0
        ? `${maxRent}만 이하`
        : maxRent >= RENT_MAX
          ? `${minRent}만 이상`
          : `${minRent}~${maxRent}만`;

  if (depStr === '전체' && rentStr === '전체') {
    return '월세: 전체';
  }
  return `월세: ${depStr} / ${rentStr}`;
});

// 이동시간 퀵버튼 요약 텍스트 (도보/대중교통 단일 시간 이내)
const travelSummaryText = computed(() => {
  const time = appliedQuickFilters.value.travelTime ?? 15;
  if (appliedQuickFilters.value.transportMode === 'WALK') {
    return `🚶 도보: ${time}분 이내`;
  }
  return `🚌 대중교통: ${time}분 이내`;
});

// 대출 상품 요약 텍스트
const loanSummaryText = computed(() => {
  const currentLoanId = appliedQuickFilters.value.selectedLoanId || 'NONE';
  const loan = LOAN_PRODUCTS.find((l) => l.id === currentLoanId);
  return loan ? loan.shortName : '대출 상품';
});

const rangeStyle = (value, min, max, color = '#3b82f6') => {
  const percent = ((value - min) / (max - min)) * 100;
  return {
    '--thumb-color': color,
    background: `linear-gradient(to right, ${color} 0%, ${color} ${percent}%, #e2e8f0 ${percent}%, #e2e8f0 100%)`,
  };
};

// 안전점수 동적 콩나물 핸들(Thumb) 색상 (점수 구간별 변색)
const safetyThumbColor = computed(() => {
  const score = filters.value.minSafetyScore;
  if (score === 0) return '#64748b'; // Slate gray (전체)
  if (score < 80) return '#f59e0b'; // Amber (보통)
  return '#10b981'; // Emerald (안심)
});

const safetyAccentClass = computed(() => {
  if (appliedQuickFilters.value.minSafetyScore >= 80) {
    return 'bg-emerald-50 text-emerald-700 border-emerald-300 font-extrabold';
  }
  if (appliedQuickFilters.value.minSafetyScore > 0) {
    return 'bg-amber-50 text-amber-700 border-amber-300 font-extrabold';
  }
  return 'bg-white hover:bg-slate-50 text-slate-700 border-slate-200';
});
</script>

<template>
  <div class="relative w-fit">
    <!-- ======================================================== -->
    <!-- 1. PC 전용 상단 6종 부유형(Floating) 퀵버튼 바 (md:inline-flex w-fit) -->
    <!-- ======================================================== -->
    <div
      class="hidden xl:inline-flex w-fit items-center gap-2 text-slate-800 z-30"
    >
      <!-- 📍 퀵버튼 1: 목적지 (고정 너비 min-w-[115px]) -->
      <div class="relative order-1">
        <button
          type="button"
          class="flex items-center justify-between gap-1.5 px-3.5 py-2 rounded-full text-xs font-bold transition-all bg-white hover:bg-slate-50 text-slate-900 border border-slate-200 shadow-sm min-w-[140px]"
          @click="togglePopover('destination')"
        >
          <span class="flex items-center gap-1">
            <span class="text-blue-600">📍</span>
            <span class="whitespace-nowrap"
              >목적지: {{ appliedQuickFilters.destination }}</span
            >
          </span>
          <span class="text-[10px] text-slate-400">▼</span>
        </button>

        <!-- 목적지 변경 드롭다운 -->
        <div
          v-if="activePopover === 'destination'"
          class="absolute top-full left-0 mt-2 w-80 rounded-2xl border border-slate-200 bg-white p-4 shadow-xl z-40"
        >
          <div class="flex items-center justify-between mb-3">
            <div class="text-sm font-black text-slate-800">목적지 검색</div>
            <button
              type="button"
              class="flex h-7 w-7 items-center justify-center rounded-full text-sm text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
              aria-label="목적지 검색 닫기"
              @click="activePopover = null"
            >
              ✕
            </button>
          </div>
          <label
            class="flex items-center gap-2 h-11 px-3 rounded-xl border border-slate-200 text-slate-400 focus-within:border-blue-500 focus-within:ring-2 focus-within:ring-blue-100"
          >
            <i class="fa-solid fa-magnifying-glass" aria-hidden="true"></i>
            <input
              v-model="destinationSearchKeyword"
              @input="handleDestinationSearchInput"
              @compositionstart="handleDestinationCompositionStart"
              @compositionend="handleDestinationCompositionEnd"
              type="text"
              autocomplete="off"
              placeholder="장소명 또는 주소를 검색하세요"
              class="flex-1 min-w-0 border-0 outline-none text-xs text-slate-700 placeholder:text-slate-400"
            />
            <button
              v-if="destinationSearchKeyword"
              type="button"
              class="flex h-6 w-6 items-center justify-center rounded-full text-sm text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
              aria-label="검색어 지우기"
              @click="clearDestinationSearch"
            >
              <i class="fa-solid fa-xmark" aria-hidden="true"></i>
            </button>
          </label>

          <!-- 🕒 최근 검색 목적지 기록 (유저 ID 결합, 세로 미니멀 리스트, 최대 10개) -->
          <div v-if="recentDestinations.length > 0" class="mt-3">
            <div
              class="flex items-center justify-between text-[11px] font-bold text-slate-400 mb-1.5 px-0.5"
            >
              <span>🕒 최근 검색 목적지 (최대 10개)</span>
            </div>
            <ul
              class="max-h-44 overflow-y-auto space-y-1 rounded-xl border border-slate-100 bg-slate-50/50 p-1.5"
            >
              <li
                v-for="item in recentDestinations.slice(0, 10)"
                :key="item.destName"
                class="flex items-center justify-between gap-2 px-2.5 py-1.5 rounded-lg bg-white border border-slate-100 hover:border-blue-300 hover:bg-blue-50/50 cursor-pointer transition-colors text-xs"
                @click="selectRecentDestination(item)"
              >
                <div class="flex items-center gap-2 min-w-0 flex-1">
                  <i
                    class="fa-solid fa-clock-rotate-left text-[10px] text-blue-500 shrink-0"
                  ></i>
                  <span class="font-bold text-slate-800 truncate text-[12px]">{{
                    item.destName
                  }}</span>
                  <small
                    v-if="item.destAddress"
                    class="text-[10px] text-slate-400 truncate hidden sm:inline"
                    >{{ item.destAddress }}</small
                  >
                </div>
                <button
                  type="button"
                  class="h-5 w-5 shrink-0 flex items-center justify-center rounded-full text-[10px] text-slate-300 hover:text-slate-600 hover:bg-slate-100 transition-colors"
                  aria-label="삭제"
                  @click.stop="removeRecentDestination(item.destName)"
                >
                  ✕
                </button>
              </li>
            </ul>
          </div>

          <p
            v-if="isDestinationSearching"
            class="mt-3 text-center text-xs text-slate-400"
          >
            검색 중이에요.
          </p>
          <p
            v-else-if="destinationSearchError"
            class="mt-3 text-center text-xs text-red-500"
          >
            {{ destinationSearchError }}
          </p>
          <ul
            v-else-if="destinationSearchResults.length"
            class="mt-3 max-h-52 overflow-y-auto rounded-xl border border-slate-100"
          >
            <li
              v-for="item in destinationSearchResults"
              :key="`${item.destName}-${item.destAddress}`"
              class="border-b border-slate-100 last:border-0"
            >
              <button
                type="button"
                class="flex w-full items-center gap-2 px-3 py-3 text-left transition-colors hover:bg-blue-50"
                @click="selectDestination(item)"
              >
                <i
                  class="fa-solid fa-location-dot text-blue-500"
                  aria-hidden="true"
                ></i>
                <span class="min-w-0 flex-1">
                  <strong class="block truncate text-xs text-slate-800">{{
                    item.destName
                  }}</strong>
                  <small class="block truncate text-[11px] text-slate-400">{{
                    item.destAddress
                  }}</small>
                </span>
                <i
                  class="fa-solid fa-chevron-right text-[10px] text-slate-300"
                  aria-hidden="true"
                ></i>
              </button>
            </li>
          </ul>
          <p
            v-else-if="
              destinationSearchKeyword.trim().length >= 2 &&
              selectedDestination?.destName !== destinationSearchKeyword
            "
            class="mt-3 rounded-xl bg-slate-50 px-3 py-4 text-center text-xs text-slate-400"
          >
            검색 결과가 없어요.
          </p>
          <div
            v-else
            class="mt-3 rounded-xl bg-slate-50 px-3 py-4 text-center text-xs text-slate-400"
          >
            장소명 또는 주소를 입력하면<br />검색 결과가 표시됩니다.
          </div>
          <button
            type="button"
            class="mt-4 w-full rounded-xl bg-blue-600 py-3 text-sm font-black text-white shadow-md transition-all hover:bg-blue-700"
            :disabled="isDestinationSaving"
            @click="applyDestination"
          >
            {{ isDestinationSaving ? '이동 중...' : '적용하기' }}
          </button>
        </div>
      </div>

      <!-- 🛡️ 퀵버튼 2: 안전 점수 설정 (목적지 바로 옆 2순위 배치, 고정 너비 min-w-[102px]) -->
      <div class="relative order-2">
        <button
          type="button"
          class="flex items-center justify-between gap-1.5 px-3.5 py-2 rounded-full text-xs font-bold transition-all border shadow-sm min-w-[124px]"
          :class="safetyAccentClass"
          @click="togglePopover('safety')"
        >
          <span class="flex items-center gap-1">
            <span>🛡️</span>
            <span class="whitespace-nowrap">{{
              appliedQuickFilters.minSafetyScore > 0
                ? `안전: ${appliedQuickFilters.minSafetyScore}점 이상`
                : '안전: 무관'
            }}</span>
          </span>
          <span class="text-[10px] text-slate-400">▼</span>
        </button>

        <!-- 안전점수 팝업 -->
        <div
          v-if="activePopover === 'safety'"
          class="absolute top-full left-0 mt-2 w-80 rounded-2xl border border-slate-200 bg-white p-4 shadow-xl z-40 space-y-4"
        >
          <!-- 헤더 및 실시간 점수 배지 -->
          <div class="flex items-center justify-between">
            <span class="text-xs font-black text-slate-800"
              >🛡️ 최소 안전 점수</span
            >
            <div class="flex items-center gap-2">
              <span
                class="text-xs font-black px-2.5 py-1 rounded-full border"
                :class="
                  filters.minSafetyScore >= 80
                    ? 'bg-emerald-50 text-emerald-600 border-emerald-200'
                    : 'bg-amber-50 text-amber-600 border-amber-200'
                "
              >
                {{
                  filters.minSafetyScore === 0
                    ? '전체 보기'
                    : `${filters.minSafetyScore}점 이상`
                }}
              </span>
              <button
                type="button"
                class="flex h-7 w-7 items-center justify-center rounded-full text-sm text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
                aria-label="안전 점수 설정 닫기"
                @click="activePopover = null"
              >
                ✕
              </button>
            </div>
          </div>

          <!-- 통합형 type="range" 게이지 슬라이더 -->
          <div class="space-y-2 pt-1">
            <input
              type="range"
              v-model.number="filters.minSafetyScore"
              min="0"
              max="90"
              step="10"
              class="w-full appearance-none cursor-pointer shadow-inner transition-all safety-range-input"
              :style="{
                '--thumb-color': safetyThumbColor,
                background:
                  filters.minSafetyScore > 0
                    ? `linear-gradient(to right, ${filters.minSafetyScore >= 80 ? '#10b981' : '#f59e0b'} 0%, ${filters.minSafetyScore >= 80 ? '#10b981' : '#f59e0b'} ${(filters.minSafetyScore / 90) * 100}%, #e2e8f0 ${(filters.minSafetyScore / 90) * 100}%, #e2e8f0 100%)`
                    : '#e2e8f0',
              }"
            />
            <div
              class="flex justify-between text-[11px] font-bold text-slate-400"
            >
              <span>0점</span>
              <span>90점</span>
            </div>
          </div>

          <!-- 1-클릭 프리셋 칩 버튼 (70점, 80점, 90점, 95점 4종) -->
          <div class="grid grid-cols-4 gap-1 pt-1">
            <button
              v-for="score in [60, 70, 80, 90]"
              :key="score"
              type="button"
              class="py-1.5 rounded-lg text-[11px] font-bold transition-all text-center border"
              :class="[
                filters.minSafetyScore === score
                  ? score >= 80
                    ? 'bg-emerald-500 text-white border-emerald-500 font-black shadow-sm'
                    : 'bg-amber-500 text-white border-amber-500 font-black shadow-sm'
                  : 'bg-slate-50 border-slate-200 text-slate-700 hover:bg-slate-100',
              ]"
              @click="filters.minSafetyScore = score"
            >
              {{ score }}점+
            </button>
          </div>

          <!-- 팝업 하단 적용하기 버튼 -->
          <button
            type="button"
            class="mt-4 w-full rounded-xl bg-blue-600 py-3 text-sm font-black text-white shadow-md transition-all hover:bg-blue-700"
            @click="
              updateFilters();
              emit('apply');
              activePopover = null;
            "
          >
            적용하기
          </button>
        </div>
      </div>

      <!-- 💰 퀵버튼 3: 보증금 + 월세/전세 (고정 너비 min-w-[125px]) -->
      <div class="relative order-3">
        <button
          type="button"
          class="flex items-center justify-between gap-1.5 px-3.5 py-2 rounded-full text-xs font-bold transition-all border shadow-sm min-w-[122px]"
          :class="[
            appliedQuickFilters.maxDeposit < DEPOSIT_MAX ||
            appliedQuickFilters.maxRent < RENT_MAX
              ? 'bg-blue-50 text-blue-600 border-blue-300 font-extrabold'
              : 'bg-white hover:bg-slate-50 text-slate-700 border-slate-200',
          ]"
          @click="togglePopover('price')"
        >
          <span class="flex items-center gap-1">
            <span>💰</span>
            <span class="whitespace-nowrap">{{ priceSummaryText }}</span>
          </span>
          <span class="text-[10px] text-slate-400">▼</span>
        </button>

        <!-- 가격 팝업 -->
        <div
          v-if="activePopover === 'price'"
          class="absolute top-full left-0 mt-2 w-80 rounded-2xl border border-slate-200 bg-white p-4 shadow-xl z-40 space-y-4"
        >
          <div class="flex items-center justify-between">
            <span class="text-sm font-black text-slate-800"
              >가격 (보증금&월세)</span
            >
            <button
              type="button"
              class="flex h-7 w-7 items-center justify-center rounded-full text-sm text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
              aria-label="가격 설정 닫기"
              @click="activePopover = null"
            >
              ✕
            </button>
          </div>

          <!-- 🏠 월세 vs 🏢 전세 선택 탭 -->
          <div class="flex bg-slate-100 p-1 rounded-xl gap-1">
            <button
              v-for="t in [
                { key: 'MONTHLY', label: '🏠 월세' },
                { key: 'JEONSE', label: '🏢 전세' },
              ]"
              :key="t.key"
              type="button"
              class="flex-1 py-1.5 rounded-lg text-xs font-bold transition-all text-center"
              :class="
                filters.tradeType === t.key
                  ? 'bg-white text-blue-600 shadow-sm font-black'
                  : 'text-slate-500 hover:text-slate-700'
              "
              @click="selectTradeType(t.key)"
            >
              {{ t.label }}
            </button>
          </div>

          <!-- 전세금/보증금 슬라이더 (Dual Range - 자유로운 교차 및 역할 체인지) -->
          <div class="space-y-1.5">
            <div class="flex justify-between text-xs font-bold text-slate-700">
              <span>{{
                filters.tradeType === 'JEONSE'
                  ? '전세 보증금 범위'
                  : '월세 보증금 범위'
              }}</span>
              <span class="text-blue-600 font-extrabold">{{
                depositAmountLabel
              }}</span>
            </div>
            <div class="relative w-full h-7 flex items-center">
              <div
                class="absolute inset-x-0 h-2 bg-slate-200 rounded-full pointer-events-none"
              ></div>
              <div
                class="absolute h-2 bg-blue-600 rounded-full pointer-events-none transition-all duration-75"
                :style="
                  getDualRangeTrackStyle(
                    depositValA,
                    depositValB,
                    0,
                    depositOptions.length - 1,
                  )
                "
              ></div>
              <input
                type="range"
                v-model.number="depositValA"
                min="0"
                :max="depositOptions.length - 1"
                step="1"
                class="dual-range-input"
                :style="{ zIndex: activeSliderThumb === 'depA' ? 30 : 20 }"
                @pointerdown="activeSliderThumb = 'depA'"
                @touchstart="activeSliderThumb = 'depA'"
              />
              <input
                type="range"
                v-model.number="depositValB"
                min="0"
                :max="depositOptions.length - 1"
                step="1"
                class="dual-range-input"
                :style="{ zIndex: activeSliderThumb === 'depB' ? 30 : 20 }"
                @pointerdown="activeSliderThumb = 'depB'"
                @touchstart="activeSliderThumb = 'depB'"
              />
            </div>
            <div
              class="flex justify-between text-[11px] font-bold text-slate-400"
            >
              <span>{{ DEPOSIT_MIN_LABEL }}</span>
              <span>{{ DEPOSIT_MAX_LABEL }}</span>
            </div>
          </div>

          <!-- 월세 슬라이더 (Dual Range - 자유로운 교차 및 역할 체인지) -->
          <div v-if="filters.tradeType === 'MONTHLY'" class="space-y-1.5">
            <div class="flex justify-between text-xs font-bold text-slate-700">
              <span>월세 금액 범위</span>
              <span class="text-blue-600 font-extrabold">{{
                rentAmountLabel
              }}</span>
            </div>
            <div class="relative w-full h-7 flex items-center">
              <div
                class="absolute inset-x-0 h-2 bg-slate-200 rounded-full pointer-events-none"
              ></div>
              <div
                class="absolute h-2 bg-blue-600 rounded-full pointer-events-none transition-all duration-75"
                :style="
                  getDualRangeTrackStyle(rentValA, rentValB, RENT_MIN, RENT_MAX)
                "
              ></div>
              <input
                type="range"
                v-model.number="rentValA"
                :min="RENT_MIN"
                :max="RENT_MAX"
                :step="RENT_STEP"
                class="dual-range-input"
                :style="{ zIndex: activeSliderThumb === 'rentA' ? 30 : 20 }"
                @pointerdown="activeSliderThumb = 'rentA'"
                @touchstart="activeSliderThumb = 'rentA'"
              />
              <input
                type="range"
                v-model.number="rentValB"
                :min="RENT_MIN"
                :max="RENT_MAX"
                :step="RENT_STEP"
                class="dual-range-input"
                :style="{ zIndex: activeSliderThumb === 'rentB' ? 30 : 20 }"
                @pointerdown="activeSliderThumb = 'rentB'"
                @touchstart="activeSliderThumb = 'rentB'"
              />
            </div>
            <div
              class="flex justify-between text-[11px] font-bold text-slate-400"
            >
              <span>{{ RENT_MIN_LABEL }}</span>
              <span>{{ RENT_MAX_LABEL }}</span>
            </div>
          </div>

          <!-- 팝업 하단 적용하기 버튼 -->
          <button
            type="button"
            class="mt-4 w-full rounded-xl bg-blue-600 py-3 text-sm font-black text-white shadow-md transition-all hover:bg-blue-700"
            @click="
              updateFilters();
              emit('apply');
              activePopover = null;
            "
          >
            적용하기
          </button>
        </div>
      </div>

      <!-- 🏦 퀵버튼 4: 대출 상품 (order-1) -->

      <!-- 🚶‍♂️/🚌 퀵버튼 5: 이동 시간 & 수단 (고정 너비 min-w-[155px]) -->
      <div class="relative order-5">
        <button
          type="button"
          class="flex items-center justify-between gap-1.5 px-3.5 py-2 rounded-full text-xs font-bold transition-all border shadow-sm min-w-[158px]"
          :class="[
            appliedQuickFilters.transportMode === 'WALK'
              ? 'bg-blue-600 text-white border-blue-600 font-black'
              : 'bg-blue-600 text-white border-blue-600 font-black',
          ]"
          @click="togglePopover('travel')"
        >
          <span class="whitespace-nowrap">{{ travelSummaryText }}</span>
          <span class="text-[10px] opacity-80">▼</span>
        </button>

        <!-- 이동시간 & 수단 팝업 (슬라이더 조절 동안만 모달창 35% 반투명 투영) -->
        <div
          v-if="activePopover === 'travel'"
          class="absolute top-full left-0 mt-2 w-80 rounded-2xl border p-4 shadow-2xl z-40 space-y-4 transition-all duration-300"
          :class="[
            isSliderDragging
              ? 'bg-white/35 border-blue-400/90 backdrop-blur-xs opacity-40 shadow-inner'
              : 'bg-white border-slate-200 opacity-100',
          ]"
        >
          <!-- 헤더 타이틀 요약 -->
          <div class="flex items-center justify-between">
            <div class="text-sm font-black text-slate-800">
              시간
              <span class="text-blue-600">
                {{
                  filters.transportMode === 'WALK'
                    ? `도보 ${filters.travelTime}분 이내`
                    : `대중교통 ${filters.travelTime}분 이내`
                }}
              </span>
            </div>
            <button
              type="button"
              class="flex h-7 w-7 items-center justify-center rounded-full text-sm text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
              aria-label="이동 시간 설정 닫기"
              @click="activePopover = null"
            >
              ✕
            </button>
          </div>

          <!-- 탭 선택: 🚶 걸어서 이동 vs 🚌 대중교통 이용 -->
          <div class="flex border-b border-slate-200">
            <button
              type="button"
              class="flex-1 py-2 text-xs font-bold transition-all relative flex items-center justify-center gap-1.5 pb-2.5"
              :class="
                filters.transportMode === 'WALK'
                  ? 'text-blue-600 font-extrabold'
                  : 'text-slate-500 hover:text-slate-800'
              "
              @click="
                filters.transportMode = 'WALK';
                filters.minTravelTime = 0;
                if (filters.travelTime < 5) filters.travelTime = 5;
                if (filters.travelTime > 40) filters.travelTime = 40;
              "
            >
              <span>🚶</span>
              <span>걸어서 이동</span>
              <div
                v-if="filters.transportMode === 'WALK'"
                class="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-600 rounded-full"
              ></div>
            </button>

            <button
              type="button"
              class="flex-1 py-2 text-xs font-bold transition-all relative flex items-center justify-center gap-1.5 pb-2.5"
              :class="
                filters.transportMode === 'TRANSIT'
                  ? 'text-blue-600 font-extrabold'
                  : 'text-slate-500 hover:text-slate-800'
              "
              @click="
                filters.transportMode = 'TRANSIT';
                filters.minTravelTime = 0;
                if (filters.travelTime < 15) filters.travelTime = 15;
              "
            >
              <span>🚌</span>
              <span>대중교통 이용</span>
              <div
                v-if="filters.transportMode === 'TRANSIT'"
                class="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-600 rounded-full"
              ></div>
            </button>
          </div>

          <!-- 🚶‍♂️ [도보 모드]: 단일 슬라이더 (핸들 1개) -->
          <div v-if="filters.transportMode === 'WALK'" class="space-y-1.5 pt-1">
            <div
              class="flex items-center justify-between text-xs font-bold text-slate-800"
            >
              <span>🎯 원하는 이동 시간</span>
              <span class="text-blue-600 font-extrabold text-sm"
                >{{ filters.travelTime }}분 이내</span
              >
            </div>
            <input
              type="range"
              v-model.number="filters.travelTime"
              min="5"
              max="40"
              step="5"
              class="w-full appearance-none cursor-pointer quick-range-input"
              :style="rangeStyle(filters.travelTime, 5, 40)"
              @pointerdown="handleSliderStart('walk')"
              @touchstart="handleSliderStart('walk')"
              @pointerup="handleSliderEnd"
              @touchend="handleSliderEnd"
              @input="updateFilters"
            />
            <div
              class="flex justify-between text-[11px] font-bold text-slate-400"
            >
              <span>5분</span>
              <span>40분</span>
            </div>
          </div>

          <!-- 🚌 [대중교통 모드]: 단일 슬라이더 (최대 이동시간) -->
          <div v-else class="space-y-1.5 pt-1">
            <div
              class="flex items-center justify-between text-xs font-bold text-slate-800"
            >
              <span>🎯 최대 이동 시간</span>
              <span class="text-blue-600 font-extrabold text-sm"
                >{{ filters.travelTime }}분 이내</span
              >
            </div>
            <input
              type="range"
              v-model.number="filters.travelTime"
              min="10"
              max="60"
              step="5"
              class="w-full appearance-none cursor-pointer quick-range-input"
              :style="rangeStyle(filters.travelTime, 10, 60)"
              @pointerdown="handleSliderStart('transit')"
              @touchstart="handleSliderStart('transit')"
              @pointerup="handleSliderEnd"
              @touchend="handleSliderEnd"
              @input="updateFilters"
            />
            <div
              class="flex justify-between text-[11px] font-bold text-slate-400"
            >
              <span>10분</span>
              <span>60분</span>
            </div>
          </div>

          <!-- 🚶‍♂️ [도보 모드]: 걸음 속도 선택 -->
          <div v-if="filters.transportMode === 'WALK'" class="space-y-2 pt-1">
            <div class="flex justify-between text-xs font-bold text-slate-800">
              <span>🚶‍♂️ 걸음 속도</span>
              <span class="text-blue-600 font-extrabold text-xs">
                {{
                  filters.walkPace === 'SLOW'
                    ? '천천히 (약 3.5km/h)'
                    : filters.walkPace === 'FAST'
                      ? '빠른 걸음 (약 5.5km/h)'
                      : '보통 걸음 (약 4.5km/h)'
                }}
              </span>
            </div>
            <div class="grid grid-cols-3 gap-1.5 p-1 bg-slate-100 rounded-xl">
              <button
                v-for="pace in [
                  { key: 'SLOW', label: '🐢 천천히' },
                  { key: 'NORMAL', label: '🚶 보통' },
                  { key: 'FAST', label: '🏃 빠르게' },
                ]"
                :key="pace.key"
                type="button"
                class="py-1.5 rounded-lg text-xs font-bold transition-all text-center"
                :class="
                  filters.walkPace === pace.key
                    ? 'bg-white text-blue-600 shadow-sm font-black'
                    : 'text-slate-500 hover:text-slate-800'
                "
                @click="
                  filters.walkPace = pace.key;
                  updateFilters();
                "
              >
                {{ pace.label }}
              </button>
            </div>
            <p
              class="text-[11px] text-slate-400 font-medium leading-normal bg-slate-50 p-2 rounded-lg border border-slate-100"
            >
              * 선택한 걸음 속도에 따라 도달 가능 범위가 자동 계산됩니다.
            </p>
          </div>

          <!-- 🚌 [대중교통 모드]: 최소 이동시간 -->
          <div
            v-if="filters.transportMode === 'TRANSIT'"
            class="space-y-1.5 pt-1"
          >
            <div
              class="flex items-center justify-between text-xs font-bold text-slate-800"
            >
              <span>⏳ 최소 이동시간</span>
              <span class="text-amber-500 font-extrabold text-sm"
                >{{
                  Math.max(0, filters.travelTime - filters.flexTime)
                }}분</span
              >
            </div>
            <input
              type="range"
              v-model.number="filters.flexTime"
              min="5"
              :max="Math.min(30, filters.travelTime)"
              step="5"
              class="w-full appearance-none cursor-pointer quick-range-input"
              :style="
                rangeStyle(
                  filters.flexTime,
                  5,
                  Math.min(30, filters.travelTime),
                  '#f59e0b',
                )
              "
              @pointerdown="handleSliderStart('flex')"
              @touchstart="handleSliderStart('flex')"
              @pointerup="handleSliderEnd"
              @touchend="handleSliderEnd"
              @input="updateFilters"
            />
            <div
              class="flex justify-between text-[11px] font-bold text-slate-400"
            >
              <span
                >{{
                  Math.max(
                    0,
                    filters.travelTime - Math.min(30, filters.travelTime),
                  )
                }}분</span
              >
              <span>{{ Math.max(0, filters.travelTime - 5) }}분</span>
            </div>
            <p
              class="text-[11px] text-slate-400 font-medium leading-normal bg-slate-50 p-2 rounded-lg border border-slate-100"
            >
              {{ Math.max(0, filters.travelTime - filters.flexTime) }}분
              미만(너무 가까운 지역) 매물은 제외하고 표시해요.
            </p>
          </div>

          <!-- 팝업 하단 적용하기 버튼 -->
          <button
            type="button"
            class="mt-4 w-full rounded-xl bg-blue-600 py-3 text-sm font-black text-white shadow-md transition-all hover:bg-blue-700"
            @click="
              updateFilters();
              emit('apply');
              activePopover = null;
            "
          >
            적용하기
          </button>
        </div>
      </div>

      <!-- ⭕ 퀵버튼 6: 이소크론 원형 영역 토글 -->
      <button
        type="button"
        class="order-6 flex items-center gap-1.5 px-3.5 py-2 rounded-full text-xs font-bold transition-all border shadow-sm"
        :class="[
          filters.showIsochrone
            ? 'bg-indigo-50 text-indigo-700 border-indigo-300 font-black'
            : 'bg-white text-slate-500 border-slate-200',
        ]"
        @click="toggleIsochrone"
      >
        <span>⭕ 영역 {{ filters.showIsochrone ? 'ON' : 'OFF' }}</span>
      </button>

      <!-- 🔄 퀵버튼 7: 온보딩 초기화 -->
      <button
        type="button"
        class="order-7 flex items-center gap-1 px-3.5 py-2 rounded-full text-xs font-bold text-slate-600 bg-white hover:bg-slate-50 hover:border-slate-300 transition-all border border-slate-200 shadow-sm"
        title="처음 설정한 온보딩 조건으로 초기화"
        @click="handleReset"
      >
        <span>↻</span>
        <span>온보딩으로 되돌리기</span>
      </button>
    </div>

    <!-- ======================================================== -->
    <!-- 2. 모바일 전용 퀵 플로팅 버튼 (HomePage.vue 원형 필터 버튼) -->
    <!-- ======================================================== -->
    <div class="flex xl:hidden items-center gap-1.5 z-30">
      <button
        type="button"
        class="filter-floating-button-circle"
        aria-label="필터 열기"
        title="필터"
        @click="emit('open-filter')"
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

      <!-- 모바일 이소크론 영역 ON/OFF 빠른 버튼 -->
      <button
        type="button"
        class="filter-floating-button-capsule"
        :class="[
          filters.showIsochrone
            ? 'bg-indigo-600 text-white border-indigo-500 font-black'
            : 'bg-white/90 text-slate-500 border-slate-200',
        ]"
        @click="toggleIsochrone"
      >
        <span>⭕ 영역 {{ filters.showIsochrone ? 'ON' : 'OFF' }}</span>
      </button>
    </div>

    <!-- ======================================================== -->
    <!-- 3. 모바일 전용 필터 바텀시트 모달 (분리 컴포넌트 마운트) -->
    <!-- ======================================================== -->
  </div>
</template>

<style scoped>
/* HomePage.vue 원형 필터 버튼 전용 스타일 */
.filter-floating-button-circle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
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

.filter-floating-button-circle:hover {
  background: #f8f9ff;
  box-shadow:
    0 5px 12px rgb(0 0 0 / 16%),
    0 2px 4px rgb(0 0 0 / 8%);
}

.filter-floating-button-circle:active {
  transform: scale(0.96);
}

.filter-icon {
  width: 19px;
  height: 19px;
}

.filter-floating-button-capsule {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.7rem;
  border-radius: 9999px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  font-size: 0.6875rem;
  font-weight: 700;
  transition: all 0.15s ease-in-out;
  border-width: 1px;
}

.filter-floating-button:active {
  transform: scale(0.95);
}

.filter-floating-button-dark {
  background-color: #0f172a;
  color: #ffffff;
  font-weight: 900;
  border-color: #334155;
}

.filter-floating-button-white {
  background-color: #ffffff;
  color: #0f172a;
  border-color: #e2e8f0;
}

/* 드롭다운 공통 슬라이더 */
.safety-range-input,
.quick-range-input {
  height: 10px;
  border: 0;
  border-radius: 999px;
}
.safety-range-input::-webkit-slider-thumb,
.quick-range-input::-webkit-slider-thumb {
  appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--thumb-color, #f59e0b);
  border: 2px solid #ffffff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    transform 0.15s ease,
    box-shadow 0.2s ease;
}

.safety-range-input::-webkit-slider-thumb:hover,
.quick-range-input::-webkit-slider-thumb:hover {
  transform: scale(1.2);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.35);
}

.safety-range-input::-moz-range-thumb,
.quick-range-input::-moz-range-thumb {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--thumb-color, #f59e0b);
  border: 2.5px solid #ffffff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    transform 0.15s ease,
    box-shadow 0.2s ease;
}

/* 듀얼 범위 슬라이더 전용 CSS */
.dual-range-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  appearance: none;
  background: transparent;
  pointer-events: none;
  z-index: 10;
  margin: 0;
}

.dual-range-input::-webkit-slider-thumb {
  pointer-events: auto;
  appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #ffffff;
  border: 2.5px solid #2563eb;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
  transition:
    transform 0.15s ease,
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.dual-range-input::-webkit-slider-thumb:hover {
  transform: scale(1.2);
  border-color: #1d4ed8;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.35);
}

.dual-range-input::-moz-range-thumb {
  pointer-events: auto;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #ffffff;
  border: 2.5px solid #2563eb;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
  transition:
    transform 0.15s ease,
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}
</style>
