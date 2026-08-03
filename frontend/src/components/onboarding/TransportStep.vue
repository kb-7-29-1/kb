<script setup>
import { ref, watch } from 'vue';

const emit = defineEmits(['update:transport']);
const props = defineProps({
  transport: {
    type: String,
    default: 'walk',
  },
});

const selectedTransport = ref(props.transport);

const transportOptions = [
  {
    id: 'walk',
    icon: '🚶',
    title: '도보 이동 선호',
    description: '학교/직장 도보권 거주',
    timeLabel: '도보 15분 자동 설정',
  },
  {
    id: 'transit',
    icon: '🚌',
    title: '대중교통 선호',
    description: '버스·지하철 연계 거주',
    timeLabel: '대중교통 15분 자동 설정',
  },
];

watch(selectedTransport, (value) => emit('update:transport', value));
</script>

<template>
  <section class="transport-step">
    <div class="transport-card">
      <div class="card-icon" aria-hidden="true">
        <i class="fa-solid fa-route"></i>
      </div>

      <h2>주로 어떻게 이동하시나요?</h2>
      <p class="description">선호하시는 귀가 교통수단 방식을 선택해주세요</p>

      <div class="transport-list" role="group" aria-label="이동수단">
        <button
          v-for="option in transportOptions"
          :key="option.id"
          type="button"
          class="transport-button"
          :class="{ active: selectedTransport === option.id }"
          @click="selectedTransport = option.id"
        >
          <span class="transport-icon" aria-hidden="true">{{ option.icon }}</span>
          <span class="transport-copy">
            <strong>{{ option.title }}</strong>
            <small>{{ option.description }}</small>
            <span class="auto-time-label">
              <i class="fa-regular fa-clock" aria-hidden="true"></i>
              {{ option.timeLabel }}
            </span>
          </span>
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.transport-step {
  display: flex;
  justify-content: center;
  width: 100%;
}

.transport-card {
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

.transport-list {
  display: grid;
  gap: 12px;
}

.auto-time-label {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  gap: 6px;
  margin-top: 4px;
  padding: 4px 7px;
  border-radius: 8px;
  background: #eff6ff;
  color: #2a60f7;
  font-size: 11px;
  font-weight: 600;
}

.transport-button {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 88px;
  gap: 20px;
  padding: 20px 25px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
  color: #0f172a;
  font-family: inherit;
  text-align: left;
}

.transport-button.active {
  border-color: #2a60f7;
  background: #eff6ff;
}

.transport-icon {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  place-items: center;
  font-size: 25px;
}

.transport-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.transport-copy strong {
  font-size: 15px;
  font-weight: 700;
}

.transport-copy small {
  color: #64748b;
  font-size: 13px;
}
</style>
