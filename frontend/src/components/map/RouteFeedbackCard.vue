<script setup>
import { computed, ref, watch } from 'vue';
import routeVoteService from '@/api/routeVoteService.js';

const props = defineProps({
  propertyId: {
    type: [Number, String],
    required: true,
  },
  destinationId: {
    type: [Number, String],
    required: true,
  },
});

const MIN_VOTES_FOR_STATS = 20;
const isVisible = ref(true);
const isLoading = ref(false);
const isSaving = ref(false);
const loadError = ref(false);
const voteSummary = ref({
  hasVoted: false,
  myVote: null,
  totalVotes: 0,
  safeCount: 0,
  safePercent: null,
  reliable: false,
});

const hasVoted = computed(() => voteSummary.value.hasVoted);
const myVote = computed(() => voteSummary.value.myVote);
const totalVotes = computed(() => Number(voteSummary.value.totalVotes || 0));
const safeCount = computed(() => Number(voteSummary.value.safeCount || 0));
const safePercent = computed(() => Number(voteSummary.value.safePercent || 0));
const hasEnoughData = computed(
  () => voteSummary.value.reliable || totalVotes.value >= MIN_VOTES_FOR_STATS,
);
const remainingVotes = computed(() => Math.max(MIN_VOTES_FOR_STATS - totalVotes.value, 0));

const activeEffect = ref(null); // null | 'SAFE' | 'UNSAFE'
let effectTimer = null;

const loadVoteSummary = async () => {
  if (!props.propertyId || !props.destinationId) return;

  isLoading.value = true;
  loadError.value = false;
  try {
    voteSummary.value = await routeVoteService.getVoteSummary(
      props.propertyId,
      props.destinationId,
    );
  } catch (error) {
    loadError.value = true;
    console.error('투표 조회 실패:', error);
  } finally {
    isLoading.value = false;
  }
};

const castVote = async (voteType) => {
  if (isSaving.value || !props.propertyId || !props.destinationId) return;

  isSaving.value = true;
  loadError.value = false;
  activeEffect.value = voteType;

  try {
    voteSummary.value = await routeVoteService.saveVote(
      props.propertyId,
      props.destinationId,
      voteType,
    );
  } catch (error) {
    loadError.value = true;
    console.error('투표 저장 실패:', error);
  } finally {
    isSaving.value = false;
    clearTimeout(effectTimer);
    effectTimer = setTimeout(() => {
      activeEffect.value = null;
    }, 1100);
  }
};

watch(
  () => [props.propertyId, props.destinationId],
  () => loadVoteSummary(),
  { immediate: true },
);
</script>

<template>
  <Transition name="card-fold" mode="out-in">
    <div
      v-if="isVisible"
      key="card-panel"
      class="route-feedback-card relative w-[240px] xl:w-[310px] rounded-xl xl:rounded-2xl border border-slate-200/80 bg-white/95 p-2.5 xl:p-4 shadow-xl backdrop-blur-md transition-all overflow-hidden text-xs"
    >
      <!-- 🎉 뾰로롱 파티클 & 피드백 완료 1초 오버레이 -->
      <Transition name="fade">
        <div
          v-if="activeEffect"
          class="absolute inset-0 z-30 flex flex-col items-center justify-center rounded-xl xl:rounded-2xl bg-white/95 backdrop-blur-md p-3 xl:p-4 text-center border border-slate-200/80 shadow-2xl"
        >
          <!-- 🍀 럭키비키 뾰로롱 파티클 -->
          <div
            v-if="activeEffect === 'SAFE'"
            class="relative flex items-center justify-center mb-1"
          >
            <span class="text-3xl xl:text-4xl animate-bounce">🍀</span>
            <span class="sparkle-particle absolute -top-3 -left-3 text-sm xl:text-lg">✨</span>
            <span class="sparkle-particle absolute -top-4 right-0 text-xs xl:text-base">🌟</span>
            <span class="sparkle-particle absolute bottom-0 -left-5 text-xs xl:text-sm">✨</span>
            <span class="sparkle-particle absolute -bottom-1 right-2 text-sm xl:text-lg">🍀</span>
          </div>

          <!-- 💩 언럭키비키 뾰로롱 파티클 -->
          <div v-else class="relative flex items-center justify-center mb-1">
            <span class="text-3xl xl:text-4xl animate-bounce">💩</span>
            <span class="sparkle-particle absolute -top-3 -left-3 text-sm xl:text-lg">💨</span>
            <span class="sparkle-particle absolute -top-4 right-0 text-xs xl:text-base">✨</span>
            <span class="sparkle-particle absolute bottom-0 -left-5 text-xs xl:text-sm">💨</span>
            <span class="sparkle-particle absolute -bottom-1 right-2 text-sm xl:text-lg">💩</span>
          </div>

          <p class="text-[11px] xl:text-xs font-black text-slate-900 mt-0.5 xl:mt-1">
            {{ activeEffect === 'SAFE' ? '완전 럭키비키! 🍀' : '언 럭키비키... 💩' }}
          </p>
          <p class="text-[9.5px] xl:text-[10.5px] font-bold text-slate-500 mt-0.5">
            {{
              activeEffect === 'SAFE'
                ? '긍정 피드백이 반짝 반영되었어요!'
                : '피드백을 학습하여 경로를 개선할게요!'
            }}
          </p>
        </div>
      </Transition>

      <div class="mb-1.5 xl:mb-2.5 flex items-center justify-between gap-1.5 border-b border-slate-100 pb-1.5 xl:pb-2">
        <div class="flex items-center gap-1 xl:gap-1.5">
          <span
            class="flex h-5 w-5 xl:h-6 xl:w-6 items-center justify-center rounded-lg bg-blue-50 text-[10px] xl:text-xs text-blue-600"
          >
            📍
          </span>
          <span class="text-[11px] xl:text-xs font-black text-slate-900">귀갓길 경로 평가</span>
        </div>
        <div class="flex flex-shrink-0 items-center gap-1 xl:gap-1.5">
          <span
            class="inline-flex items-center gap-1 rounded-full border border-emerald-200 bg-emerald-50 px-1.5 xl:px-2 py-0.5 text-[9px] xl:text-[10px] font-extrabold text-emerald-600"
          >
            <span class="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
            실시간
          </span>
          <button
            type="button"
            class="flex h-4 w-4 xl:h-5 xl:w-5 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
            aria-label="경로 평가 카드 닫기"
            @click="isVisible = false"
          >
            <i class="fa-solid fa-xmark text-[10px] xl:text-[11px]" aria-hidden="true"></i>
          </button>
        </div>
      </div>

      <p class="mb-1 text-[11px] xl:text-xs font-bold text-slate-800">이 경로는 어떠셨나요?</p>

      <div v-if="isLoading" class="mb-3 flex items-center gap-2 text-[12px] text-slate-500">
        <i class="fa-solid fa-spinner animate-spin text-blue-500" aria-hidden="true"></i>
        <span>투표 정보를 불러오는 중이에요</span>
      </div>

      <p v-else-if="loadError" class="mb-2 xl:mb-3 text-[10.5px] xl:text-[12px] leading-snug text-rose-500">
        투표 정보를 불러오지 못했어요. 잠시 후 다시 시도해주세요
      </p>

      <p v-else class="mb-2 xl:mb-3 text-[10.5px] xl:text-[12px] leading-snug text-slate-500">
        <template v-if="!hasVoted">
          아직 수집된 데이터가 부족해요. 가장 먼저 귀갓길 소감을 남겨주세요!
        </template>
        <template v-else-if="hasEnoughData">
          이 경로를 이용한 <strong class="font-black text-slate-900">{{ totalVotes }}명</strong> 중
          <strong class="font-black text-blue-600">총 {{ safeCount }}명 ({{ safePercent }}%)</strong
          >이 안전하다고 평가했어요.
        </template>
        <template v-else>
          의견이 반영되었어요. 신뢰있는 결과까지
          <strong class="font-black text-blue-600">{{ remainingVotes }}명</strong>의 의견이 더
          필요해요.
        </template>
      </p>

      <div class="flex gap-1.5 xl:gap-2">
        <button
          type="button"
          class="flex flex-1 items-center justify-center gap-1 rounded-lg xl:rounded-xl border py-1.5 xl:py-2 text-[10.5px] xl:text-[12px] transition-all active:scale-95 disabled:cursor-not-allowed disabled:opacity-60"
          :class="
            myVote === 'SAFE'
              ? 'border-emerald-600 bg-emerald-600 font-black text-white shadow-md ring-2 ring-emerald-200'
              : 'border-emerald-200 bg-emerald-50 font-bold text-emerald-700 hover:bg-emerald-100'
          "
          :disabled="isSaving || isLoading"
          @click="castVote('SAFE')"
        >
          <span aria-hidden="true" class="text-xs xl:text-sm">🍀</span>
          <span>완전 럭키비키</span>
        </button>

        <button
          type="button"
          class="flex flex-1 items-center justify-center gap-1 rounded-lg xl:rounded-xl border py-1.5 xl:py-2 text-[10.5px] xl:text-[12px] transition-all active:scale-95 disabled:cursor-not-allowed disabled:opacity-60"
          :class="
            myVote === 'UNSAFE'
              ? 'border-rose-600 bg-rose-600 font-black text-white shadow-md ring-2 ring-rose-200'
              : 'border-rose-200 bg-rose-50 font-bold text-rose-700 hover:bg-rose-100'
          "
          :disabled="isSaving || isLoading"
          @click="castVote('UNSAFE')"
        >
          <span aria-hidden="true" class="text-xs xl:text-sm">💩</span>
          <span>언 럭키비키...</span>
        </button>
      </div>
    </div>

    <!-- 📍 미니 플로팅 경로 평가 다시 펼치기 버튼 -->
    <button
      v-else
      key="floating-btn"
      type="button"
      class="inline-flex h-10 w-10 items-center justify-center rounded-full border border-blue-100 bg-white/95 text-sm text-[#4058f5] shadow-lg backdrop-blur transition-all hover:-translate-y-0.5 hover:border-blue-200 hover:bg-blue-50/95 active:translate-y-0 cursor-pointer"
      aria-label="귀갓길 경로 평가 다시 펼치기"
      title="귀갓길 경로 평가 펼치기"
      @click="isVisible = true"
    >
      <i class="fa-solid fa-route" aria-hidden="true"></i>
    </button>
  </Transition>
</template>

<style scoped>
/* 🔮 카드 접힘/열림 뾰로롱 스위시 액션 애니메이션 */
.card-fold-enter-active {
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.card-fold-leave-active {
  transition: all 0.38s cubic-bezier(0.4, 0, 0.2, 1);
}

.card-fold-enter-from {
  opacity: 0;
  transform: scale(0.3) translateY(12px) rotate(-8deg);
}

.card-fold-leave-to {
  opacity: 0;
  transform: scale(0.2) translateY(24px) rotate(12deg);
  filter: blur(4px);
}

@keyframes pop-sparkle {
  0% {
    transform: scale(0.3) translateY(0) rotate(0deg);
    opacity: 0;
  }
  50% {
    transform: scale(1.3) translateY(-14px) rotate(15deg);
    opacity: 1;
  }
  100% {
    transform: scale(1) translateY(-28px) rotate(-10deg);
    opacity: 0;
  }
}

.sparkle-particle {
  animation: pop-sparkle 1s ease-out forwards;
}
</style>
