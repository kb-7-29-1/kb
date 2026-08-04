<script setup>
import { ref, computed, watch } from 'vue';

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
    default: () => ({
      destination: '세종대학교',
      tradeType: 'MONTHLY', // 'MONTHLY' | 'JEONSE'
      maxDeposit: 5000, // 만원
      maxRent: 100, // 만원
      minSafetyScore: 0, // 0 ~ 100
      transportMode: 'WALK', // 'WALK' | 'TRANSIT'
      travelTime: 15, // 5분 ~ 60분 (도보 min 5, 대중교통 min 10)
      walkPace: 'NORMAL', // 'SLOW' | 'NORMAL' | 'FAST'
      flexTime: 10, // ±5분 ~ ±20분
      showIsochrone: true,
      selectedAmenities: [],
    }),
  },
  totalCount: {
    type: Number,
    default: 0,
  },
});

const emit = defineEmits(['update:modelValue', 'reset', 'apply', 'open-filter']);

// 로컬 반응형 상태
const filters = ref({
  tradeType: 'MONTHLY',
  travelTime: 15,
  walkPace: 'NORMAL',
  flexTime: 10,
  ...props.modelValue,
});

// 모바일 바텀시트 모달 상태
// PC 드롭다운 열림 상태 (activePopover: null | 'destination' | 'price' | 'safety' | 'travel')
const activePopover = ref(null);

const PRESET_COORDS = {
  세종대학교: { address: '서울특별시 광진구 능동로 209', lat: 37.5502, lng: 127.0731 },
  건국대학교: { address: '서울특별시 광진구 능동로 120', lat: 37.5408, lng: 127.0793 },
  강남역: { address: '서울특별시 강남구 강남대로 지하396', lat: 37.4979, lng: 127.0276 },
  역삼역: { address: '서울특별시 강남구 테헤란로 지하156', lat: 37.5006, lng: 127.0365 },
  성수역: { address: '서울특별시 성동구 아차산로 113', lat: 37.5445, lng: 127.0557 },
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

const selectDestinationOption = (dest) => {
  filters.value.destination = dest;
  if (PRESET_COORDS[dest]) {
    filters.value.destinationAddress = PRESET_COORDS[dest].address;
    filters.value.destinationLat = PRESET_COORDS[dest].lat;
    filters.value.destinationLng = PRESET_COORDS[dest].lng;
  } else {
    // 유저 지정 새 장소 시 기존 좌표 초기화 (지오코더가 주소 기반 자동 재생성)
    delete filters.value.destinationLat;
    delete filters.value.destinationLng;
  }
  updateFilters();
  activePopover.value = null;
};

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
  },
  { deep: true },
);

// 필터 상태 변경 시 부모로 전달
const updateFilters = () => {
  emit('update:modelValue', { ...filters.value });
};

// 드롭다운 토글
const togglePopover = (name) => {
  activePopover.value = activePopover.value === name ? null : name;
};

// 이소크론 영역 토글 (ON <-> OFF)
const toggleIsochrone = () => {
  filters.value.showIsochrone = !filters.value.showIsochrone;
  updateFilters();
};

// 필터 초기화
const handleReset = () => {
  filters.value = {
    destination: '세종대학교',
    destinationLat: 37.5502,
    destinationLng: 127.0731,
    tradeType: 'MONTHLY',
    maxDeposit: 5000,
    maxRent: 100,
    minSafetyScore: 0,
    transportMode: 'WALK',
    travelTime: 15,
    walkPace: 'NORMAL',
    flexTime: 10,
    showIsochrone: true,
    selectedAmenities: [],
  };
  activePopover.value = null;
  updateFilters();
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

// 가격 요약 텍스트
const priceSummaryText = computed(() => {
  if (filters.value.tradeType === 'JEONSE') {
    return filters.value.maxDeposit >= 5000 ? '전세: 전체' : `전세: ${filters.value.maxDeposit}`;
  }
  if (filters.value.maxDeposit >= 5000 && filters.value.maxRent >= 100) {
    return '월세: 전체';
  }
  const deposit = filters.value.maxDeposit >= 5000 ? '전체' : filters.value.maxDeposit;
  const rent = filters.value.maxRent >= 100 ? '전체' : filters.value.maxRent;
  return `월세: ${deposit}/${rent}`;
});

// 안전점수 동적 콩나물 핸들(Thumb) 색상 (점수 구간별 변색)
const safetyThumbColor = computed(() => {
  const score = filters.value.minSafetyScore;
  if (score === 0) return '#64748b'; // Slate gray (전체)
  if (score < 70) return '#f59e0b'; // Amber (보통)
  if (score < 90) return '#10b981'; // Emerald (안심)
  return '#2563eb'; // Royal Blue (최상급)
});
</script>

<template>
  <div class="relative w-fit">
    <!-- ======================================================== -->
    <!-- 1. PC 전용 상단 6종 부유형(Floating) 퀵버튼 바 (md:inline-flex w-fit) -->
    <!-- ======================================================== -->
    <div class="hidden md:inline-flex w-fit items-center gap-2 text-slate-800 z-30">
      <!-- 📍 퀵버튼 1: 목적지 (고정 너비 min-w-[115px]) -->
      <div class="relative order-1">
        <button
          type="button"
          class="flex items-center justify-between gap-1.5 px-3.5 py-2 rounded-full text-xs font-bold transition-all bg-white hover:bg-slate-50 text-slate-900 border border-slate-200 shadow-sm min-w-[140px]"
          @click="togglePopover('destination')"
        >
          <span class="flex items-center gap-1">
            <span class="text-blue-600">📍</span>
            <span class="whitespace-nowrap">목적지: {{ filters.destination }}</span>
          </span>
          <span class="text-[10px] text-slate-400">▼</span>
        </button>

        <!-- 목적지 변경 드롭다운 -->
        <div
          v-if="activePopover === 'destination'"
          class="absolute top-full left-0 mt-2 w-64 bg-white rounded-2xl shadow-2xl border border-slate-200 p-3 z-40 space-y-2"
        >
          <div class="text-xs font-black text-slate-700">주 목적지 변경</div>
          <div class="space-y-1 max-h-60 overflow-y-auto">
            <button
              v-for="dest in destinationList"
              :key="dest"
              type="button"
              class="w-full text-left px-3 py-2 rounded-xl text-xs font-bold transition-all flex items-center justify-between"
              :class="
                filters.destination === dest
                  ? 'bg-blue-50 text-blue-600 font-black'
                  : 'text-slate-700 hover:bg-slate-100'
              "
              @click="selectDestinationOption(dest)"
            >
              <span>📍 {{ dest }}</span>
              <span v-if="filters.destination === dest">✓</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 🛡️ 퀵버튼 2: 안전 점수 설정 (목적지 바로 옆 2순위 배치, 고정 너비 min-w-[102px]) -->
      <div class="relative order-3">
        <button
          type="button"
          class="flex items-center justify-between gap-1.5 px-3.5 py-2 rounded-full text-xs font-bold transition-all border shadow-sm min-w-[124px]"
          :class="[
            filters.minSafetyScore > 0
              ? 'bg-amber-50 text-amber-700 border-amber-300 font-extrabold'
              : 'bg-white hover:bg-slate-50 text-slate-700 border-slate-200',
          ]"
          @click="togglePopover('safety')"
        >
          <span class="flex items-center gap-1">
            <span>🛡️</span>
            <span class="whitespace-nowrap">{{
              filters.minSafetyScore > 0 ? `안전: ${filters.minSafetyScore}점 이상` : '안전: 무관'
            }}</span>
          </span>
          <span class="text-[10px] text-slate-400">▼</span>
        </button>

        <!-- 안전점수 팝업 -->
        <div
          v-if="activePopover === 'safety'"
          class="absolute top-full left-0 mt-2 w-72 bg-white rounded-2xl shadow-2xl border border-slate-200 p-4 z-40 space-y-4"
        >
          <!-- 헤더 및 실시간 점수 배지 -->
          <div class="flex items-center justify-between">
            <span class="text-xs font-black text-slate-800">🛡️ 최저 안전점수</span>
            <span
              class="text-xs font-black px-2.5 py-1 rounded-full bg-amber-50 text-amber-600 border border-amber-200"
            >
              {{ filters.minSafetyScore === 0 ? '전체 보기' : `${filters.minSafetyScore}점 이상` }}
            </span>
          </div>

          <!-- 통합형 type="range" 게이지 슬라이더 -->
          <div class="space-y-2 pt-1">
            <input
              type="range"
              v-model.number="filters.minSafetyScore"
              min="0"
              max="100"
              step="5"
              class="w-full h-3.5 rounded-lg appearance-none cursor-pointer border border-slate-200 shadow-inner transition-all safety-range-input"
              :style="{
                '--thumb-color': safetyThumbColor,
                background:
                  filters.minSafetyScore > 0
                    ? `linear-gradient(to right, #f59e0b 0%, #10b981 ${filters.minSafetyScore / 2}%, #3b82f6 ${filters.minSafetyScore}%, #e2e8f0 ${filters.minSafetyScore}%, #e2e8f0 100%)`
                    : '#e2e8f0',
              }"
              @input="updateFilters"
            />
            <div class="flex justify-between text-[11px] font-bold text-slate-400">
              <span>0점</span>
              <span>50점</span>
              <span>100점</span>
            </div>
          </div>

          <!-- 1-클릭 프리셋 칩 버튼 (70점, 80점, 90점, 95점 4종) -->
          <div class="grid grid-cols-4 gap-1 pt-1">
            <button
              v-for="score in [70, 80, 90, 95]"
              :key="score"
              type="button"
              class="py-1.5 rounded-lg text-[11px] font-bold transition-all text-center border"
              :class="[
                filters.minSafetyScore === score
                  ? 'bg-amber-500 text-white border-amber-500 font-black shadow-sm'
                  : 'bg-slate-50 border-slate-200 text-slate-700 hover:bg-slate-100',
              ]"
              @click="
                filters.minSafetyScore = score;
                updateFilters();
              "
            >
              {{ score }}점+
            </button>
          </div>

          <!-- 팝업 하단 적용하기 버튼 -->
          <button
            type="button"
            class="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-black transition-all text-center shadow-md mt-1"
            @click="
              updateFilters();
              activePopover = null;
            "
          >
            적용하기
          </button>
        </div>
      </div>

      <!-- 💰 퀵버튼 3: 보증금 + 월세/전세 (고정 너비 min-w-[125px]) -->
      <div class="relative order-2">
        <button
          type="button"
          class="flex items-center justify-between gap-1.5 px-3.5 py-2 rounded-full text-xs font-bold transition-all border shadow-sm min-w-[122px]"
          :class="[
            filters.maxDeposit < 5000 || filters.maxRent < 100 || filters.tradeType === 'JEONSE'
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
          class="absolute top-full left-0 mt-2 w-72 bg-white rounded-2xl shadow-2xl border border-slate-200 p-4 z-40 space-y-4"
        >
          <div class="text-xs font-black text-slate-800 flex justify-between items-center">
            <span>가격 & 거래 조건</span>
            <button
              type="button"
              class="text-slate-400 text-xs hover:text-slate-600"
              @click="activePopover = null"
            >
              ✕
            </button>
          </div>

          <!-- 거래 유형 (월세 vs 전세) -->
          <div class="flex bg-slate-100 p-1 rounded-xl gap-1">
            <button
              v-for="t in [
                { key: 'MONTHLY', label: '🏠 월세' },
                { key: 'JEONSE', label: '🏢 전세' },
              ]"
              :key="t.key"
              type="button"
              class="flex-1 py-2 rounded-lg text-xs font-bold transition-all text-center"
              :class="
                filters.tradeType === t.key
                  ? 'bg-white text-blue-600 shadow-sm font-extrabold'
                  : 'text-slate-500 hover:text-slate-800'
              "
              @click="
                filters.tradeType = t.key;
                updateFilters();
              "
            >
              {{ t.label }}
            </button>
          </div>

          <!-- 보증금 슬라이더 -->
          <div class="space-y-1.5">
            <div class="flex justify-between text-xs font-bold text-slate-700">
              <span>{{
                filters.tradeType === 'JEONSE' ? '최대 전세 보증금' : '최대 월세 보증금'
              }}</span>
              <span class="text-blue-600 font-extrabold">{{
                filters.maxDeposit >= 5000 ? '제한없음' : `${filters.maxDeposit} 만원`
              }}</span>
            </div>
            <input
              type="range"
              v-model.number="filters.maxDeposit"
              min="500"
              max="5000"
              step="500"
              class="w-full accent-blue-600 cursor-pointer"
            />
          </div>

          <!-- 월세 슬라이더 -->
          <div v-if="filters.tradeType === 'MONTHLY'" class="space-y-1.5">
            <div class="flex justify-between text-xs font-bold text-slate-700">
              <span>최대 월세</span>
              <span class="text-blue-600 font-extrabold">{{
                filters.maxRent >= 100 ? '제한없음' : `${filters.maxRent} 만원`
              }}</span>
            </div>
            <input
              type="range"
              v-model.number="filters.maxRent"
              min="20"
              max="100"
              step="10"
              class="w-full accent-blue-600 cursor-pointer"
            />
          </div>

          <!-- 팝업 하단 적용하기 버튼 -->
          <button
            type="button"
            class="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-black transition-all text-center shadow-md mt-2"
            @click="
              updateFilters();
              activePopover = null;
            "
          >
            적용하기
          </button>
        </div>
      </div>

      <!-- 🚶‍♂️/🚌 퀵버튼 4: 이동 시간 & 수단 (고정 너비 min-w-[155px]) -->
      <div class="relative order-4">
        <button
          type="button"
          class="flex items-center justify-between gap-1.5 px-3.5 py-2 rounded-full text-xs font-bold transition-all border shadow-sm min-w-[158px]"
          :class="[
            filters.transportMode === 'WALK'
              ? 'bg-blue-600 text-white border-blue-600 font-black'
              : 'bg-blue-600 text-white border-blue-600 font-black',
          ]"
          @click="togglePopover('travel')"
        >
          <span class="whitespace-nowrap"
            >{{ filters.transportMode === 'WALK' ? '🚶 도보' : '🚌 대중교통' }}:
            {{ filters.travelTime }}분 이내</span
          >
          <span class="text-[10px] opacity-80">▼</span>
        </button>

        <!-- 이동시간 & 수단 팝업 -->
        <div
          v-if="activePopover === 'travel'"
          class="absolute top-full left-0 mt-2 w-80 bg-white rounded-2xl shadow-2xl border border-slate-200 p-4 z-40 space-y-4"
        >
          <!-- 헤더 타이틀 요약 -->
          <div class="flex items-center justify-between">
            <div class="text-sm font-black text-slate-900">
              시간
              <span class="text-blue-600"
                >{{ filters.transportMode === 'WALK' ? '도보' : '대중교통' }}
                {{ filters.travelTime }}분 이내</span
              >
            </div>
            <button
              type="button"
              class="text-slate-400 text-xs hover:text-slate-600"
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
                if (filters.travelTime < 5) filters.travelTime = 5;
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
                if (filters.travelTime < 10) filters.travelTime = 10;
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

          <!-- 슬라이더 1: 🎯 원하는 이동 시간 (도보 최소 5분 / 대중교통 최소 10분) -->
          <div class="space-y-1.5 pt-1">
            <div class="flex items-center justify-between text-xs font-bold text-slate-800">
              <span>🎯 원하는 이동 시간</span>
              <span class="text-blue-600 font-extrabold text-sm">{{ filters.travelTime }}분</span>
            </div>
            <input
              type="range"
              v-model.number="filters.travelTime"
              :min="filters.transportMode === 'WALK' ? 5 : 10"
              max="60"
              step="5"
              class="w-full accent-blue-600 cursor-pointer"
              @input="updateFilters"
            />
            <div class="flex justify-between text-[11px] font-bold text-slate-400">
              <span>{{ filters.transportMode === 'WALK' ? '5분' : '10분' }}</span>
              <span>60분</span>
            </div>
          </div>

          <!-- 🚶‍♂️ [도보 모드]: 걸음 속도 선택 -->
          <div v-if="filters.transportMode === 'WALK'" class="space-y-2 pt-1">
            <div class="flex items-center justify-between text-xs font-bold text-slate-800">
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

          <!-- 🚌 [대중교통 모드]: 앞뒤 여유 시간 슬라이더 -->
          <div v-else class="space-y-1.5 pt-1">
            <div class="flex items-center justify-between text-xs font-bold text-slate-800">
              <span>⏳ 앞뒤 여유 시간</span>
              <span class="text-amber-500 font-extrabold text-sm">±{{ filters.flexTime }}분</span>
            </div>
            <input
              type="range"
              v-model.number="filters.flexTime"
              min="5"
              max="20"
              step="5"
              class="w-full accent-amber-500 cursor-pointer"
              @input="updateFilters"
            />
            <div class="flex justify-between text-[11px] font-bold text-slate-400">
              <span>±5분</span>
              <span>±20분</span>
            </div>
          </div>

          <!-- 팝업 하단 적용하기 버튼 -->
          <button
            type="button"
            class="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-black transition-all text-center shadow-md mt-2"
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

      <!-- ⭕ 퀵버튼 5: 이소크론 원형 영역 토글 -->
      <button
        type="button"
        class="order-5 flex items-center gap-1.5 px-3.5 py-2 rounded-full text-xs font-bold transition-all border shadow-sm"
        :class="[
          filters.showIsochrone
            ? 'bg-indigo-50 text-indigo-700 border-indigo-300 font-black'
            : 'bg-white text-slate-500 border-slate-200',
        ]"
        @click="toggleIsochrone"
      >
        <span>⭕ 영역 {{ filters.showIsochrone ? 'ON' : 'OFF' }}</span>
      </button>

      <!-- 🔄 퀵버튼 6: 초기화 -->
      <button
        type="button"
        class="order-6 flex items-center gap-1 px-3.5 py-2 rounded-full text-xs font-bold text-slate-600 bg-white hover:bg-slate-50 transition-all border border-slate-200 shadow-sm"
        title="필터 초기화"
        @click="handleReset"
      >
        <span>↻</span>
        <span>초기화</span>
      </button>
    </div>

    <!-- ======================================================== -->
    <!-- 2. 모바일 전용 퀵 플로팅 버튼 (HomePage.vue 원형 필터 버튼) -->
    <!-- ======================================================== -->
    <div class="flex md:hidden items-center gap-1.5 z-30">
      <button
        type="button"
        class="filter-floating-button-circle"
        aria-label="필터 열기"
        title="필터"
        @click="emit('open-filter')"
      >
        <svg class="filter-icon" viewBox="0 0 32 32" fill="none" aria-hidden="true">
          <path d="M5 8H27" stroke="currentColor" stroke-width="2.8" stroke-linecap="round" />
          <circle cx="20" cy="8" r="3.2" fill="currentColor" />

          <path d="M5 16H27" stroke="currentColor" stroke-width="2.8" stroke-linecap="round" />
          <circle cx="11" cy="16" r="3.2" fill="currentColor" />

          <path d="M5 24H27" stroke="currentColor" stroke-width="2.8" stroke-linecap="round" />
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
  width: 44px;
  height: 44px;
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
  width: 22px;
  height: 22px;
}

.filter-floating-button-capsule {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.625rem 0.875rem;
  border-radius: 9999px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  font-size: 0.75rem;
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

/* 동적 콩나물 핸들(Thumb) 색상 및 스케일 애니메이션 */
.safety-range-input::-webkit-slider-thumb {
  appearance: none;
  width: 20px;
  height: 20px;
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

.safety-range-input::-webkit-slider-thumb:hover {
  transform: scale(1.2);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.35);
}

.safety-range-input::-moz-range-thumb {
  width: 20px;
  height: 20px;
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
</style>
