<script setup>
import { computed } from 'vue';

const props = defineProps({
  destination: {
    type: String,
    default: '',
  },
  transportMode: {
    type: String,
    default: 'WALK',
  },
  travelTime: {
    type: Number,
    default: 15,
  },
  maxDeposit: {
    type: Number,
    default: 3000,
  },
  maxRent: {
    type: Number,
    default: 70,
  },
  minSafetyScore: {
    type: Number,
    default: 0,
  },
  showClose: {
    type: Boolean,
    default: true,
  },
});

defineEmits(['close']);

const transportLabel = computed(() => (props.transportMode === 'TRANSIT' ? '대중교통' : '도보'));
const destinationLabel = computed(() => props.destination?.trim() || '목적지를 설정해 주세요');
const priceLabel = computed(() => {
  if (props.maxRent === 0) return `전세 ${props.maxDeposit.toLocaleString()}만 이하`;
  return `보증 ${props.maxDeposit.toLocaleString()}만 · 월세 ${props.maxRent}만 이하`;
});
</script>

<template>
  <section class="summary">
    <div class="summary-top">
      <div>
        <p class="summary-label">목적지</p>
        <div class="summary-title">
          <i class="fa-solid fa-location-dot location-icon" aria-hidden="true"></i>
          <strong>{{ destinationLabel }}</strong>
        </div>
      </div>

      <button
        v-if="showClose"
        class="close-button"
        type="button"
        aria-label="필터 닫기"
        @click="$emit('close')"
      >
        ×
      </button>
    </div>

    <div class="condition-chips">
      <span>{{ transportLabel }} {{ travelTime }}분</span>
      <span>{{ priceLabel }}</span>
      <span v-if="minSafetyScore > 0">안전 {{ minSafetyScore }}점 이상</span>
    </div>
  </section>
</template>

<style scoped>
.summary {
  position: relative;
  padding-right: 42px;
  margin: 20px 0px;
}

.summary-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.summary-label {
  margin: 0 0 5px;
  color: #7d8797;
  font-size: 12px;
  font-weight: 600;
}

.summary-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #1e293b;
  font-size: 18px;
}

.location-icon {
  color: #3d55f6;
  font-size: 14px;
}

.close-button {
  position: absolute;
  top: 0;
  right: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #94a3b8;
  font-size: 0;
  line-height: 1;
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    color 0.15s ease;
}

.close-button::after {
  content: '×';
  font-size: 23px;
  line-height: 1;
}

.close-button:hover {
  background: #f1f5f9;
  color: #475569;
}

.condition-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.condition-chips span {
  padding: 5px 9px;
  border: 1px solid #dce4ff;
  border-radius: 999px;
  background: #f3f6ff;
  color: #5470ed;
  font-size: 11px;
  font-weight: 600;
}
</style>
