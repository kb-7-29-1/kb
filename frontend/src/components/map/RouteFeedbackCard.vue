<template>
  <div
    v-if="isVisible"
    class="w-[300px] rounded-2xl border border-white/10 bg-[#101d3f] p-4 shadow-2xl"
  >
    <!-- 헤더 -->
    <div class="mb-2.5 flex items-center justify-between gap-2">
      <div class="flex items-center gap-1.5">
        <svg
          class="h-4 w-4 flex-shrink-0 text-white"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
          <circle cx="9" cy="7" r="4" />
          <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
          <path d="M16 3.13a4 4 0 0 1 0 7.75" />
        </svg>
        <span class="text-[13px] font-extrabold text-white">귀갓길 경로 실시간 평가</span>
      </div>
      <div class="flex flex-shrink-0 items-center gap-1.5">
        <span class="rounded-full bg-lime-400 px-2 py-0.5 text-[10px] font-bold text-slate-900">
          실시간
        </span>
        <button
          type="button"
          class="flex h-5 w-5 items-center justify-center rounded-full text-white/50 transition-colors hover:bg-white/10 hover:text-white"
          aria-label="카드 닫기"
          @click="isVisible = false"
        >
          ✕
        </button>
      </div>
    </div>

    <!-- 질문 -->
    <p class="mb-1.5 text-[13px] font-semibold text-white">이 경로를 이용해보셨나요?</p>

    <!-- 통계 (데이터 충분) / 참여 유도 (데이터 부족) -->
    <p class="mb-3 text-[12px] leading-5 text-slate-300">
      <template v-if="hasEnoughData">
        이 경로를 이용하신
        <strong class="font-extrabold text-white">{{ totalVotes }}명</strong>
        중
        <strong class="font-extrabold text-white">{{ positiveVotes }}명({{ positiveRate }}%)</strong>
        이 럭키비키하다고 평가했어요.
      </template>
      <template v-else>
        아직 수집된 데이터가 부족해요. 가장 먼저 귀갓길 소감을 남겨주세요!
      </template>
    </p>

    <!-- 투표 버튼 -->
    <div class="flex gap-2">
      <button
        type="button"
        class="flex flex-1 items-center justify-center gap-1 rounded-full py-2 text-[12px] font-bold text-white transition-all disabled:opacity-60"
        :class="
          myVote === 'POSITIVE'
            ? 'bg-emerald-500 ring-2 ring-emerald-300'
            : 'bg-emerald-500/90 hover:bg-emerald-500'
        "
        :disabled="myVote !== null"
        @click="castVote('POSITIVE')"
      >
        <span aria-hidden="true">✨</span>
        <span>완전 럭키비키!</span>
      </button>
      <button
        type="button"
        class="flex flex-1 items-center justify-center gap-1 rounded-full py-2 text-[12px] font-bold text-white transition-all disabled:opacity-60"
        :class="
          myVote === 'NEGATIVE'
            ? 'bg-rose-900 ring-2 ring-rose-400'
            : 'bg-rose-900/90 hover:bg-rose-900'
        "
        :disabled="myVote !== null"
        @click="castVote('NEGATIVE')"
      >
        <span aria-hidden="true">🛡️</span>
        <span>안 럭키비키...</span>
      </button>
    </div>

    <p v-if="myVote" class="mt-2 text-center text-[11px] font-semibold text-slate-300">
      소중한 의견 감사해요! 다음 방문자에게도 도움이 될 거예요.
    </p>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

// TODO: 백엔드 연동 시 매물/목적지 조합별 실제 통계 조회 및 투표 API로 교체
const isVisible = ref(true);
const totalVotes = ref(101);
const positiveVotes = ref(92);
const myVote = ref(null);

const MIN_VOTES_FOR_STATS = 5;

const hasEnoughData = computed(() => totalVotes.value >= MIN_VOTES_FOR_STATS);
const positiveRate = computed(() =>
  totalVotes.value > 0 ? Math.round((positiveVotes.value / totalVotes.value) * 100) : 0,
);

const castVote = (vote) => {
  if (myVote.value !== null) return;
  myVote.value = vote;
  totalVotes.value += 1;
  if (vote === 'POSITIVE') positiveVotes.value += 1;
};
</script>