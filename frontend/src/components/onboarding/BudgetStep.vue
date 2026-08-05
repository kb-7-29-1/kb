<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import api from '@/api/api';
import { useAuthStore } from '@/stores/useAuthStore.js';
import {
  DEPOSIT_MAX_LABEL,
  DEPOSIT_MIN_LABEL,
  DEPOSIT_OPTIONS,
  RENT_MAX,
  RENT_MAX_LABEL,
  RENT_MIN,
  RENT_MIN_LABEL,
  RENT_STEP,
} from '@/utils/budget';

const emit = defineEmits(['update:deposit', 'update:monthly-rent']);
const props = defineProps({
  deposit: {
    type: Number,
    default: 3000,
  },
  monthlyRent: {
    type: Number,
    default: 70,
  },
});

const depositOptions = DEPOSIT_OPTIONS;
const depositIndex = ref(Math.max(0, depositOptions.indexOf(props.deposit)));
const monthlyRent = ref(props.monthlyRent);

const deposit = computed(() => depositOptions[depositIndex.value]);

const depositLabel = computed(() => {
  if (deposit.value === 10000) return '1억원';
  return `${deposit.value.toLocaleString()}만원`;
});

const monthlyRentLabel = computed(() => {
  if (monthlyRent.value === 0) return '전세';
  return `${monthlyRent.value}만원 이하`;
});

const authStore = useAuthStore();
const loan = ref(null);
const loanLoading = ref(false);

const age = computed(() => {
  const birthDate = authStore.user?.birthDate;
  if (!birthDate) return null;
  const birthYear = new Date(birthDate).getFullYear();
  return new Date().getFullYear() - birthYear + 1;
});

const fetchLoan = async () => {
  loanLoading.value = true;
  try {
    const response = await api.get('/loan/onboarding-recommend', {
      params: {
        deposit: deposit.value,
        monthlyRent: monthlyRent.value,
        age: age.value,
      },
    });
    loan.value = response.data;
  } catch (error) {
    console.log('대출상품 조회 실패', error);
    loan.value = null;
  } finally {
    loanLoading.value = false;
  }
};

watch([deposit, monthlyRent], fetchLoan);
onMounted(fetchLoan);

const estimatedLoan = computed(() => deposit.value * 4);
const estimatedPropertyBudget = computed(() => deposit.value + estimatedLoan.value);

const formatAmount = (amount) => {
  if (amount === null || amount === undefined) return '-';
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
          v-model.number="depositIndex"
          type="range"
          min="0"
          :max="depositOptions.length - 1"
          step="1"
          :style="rangeStyle(depositIndex, 0, depositOptions.length - 1)"
        />
        <div class="range-labels">
          <span>{{ DEPOSIT_MIN_LABEL }}</span>
          <span>{{ DEPOSIT_MAX_LABEL }}</span>
        </div>
      </div>

      <div class="budget-field">
        <div class="field-heading">
          <label for="monthly-rent">월세</label>
          <strong>{{ monthlyRentLabel }}</strong>
        </div>
        <input
          id="monthly-rent"
          v-model.number="monthlyRent"
          type="range"
          :min="RENT_MIN"
          :max="RENT_MAX"
          :step="RENT_STEP"
          :style="rangeStyle(monthlyRent, RENT_MIN, RENT_MAX)"
        />
        <div class="range-labels">
          <span>{{ RENT_MIN_LABEL }}</span>
          <span>{{ RENT_MAX_LABEL }}</span>
        </div>
      </div>

      <aside v-if="loanLoading" class="loan-panel">
        <p style="color: #64748b; font-size: 12px">맞춤 금융 상품을 찾고 있어요...</p>
      </aside>

      <aside v-else-if="loan" class="loan-panel">
        <div class="loan-icon" aria-hidden="true">
          <i class="fa-solid fa-building-columns"></i>
        </div>
        <div class="loan-copy">
          <span>맞춤 금융 상품</span>
          <strong>{{ loan.productName }}</strong>
          <small>{{ loan.companyName }} · {{ loan.rateInfo }}</small>
        </div>
        <div class="recommendation-result">
          <template v-if="loan.loanRatio > 0">
            <p>
              보유 자금 <b>{{ depositLabel }}</b> + 예상 대출
              <b>{{ formatAmount(loan.expectedLoanAmount) }}</b>
              ({{ Math.round(loan.loanRatio * 100) }}% 적용)
            </p>
            <strong>최대 {{ formatAmount(loan.maxSearchAmount) }} 매물 탐색 가능</strong>
          </template>
          <template v-else>
            <p v-if="monthlyRent === 0">전세 기준 대출 한도</p>
            <p v-else>월세 {{ monthlyRent }}만원 이하 기준 대출 한도</p>
            <strong>{{ loan.loanLimit }} 대출 가능!</strong>
          </template>
          <p v-if="loan.target" class="target-info">대상 : {{ loan.target }}</p>
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

.recommendation-result .target-info {
  margin-top: 6px;
  color: #94a3b8;
  font-size: 10px;
}
</style>
