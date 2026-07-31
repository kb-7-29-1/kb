<script setup>
import { computed, ref } from 'vue';

const depositOptions = [
  100, 200, 300, 400, 500, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000,
];
const depositIndex = ref(depositOptions.indexOf(3000));
const monthlyRent = ref(70);

const deposit = computed(() => depositOptions[depositIndex.value]);

const depositLabel = computed(() => {
  if (deposit.value === 10000) return '1억원';
  return `${deposit.value.toLocaleString()}만원`;
});

const monthlyRentLabel = computed(() => {
  if (monthlyRent.value === 0) return '전세';
  return `${monthlyRent.value}만원 이하`;
});

const estimatedLoan = computed(() => deposit.value * 4);
const estimatedPropertyBudget = computed(() => deposit.value + estimatedLoan.value);

const formatAmount = (amount) => {
  if (amount >= 10000) {
    const eok = Math.floor(amount / 10000);
    const remainder = amount % 10000;
    return remainder ? `${eok}억 ${remainder.toLocaleString()}만원` : `${eok}억원`;
  }

  return `${amount.toLocaleString()}만원`;
};

const rangeStyle = (value, min, max) => {
  const percent = ((value - min) / (max - min)) * 100;

  return {
    background: `linear-gradient(to right, #2a60f7 0%, #2a60f7 ${percent}%, #e2e8f0 ${percent}%, #e2e8f0 100%)`,
  };
};
</script>

<template>
  <section class="budget-step">
    <div class="budget-card">
      <div class="card-icon" aria-hidden="true">
        <i class="fa-solid fa-wallet"></i>
      </div>

      <h2>자산 정보를 알려주세요</h2>
      <p class="description">부담 없는 예산에 맞는 매물을 추천해 드릴게요</p>

      <div class="budget-field">
        <div class="field-heading">
          <label for="deposit">보증금</label>
          <strong>{{ depositLabel }} 이하</strong>
        </div>
        <input
          id="deposit"
          v-model="depositIndex"
          type="range"
          min="0"
          :max="depositOptions.length - 1"
          step="1"
          :style="rangeStyle(depositIndex, 0, depositOptions.length - 1)"
        />
        <div class="range-labels">
          <span>100만원</span>
          <span>1억원</span>
        </div>
      </div>

      <div class="budget-field">
        <div class="field-heading">
          <label for="monthly-rent">월세</label>
          <strong>{{ monthlyRentLabel }}</strong>
        </div>
        <input
          id="monthly-rent"
          v-model="monthlyRent"
          type="range"
          min="0"
          max="150"
          step="5"
          :style="rangeStyle(monthlyRent, 0, 150)"
        />
        <div class="range-labels">
          <span>전세</span>
          <span>150만원</span>
        </div>
      </div>

      <aside class="loan-panel">
        <div class="loan-icon" aria-hidden="true">
          <i class="fa-solid fa-building-columns"></i>
        </div>
        <div class="loan-copy">
          <span>맞춤 금융 상품</span>
          <strong>KB 주택전세자금대출</strong>
          <small>내 예산에 맞춰 예상 가능한 매물 금액을 안내해 드려요</small>
        </div>
        <div class="recommendation-result">
          <p>
            보유 자금 <b>{{ depositLabel }}</b> + 예상 대출
            <b>{{ formatAmount(estimatedLoan) }}</b>
            (80% 적용)
          </p>
          <strong>최대 {{ formatAmount(estimatedPropertyBudget) }} 매물 탐색 가능</strong>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.budget-step {
  display: flex;
  justify-content: center;
  width: 100%;
}

.budget-card {
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
  margin: 8px 0 28px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.5;
}

.budget-field + .budget-field {
  margin-top: 28px;
}

.field-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.field-heading label {
  color: #334155;
  font-size: 15px;
  font-weight: 700;
}

.field-heading strong {
  color: #2a60f7;
  font-size: 14px;
  font-weight: 700;
}

input[type='range'] {
  appearance: none;
  -webkit-appearance: none;
  width: 100%;
  height: 8px;
  margin: 0;
  border-radius: 999px;
  outline: 0;
  cursor: pointer;
  transition: background 0.15s ease;
}

input[type='range']::-webkit-slider-thumb {
  width: 22px;
  height: 22px;
  appearance: none;
  -webkit-appearance: none;
  border: 3px solid #fff;
  border-radius: 50%;
  background: #2a60f7;
  box-shadow: 0 1px 4px rgba(42, 96, 247, 0.35);
}

input[type='range']::-moz-range-thumb {
  width: 16px;
  height: 16px;
  border: 3px solid #fff;
  border-radius: 50%;
  background: #2a60f7;
  box-shadow: 0 1px 4px rgba(42, 96, 247, 0.35);
}

.range-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  color: #94a3b8;
  font-size: 11px;
}

.loan-panel {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 30px;
  padding: 16px;
  border: 1px solid #dbeafe;
  border-radius: 14px;
  background: #eff6ff;
}

.loan-icon {
  display: grid;
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  place-items: center;
  border-radius: 10px;
  background: #fff;
  color: #2a60f7;
  font-size: 15px;
}

.loan-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.loan-copy span {
  color: #2a60f7;
  font-size: 11px;
  font-weight: 700;
}

.loan-copy strong {
  overflow: hidden;
  color: #1e3a8a;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.loan-copy small {
  color: #64748b;
  font-size: 11px;
}

.recommendation-result {
  width: 100%;
  padding: 12px;
  border-radius: 10px;
  background: #fff;
}

.recommendation-result p {
  margin: 0;
  color: #64748b;
  font-size: 11px;
  line-height: 1.5;
}

.recommendation-result p b {
  color: #2a60f7;
  font-weight: 700;
}

.recommendation-result > strong {
  display: block;
  margin-top: 5px;
  color: #1e3a8a;
  font-size: 14px;
  font-weight: 700;
}
</style>
