<script setup>
import { computed } from 'vue';

const props = defineProps({
  property: {
    type: Object,
    required: true,
  },
  isSelected: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['select', 'toggle-bookmark']);

// 건물 종류 텍스트 변환
const buildingTypeText = computed(() => {
  const types = { 1: '빌라/연립', 2: '다가구', 3: '오피스텔' };
  return types[props.property.buildingType] || props.property.propertyType || '주택';
});

// 방 종류 텍스트 변환
const roomTypeText = computed(() => {
  const rooms = { 1: '원룸', 2: '투룸' };
  return rooms[props.property.roomType] || '원룸';
});

const formatPriceAmount = (amount, rent = false) => {
  const value = Number(amount || 0);

  if (value >= 10000) {
    const eok = value / 10000;
    return {
      value: Number.isInteger(eok) ? String(eok) : eok.toFixed(1).replace(/\.0$/, ''),
      unit: rent ? '억' : '억원',
    };
  }

  return {
    value: value.toLocaleString(),
    unit: rent ? '만' : '만원',
  };
};

const formattedDeposit = computed(() => formatPriceAmount(props.property.deposit));
const formattedMonthlyRent = computed(() => formatPriceAmount(props.property.monthlyRent, true));
const isJeonse = computed(() => Number(props.property.monthlyRent || 0) === 0);

// 안전점수 색상 클래스
const safetyScoreClass = computed(() => {
  const score = props.property.safetyScore || 0;
  if (score >= 80) return 'bg-emerald-500/10 text-emerald-600';
  if (score >= 60) return 'bg-amber-500/10 text-amber-600';
  return 'bg-rose-500/10 text-rose-600';
});
</script>

<template>
  <div
    class="group relative flex items-center gap-3 p-3 rounded-xl bg-white border transition-all duration-200 cursor-pointer hover:shadow-md hover:-translate-y-0.5 md:p-3.5"
    :class="[
      isSelected
        ? 'border-blue-600 ring-2 ring-blue-500/20 bg-blue-50/30'
        : 'border-[#dbe3ef] hover:border-[#cbd6e6]',
    ]"
    @click="emit('select', property)"
  >
    <!-- 매물 썸네일 -->
    <div
      class="relative w-20 h-20 rounded-lg overflow-hidden bg-slate-100 shrink-0 md:w-28 md:h-28"
    >
      <img
        :src="
          property.thumbnailUrl ||
          'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=400&q=80'
        "
        :alt="property.title"
        class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
      />
    </div>

    <!-- 매물 정보 -->
    <div class="flex-1 flex flex-col justify-between min-w-0 pr-8 md:pr-9">
      <div>
        <div class="flex items-center gap-1.5 mb-1">
          <span
            class="inline-flex items-center rounded-md bg-[#eef1ff] px-1.5 py-0.5 text-[10px] font-bold text-[#4767f7] md:text-[11px]"
          >
            {{ buildingTypeText }} · {{ roomTypeText }}
          </span>
          <span
            class="inline-flex items-center gap-1 rounded-md px-1.5 py-0.5 text-[10px] font-bold md:text-[11px]"
            :class="safetyScoreClass"
          >
            <i class="fa-solid fa-shield-halved text-[9px]" aria-hidden="true"></i>
            {{ property.safetyScore || 85 }}점
          </span>
          <span
            v-if="property.dealCount && property.dealCount > 1"
            class="px-1.5 py-0.5 rounded text-[11px] font-bold bg-blue-50 text-blue-600 border border-blue-200"
          >
            🏢 거래 {{ property.dealCount }}건
          </span>
        </div>

        <h3
          class="flex items-baseline gap-1 whitespace-nowrap text-[15px] font-bold text-slate-900 md:text-base"
        >
          <template v-if="isJeonse">
            <span>전세</span>
            <span>{{ formattedDeposit.value }}</span>
            <span class="-ml-0.5 text-[11px] font-medium text-slate-400">{{
              formattedDeposit.unit
            }}</span>
          </template>
          <template v-else>
            <span>보증</span>
            <span>{{ formattedDeposit.value }}</span>
            <span class="-ml-0.5 text-[11px] font-medium text-slate-400">{{
              formattedDeposit.unit
            }}</span>
            <span class="mx-0 text-slate-300">/</span>
            <span>월</span>
            <span>{{ formattedMonthlyRent.value }}</span>
            <span class="-ml-0.5 text-[11px] font-medium text-slate-400">{{
              formattedMonthlyRent.unit
            }}</span>
          </template>
        </h3>

        <p class="text-xs text-slate-500 truncate mt-0.5">
          {{ property.title || property.address }}
        </p>
      </div>

      <div class="text-[10px] text-slate-500 mt-0.5 md:mt-2 md:text-[11px]">
        <span
          >{{ property.floor ? `${property.floor}층` : '3층' }} ·
          {{ property.area ? `${property.area}m²` : '24.5m²' }}</span
        >
      </div>
    </div>

    <!-- 찜 버튼 -->
    <button
      type="button"
      class="absolute right-2.5 top-2.5 inline-flex h-7 w-7 items-center justify-center border-0 bg-transparent p-0 transition-transform hover:scale-110 md:right-3 md:top-3"
      :aria-label="property.isBookmarked ? '관심 매물 해제' : '관심 매물 등록'"
      @click.stop="emit('toggle-bookmark', property.propertyId)"
    >
      <svg
        viewBox="0 0 24 24"
        class="h-[18px] w-[18px] transition-colors"
        :class="
          property.isBookmarked
            ? 'fill-[#dc4b5d] text-[#dc4b5d]'
            : 'fill-none text-[#94a3b8] hover:text-[#64748b]'
        "
        fill="none"
        stroke="currentColor"
        stroke-width="1.7"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <path
          d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78L12 21.23l8.84-8.84a5.5 5.5 0 0 0 0-7.78Z"
        />
      </svg>
    </button>
  </div>
</template>
