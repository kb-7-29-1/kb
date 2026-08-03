<script setup>
import { ref, computed, watch } from 'vue';
import AmenityFilter from './AmenityFilter.vue';
import FilterTabs from './FilterTabs.vue';
import OnboardingSummary from './OnboardingSummary.vue';

const props = defineProps({
  isOpen: {
    type: Boolean,
    default: false,
  },
  modelValue: {
    type: Object,
    required: true,
  },
  initialTab: {
    type: String,
    default: 'all', // 'all' | 'amenity'
  },
  totalCount: {
    type: Number,
    default: 0,
  },
  safetyThumbColor: {
    type: String,
    default: '#f59e0b',
  },
});

const emit = defineEmits(['update:isOpen', 'update:modelValue', 'reset', 'update-filters']);

// 현재 바텀시트 내부 활성 탭 ('all' = 전체 조건 / 'amenity' = 편의시설 필터)
const activeTab = ref(props.initialTab || 'all');

watch(
  () => props.initialTab,
  (newTab) => {
    if (newTab) activeTab.value = newTab === 'general' ? 'all' : newTab;
  },
);

// 로컬 양방향 바인딩 래퍼
const filters = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const close = () => {
  emit('update:isOpen', false);
};

const handleReset = () => {
  emit('reset');
};

const notifyUpdate = () => {
  emit('update-filters');
};
</script>

<template>
  <Teleport to="body">
    <div v-if="isOpen" class="fixed inset-0 z-50 flex items-end justify-center md:hidden">
      <!-- Dimmed 배경 오버레이 -->
      <div
        class="fixed inset-0 bg-slate-950/60 backdrop-blur-sm transition-opacity"
        @click="close"
      ></div>

      <!-- 슬라이드 업 바텀시트 -->
      <div
        class="relative w-full max-h-[85vh] bg-white rounded-t-3xl shadow-2xl flex flex-col z-10 overflow-hidden animate-slide-up"
      >
        <!-- 1. 바텀시트 상단 드래그 핸들 -->
        <div class="h-5 bg-white relative">
          <span
            class="w-28 h-1 bg-slate-200 rounded-full mx-auto absolute top-5 left-1/2 -translate-x-1/2"
          ></span>
        </div>

        <div class="px-4 pt-3 pb-3 border-b border-slate-100 space-y-3">
          <OnboardingSummary
            :destination="filters.destination"
            :transport-mode="filters.transportMode"
            :travel-time="filters.travelTime"
            :max-deposit="filters.maxDeposit"
            :max-rent="filters.maxRent"
            :min-safety-score="filters.minSafetyScore"
            @close="close"
          />

          <!-- 2. FilterPanel 계승 FilterTabs (전체 필터 ↔ 편의시설 필터) -->
          <FilterTabs :active-tab="activeTab" @change="activeTab = $event" />
        </div>

        <!-- 3. 탭별 스크롤 내용 영역 -->
        <div class="flex-1 overflow-y-auto p-5">
          <!-- 🎨 전체 조건 필터 (목적지 / 이동시간 / 금액 / 안전점수) -->
          <div v-show="activeTab === 'all' || activeTab === 'general'" class="space-y-6">
            <!-- 섹션 1: 목적지 선택 -->
            <div class="space-y-2">
              <label class="text-xs font-black text-slate-800 flex items-center gap-1.5">
                <span>📍</span>
                <span>주 목적지</span>
              </label>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="dest in ['세종대학교', '건국대학교', '강남역', '역삼역', '성수역']"
                  :key="dest"
                  type="button"
                  class="px-3 py-2 rounded-xl text-xs font-bold transition-all border"
                  :class="
                    filters.destination === dest
                      ? 'bg-blue-600 text-white border-blue-600 font-black'
                      : 'bg-slate-50 text-slate-700 border-slate-200'
                  "
                  @click="
                    filters.destination = dest;
                    notifyUpdate();
                  "
                >
                  {{ dest }}
                </button>
              </div>
            </div>

            <!-- 섹션 2: 최저 안전점수 게이지 슬라이더 (주 목적지 바로 아래 2순위 배치) -->
            <div class="space-y-3">
              <div class="flex items-center justify-between">
                <label class="text-xs font-black text-slate-800 flex items-center gap-1.5">
                  <span>🛡️</span>
                  <span>최저 안심 귀갓길 점수</span>
                </label>
                <span
                  class="text-xs font-black px-2.5 py-1 rounded-full bg-amber-50 text-amber-600 border border-amber-200"
                >
                  {{
                    filters.minSafetyScore === 0 ? '전체 보기' : `${filters.minSafetyScore}점 이상`
                  }}
                </span>
              </div>

              <div class="space-y-2">
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
                  @input="notifyUpdate"
                />
                <div class="flex justify-between text-[11px] font-bold text-slate-400">
                  <span>0점</span>
                  <span>50점</span>
                  <span>100점</span>
                </div>
              </div>

              <!-- 1-클릭 프리셋 칩 버튼 (70점, 80점, 90점, 95점 4종) -->
              <div class="grid grid-cols-4 gap-1.5 pt-1">
                <button
                  v-for="score in [70, 80, 90, 95]"
                  :key="score"
                  type="button"
                  class="py-2 rounded-xl text-xs font-bold transition-all border text-center"
                  :class="[
                    filters.minSafetyScore === score
                      ? 'bg-amber-500 text-white border-amber-500 font-black shadow-sm'
                      : 'bg-slate-50 border-slate-200 text-slate-700',
                  ]"
                  @click="
                    filters.minSafetyScore = score;
                    notifyUpdate();
                  "
                >
                  {{ score }}점+
                </button>
              </div>
            </div>

            <!-- 섹션 3: 이동 시간 & 수단 모드 -->
            <div class="space-y-3">
              <div class="text-xs font-black text-slate-900">
                시간
                <span class="text-blue-600"
                  >{{ filters.transportMode === 'WALK' ? '도보' : '대중교통' }}
                  {{ filters.travelTime }}분 이내</span
                >
              </div>

              <!-- 탭 선택 -->
              <div class="flex border-b border-slate-200">
                <button
                  type="button"
                  class="flex-1 py-2 text-xs font-bold transition-all relative flex items-center justify-center gap-1.5 pb-2"
                  :class="
                    filters.transportMode === 'WALK'
                      ? 'text-blue-600 font-extrabold'
                      : 'text-slate-500'
                  "
                  @click="
                    filters.transportMode = 'WALK';
                    if (filters.travelTime < 5) filters.travelTime = 5;
                    notifyUpdate();
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
                  class="flex-1 py-2 text-xs font-bold transition-all relative flex items-center justify-center gap-1.5 pb-2"
                  :class="
                    filters.transportMode === 'TRANSIT'
                      ? 'text-blue-600 font-extrabold'
                      : 'text-slate-500'
                  "
                  @click="
                    filters.transportMode = 'TRANSIT';
                    if (filters.travelTime < 10) filters.travelTime = 10;
                    notifyUpdate();
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

              <!-- 슬라이더 1: 원하는 이동 시간 -->
              <div class="space-y-1.5 pt-1">
                <div class="flex justify-between text-xs font-bold text-slate-800">
                  <span>🎯 원하는 이동 시간</span>
                  <span class="text-blue-600 font-extrabold">{{ filters.travelTime }}분</span>
                </div>
                <input
                  type="range"
                  v-model.number="filters.travelTime"
                  :min="filters.transportMode === 'WALK' ? 5 : 10"
                  max="60"
                  step="5"
                  class="w-full accent-blue-600 cursor-pointer"
                  @input="notifyUpdate"
                />
                <div class="flex justify-between text-[11px] font-bold text-slate-400">
                  <span>{{ filters.transportMode === 'WALK' ? '5분' : '10분' }}</span>
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
                        : 'text-slate-500'
                    "
                    @click="
                      filters.walkPace = pace.key;
                      notifyUpdate();
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
              <div v-else class="space-y-1.5">
                <div class="flex justify-between text-xs font-bold text-slate-800">
                  <span>⏳ 앞뒤 여유 시간</span>
                  <span class="text-amber-500 font-extrabold">±{{ filters.flexTime }}분</span>
                </div>
                <input
                  type="range"
                  v-model.number="filters.flexTime"
                  min="5"
                  max="20"
                  step="5"
                  class="w-full accent-amber-500 cursor-pointer"
                  @input="notifyUpdate"
                />
                <div class="flex justify-between text-[11px] font-bold text-slate-400">
                  <span>±5분</span>
                  <span>±20분</span>
                </div>
              </div>
            </div>

            <!-- 섹션 4: 거래 유형 및 조건 (월세 vs 전세 2종) -->
            <div class="space-y-3">
              <label class="text-xs font-black text-slate-800 flex items-center gap-1.5">
                <span>💰</span>
                <span>보증금 및 월세 제한</span>
              </label>
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
                      ? 'bg-white text-blue-600 shadow-sm font-black'
                      : 'text-slate-500'
                  "
                  @click="
                    filters.tradeType = t.key;
                    notifyUpdate();
                  "
                >
                  {{ t.label }}
                </button>
              </div>

              <!-- 보증금 슬라이더 -->
              <div class="space-y-1">
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
                  @input="notifyUpdate"
                />
              </div>

              <!-- 월세 슬라이더 -->
              <div v-if="filters.tradeType === 'MONTHLY'" class="space-y-1">
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
                  @input="notifyUpdate"
                />
              </div>
            </div>
          </div>

          <!-- 탭 2: 🛍️ 주변 편의시설 필터 (FilterPanel 계승) -->
          <div v-show="activeTab === 'amenity'" class="space-y-4 pt-2">
            <div
              class="p-3 bg-blue-50/80 rounded-2xl border border-blue-200/80 text-xs text-blue-900 font-bold flex items-center gap-2"
            >
              <span>🛍️</span>
              <span>7종 편의시설 도보 제한 시간을 개별 조절하여 매물을 추천받으세요.</span>
            </div>
            <AmenityFilter
              @apply="
                (selected) => {
                  filters.selectedAmenities = selected;
                  notifyUpdate();
                }
              "
            />
          </div>
        </div>

        <!-- 바텀시트 하단 스티키 고정 하단바 -->
        <div class="p-4 border-t border-slate-100 bg-white flex items-center gap-3">
          <button
            type="button"
            class="px-4 py-3.5 rounded-2xl bg-slate-100 text-slate-600 text-xs font-bold hover:bg-slate-200 transition-all flex items-center gap-1 shrink-0"
            @click="handleReset"
          >
            <span>🔄</span>
            <span>초기화</span>
          </button>
          <button
            type="button"
            class="flex-1 py-3.5 rounded-2xl bg-slate-900 text-white text-sm font-black shadow-lg hover:bg-slate-800 transition-all text-center"
            @click="close"
          >
            필터 적용하기 (총 {{ totalCount }}개 매물)
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
@keyframes slideUp {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

.animate-slide-up {
  animation: slideUp 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

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
