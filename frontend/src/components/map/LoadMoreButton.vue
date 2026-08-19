<script setup>
import { computed } from 'vue';

const props = defineProps({
  isLoading: {
    type: Boolean,
    default: false,
  },
  isMapMoved: {
    type: Boolean,
    default: false,
  },
  visibleCount: {
    type: Number,
    required: true,
  },
  baseCount: {
    type: Number,
    default: 0,
  },
  totalCount: {
    type: Number,
    required: true,
  },
  lastLoadedDate: {
    type: String,
    default: '',
  },
  showAllLoadedToast: {
    type: Boolean,
    default: false,
  },
});

defineEmits(['click']);

const subText = computed(() => {
  if (props.isMapMoved) {
    return '📍 현재 화면 영역 매물 탐색';
  }
  if (props.lastLoadedDate) {
    return `📅 ${props.lastLoadedDate} 이전 등록 매물`;
  }
  return '📅 이전 등록 매물 순차 탐색';
});
</script>

<template>
  <div
    class="absolute bottom-14 xl:bottom-6 left-1/2 -translate-x-1/2 z-30 pointer-events-auto flex flex-col items-center gap-2"
  >
    <!-- 🎉 모든 매물 수집 완료 토스트 알림 배너 (버튼과 100% 동일 센터 정렬) -->
    <Transition name="toast">
      <div
        v-if="showAllLoadedToast"
        class="bg-slate-900/95 text-white px-4 py-2 rounded-full text-xs font-extrabold shadow-2xl backdrop-blur-md flex items-center gap-1.5 border border-slate-700 pointer-events-none whitespace-nowrap animate-bounce"
      >
        <span class="text-sm">🎉</span>
        <span>이 지역에서 조건에 부합하는 모든 매물을 다 불러왔어요!</span>
      </div>
    </Transition>

    <button
      type="button"
      class="flex flex-col items-center justify-center px-6 py-2 rounded-full font-extrabold shadow-xl transition-all duration-200 backdrop-blur-md active:scale-95 group cursor-pointer leading-tight"
      :class="[
        isMapMoved
          ? 'bg-blue-600 text-white border border-blue-500 shadow-blue-300/40 hover:bg-blue-700 animate-pulse'
          : 'bg-white/95 text-slate-800 border border-slate-200/90 hover:bg-blue-600 hover:text-white hover:border-blue-600',
      ]"
      :disabled="isLoading"
      @click="$emit('click')"
    >
      <!-- 1st Row: 메인 버튼 문구 -->
      <div class="flex items-center gap-1.5">
        <i
          v-if="isLoading"
          class="fa-solid fa-spinner animate-spin text-xs"
          :class="
            isMapMoved ? 'text-white' : 'text-blue-500 group-hover:text-white'
          "
          aria-hidden="true"
        ></i>
        <i
          v-else-if="isMapMoved"
          class="fa-solid fa-rotate-right text-white text-xs"
          aria-hidden="true"
        ></i>
        <i
          v-else
          class="fa-solid fa-plus text-blue-600 group-hover:text-white text-xs"
          aria-hidden="true"
        ></i>

        <span class="text-[12px] font-black tracking-tight">
          {{
            isLoading
              ? isMapMoved
                ? '이 위치 매물 검색 중...'
                : '다음 매물 불러오는 중...'
              : isMapMoved
                ? '이 위치에서 매물 재검색'
                : totalCount && visibleCount < totalCount
                  ? `매물 더보기 (${visibleCount.toLocaleString()} / ${totalCount.toLocaleString()}개)`
                  : `매물 전체 수집 완료 (${visibleCount.toLocaleString()}개)`
          }}
        </span>
      </div>

      <!-- 2nd Row: 동적 슬림 서브 날짜 안내 -->
      <div
        class="text-[10px] font-medium opacity-75 mt-0.5"
        :class="
          isMapMoved
            ? 'text-blue-100'
            : 'text-slate-400 group-hover:text-blue-100'
        "
      >
        {{ subText }}
      </div>
    </button>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s ease-out;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
