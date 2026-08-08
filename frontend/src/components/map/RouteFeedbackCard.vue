<template>
  <div
    v-if="isVisible"
    class="w-[310px] rounded-2xl border border-slate-200/80 bg-white/95 backdrop-blur-md p-4 shadow-xl transition-all"
  >
    <!-- 헤더 -->
    <div
      class="mb-2.5 flex items-center justify-between gap-2 border-b border-slate-100 pb-2"
    >
      <div class="flex items-center gap-1.5">
        <span
          class="flex h-6 w-6 items-center justify-center rounded-lg bg-blue-50 text-blue-600 text-xs"
        >
          📍
        </span>
        <span class="text-xs font-black text-slate-900"
          >귀갓길 경로 실시간 평가</span
        >
      </div>
      <div class="flex flex-shrink-0 items-center gap-1.5">
        <span
          class="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-[10px] font-extrabold text-emerald-600 border border-emerald-200"
        >
          <span
            class="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse"
          ></span>
          실시간
        </span>
        <button
          type="button"
          class="flex h-5 w-5 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
          aria-label="카드 닫기"
          @click="isVisible = false"
        >
          ✕
        </button>
      </div>
    </div>

    <!-- 질문 -->
    <p class="mb-1 text-xs font-bold text-slate-800">
      이 경로를 이용해보셨나요?
    </p>

    <!-- 통계 (데이터 충분) / 참여 유도 (데이터 부족) -->
    <p class="mb-3 text-[12px] leading-snug text-slate-500">
      <template v-if="hasEnoughData">
        이 경로를 이용하신
        <strong class="font-black text-slate-900">{{ totalVotes }}명</strong>
        중
        <strong class="font-black text-blue-600"
          >{{ positiveVotes }}명({{ positiveRate }}%)</strong
        >이 럭키비키하다고 평가했어요.
      </template>
      <template v-else>
        아직 수집된 데이터가 부족해요. 가장 먼저 귀갓길 소감을 남겨주세요!
      </template>
    </p>

    <!-- 자유롭게 스위칭 가능한 투표 버튼 -->
    <div class="flex gap-2">
      <button
        type="button"
        class="flex flex-1 items-center justify-center gap-1 rounded-xl py-2 text-[12px] transition-all cursor-pointer border active:scale-95"
        :class="
          myVote === 'POSITIVE'
            ? 'bg-emerald-600 text-white font-black border-emerald-600 shadow-md ring-2 ring-emerald-200'
            : 'bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border-emerald-200 font-bold'
        "
        @click="castVote('POSITIVE')"
      >
        <span aria-hidden="true">✨</span>
        <span>럭키비키!</span>
      </button>

      <button
        type="button"
        class="flex flex-1 items-center justify-center gap-1 rounded-xl py-2 text-[12px] transition-all cursor-pointer border active:scale-95"
        :class="
          myVote === 'NEGATIVE'
            ? 'bg-rose-600 text-white font-black border-rose-600 shadow-md ring-2 ring-rose-200'
            : 'bg-rose-50 hover:bg-rose-100 text-rose-700 border-rose-200 font-bold'
        "
        @click="castVote('NEGATIVE')"
      >
        <span aria-hidden="true">💩</span>
        <span>안 럭키비키...</span>
      </button>
    </div>

    <!-- 스위칭 완료 피드백 안내 -->
    <p
      v-if="myVote"
      class="mt-2 text-center text-[11px] font-bold text-blue-600 animate-fade-in"
    >
      {{
        myVote === 'POSITIVE'
          ? '✨ 오예! 완전 럭키비키 기운이 반영되었어요!'
          : '💩 앗... 다음엔 꼭 안전하고 럭키하길 바라요!'
      }}
    </p>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const isVisible = ref(true);
const totalVotes = ref(101);
const positiveVotes = ref(92);
const myVote = ref(null);

const MIN_VOTES_FOR_STATS = 5;

const hasEnoughData = computed(() => totalVotes.value >= MIN_VOTES_FOR_STATS);
const positiveRate = computed(() =>
  totalVotes.value > 0
    ? Math.round((positiveVotes.value / totalVotes.value) * 100)
    : 0,
);

// 투표 선택/스위치/해제 유연한 토글 제어
const castVote = (vote) => {
  if (myVote.value === vote) {
    // 이미 클릭된 버튼 다시 누르면 취소
    myVote.value = null;
    totalVotes.value -= 1;
    if (vote === 'POSITIVE') positiveVotes.value -= 1;
    return;
  }

  if (myVote.value === null) {
    // 최초 클릭
    myVote.value = vote;
    totalVotes.value += 1;
    if (vote === 'POSITIVE') positiveVotes.value += 1;
  } else {
    // 반대 상태로 스위칭
    if (vote === 'POSITIVE') {
      positiveVotes.value += 1;
    } else {
      positiveVotes.value -= 1;
    }
    myVote.value = vote;
  }
};
</script>
