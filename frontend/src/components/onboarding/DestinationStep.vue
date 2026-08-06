<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue';
import onboardingApi from '@/api/onboardingApi';

const emit = defineEmits(['select-destination', 'update:purpose']);
const props = defineProps({
  purpose: {
    type: String,
    default: 'school',
  },
  selectedDestination: {
    type: Object,
    default: null,
  },
});

const selectedPurpose = ref(props.purpose);
const keyword = ref(props.selectedDestination?.destName ?? '');
const destinations = ref([]);
const selectedDestination = ref(props.selectedDestination);
const destinationInput = ref(null);
const isSearching = ref(false);
const searchError = ref('');
const isComposing = ref(false);
const isSelectingDestination = ref(false);

const purposes = [
  { id: 'school', label: '학교', icon: '🎓' },
  { id: 'work', label: '직장', icon: '💼' },
  { id: 'etc', label: '기타', icon: '🏠' },
];

let searchTimer;
let searchRequestId = 0;
let selectionReleaseTimer;

const beginDestinationSelection = () => {
  clearTimeout(selectionReleaseTimer);
  isSelectingDestination.value = true;
};

const finishDestinationSelection = () => {
  clearTimeout(selectionReleaseTimer);
  selectionReleaseTimer = setTimeout(() => {
    isSelectingDestination.value = false;
  }, 100);
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

  if (isSelectingDestination.value) return;
  keyword.value = event.target.value;
  scheduleSearch(keyword.value);
};

const handleSearchInput = (event) => {
  if (isSelectingDestination.value) return;
  keyword.value = event.target.value;
  scheduleSearch(event.target.value);
};

const clearSearch = () => {
  cancelPendingSearch();
  keyword.value = '';
  destinations.value = [];
  selectedDestination.value = null;
  searchError.value = '';
  emit('select-destination', null);

  nextTick(() => destinationInput.value?.focus());
};

const selectDestination = (destination) => {
  beginDestinationSelection();
  cancelPendingSearch();
  selectedDestination.value = destination;
  keyword.value = destination.destName;
  destinations.value = [];
  searchError.value = '';
  emit('select-destination', destination);
  finishDestinationSelection();
};

const scheduleSearch = (value) => {
  clearTimeout(searchTimer);
  searchError.value = '';
  const requestId = ++searchRequestId;

  const searchKeyword = value.trim();
  if (searchKeyword.length < 2 || selectedDestination.value?.destName === value) {
    destinations.value = [];
    isSearching.value = false;
    return;
  }

  selectedDestination.value = null;
  destinations.value = [];
  isSearching.value = true;
  searchTimer = setTimeout(async () => {
    try {
      const results = await onboardingApi.searchDestinations(searchKeyword);
      if (requestId !== searchRequestId) return;
      destinations.value = results;
    } catch (error) {
      if (requestId !== searchRequestId) return;
      destinations.value = [];
      searchError.value = '목적지를 불러오지 못했어요. 잠시 후 다시 검색해 주세요.';
      console.error('DESTINATION SEARCH ERROR: ', error);
    } finally {
      if (requestId === searchRequestId) isSearching.value = false;
    }
  }, 300);
};

watch(selectedPurpose, (value) => emit('update:purpose', value));

onBeforeUnmount(() => {
  clearTimeout(searchTimer);
  clearTimeout(selectionReleaseTimer);
});
</script>

<template>
  <section class="destination-step">
    <div class="destination-card">
      <div class="card-icon" aria-hidden="true">
        <i class="fa-solid fa-location-dot"></i>
      </div>

      <h2>자주 방문하는 장소를 알려주세요</h2>
      <p class="description">목적지를 기준으로 나에게 맞는 매물을 찾아드려요</p>

      <div class="purpose-list" role="group" aria-label="목적지 유형">
        <button
          v-for="purpose in purposes"
          :key="purpose.id"
          type="button"
          class="purpose-button"
          :class="{ active: selectedPurpose === purpose.id }"
          @click="selectedPurpose = purpose.id"
        >
          <span class="purpose-icon" aria-hidden="true">{{ purpose.icon }}</span>
          {{ purpose.label }}
        </button>
      </div>

      <div class="search-field">
        <label class="search-label" for="destination">학교/직장 이름 또는 주소</label>
        <div class="search-box">
          <i class="fa-solid fa-magnifying-glass" aria-hidden="true"></i>
          <input
            id="destination"
            ref="destinationInput"
            v-model="keyword"
            @input="handleSearchInput"
            @compositionstart="handleCompositionStart"
            @compositionend="handleCompositionEnd"
            type="text"
            autocomplete="off"
            placeholder="예: 세종대학교, KB국민은행"
          />
          <button
            v-if="keyword"
            type="button"
            class="clear-button"
            aria-label="검색어 지우기"
            @click="clearSearch"
          >
            <i class="fa-solid fa-xmark" aria-hidden="true"></i>
          </button>
        </div>

        <p v-if="isSearching" class="search-message">검색 중이에요.</p>
        <p v-else-if="searchError" class="search-message error">{{ searchError }}</p>
        <p
          v-else-if="keyword.trim().length >= 2 && !selectedDestination && !destinations.length"
          class="search-message"
        >
          검색 결과가 없어요.
        </p>

        <div class="search-feedback">
          <ul v-if="destinations.length" class="suggestion-list">
            <li v-for="item in destinations" :key="`${item.destName}-${item.destAddress}`">
              <button type="button" @pointerdown.prevent="selectDestination(item)">
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
      </div>

      <p v-if="selectedDestination" class="selected-message">
        <i class="fa-solid fa-circle-check" aria-hidden="true"></i>
        {{ selectedDestination.destName }}을(를) 선택했어요.
      </p>
    </div>
  </section>
</template>

<style scoped>
.destination-step {
  display: flex;
  justify-content: center;
  width: 100%;
}
.destination-card {
  width: 100%;
  box-sizing: border-box;
  padding: 24px 20px;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 4px 20px -2px rgba(15, 23, 42, 0.06);
}
.card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  margin-bottom: 16px;
  border-radius: 12px;
  background: #eff6ff;
  color: #2a60f7;
  font-size: 17px;
}
h2 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.4;
}
.description {
  margin: 8px 0 24px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.5;
}
.purpose-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 24px;
}
.purpose-button {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 80px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
  color: #64748b;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
}
.purpose-icon {
  font-size: 20px;
  line-height: 1;
}
.purpose-button.active {
  border-color: #2a60f7;
  background: #eff6ff;
  color: #2a60f7;
}
.search-label {
  display: block;
  margin-bottom: 8px;
  color: #334155;
  font-size: 14px;
  font-weight: 600;
}
.search-field {
  position: relative;
}
.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 50px;
  box-sizing: border-box;
  padding: 0 14px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  color: #94a3b8;
}
.search-box:focus-within {
  border-color: #2a60f7;
  box-shadow: 0 0 0 3px #eff6ff;
}
input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  color: #0f172a;
  font: inherit;
  font-size: 14px;
}
input::placeholder {
  color: #94a3b8;
}
.clear-button {
  display: grid;
  width: 24px;
  height: 24px;
  padding: 0;
  place-items: center;
  border: 0;
  background: transparent;
  color: #94a3b8;
  font-size: 14px;
}
.search-message {
  margin: 10px 2px 0;
  color: #64748b;
  font-size: 12px;
}
.search-message.error {
  color: #dc2626;
}
.suggestion-list {
  margin: 12px 0 0;
  padding: 0;
  max-height: 248px;
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  list-style: none;
}
.suggestion-list::-webkit-scrollbar {
  width: 4px;
}
.suggestion-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #cbd5e1;
}
.suggestion-list li + li {
  border-top: 1px solid #eef2f7;
}
.suggestion-list button {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 12px;
  padding: 14px;
  border: 0;
  background: #fff;
  color: #2a60f7;
  text-align: left;
}
.suggestion-list span {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  color: #0f172a;
  font-size: 14px;
}
.suggestion-list small {
  overflow: hidden;
  color: #94a3b8;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.suggestion-list > li > button > :last-child {
  color: #94a3b8;
  font-size: 12px;
}
.selected-message {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 12px 2px 0;
  color: #2563eb;
  font-size: 12px;
}

@media (min-width: 768px) {
  .search-feedback {
    position: absolute;
    top: 100%;
    right: 0;
    left: 0;
    z-index: 20;
    padding-top: 10px;
  }

  .search-feedback .suggestion-list {
    margin: 0;
    background: #fff;
    box-shadow: 0 16px 32px -18px rgba(15, 23, 42, 0.3);
  }
}
</style>
