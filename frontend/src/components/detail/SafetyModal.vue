<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal-container">
      <!-- 헤더 영역 -->
      <header class="modal-header">
        <h2 class="modal-title">안전 점수 산정 기준</h2>
        <button class="close-btn" aria-label="닫기" @click="emit('close')">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#333" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
        </button>
      </header>

      <!-- 총점 영역 -->
      <section class="score-section" :style="{ backgroundColor: safetyScoreTheme.background }">
        <div class="score-headline">
          <span class="score-number" :style="{ color: safetyScoreTheme.color }">{{ safetyScore === null ? '-' : `${safetyScore}점` }}</span>
          <span class="score-status" :style="{ color: safetyScoreTheme.color }"> · {{ safetyGrade }}</span>
        </div>
        <p class="score-description">기관과 안전 데이터를 종합한 점수예요.</p>
      </section>

      <!-- 귀갓길 정보 -->
      <section v-if="routeDistanceLabel || routeTimeLabel" class="route-info-section">
        <div class="icon-wrap small-icon purple-text">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>
        </div>
        <div class="info-content">
          <div class="info-title">분석한 귀갓길</div>
          <div class="info-desc">목적지까지 총 {{ routeDistanceLabel || '-' }} · 도보 약 {{ routeTimeLabel || '-' }}</div>
        </div>
      </section>

      <!-- 안전 요소 리스트 -->
      <section class="factors-section">
        <h3 class="section-title">점수에 반영된 안전 요소</h3>

        <ul class="factor-list">
          <!-- CCTV -->
          <li class="factor-item">
            <div class="icon-wrap bg-purple purple-text">
              <i class="fa-solid fa-video" aria-hidden="true"></i>
            </div>
            <div class="info-content">
              <div class="info-title">CCTV 분포</div>
              <div class="info-desc">경로 반경 50m 내 {{ cctvCount }}개</div>
              <div class="info-desc">
                {{ formatAverageGap('CCTV', cctvAverageGapMeters) }}
              </div>
            </div>
          </li>

          <!-- 가로등 -->
          <li class="factor-item">
            <div class="icon-wrap bg-yellow yellow-text">
              <i class="fa-solid fa-lightbulb" aria-hidden="true"></i>
            </div>
            <div class="info-content">
              <div class="info-title">가로등 분포</div>
              <div class="info-desc">경로 반경 30m 내 {{ streetLampCount }}개</div>
              <div class="info-desc">
                {{ formatAverageGap('가로등', streetLampAverageGapMeters) }}
              </div>
            </div>
          </li>

          <!-- 치안시설 -->
          <li class="factor-item">
            <div class="icon-wrap bg-green green-text">
              <i class="fa-solid fa-user-shield" aria-hidden="true"></i>
            </div>
            <div class="info-content">
              <div class="info-title">치안시설 접근성</div>
              <!-- 조건문을 사용하여 텍스트를 두 줄로 분리 -->
              <template v-if="nearestPoliceDistanceMeters !== null">
                <div class="info-desc">가장 가까운 파출소 약 {{ nearestPoliceDistanceMeters }}m</div>
                <div class="info-desc">도보 약 {{ nearestPoliceWalkMinutes }}분</div>
              </template>
              <template v-else-if="isCalculating">
                <div class="info-desc">가장 가까운 파출소 거리 계산 중</div>
              </template>
              <template v-else>
                <div class="info-desc">가장 가까운 파출소 거리 계산 결과 없음</div>
              </template>
            </div>
          </li>
        </ul>
      </section>

      <!-- 하단 안내 문구 박스 -->
      <section class="notice-box">
        <p class="notice-main">점수는 귀갓길 거리와 경로 유형, CCTV·가로등의 분포, 파출소 접근성을 함께 반영해 계산해요.</p>
        <div class="notice-sub">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
          <span>공공데이터를 바탕으로 분석하며, 실제 환경이나 체감 안전과 차이가 있을 수 있어요.</span>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  property: {
    type: Object,
    default: null,
  },
  safetyBreakdown: {
    type: Object,
    default: null,
  },
  safetyRoute: {
    type: Object,
    default: null,
  },
  isCalculating: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['close']);

const safetyScore = computed(() => {
  const value = Number(props.property?.safetyScore);
  return Number.isFinite(value) ? Math.min(Math.max(value, 0), 100) : null;
});

const safetyGrade = computed(() => {
  if (safetyScore.value === null) return '점수 없음';
  if (safetyScore.value >= 80) return '안심';
  if (safetyScore.value >= 60) return '보통';
  return '주의 필요';
});

const safetyScoreTheme = computed(() => {
  if (safetyScore.value === null) return { color: '#64748b', background: '#f1f5f9' };
  if (safetyScore.value >= 80) return { color: '#22a06b', background: '#ecfdf3' };
  if (safetyScore.value >= 60) return { color: '#e69a1d', background: '#fff7e8' };
  return { color: '#e25858', background: '#fff1f2' };
});

const cctvCount = computed(() => props.property?.cctvCount ?? 0);
const streetLampCount = computed(
    () => props.property?.streetLampCount ?? props.property?.streetlightCount ?? 0,
);
const cctvAverageGapMeters = computed(
    () => props.safetyBreakdown?.cctvAverageGapMeters ?? null,
);
const streetLampAverageGapMeters = computed(
    () => {
      const calculatedGap = props.safetyBreakdown?.streetLightAverageGapMeters;
      if (calculatedGap !== null && calculatedGap !== undefined) return calculatedGap;

      const routeDistance = Number(props.safetyRoute?.distanceMeters);
      const count = Number(streetLampCount.value);
      if (!Number.isFinite(routeDistance) || routeDistance <= 0 || !Number.isFinite(count) || count <= 0) {
        return null;
      }
      return Math.round((routeDistance / count) * 10) / 10;
    },
);
const routeDistanceLabel = computed(() => {
  const meters = Number(props.safetyRoute?.distanceMeters);
  if (!Number.isFinite(meters) || meters <= 0) return '';
  return meters >= 1000 ? `${(meters / 1000).toFixed(1)}km` : `${Math.round(meters)}m`;
});
const routeTimeLabel = computed(() => {
  const seconds = Number(props.safetyRoute?.totalTimeSeconds);
  if (!Number.isFinite(seconds) || seconds <= 0) return '';
  return `${Math.max(1, Math.ceil(seconds / 60))}분`;
});
const formatAverageGap = (label, value) => {
  if (value !== null) return `${label} 간 평균 거리 약 ${value}m`;
  return props.isCalculating ? `${label} 간 평균 거리 계산 중` : `${label} 간 평균 거리 계산 결과 없음`;
};
const nearestPoliceDistanceMeters = computed(
    () => props.safetyBreakdown?.nearestPoliceDistanceMeters ?? null,
);
const nearestPoliceWalkMinutes = computed(() => {
  if (nearestPoliceDistanceMeters.value === null) return null;
  return Math.max(1, Math.ceil(nearestPoliceDistanceMeters.value / 75));
});

// formatPoliceDistance() 함수는 템플릿에서 직접 처리하므로 삭제했습니다.
const hasPoliceStation = computed(() => Boolean(props.property?.hasPoliceStation));
</script>

<style scoped>
* {
  box-sizing: border-box;
  font-family: 'Pretendard', 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif;
}

/* 모달 백그라운드 */
.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: rgba(0, 0, 0, 0.4);
  padding: 20px;
}

/* 모달 컨테이너 */
.modal-container {
  width: 100%;
  max-width: 400px;
  background-color: #ffffff;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

/* 헤더 */
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  color: #111;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 총점 영역 */
.score-section {
  background-color: #F7F7FA;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
}

.score-headline {
  margin-bottom: 8px;
}

.score-number {
  font-size: 22px;
  font-weight: 800;
  color: #F5872A;
}

.score-status {
  font-size: 18px;
  font-weight: 700;
  color: #111;
}

.score-description {
  font-size: 13px;
  color: #666;
  margin: 0;
}

/* 귀갓길 정보 */
.route-info-section {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 28px;
  padding: 0 4px;
}

/* 아이콘 공통 */
.icon-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
}

.icon-wrap .fa-solid {
  font-family: "Font Awesome 7 Free" !important;
  font-size: 20px;
  font-weight: 900;
  line-height: 1;
}

.small-icon {
  margin-top: 2px;
}

.bg-purple { background-color: #F2EFFF; border-radius: 50%; width: 44px; height: 44px; }
.purple-text { color: #6B4EFF; }

.bg-yellow { background-color: #FFF9E6; border-radius: 50%; width: 44px; height: 44px; }
.yellow-text { color: #F5B041; }

.bg-green { background-color: #EAF7ED; border-radius: 50%; width: 44px; height: 44px; }
.green-text { color: #4CAF50; }

/* 텍스트 정보 공통 */
.info-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-title {
  font-size: 15px;
  font-weight: 600;
  color: #222;
}

.info-desc {
  font-size: 13px;
  color: #666;
}

/* 안전 요소 리스트 */
.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #222;
  margin: 0 0 16px 4px;
}

.factor-list {
  list-style: none;
  padding: 0;
  margin: 0 0 24px 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.factor-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 0 4px;
}

/* 하단 안내 문구 박스 */
.notice-box {
  background-color: #F7F7FA;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}

.notice-main {
  font-size: 13px;
  color: #444;
  line-height: 1.5;
  margin: 0 0 12px 0;
  word-break: keep-all;
}

.notice-sub {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 12px;
  color: #777;
  line-height: 1.4;
  word-break: keep-all;
}

.notice-sub svg {
  flex-shrink: 0;
  margin-top: 2px;
}
</style>
