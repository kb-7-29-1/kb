<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';

const props = defineProps({
  destination: {
    type: String,
    default: '세종대학교',
  },
  transport: {
    type: String,
    default: '도보 15분 이내',
  },
  deposit: {
    type: String,
    default: '3,000만원 이하',
  },
  rent: {
    type: String,
    default: '70만원 이하',
  },
  safety: {
    type: String,
    default: '80점 이상',
  },
});

const router = useRouter();

const conditions = computed(() => [
  { icon: '📍', label: '주 목적지', value: props.destination },
  { icon: '🚶', label: '이동 수단', value: props.transport },
  { icon: '💰', label: '보증금 예산', value: props.deposit },
  { icon: '🏢', label: '월세 예산', value: props.rent },
  { icon: '🛡️', label: '안전 점수', value: props.safety },
]);

const goOnboarding = () => router.push('/onboarding');
</script>

<template>
  <section class="onboarding-panel" aria-labelledby="onboarding-panel-title">
    <div class="panel-heading">
      <h2 id="onboarding-panel-title">내 탐색 조건</h2>
      <button type="button" class="edit-link" @click="goOnboarding">
        수정
        <i class="fa-solid fa-chevron-right" aria-hidden="true"></i>
      </button>
    </div>

    <ul class="condition-list">
      <li v-for="condition in conditions" :key="condition.label">
        <span class="condition-icon" aria-hidden="true">{{ condition.icon }}</span>
        <span class="condition-label">{{ condition.label }}</span>
        <strong>{{ condition.value }}</strong>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.onboarding-panel {
  box-sizing: border-box;
  padding: 18px 20px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
}

.panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

h2 {
  margin: 0;
  color: #17191d;
  font-size: 15px;
  font-weight: 700;
}

.edit-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #4767f7;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.edit-link i {
  font-size: 10px;
}

.condition-list {
  display: grid;
  gap: 12px;
  margin: 18px 0 2px;
  padding: 0;
  list-style: none;
}

.condition-list li {
  display: grid;
  grid-template-columns: 20px 80px minmax(0, 1fr);
  align-items: center;
  min-height: 20px;
  gap: 5px;
}

.condition-icon {
  font-size: 12px;
  line-height: 1;
}

.condition-label {
  color: #6d7480;
  font-size: 12px;
}

.condition-list strong {
  overflow: hidden;
  color: #4b5565;
  font-size: 12px;
  font-weight: 700;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
