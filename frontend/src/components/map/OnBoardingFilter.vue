<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import onboardingApi from '@/api/onboardingApi';

const props = defineProps({
  onboarding: {
    type: Object,
    default: null,
  },
});
const emit = defineEmits(['select-destination']);

const minSafetyScore = ref(40);
const depositOptions = [
  100, 200, 300, 400, 500, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000,
];
const depositIndex = ref(depositOptions.length - 1);
const maxRent = ref(120);
const transportMode = ref('walk');
const travelTime = ref(15);
const flexTime = ref(10);
const selectedDestination = ref(null);
const searchKeyword = ref('');
const searchResults = ref([]);
const isSearching = ref(false);
const searchError = ref('');
let searchTimer;

const rangeStyle = (value, min, max, color = '#3d55f6') => {
  const percent = ((value - min) / (max - min)) * 100;

  return {
    background: `linear-gradient(to right, ${color} 0%, ${color} ${percent}%, #e5e7eb ${percent}%, #e5e7eb 100%)`,
  };
};

const maxDeposit = computed(() => depositOptions[depositIndex.value]);
const depositLabel = computed(() =>
  maxDeposit.value === 10000 ? '1억원' : `${maxDeposit.value.toLocaleString()}만원`,
);
const rentLabel = computed(() => `${maxRent.value}만원`);
const safetyLabel = computed(() => `${minSafetyScore.value}점`);
const destinationName = computed(
  () =>
    selectedDestination.value?.destName ?? props.onboarding?.destination?.destName ?? '세종대학교',
);

const selectDestination = (destination) => {
  selectedDestination.value = destination;
  searchKeyword.value = destination.destName;
  searchResults.value = [];
  searchError.value = '';
  emit('select-destination', destination);
};

const clearSearch = () => {
  searchKeyword.value = '';
  searchResults.value = [];
  searchError.value = '';
};

watch(searchKeyword, (value) => {
  clearTimeout(searchTimer);
  searchError.value = '';

  const keyword = value.trim();
  if (keyword.length < 2 || selectedDestination.value?.destName === value) {
    searchResults.value = [];
    isSearching.value = false;
    return;
  }

  searchTimer = setTimeout(async () => {
    isSearching.value = true;
    try {
      searchResults.value = await onboardingApi.searchDestinations(keyword);
    } catch (error) {
      searchResults.value = [];
      searchError.value = '목적지를 불러오지 못했어요. 잠시 후 다시 검색해 주세요.';
      console.error('FILTER DESTINATION SEARCH ERROR: ', error);
    } finally {
      isSearching.value = false;
    }
  }, 300);
});

watch(
  () => props.onboarding,
  (onboarding) => {
    if (!onboarding) return;

    const savedDepositIndex = depositOptions.indexOf(Number(onboarding.budgetDeposit));
    if (savedDepositIndex >= 0) depositIndex.value = savedDepositIndex;

    maxRent.value = Number(onboarding.budgetRent);
    minSafetyScore.value = Number(onboarding.minSafetyScore);
    transportMode.value = onboarding.transportMode === 'TRANSIT' ? 'transit' : 'walk';
    travelTime.value = Number(onboarding.maxTravelTime);
  },
  { immediate: true },
);

onBeforeUnmount(() => clearTimeout(searchTimer));
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
          v-model="searchKeyword"
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
          <button type="button" @click="selectDestination(item)">
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

    <div class="filter-section">
      <div class="section-heading">
        <p>
          보증금 <strong>{{ depositLabel }}</strong> 이하
        </p>
      </div>
      <input
        v-model.number="depositIndex"
        type="range"
        min="0"
        :max="depositOptions.length - 1"
        step="1"
        :style="rangeStyle(depositIndex, 0, depositOptions.length - 1)"
      />
      <div class="range-labels"><span>100만원</span><span>1억원</span></div>
    </div>

    <div class="filter-section">
      <div class="section-heading">
        <p>
          월세 <strong>{{ rentLabel }}</strong> 이하
        </p>
      </div>
      <input
        v-model.number="maxRent"
        type="range"
        min="0"
        max="150"
        step="5"
        :style="rangeStyle(maxRent, 0, 150)"
      />
      <div class="range-labels"><span>전세</span><span>150만원</span></div>
    </div>

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
  overflow: hidden;
  border: 1px solid #e1e5eb;
  border-radius: 10px;
  list-style: none;
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
