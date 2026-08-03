<script setup>
import { ref, computed, watch } from 'vue';
import MobileFilterBottomSheet from './MobileFilterBottomSheet.vue';

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

const emit = defineEmits(['update:modelValue', 'reset', 'apply']);

// 로컬 반응형 상태
const filters = ref({
  tradeType: 'MONTHLY',
  travelTime: 15,
  walkPace: 'NORMAL',
  flexTime: 10,
  ...props.modelValue,
});

// 모바일 바텀시트 모달 상태
const isMobileModalOpen = ref(false);
// 모바일 바텀시트 모달 활성 탭 상태 ('general' | 'amenity')
const activeMobileTab = ref('general');

const openMobileFilter = (tab = 'general') => {
  activeMobileTab.value = tab;
  isMobileModalOpen.value = true;
};

// PC 드롭다운 열림 상태 (activePopover: null | 'destination' | 'price' | 'safety' | 'travel')
const activePopover = ref(null);

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
    return filters.value.maxDeposit >= 5000
      ? '전세 전체'
      : `전세 보증금 ${filters.value.maxDeposit}만`;
  }
  if (filters.value.maxDeposit >= 5000 && filters.value.maxRent >= 100) {
    return '월세 전체';
  }
  let text = '월세 ';
  if (filters.value.maxDeposit < 5000) text += `${filters.value.maxDeposit}만`;
  if (filters.value.maxRent < 100) text += `/${filters.value.maxRent}만`;
  return text;
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
    <div
      class="hidden md:inline-flex w-fit items-center gap-2 px-3 py-2 bg-white/95 backdrop-blur-md rounded-2xl shadow-xl border border-slate-200/80 text-slate-800 z-30"
    >
      <!-- 📍 퀵버튼 1: 목적지 (고정 너비 min-w-[115px]) -->
      <div class="relative">
        <button
          type="button"
          class="flex items-center justify-between gap-1.5 px-3 py-2 rounded-xl text-xs font-bold transition-all bg-slate-100 hover:bg-slate-200/80 text-slate-900 border border-slate-200 min-w-[115px]"
          @click="togglePopover('destination')"
        >
          <span class="flex items-center gap-1">
            <span class="text-blue-600">⌖</span>
            <span class="whitespace-nowrap">{{ filters.destination }}</span>
          </span>
          <span class="text-[10px] text-slate-400">▼</span>
        </button>

        <!-- 목적지 변경 드롭다운 -->
        <div
          v-if="activePopover === 'destination'"
          class="absolute top-full left-0 mt-2 w-64 bg-white rounded-2xl shadow-2xl border border-slate-200 p-3 z-40 space-y-2"
        >
          <div class="text-xs font-black text-slate-700">주 목적지 변경</div>
          <div class="space-y-1">
            <button
              v-for="dest in ['세종대학교', '건국대학교', '강남역', '역삼역', '성수역']"
              :key="dest"
              type="button"
              class="w-full text-left px-3 py-2 rounded-xl text-xs font-bold transition-all flex items-center justify-between"
              :class="
                filters.destination === dest
                  ? 'bg-blue-50 text-blue-600 font-black'
                  : 'text-slate-700 hover:bg-slate-100'
              "
              @click="
                filters.destination = dest;
                updateFilters();
                activePopover = null;
              "
            >
              <span>📍 {{ dest }}</span>
              <span v-if="filters.destination === dest">✓</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 🛡️ 퀵버튼 2: 안전 점수 설정 (목적지 바로 옆 2순위 배치, 고정 너비 min-w-[102px]) -->
      <div class="relative">
        <button
          type="button"
          class="flex items-center justify-between gap-1.5 px-3 py-2 rounded-xl text-xs font-bold transition-all border min-w-[102px]"
          :class="[
            filters.minSafetyScore > 0
              ? 'bg-amber-50 text-amber-700 border-amber-300 font-extrabold'
              : 'bg-slate-100 hover:bg-slate-200/80 text-slate-700 border-slate-200',
          ]"
          @click="togglePopover('safety')"
        >
          <span class="flex items-center gap-1">
            <span>🛡️</span>
            <span class="whitespace-nowrap">{{
              filters.minSafetyScore > 0 ? `안전 ${filters.minSafetyScore}점+` : '안전점수'
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
      <div class="relative">
        <button
          type="button"
          class="flex items-center justify-between gap-1.5 px-3 py-2 rounded-xl text-xs font-bold transition-all border min-w-[125px]"
          :class="[
            filters.maxDeposit < 5000 || filters.maxRent < 100 || filters.tradeType === 'JEONSE'
              ? 'bg-blue-50 text-blue-600 border-blue-300 font-extrabold'
              : 'bg-slate-100 hover:bg-slate-200/80 text-slate-700 border-slate-200',
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
              @input="updateFilters"
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
              @input="updateFilters"
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
      <div class="relative">
        <button
          type="button"
          class="flex items-center justify-between gap-1.5 px-3 py-2 rounded-xl text-xs font-bold transition-all border shadow-sm min-w-[155px]"
          :class="[
            filters.transportMode === 'WALK'
              ? 'bg-emerald-500 text-white border-emerald-500 font-black'
              : 'bg-blue-600 text-white border-blue-600 font-black',
          ]"
          @click="togglePopover('travel')"
        >
          <span class="whitespace-nowrap"
            >{{ filters.transportMode === 'WALK' ? '🚶‍♂️ 도보' : '🚌 대중교통' }}
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
                updateFilters();
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
                updateFilters();
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
        class="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-bold transition-all border"
        :class="[
          filters.showIsochrone
            ? 'bg-indigo-50 text-indigo-700 border-indigo-300 font-black'
            : 'bg-slate-100 text-slate-400 border-slate-200 line-through',
        ]"
        @click="toggleIsochrone"
      >
        <span>⭕ 영역 {{ filters.showIsochrone ? 'ON' : 'OFF' }}</span>
      </button>

      <!-- 🔄 퀵버튼 6: 초기화 -->
      <button
        type="button"
        class="p-2 rounded-xl text-xs font-bold text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-all border border-transparent hover:border-slate-200"
        title="필터 초기화"
        @click="handleReset"
      >
        <span>🔄</span>
      </button>
    </div>

    <!-- ======================================================== -->
    <!-- 2. 모바일 전용 퀵 플로팅 버튼 2종 구분 (md:hidden)       -->
    <!-- ======================================================== -->
    <div class="flex md:hidden items-center gap-1.5 z-30">
      <!-- 모바일 버튼 1: [🎨 조건 필터] -->
      <button
        type="button"
        class="flex items-center gap-1.5 px-3.5 py-2.5 bg-slate-900 text-white rounded-full shadow-xl text-xs font-black transition-transform active:scale-95 border border-slate-700"
        @click="openMobileFilter('general')"
      >
        <span>🎨 전체 필터</span>
      </button>

      <!-- 모바일 버튼 2: [🛍️ 편의시설 (N)] -->
      <button
        type="button"
        class="flex items-center gap-1.5 px-3.5 py-2.5 bg-white text-slate-900 rounded-full shadow-xl text-xs font-bold transition-transform active:scale-95 border border-slate-200"
        :class="{
          'border-blue-500 font-black text-blue-600 bg-blue-50':
            filters.selectedAmenities?.length > 0,
        }"
        @click="openMobileFilter('amenity')"
      >
        <span>🛍️ 편의시설</span>
        <span>
          {{ activeFilterCount }}
        </span>
      </button>

      <!-- 모바일 이소크론 영역 ON/OFF 빠른 버튼 -->
      <button
        type="button"
        class="flex items-center gap-1.5 px-3 py-2.5 rounded-full shadow-lg text-xs font-bold transition-all backdrop-blur-md border"
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
    <MobileFilterBottomSheet
      v-model:is-open="isMobileModalOpen"
      v-model="filters"
      :initial-tab="activeMobileTab"
      :total-count="totalCount"
      :safety-thumb-color="safetyThumbColor"
      @reset="handleReset"
      @update-filters="updateFilters"
    />
  </div>
</template>

<style scoped>
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
