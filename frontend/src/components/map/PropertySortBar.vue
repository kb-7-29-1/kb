<script setup>
import { computed, ref } from 'vue';

const props = defineProps({
  modelValue: {
    type: String,
    default: 'RECOMMENDED',
  },
  totalCount: {
    type: Number,
    default: 0,
  },
  isLoading: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['update:modelValue']);
const isDesktopSortExpanded = ref(false);

const sortOptions = computed(() => [
  {
    key: 'RECOMMENDED',
    type: 'RECOMMENDED',
    label: '추천순',
    icon: 'fa-solid fa-thumbs-up',
    isActive: props.modelValue === 'RECOMMENDED',
  },
  {
    key: 'DEPOSIT',
    type: 'DEPOSIT',
    label:
      props.modelValue === 'DEPOSIT_DESC'
        ? '보증금 높은순 🔺'
        : props.modelValue === 'DEPOSIT_ASC'
          ? '보증금 낮은순 🔻'
          : '보증금 ↕',
    icon:
      props.modelValue === 'DEPOSIT_DESC'
        ? 'fa-solid fa-arrow-up-wide-short'
        : 'fa-solid fa-arrow-down-wide-short',
    isActive: props.modelValue === 'DEPOSIT_ASC' || props.modelValue === 'DEPOSIT_DESC',
  },
  {
    key: 'RENT',
    type: 'RENT',
    label:
      props.modelValue === 'RENT_DESC'
        ? '월세 높은순 🔺'
        : props.modelValue === 'RENT_ASC'
          ? '월세 낮은순 🔻'
          : '월세 ↕',
    icon:
      props.modelValue === 'RENT_DESC'
        ? 'fa-solid fa-arrow-up-wide-short'
        : 'fa-solid fa-arrow-down-wide-short',
    isActive: props.modelValue === 'RENT_ASC' || props.modelValue === 'RENT_DESC',
  },
  {
    key: 'SAFETY_DESC',
    type: 'SAFETY_DESC',
    label: '안전점수 높은순',
    icon: 'fa-solid fa-shield-halved',
    isActive: props.modelValue === 'SAFETY_DESC',
  },
  {
    key: 'AREA_DESC',
    type: 'AREA_DESC',
    label: '면적 넓은순',
    icon: 'fa-solid fa-up-right-and-down-left-from-center',
    isActive: props.modelValue === 'AREA_DESC',
  },
]);

const handleSortClick = (opt) => {
  if (opt.type === 'DEPOSIT') {
    if (props.modelValue === 'DEPOSIT_ASC') {
      emit('update:modelValue', 'DEPOSIT_DESC');
    } else {
      emit('update:modelValue', 'DEPOSIT_ASC');
    }
  } else if (opt.type === 'RENT') {
    if (props.modelValue === 'RENT_ASC') {
      emit('update:modelValue', 'RENT_DESC');
    } else {
      emit('update:modelValue', 'RENT_ASC');
    }
  } else {
    emit('update:modelValue', opt.type);
  }
};

const desktopPrimarySortOptions = computed(() => sortOptions.value.slice(0, 3));
const desktopExtraSortOptions = computed(() => sortOptions.value.slice(3));
</script>

<template>
  <div class="property-sort-bar-container">
    <!-- 모바일: 가로 스크롤 정렬 버튼 -->
    <div class="mobile-sort-options flex items-center gap-1.5 overflow-x-auto pb-1 xl:hidden">
      <button
        v-for="opt in sortOptions"
        :key="opt.key"
        type="button"
        class="shrink-0 rounded-full border px-3 py-1.5 text-[11px] font-semibold transition-all flex items-center gap-1"
        :class="[
          opt.isActive
            ? 'border-[#4058f5] bg-[#eef1ff] text-[#4058f5]'
            : 'border-slate-200 bg-white text-slate-500 hover:border-slate-300 hover:bg-slate-50',
        ]"
        @click="handleSortClick(opt)"
      >
        <i :class="[opt.icon, 'text-[10px]']" aria-hidden="true"></i>
        <span>{{ opt.label }}</span>
      </button>
    </div>

    <!-- PC: 너비 안에서 펼쳐지는 정렬 버튼 -->
    <section class="hidden space-y-2 pt-3 xl:block">
      <p
        v-if="isLoading"
        class="m-0 flex items-center gap-1.5 text-[13px] font-bold text-[#5267e8]"
      >
        <i class="fa-solid fa-spinner animate-spin" aria-hidden="true"></i>
        안전 분석 중
      </p>
      <p v-else class="m-0 text-[13px] font-bold text-slate-500">총 {{ totalCount }}개 매물</p>
      <div class="flex items-center gap-1.5 pb-1">
        <button
          v-for="opt in desktopPrimarySortOptions"
          :key="opt.key"
          type="button"
          class="shrink-0 rounded-full border px-3 py-1.5 text-[11px] font-semibold transition-all flex items-center gap-1"
          :class="[
            opt.isActive
              ? 'border-[#4058f5] bg-[#eef1ff] text-[#4058f5]'
              : 'border-slate-200 bg-white text-slate-500 hover:border-slate-300 hover:bg-slate-50',
          ]"
          @click="handleSortClick(opt)"
        >
          <i :class="[opt.icon, 'text-[10px]']" aria-hidden="true"></i>
          <span>{{ opt.label }}</span>
        </button>
        <button
          type="button"
          class="inline-flex h-[29px] w-[29px] shrink-0 items-center justify-center rounded-full border text-[11px] transition-all"
          :class="
            isDesktopSortExpanded
              ? 'border-[#4058f5] bg-[#eef1ff] text-[#4058f5]'
              : 'border-slate-200 bg-white text-slate-500 hover:border-slate-300 hover:bg-slate-50'
          "
          :aria-label="isDesktopSortExpanded ? '추가 정렬 옵션 접기' : '추가 정렬 옵션 펼치기'"
          @click="isDesktopSortExpanded = !isDesktopSortExpanded"
        >
          <i
            :class="isDesktopSortExpanded ? 'fa-solid fa-chevron-up' : 'fa-solid fa-ellipsis'"
            aria-hidden="true"
          ></i>
        </button>
      </div>
      <Transition name="desktop-sort-expand">
        <div v-if="isDesktopSortExpanded" class="flex flex-wrap items-center gap-1.5 pb-1">
          <button
            v-for="opt in desktopExtraSortOptions"
            :key="opt.key"
            type="button"
            class="shrink-0 rounded-full border px-3 py-1.5 text-[11px] font-semibold transition-all flex items-center gap-1"
            :class="[
              opt.isActive
                ? 'border-[#4058f5] bg-[#eef1ff] text-[#4058f5]'
                : 'border-slate-200 bg-white text-slate-500 hover:border-slate-300 hover:bg-slate-50',
            ]"
            @click="handleSortClick(opt)"
          >
            <i :class="[opt.icon, 'text-[10px]']" aria-hidden="true"></i>
            <span>{{ opt.label }}</span>
          </button>
        </div>
      </Transition>
    </section>
  </div>
</template>

<style scoped>
.desktop-sort-expand-enter-active,
.desktop-sort-expand-leave-active {
  overflow: hidden;
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.desktop-sort-expand-enter-from,
.desktop-sort-expand-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
