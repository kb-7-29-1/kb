<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import onboardingApi from '@/api/onboardingApi';
import {
  DEPOSIT_MAX_LABEL,
  DEPOSIT_MIN_LABEL,
  DEPOSIT_OPTIONS,
  RENT_MAX,
  RENT_MAX_LABEL,
  RENT_MIN,
  RENT_MIN_LABEL,
  RENT_STEP,
  formatDepositAmount,
  LOAN_PRODUCTS,
} from '@/utils/budget';

const props = defineProps({
  onboarding: {
    type: Object,
    default: null,
  },
  appliedFilters: {
    type: Object,
    default: null,
  },
});

const minSafetyScore = ref(40);
const leaseType = ref('monthly');
const depositOptions = DEPOSIT_OPTIONS;
const depositMinIndex = ref(0);
const depositMaxIndex = ref(depositOptions.length - 1);
const minRentStart = ref(RENT_MIN);
const maxRent = ref(120);
const selectedLoanId = ref('NONE');
const transportMode = ref('walk');
const travelTime = ref(15);
const flexTime = ref(10);
const selectedDestination = ref(null);
const searchKeyword = ref('');
const destinationSearchInput = ref(null);
const searchResults = ref([]);
const isSearching = ref(false);
const searchError = ref('');
const isComposing = ref(false);
const isSelectingDestination = ref(false);
let searchTimer;
let searchRequestId = 0;
let selectionReleaseTimer;
let compositionEndTimer;

const beginDestinationSelection = () => {
  clearTimeout(selectionReleaseTimer);
  isSelectingDestination.value = true;
};

const finishDestinationSelection = () => {
  clearTimeout(selectionReleaseTimer);
  selectionReleaseTimer = setTimeout(() => {
    isSelectingDestination.value = false;
  }, 180);
};

const cancelPendingSearch = () => {
  clearTimeout(searchTimer);
  searchRequestId += 1;
  isSearching.value = false;
};

const handleCompositionStart = () => {
  isComposing.value = true;
};

const handleCompositionEnd = (event) => {
  isComposing.value = false;
  const completedKeyword = event.target.value;

  clearTimeout(compositionEndTimer);
  compositionEndTimer = setTimeout(() => {
    if (isSelectingDestination.value) return;
    searchKeyword.value = completedKeyword;
    scheduleSearch(completedKeyword);
  }, 40);
};

const handleSearchInput = (event) => {
  if (isSelectingDestination.value) return;
  searchKeyword.value = event.target.value;
  scheduleSearch(event.target.value);
};

const rangeStyle = (value, min, max, color = '#3d55f6') => {
  const percent = ((value - min) / (max - min)) * 100;

  return {
    background: `linear-gradient(to right, ${color} 0%, ${color} ${percent}%, #e5e7eb ${percent}%, #e5e7eb 100%)`,
  };
};

const dualRangeStyle = (minVal, maxVal, min, max, color = '#3d55f6') => {
  const minPercent = ((minVal - min) / (max - min)) * 100;
  const maxPercent = ((maxVal - min) / (max - min)) * 100;
  return {
    background: `linear-gradient(to right, #e5e7eb 0%, #e5e7eb ${minPercent}%, ${color} ${minPercent}%, ${color} ${maxPercent}%, #e5e7eb ${maxPercent}%, #e5e7eb 100%)`,
  };
};

const minDeposit = computed(() => depositOptions[depositMinIndex.value]);
const maxDeposit = computed(() => depositOptions[depositMaxIndex.value]);
const depositRangeLabel = computed(
  () => `${formatDepositAmount(minDeposit.value)} ~ ${formatDepositAmount(maxDeposit.value)}`,
);
const rentRangeLabel = computed(() =>
  leaseType.value === 'jeonse' ? '해당 없음' : `${minRentStart.value}만원 ~ ${maxRent.value}만원`,
);
const safetyLabel = computed(() => `${minSafetyScore.value}점`);
const destinationName = computed(
  () =>
    selectedDestination.value?.destName ?? props.onboarding?.destination?.destName ?? '세종대학교',
);

const selectDestination = (destination) => {
  beginDestinationSelection();
  cancelPendingSearch();
  selectedDestination.value = destination;
  searchKeyword.value = destination.destName;
  searchResults.value = [];
  searchError.value = '';
  finishDestinationSelection();
};

const clearSearch = () => {
  cancelPendingSearch();
  searchKeyword.value = '';
  searchResults.value = [];
  searchError.value = '';

  nextTick(() => destinationSearchInput.value?.focus());
};

const scheduleSearch = (value) => {
  clearTimeout(searchTimer);
  searchError.value = '';
  const requestId = ++searchRequestId;

  const keyword = value.trim();
  if (keyword.length < 2 || selectedDestination.value?.destName === value) {
    searchResults.value = [];
    isSearching.value = false;
    return;
  }

  isSearching.value = true;
  searchTimer = setTimeout(async () => {
    try {
      const results = await onboardingApi.searchDestinations(keyword);
      if (requestId !== searchRequestId) return;
      searchResults.value = results;
    } catch (error) {
      if (requestId !== searchRequestId) return;
      searchResults.value = [];
      searchError.value = '목적지를 불러오지 못했어요. 잠시 후 다시 검색해 주세요.';
      console.error('FILTER DESTINATION SEARCH ERROR: ', error);
    } finally {
      if (requestId === searchRequestId) isSearching.value = false;
    }
  }, 300);
};

const selectLeaseType = (type) => {
  leaseType.value = type;
  if (type === 'jeonse') {
    minRentStart.value = 0;
    maxRent.value = 0;
  }
};

const onDepositMinInput = () => {
  if (depositMinIndex.value > depositMaxIndex.value) {
    depositMinIndex.value = depositMaxIndex.value;
  }
};
const onDepositMaxInput = () => {
  if (depositMaxIndex.value < depositMinIndex.value) {
    depositMaxIndex.value = depositMinIndex.value;
  }
};
const onRentMinInput = () => {
  if (minRentStart.value > maxRent.value) {
    minRentStart.value = maxRent.value;
  }
};
const onRentMaxInput = () => {
  if (maxRent.value < minRentStart.value) {
    maxRent.value = minRentStart.value;
  }
};

watch(
  [() => props.onboarding, () => props.appliedFilters],
  ([onboarding, appliedFilters]) => {
    const filters = appliedFilters ?? onboarding;
    if (!filters) return;

    selectedDestination.value = appliedFilters?.destination ?? null;

    const savedMinIndex = depositOptions.indexOf(Number(filters.budgetDepositMin));
    if (savedMinIndex >= 0) depositMinIndex.value = savedMinIndex;

    const savedMaxIndex = depositOptions.indexOf(Number(filters.budgetDeposit));
    if (savedMaxIndex >= 0) depositMaxIndex.value = savedMaxIndex;

    minRentStart.value = Number(filters.budgetRentMin ?? RENT_MIN);
    maxRent.value = Number(filters.budgetRent);
    leaseType.value = Number(filters.budgetRent) === 0 ? 'jeonse' : 'monthly';
    minSafetyScore.value = Number(filters.minSafetyScore);
    transportMode.value = filters.transportMode === 'TRANSIT' ? 'transit' : 'walk';
    travelTime.value = Number(filters.maxTravelTime);
  },
  { immediate: true, deep: true },
);

const resetFilters = () => {
  const onboarding = props.onboarding;

  selectedDestination.value = null;
  searchKeyword.value = '';
  searchResults.value = [];
  searchError.value = '';

  if (!onboarding) return;

  const savedMinIndex = depositOptions.indexOf(Number(onboarding.budgetDepositMin));
  if (savedMinIndex >= 0) depositMinIndex.value = savedMinIndex;
  else depositMinIndex.value = 0;

  const savedMaxIndex = depositOptions.indexOf(Number(onboarding.budgetDeposit));
  if (savedMaxIndex >= 0) depositMaxIndex.value = savedMaxIndex;

  minRentStart.value = Number(onboarding.budgetRentMin ?? RENT_MIN);
  maxRent.value = Number(onboarding.budgetRent);
  leaseType.value = Number(onboarding.budgetRent) === 0 ? 'jeonse' : 'monthly';
  minSafetyScore.value = Number(onboarding.minSafetyScore);
  transportMode.value = onboarding.transportMode === 'TRANSIT' ? 'transit' : 'walk';
  travelTime.value = Number(onboarding.maxTravelTime);
  if (onboarding.selectedLoanId) selectedLoanId.value = onboarding.selectedLoanId;
};

const getFilters = () => ({
  destination: selectedDestination.value ?? props.onboarding?.destination ?? null,
  selectedDestination: selectedDestination.value,
  transportMode: transportMode.value.toUpperCase(),
  maxTravelTime: travelTime.value,
  leaseType: leaseType.value,
  budgetDepositMin: minDeposit.value,
  budgetDeposit: maxDeposit.value,
  budgetRentMin: leaseType.value === 'jeonse' ? 0 : minRentStart.value,
  budgetRent: leaseType.value === 'jeonse' ? 0 : maxRent.value,
  minSafetyScore: minSafetyScore.value,
  selectedLoanId: selectedLoanId.value,
});

defineExpose({ getFilters, resetFilters });

onBeforeUnmount(() => {
  clearTimeout(searchTimer);
  clearTimeout(selectionReleaseTimer);
  clearTimeout(compositionEndTimer);
});
</script>

<template>
  <section class="onboarding-filter" aria-label="전체 조건 필터">
    <div class="filter-section destination-section">
      <p class="section-label">
        목적지 <strong>{{ destinationName }}</strong>
      </p>
      <label class="search-field">
        <i class="fa-solid fa-magnifying-glass" aria-hidden="true"></i>
        <input
          ref="destinationSearchInput"
          v-model="searchKeyword"
          @input="handleSearchInput"
          @compositionstart="handleCompositionStart"
          @compositionend="handleCompositionEnd"
          type="text"
          autocomplete="off"
          placeholder="예: 연세대학교, 삼성전자 서초사옥"
        />
        <button
          v-if="searchKeyword"
          type="button"
          class="clear-search-button"
          aria-label="검색어 지우기"
          @click="clearSearch"
        >
          <i class="fa-solid fa-xmark" aria-hidden="true"></i>
        </button>
      </label>
      <p v-if="isSearching" class="search-message">검색 중이에요.</p>
      <p v-else-if="searchError" class="search-message error">{{ searchError }}</p>
      <p
        v-else-if="
          searchKeyword.trim().length >= 2 &&
          !searchResults.length &&
          selectedDestination?.destName !== searchKeyword
        "
        class="search-message"
      >
        검색 결과가 없어요.
      </p>
      <ul v-if="searchResults.length" class="search-result-list">
        <li v-for="item in searchResults" :key="`${item.destName}-${item.destAddress}`">
          <button type="button" @pointerdown.capture.prevent="selectDestination(item)">
            <i class="fa-solid fa-location-dot" aria-hidden="true"></i>
            <span>
              <strong>{{ item.destName }}</strong>
              <small>{{ item.destAddress }}</small>
            </span>
            <i class="fa-solid fa-chevron-right" aria-hidden="true"></i>
          </button>
        </li>
      </ul>
    </div>

    <!-- 🛡️ 최소 안전 점수 섹션 (목적지 검색 바로 아래 배치) -->
    <div class="filter-section">
      <div class="section-heading">
        <p>
          최소 안전 점수 <strong>{{ safetyLabel }}</strong> 이상
        </p>
      </div>
      <input
        v-model.number="minSafetyScore"
        type="range"
        min="0"
        max="90"
        step="10"
        :style="rangeStyle(minSafetyScore, 0, 90)"
      />
      <div class="range-labels"><span>0점</span><span>90점</span></div>
    </div>

    <div class="filter-section budget-section">
      <!-- 🏠 거래유형(월세/전세) 토글 -->
      <div class="section-heading">
        <p>거래유형</p>
      </div>
      <div class="transport-options">
        <button
          type="button"
          :class="{ active: leaseType === 'monthly' }"
          @click="selectLeaseType('monthly')"
        >
          <span>🏠</span> 월세
        </button>
        <button
          type="button"
          :class="{ active: leaseType === 'jeonse' }"
          @click="selectLeaseType('jeonse')"
        >
          <span>🏢</span> 전세
        </button>
      </div>

      <!-- 보증금 -->
      <div class="section-heading sub-heading">
        <p>
          보증금 <strong>{{ depositRangeLabel }}</strong>
        </p>
      </div>
      <div class="dual-range-track">
        <div
          class="dual-range-track__fill"
          :style="dualRangeStyle(depositMinIndex, depositMaxIndex, 0, depositOptions.length - 1)"
        ></div>
        <input
          v-model.number="depositMinIndex"
          type="range"
          min="0"
          :max="depositOptions.length - 1"
          step="1"
          class="range-min"
          @input="onDepositMinInput"
        />
        <input
          v-model.number="depositMaxIndex"
          type="range"
          min="0"
          :max="depositOptions.length - 1"
          step="1"
          class="range-max"
          @input="onDepositMaxInput"
        />
      </div>
      <div class="range-labels">
        <span>{{ DEPOSIT_MIN_LABEL }}</span
        ><span>{{ DEPOSIT_MAX_LABEL }}</span>
      </div>

      <!-- 월세 (월세 선택 시에만 노출) -->
      <div v-if="leaseType === 'monthly'" class="sub-block">
        <div class="section-heading sub-heading">
          <p>
            월세 <strong>{{ rentRangeLabel }}</strong>
          </p>
        </div>
        <div class="dual-range-track">
          <div
            class="dual-range-track__fill"
            :style="dualRangeStyle(minRentStart, maxRent, RENT_MIN, RENT_MAX)"
          ></div>
          <input
            v-model.number="minRentStart"
            type="range"
            :min="RENT_MIN"
            :max="RENT_MAX"
            :step="RENT_STEP"
            class="range-min"
            @input="onRentMinInput"
          />
          <input
            v-model.number="maxRent"
            type="range"
            :min="RENT_MIN"
            :max="RENT_MAX"
            :step="RENT_STEP"
            class="range-max"
            @input="onRentMaxInput"
          />
        </div>
        <div class="range-labels">
          <span>{{ RENT_MIN_LABEL }}</span
          ><span>{{ RENT_MAX_LABEL }}</span>
        </div>
      </div>
    </div>

    <div class="filter-section travel-section">
      <div class="section-heading">
        <p>
          시간
          <strong
            >{{ transportMode === 'walk' ? '도보' : '대중교통' }} {{ travelTime }}분 이내</strong
          >
        </p>
      </div>

      <div class="transport-options">
        <button
          type="button"
          :class="{ active: transportMode === 'walk' }"
          @click="
            transportMode = 'walk';
            if (travelTime > 40) travelTime = 40;
          "
        >
          <span>🚶</span> 걸어서 이동
        </button>
        <button
          type="button"
          :class="{ active: transportMode === 'transit' }"
          @click="
            transportMode = 'transit';
            if (travelTime < 15) travelTime = 15;
            if (flexTime > travelTime) flexTime = travelTime;
          "
        >
          <span>🚌</span> 대중교통 이용
        </button>
      </div>

      <div class="travel-control">
        <div class="section-heading compact">
          <p v-if="transportMode === 'walk'">
            🚶 걸어갈 최대 시간 <strong>{{ travelTime }}분 이내</strong>
          </p>
          <p v-else>
            🎯 원하는 이동 시간 <strong>{{ travelTime }}분 이내</strong>
          </p>
        </div>
        <input
          v-model.number="travelTime"
          type="range"
          :min="transportMode === 'walk' ? 5 : 15"
          :max="transportMode === 'walk' ? 40 : 60"
          step="5"
          :style="
            rangeStyle(
              travelTime,
              transportMode === 'walk' ? 5 : 15,
              transportMode === 'walk' ? 40 : 60,
            )
          "
          @input="if (transportMode === 'transit' && flexTime > travelTime) flexTime = travelTime;"
        />
        <div class="range-labels">
          <span>{{ transportMode === 'walk' ? '5분' : '15분' }}</span
          ><span>{{ transportMode === 'walk' ? '40분' : '60분' }}</span>
        </div>
      </div>

      <div v-if="transportMode === 'transit'" class="flex-time-control">
        <div class="section-heading compact">
          <p>
            ⏳ 최소 이동 시간 <strong>{{ flexTime }}분 이상</strong>
          </p>
        </div>
        <input
          v-model.number="flexTime"
          type="range"
          min="5"
          :max="Math.min(30, travelTime)"
          step="5"
          class="flex-time-range"
          :style="rangeStyle(flexTime, 5, Math.min(30, travelTime), '#f59e0b')"
        />
        <div class="range-labels">
          <span>5분</span><span>{{ Math.min(30, travelTime) }}분</span>
        </div>
        <p class="travel-guide">
          <i class="fa-solid fa-circle-info" aria-hidden="true"></i>
          {{ flexTime }}분~{{ travelTime }}분 이내 매물을 조회해요
        </p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.onboarding-filter {
  padding: 10px 0 30px;
}
.filter-section {
  padding: 20px 0;
  border-bottom: 1px solid #edf0f5;
}
.filter-section:first-child {
  padding-top: 10px;
}
.filter-section:last-child {
  border-bottom: 0;
}

.section-label,
.section-heading p {
  margin: 0;
  color: #374151;
  font-size: 13px;
  font-weight: 700;
}
.section-label strong,
.section-heading strong {
  color: #3d55f6;
  font-weight: 800;
}
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.section-heading.compact {
  margin: 0 0 10px;
}

.search-field {
  display: flex;
  align-items: center;
  gap: 9px;
  height: 42px;
  margin-top: 10px;
  padding: 0 12px;
  border: 1px solid #e1e5eb;
  border-radius: 10px;
  color: #a1a8b5;
}
.search-field:focus-within {
  border-color: #3d55f6;
  box-shadow: 0 0 0 3px #eef1ff;
}
.search-field input {
  width: 100%;
  border: 0;
  outline: 0;
  color: #374151;
  font: inherit;
  font-size: 12px;
}
.search-field input::placeholder {
  color: #b5bcc7;
}
.clear-search-button {
  display: grid;
  padding: 0;
  place-items: center;
  border: 0;
  background: transparent;
  color: #a1a8b5;
  font-size: 10px;
}
.search-message {
  margin: 8px 2px 0;
  color: #7b8797;
  font-size: 12px;
}
.search-message.error {
  color: #dc2626;
}
.search-result-list {
  margin: 8px 0 0;
  padding: 0;
  max-height: 220px;
  overflow-y: auto;
  border: 1px solid #e1e5eb;
  border-radius: 10px;
  list-style: none;
}
.search-result-list::-webkit-scrollbar {
  width: 4px;
}
.search-result-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #cbd5e1;
}
.search-result-list li + li {
  border-top: 1px solid #edf0f5;
}
.search-result-list button {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 10px;
  padding: 11px 12px;
  border: 0;
  background: #fff;
  color: #3d55f6;
  text-align: left;
}
.search-result-list span {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  color: #374151;
  font-size: 12px;
}
.search-result-list small {
  overflow: hidden;
  color: #9aa3b0;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.search-result-list > li > button > :last-child {
  color: #a1a8b5;
  font-size: 11px;
}

input[type='range'] {
  width: 100%;
  height: 6px;
  margin: 0;
  appearance: none;
  border-radius: 999px;
  cursor: pointer;
}
input[type='range']::-webkit-slider-thumb {
  width: 17px;
  height: 17px;
  appearance: none;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #3d55f6;
  box-shadow: 0 1px 4px rgb(61 85 246 / 40%);
}
input[type='range']::-moz-range-thumb {
  width: 14px;
  height: 14px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #3d55f6;
  box-shadow: 0 1px 4px rgb(61 85 246 / 40%);
}
.flex-time-range::-webkit-slider-thumb {
  background: #f59e0b !important;
  box-shadow: 0 1px 4px rgb(245 158 11 / 40%) !important;
}
.flex-time-range::-moz-range-thumb {
  background: #f59e0b !important;
  box-shadow: 0 1px 4px rgb(245 158 11 / 40%) !important;
}
.range-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  color: #a1a8b5;
  font-size: 10px;
}

.dual-range-track {
  position: relative;
  height: 17px;
}

.dual-range-track__fill {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 6px;
  border-radius: 999px;
  transform: translateY(-50%);
  pointer-events: none;
}

.dual-range-track input[type='range'] {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 17px;
  margin: 0;
  background: transparent;
  pointer-events: none;
}

.dual-range-track input[type='range']::-webkit-slider-thumb {
  pointer-events: auto;
}

.dual-range-track input[type='range']::-moz-range-thumb {
  pointer-events: auto;
}

.dual-range-track .range-max {
  z-index: 2;
}

.dual-range-track .range-min {
  z-index: 1;
}

.budget-section .sub-heading {
  margin-top: 20px;
}

.budget-section .sub-heading:first-of-type {
  margin-top: 20px; /* 거래유형 버튼과 보증금 사이 간격 */
}

.sub-block {
  margin-top: 4px;
}

.transport-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.transport-options button,
.transport-options button {
  min-height: 38px;
  border: 1px solid #e1e5eb;
  border-radius: 10px;
  background: #fff;
  color: #697386;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.transport-options button.active,
.transport-options button.active {
  border-color: #3d55f6;
  background: #eef1ff;
  color: #3d55f6;
}

.travel-control {
  margin-top: 20px;
}
.flex-time-control {
  margin-top: 22px;
}
.travel-guide {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 10px 0 0;
  color: #8b95a7;
  font-size: 11px;
  line-height: 1.4;
}
.travel-guide i {
  color: #f59e0b;
}
</style>
