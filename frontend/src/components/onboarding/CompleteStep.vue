<script setup>
import { computed } from 'vue';

const props = defineProps({
  selectedDestination: {
    type: Object,
    default: null,
  },
  transport: {
    type: String,
    default: 'walk',
  },
  deposit: {
    type: Number,
    default: 3000,
  },
  monthlyRent: {
    type: Number,
    default: 70,
  },
  safety: {
    type: String,
    default: 'high',
  },
});

const formatDeposit = (amount) => {
  if (amount >= 10000) {
    const eok = Math.floor(amount / 10000);
    const remainder = amount % 10000;
    return remainder ? `${eok}억 ${remainder.toLocaleString()}만원` : `${eok}억원`;
  }

  return `${amount.toLocaleString()}만원`;
};

const summaryItems = computed(() => [
  {
    icon: 'fa-location-dot',
    label: '주 목적지',
    value: props.selectedDestination?.destName ?? '목적지를 선택해 주세요',
    tone: 'blue',
  },
  {
    icon: props.transport === 'walk' ? 'fa-person-walking' : 'fa-bus',
    label: '이동 수단',
    value: props.transport === 'walk' ? '도보 (최대 15분)' : '대중교통 (최대 15분)',
    tone: 'orange',
  },
  {
    icon: 'fa-wallet',
    label: '보증금',
    value: `${formatDeposit(props.deposit)} 이하`,
    tone: 'purple',
  },
  {
    icon: 'fa-won-sign',
    label: '월세',
    value: props.monthlyRent === 0 ? '전세' : `${props.monthlyRent.toLocaleString()}만원 이하`,
    tone: 'blue',
  },
  {
    icon: 'fa-shield-heart',
    label: '안전 기준',
    value: props.safety === 'high' ? '매우 안전한 곳만 (80점 이상)' : '안전한 곳 위주 (60점 이상)',
    tone: 'green',
  },
]);
</script>

<template>
  <section class="complete-step">
    <div class="complete-card">
      <div class="complete-icon" aria-hidden="true">
        <i class="fa-solid fa-check"></i>
      </div>

      <h2>설정 완료!</h2>
      <p class="description">입력하신 조건을 바탕으로 맞춤 매물을 찾아드려요</p>

      <ul class="summary-list" aria-label="내 탐색 조건">
        <li v-for="item in summaryItems" :key="item.label">
          <span class="summary-icon" :class="item.tone" aria-hidden="true">
            <i class="fa-solid" :class="item.icon"></i>
          </span>
          <span class="summary-label">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </li>
      </ul>
    </div>
  </section>
</template>

<style scoped>
.complete-step {
  display: flex;
  justify-content: center;
  width: 100%;
}

.complete-card {
  width: 100%;
  box-sizing: border-box;
  padding: 28px 20px 22px;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 4px 20px -2px rgba(15, 23, 42, 0.06);
}

.complete-icon {
  display: grid;
  width: 40px;
  height: 40px;
  margin-bottom: 16px;
  place-items: center;
  border-radius: 12px;
  background: #ecfdf5;
  color: #27ae60;
  font-size: 20px;
}

h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.4;
}

.description {
  margin: 8px 0 24px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.5;
}

.summary-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.summary-list li {
  display: grid;
  grid-template-columns: 30px 1fr auto;
  align-items: center;
  gap: 9px;
  min-height: 50px;
  padding: 0 12px;
  border: 1px solid #e9eef5;
  border-radius: 10px;
  background: #f8fafc;
}

.summary-icon {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 8px;
  font-size: 11px;
}

.summary-icon.blue {
  background: #eff6ff;
  color: #2a60f7;
}

.summary-icon.orange {
  background: #fff7ed;
  color: #ea8b24;
}

.summary-icon.purple {
  background: #f5f3ff;
  color: #7c63d9;
}

.summary-icon.green {
  background: #ecfdf5;
  color: #27ae60;
}

.summary-label {
  color: #64748b;
  font-size: 12px;
}

.summary-list strong {
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}
</style>
