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
  currentPage: {
    type: Number,
    default: 1,
  },
  lastLoadedDate: {
    type: String,
    default: '',
  },
  showAllLoadedToast: {
    type: Boolean,
    default: false,
  },
  mobilePanelHeight: {
    type: String,
    default: 'HALF',
  },
  dragPixelHeight: {
    type: Number,
    default: null,
  },
  isDragging: {
    type: Boolean,
    default: false,
  },
});

defineEmits(['click']);

const totalPages = computed(() => {
  if (!props.totalCount) return 1;
  return Math.max(1, Math.ceil(props.totalCount / 200));
});

const displayPage = computed(() => {
  const cur = Math.max(1, Number(props.currentPage) || 1);
  return Math.min(cur, totalPages.value);
});

const subText = computed(() => {
  if (props.isMapMoved) {
    return '📍 현재 화면 영역 매물 탐색';
  }
  if (props.lastLoadedDate) {
    return `📦 ${props.lastLoadedDate} 200개 단위로 불러와요`;
  }
  return '📦 200개 단위로 불러와요';
});

const hasMore = computed(() => {
  if (props.isMapMoved) return false;
  if (props.showAllLoadedToast) return false;
  return totalPages.value > 1 && displayPage.value < totalPages.value;
});

const isHiddenOverHalf = computed(() => {
  if (typeof window !== 'undefined' && window.innerWidth >= 1280) {
    return false; // PC 데스크톱은 항상 노출
  }
  if (props.mobilePanelHeight === 'EXPANDED') {
    return true;
  }
  if (props.dragPixelHeight != null) {
    const winHeight = typeof window !== 'undefined' ? window.innerHeight : 800;
    return props.dragPixelHeight > winHeight * 0.5;
  }
  return false;
});

const dynamicBottom = computed(() => {
  if (typeof window !== 'undefined' && window.innerWidth >= 1280) {
    return undefined; // PC 데스크톱은 xl:bottom-6 기본값 사용
  }
  if (props.dragPixelHeight != null) {
    return `${props.dragPixelHeight + 14}px`;
  }
  if (props.mobilePanelHeight === 'EXPANDED') {
    return '52%'; // 50% 넘어가서 페이드아웃 될 때 자연스러운 위치
  }
  if (props.mobilePanelHeight === 'COLLAPSED') {
    return '50px'; // 36px 손잡이 + 14px 여백
  }
  return 'calc(28% + 14px)';
});
</script>

<template>
  <div
    class="absolute xl:bottom-6 left-1/2 -translate-x-1/2 z-30 flex flex-col items-center gap-2"
    :class="[
      isDragging ? 'transition-none' : 'transition-all duration-300 ease-out',
      isHiddenOverHalf
        ? 'opacity-0 pointer-events-none scale-90 translate-y-3'
        : 'opacity-100 pointer-events-auto scale-100 translate-y-0',
    ]"
    :style="dynamicBottom ? { bottom: dynamicBottom } : {}"
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
          : 'bg-white/95 text-slate-800 border border-blue-200/90 hover:bg-blue-600 hover:text-white hover:border-blue-600 shadow-xl',
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
            isMapMoved
              ? 'text-white'
              : 'text-blue-500 group-hover:text-white'
          "
          aria-hidden="true"
        ></i>
        <i
          v-else-if="isMapMoved"
          class="fa-solid fa-rotate-right text-white text-xs"
          aria-hidden="true"
        ></i>
        <i
          v-else-if="hasMore"
          class="fa-solid fa-plus text-blue-600 group-hover:text-white text-xs"
          aria-hidden="true"
        ></i>
        <i
          v-else
          class="fa-solid fa-circle-check text-blue-600 group-hover:text-white text-xs"
          aria-hidden="true"
        ></i>

        <span
          class="text-[12px] font-black tracking-tight flex items-center gap-1.5"
        >
          <template v-if="isLoading">
            {{
              isMapMoved
                ? '이 위치 매물 검색 중...'
                : '다음 200개 불러오는 중...'
            }}
          </template>
          <template v-else-if="isMapMoved"> 이 위치에서 매물 재검색 </template>
          <template v-else-if="hasMore">
            <span>매물 더보기 ({{ displayPage }}/{{ totalPages }})</span>
            <span
              class="inline-flex h-2 w-2 rounded-full bg-blue-500 group-hover:bg-white animate-pulse"
              title="다음 200개 매물 탐색 가능"
            ></span>
          </template>
          <template v-else>
            <!-- <span>매물 전체 수집 완료 ({{ totalPages }}/{{ totalPages }})</span> -->
            <span>매물 전체 수집 완료</span>
            <span
              class="inline-flex h-2 w-2 rounded-full bg-emerald-500"
              title="모든 매물 수집 완료"
            ></span>
          </template>
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
