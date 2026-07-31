<script setup>
import { computed, ref } from 'vue';

const selectedPurpose = ref('school');
const destination = ref('');

const purposes = [
  { id: 'school', label: '학교', icon: '🎓' },
  { id: 'work', label: '직장', icon: '💼' },
  { id: 'etc', label: '기타', icon: '🏠' },
];

const showSuggestions = computed(() => destination.value.trim().length > 0);
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

      <label class="search-label" for="destination">학교/직장 이름 또는 주소</label>
      <div class="search-box">
        <i class="fa-solid fa-magnifying-glass" aria-hidden="true"></i>
        <input
          id="destination"
          v-model="destination"
          type="text"
          placeholder="예) 세종대학교, KB국민은행 신관"
        />
        <button
          v-if="destination"
          type="button"
          class="clear-button"
          aria-label="검색어 지우기"
          @click="destination = ''"
        >
          <i class="fa-solid fa-xmark" aria-hidden="true"></i>
        </button>
      </div>

      <ul v-if="showSuggestions" class="suggestion-list">
        <li>
          <button type="button">
            <i class="fa-solid fa-location-dot" aria-hidden="true"></i>
            <span>
              <strong>{{ destination }}</strong>
              <small>목적지 검색 API 연결 후 표시</small>
            </span>
            <i class="fa-solid fa-chevron-right" aria-hidden="true"></i>
          </button>
        </li>
      </ul>
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

.suggestion-list {
  margin: 12px 0 0;
  padding: 0;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  list-style: none;
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
  color: #94a3b8;
  font-size: 12px;
}

.suggestion-list > li > button > :last-child {
  color: #94a3b8;
  font-size: 12px;
}
</style>
