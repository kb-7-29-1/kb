<script setup>
import { computed, ref, watch } from 'vue';

const emit = defineEmits(['update:safety']);
const props = defineProps({
  safety: {
    type: String,
    default: 'high',
  },
});

const selectedSafety = ref(props.safety);

const safetyOptions = [
  {
    id: 'high',
    title: '매우 안전한 곳만',
    description: 'CCTV·가로등이 촘촘하고 파출소가 인접한 최고 안심 매물',
    score: '80+',
    scoreLabel: '안전 점수 80점 이상',
    tone: 'safe',
  },
  {
    id: 'normal',
    title: '적당히 안전하면 돼요',
    description: '주요 동선에 CCTV·가로등이 잘 갖춰진 매물',
    score: '60+',
    scoreLabel: '안전 점수 60점 이상',
    tone: 'normal',
  },
];

const selectedOption = computed(() =>
  safetyOptions.find((option) => option.id === selectedSafety.value),
);

watch(selectedSafety, (value) => emit('update:safety', value));
</script>

<template>
  <section class="safety-step">
    <div class="safety-card">
      <div class="card-icon" aria-hidden="true">
        <i class="fa-solid fa-shield-heart"></i>
      </div>

      <h2>안전을 얼마나 중시하시나요?</h2>
      <p class="description">원하는 안전 기준에 맞춰 매물을 추천해 드릴게요</p>

      <div class="safety-list" role="group" aria-label="안전 선호도">
        <button
          v-for="option in safetyOptions"
          :key="option.id"
          type="button"
          class="safety-button"
          :class="[{ active: selectedSafety === option.id }, option.tone]"
          @click="selectedSafety = option.id"
        >
          <span class="safety-copy">
            <strong>{{ option.title }}</strong>
            <small>{{ option.description }}</small>
            <em class="score-text">{{ option.scoreLabel }}</em>
          </span>
          <span class="score-badge">{{ option.score }}</span>
        </button>
      </div>

      <p class="selection-guide">
        <i class="fa-solid fa-circle-info" aria-hidden="true"></i>
        현재 <b :class="selectedOption.tone">{{ selectedOption.title }}</b> 기준으로 매물을 추천해
        드려요
      </p>
    </div>
  </section>
</template>

<style scoped>
.safety-step {
  display: flex;
  justify-content: center;
  width: 100%;
}

.safety-card {
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

.safety-list {
  display: grid;
  gap: 10px;
}

.safety-button {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  width: 100%;
  min-height: 68px;
  gap: 10px;
  padding: 13px 14px;
  border: 1px solid transparent;
  border-radius: 10px;
  color: #0f172a;
  font-family: inherit;
  text-align: left;
}

.safety-button.safe {
  border-color: #c7ead5;
  background: #f1fbf5;
}

.safety-button.normal {
  border-color: #f2dfb6;
  background: #fffaf0;
}

.safety-button.active {
  box-shadow: 0 0 0 2px rgba(15, 23, 42, 0.06);
}

.safety-button.safe.active {
  border-color: #70bd90;
  box-shadow: 0 0 0 2px rgba(112, 189, 144, 0.16);
}

.safety-button.normal.active {
  border-color: #dfb45c;
  box-shadow: 0 0 0 2px rgba(223, 180, 92, 0.16);
}

.safety-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.safety-copy strong {
  font-size: 14px;
  font-weight: 700;
}

.safe .safety-copy strong {
  color: #167542;
}

.normal .safety-copy strong {
  color: #a86100;
}

.safety-copy small {
  color: #64748b;
  font-size: 11px;
  line-height: 1.4;
}

.safe .safety-copy small {
  color: #518969;
}

.normal .safety-copy small {
  color: #9d762e;
}

.score-text {
  align-self: flex-start;
  margin-top: 2px;
  font-size: 10px;
  font-style: normal;
  font-weight: 700;
}

.safe .score-text {
  color: #229653;
}

.normal .score-text {
  color: #c98212;
}

.score-badge {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
}

.safe .score-badge {
  background: #35b86c;
}

.normal .score-badge {
  background: #d99015;
}

.selection-guide {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  margin: 18px 0 0;
  padding: 12px;
  border-radius: 10px;
  font-size: 12px;
  line-height: 1.5;
}

.selection-guide {
  background: #f8fafc;
  color: #64748b;
}

.selection-guide i {
  margin-top: 2px;
}

.selection-guide i {
  color: #2a60f7;
}

.selection-guide b {
  color: #334155;
}

.selection-guide b.safe {
  color: #167542;
}

.selection-guide b.normal {
  color: #a86100;
}
</style>
