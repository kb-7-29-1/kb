<script setup>
import { computed, nextTick, ref, watch } from 'vue';
import { formatDeposit } from '@/utils/priceFormatter';
import WalkingTime from '@/components/property/WalkingTime.vue';
import CommentSection from '@/components/detail/CommentSection.vue';
import SafetyModal from '@/components/detail/SafetyModal.vue';
import { useAuthStore } from '@/stores/useAuthStore.js';
import api from '@/api/api.js';
import safetyService from '@/api/safetyService.js';
import { getBankLogoUrl } from '@/utils/bankLogo';
import { getBankLinkUrl } from '@/utils/bankLink';

const authStore = useAuthStore();

const age = computed(() => {
  const birthDate = authStore.user?.birthDate;
  if (!birthDate) return null;
  return new Date().getFullYear() - new Date(birthDate).getFullYear() + 1;
});

const loanList = ref([]);
const loanListLoading = ref(false);
const isLoanOpen = ref(false);
const isSafetyModalOpen = ref(false);
const safetyDetails = ref(null);
const isSafetyDetailsLoading = ref(false);
const isImagePreviewOpen = ref(false);

const props = defineProps({
  isOpen: {
    type: Boolean,
    default: false,
  },
  isInline: {
    type: Boolean,
    default: false,
  },
  property: {
    type: Object,
    default: null,
  },
  amenities: {
    type: Array,
    default: () => [],
  },
  destination: {
    type: Object,
    default: null,
  },
  isBookmarkPending: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['close', 'toggle-bookmark']);

const detailScrollRef = ref(null);
const detailSessionKey = ref(0);
const isAgeInfoPopoverOpen = ref(false);
const showShareToast = ref(false);
let shareToastTimer = null;

const handleCopyShareLink = () => {
  if (!navigator.clipboard) return;
  const currentUrl = window.location.href;
  navigator.clipboard.writeText(currentUrl).then(() => {
    showShareToast.value = true;
    clearTimeout(shareToastTimer);
    shareToastTimer = setTimeout(() => {
      showShareToast.value = false;
    }, 2500);
  });
};

const resetDetailView = async () => {
  isLoanOpen.value = false;
  isSafetyModalOpen.value = false;
  isImagePreviewOpen.value = false;
  safetyDetails.value = null;
  isSafetyDetailsLoading.value = false;
  detailSessionKey.value += 1;
  await nextTick();
  detailScrollRef.value?.scrollTo({ top: 0 });
};

watch(
  () => props.isOpen,
  (isOpen, wasOpen) => {
    if (!isOpen || wasOpen) return;
    resetDetailView();
  },
);

// 가격 포맷팅
const depositLabel = computed(() => {
  if (!props.property) return '';
  return `${formatDeposit(props.property.deposit)}원`;
});

const hasSafetyScore = computed(() => {
  const value = props.property?.safetyScore;
  return value !== null && value !== undefined && value !== '' && Number.isFinite(Number(value));
});

// 안전점수 색상
const safetyScoreClass = computed(() => {
  if (!hasSafetyScore.value) return 'bg-slate-100 text-slate-500';
  const score = Number(props.property.safetyScore);
  if (score >= 80) return 'bg-emerald-500/10 text-emerald-600';
  if (score >= 60) return 'bg-amber-500/10 text-amber-600';
  return 'bg-rose-500/10 text-rose-600';
});

const safetyScoreValue = computed(() => {
  if (!hasSafetyScore.value) return 0;
  return Math.min(Math.max(Number(props.property.safetyScore), 0), 100);
});

const safetyScoreEndPoint = computed(() => {
  const radians = (safetyScoreValue.value / 100) * Math.PI * 2;
  return {
    x: 36 + 30 * Math.cos(radians),
    y: 36 + 30 * Math.sin(radians),
  };
});

const safetyGradeLabel = computed(() => {
  if (!hasSafetyScore.value) return '계산되지 않음';
  if (safetyScoreValue.value >= 80) return '안심';
  if (safetyScoreValue.value >= 60) return '보통';
  return '주의 필요';
});

const safetyReport = computed(() => {
  if (!hasSafetyScore.value) {
    return {
      tone: 'pending',
      color: '#94a3b8',
    };
  }

  if (safetyScoreValue.value >= 80) {
    return {
      tone: 'safe',
      color: '#22a06b',
    };
  }

  if (safetyScoreValue.value >= 60) {
    return {
      tone: 'caution',
      color: '#e69a1d',
    };
  }

  return {
    tone: 'warning',
    color: '#e25858',
  };
});

const buildingAge = computed(() => {
  const builtYear = parseInt(props.property?.builtYear || '2022', 10);
  if (!Number.isInteger(builtYear) || builtYear <= 0) return null;
  return Math.max(new Date().getFullYear() - builtYear, 0);
});

// 4단계 세부 건물 연차 분류 (신축 / 준신축 / 구축 / 노후)
const buildingAgeCategory = computed(() => {
  if (buildingAge.value === null) {
    return {
      label: '정보 없음',
      class: 'text-slate-400',
      desc: '건축물대장 준공 연도 데이터가 없습니다.',
    };
  }
  const age = buildingAge.value;
  if (age <= 5) {
    return {
      label: '✨ 신축 (5년 이내)',
      class: 'text-emerald-600',
      desc: '준공 5년 이내 건물입니다 (하자보수 및 관리가 우수함)',
    };
  }
  if (age <= 10) {
    return {
      label: '🏢 준신축 (10년 이내)',
      class: 'text-blue-600',
      desc: '준공 5년 초과~10년 이내 건물입니다 (보존 상태 양호)',
    };
  }
  if (age <= 20) {
    return {
      label: '🏠 구축 (20년 이내)',
      class: 'text-indigo-600',
      desc: '준공 10년 초과~20년 이내 건물입니다 (일반적 구축 주택)',
    };
  }
  return {
    label: '🛠️ 노후 (20년 초과)',
    class: 'text-amber-600',
    desc: '준공 20년을 초과한 노후 건물입니다 (시설 수리 여부 확인 권장)',
  };
});

const formattedUseAprDay = computed(() => {
  const raw = props.property?.useAprDay;
  if (!raw) return null;
  const clean = String(raw).replace(/[^0-9]/g, '');
  if (clean.length === 8) {
    return `${clean.substring(0, 4)}.${clean.substring(4, 6)}.${clean.substring(6, 8)} 승인`;
  }
  return `${props.property?.builtYear || '2022'}년 승인`;
});

const fetchLoanList = async () => {
  if (!props.property) return;
  loanListLoading.value = true;
  try {
    const response = await api.get('/loan/property-recommend', {
      params: {
        deposit: props.property.deposit,
        monthlyRent: props.property.monthlyRent,
        age: age.value,
      },
    });
    loanList.value = response.data;
  } catch (error) {
    loanList.value = [];
  } finally {
    loanListLoading.value = false;
  }
};

const openBankLink = (companyName) => {
  const url = getBankLinkUrl(companyName);
  if (url) {
    window.open(url, '_blank', 'noopener,noreferrer');
  }
};

const openSafetyModal = async () => {
  isSafetyModalOpen.value = true;
  safetyDetails.value = null;
  if (!props.property || !props.destination?.lat || !props.destination?.lng) return;

  isSafetyDetailsLoading.value = true;
  try {
    safetyDetails.value = await safetyService.getSafetyDetails({
      propertyId: props.property.propertyId,
      propertyName: props.property.title || props.property.address,
      destinationId: props.destination.id || null,
      destinationName: props.destination.name,
      destinationAddress: props.destination.address,
      destinationLatitude: props.destination.lat,
      destinationLongitude: props.destination.lng,
    });
  } catch (error) {
    console.error('SAFETY DETAIL CALCULATION ERROR:', error);
  } finally {
    isSafetyDetailsLoading.value = false;
  }
};

watch(
  () => props.property?.propertyId,
  (propertyId) => {
    if (!propertyId) return;
    resetDetailView();
    fetchLoanList();
  },
  { immediate: true },
);

const SAMPLE_PROPERTY_IMAGES = [
  'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1493809842364-78817add7ffb?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=800&q=80',
];

const detailImageUrl = computed(() => {
  if (props.property?.imageUrls?.length) return props.property.imageUrls[0];
  if (props.property?.thumbnailUrl) return props.property.thumbnailUrl;
  const idx = Math.abs(Number(props.property?.propertyId || 0)) % SAMPLE_PROPERTY_IMAGES.length;
  return SAMPLE_PROPERTY_IMAGES[idx];
});
</script>

<template>
  <div class="contents">
    <div
      v-if="isImagePreviewOpen && property"
      class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/75 p-5 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-label="매물 이미지 크게 보기"
      @click.self="isImagePreviewOpen = false"
    >
      <button
        type="button"
        class="absolute right-5 top-5 inline-flex h-10 w-10 items-center justify-center rounded-full bg-white/15 text-xl leading-none text-white transition hover:bg-white/25"
        aria-label="이미지 크게 보기 닫기"
        @click="isImagePreviewOpen = false"
      >
        ×
      </button>
      <img
        :src="detailImageUrl"
        :alt="`${property.title || property.address} 매물 이미지`"
        class="max-h-full max-w-full rounded-2xl object-contain shadow-2xl"
      />
    </div>

    <!-- 🎉 공유 링크 복사 완료 토스트 알림 배너 -->
    <Transition name="toast">
      <div
        v-if="showShareToast"
        class="fixed top-16 right-12 z-50 bg-slate-900/95 text-white px-4 py-2.5 rounded-2xl text-xs font-extrabold shadow-2xl backdrop-blur-md flex items-center gap-2 border border-slate-700 pointer-events-none whitespace-nowrap animate-bounce"
      >
        <span class="text-sm">🔗</span>
        <span>매물 전용 링크가 복사되었어요! 이웃에게 공유해보세요.</span>
      </div>
    </Transition>

    <!-- Backdrop Overlay 제거 (지도 화면 위 회색 잔여 오버레이 발생 방지) -->

    <!-- 380px Slide-Over Panel (PC 고정 / 모바일 인라인 지원) -->
    <component
      v-if="isOpen || isInline"
      :is="isInline ? 'div' : 'aside'"
      class="property-detail-panel bg-white flex flex-col transition-transform duration-300 ease-in-out"
      :class="[
        isInline
          ? 'w-full h-full'
          : 'hidden xl:flex fixed right-0 bottom-0 w-full sm:w-[380px] z-40 shadow-2xl border-l border-slate-200',
        !isInline &&
          (isOpen
            ? 'translate-x-0 opacity-100 pointer-events-auto'
            : 'translate-x-full opacity-0 pointer-events-none invisible'),
      ]"
    >
      <!-- 🚪 PC 전용 좌측 외곽 길고 슬림한 패널 접기/닫기 토글 버튼 -->
      <button
        v-if="!isInline && isOpen"
        type="button"
        class="group absolute top-1/2 -left-6 -translate-y-1/2 flex h-24 w-6 items-center justify-center rounded-l-xl border-l border-t border-b border-slate-200/90 bg-white/95 text-slate-500 shadow-lg transition-all duration-200 hover:bg-slate-50 hover:text-blue-600 active:scale-95 cursor-pointer backdrop-blur-md z-50"
        title="상세 정보 창 접기"
        @click="emit('close')"
      >
        <i
          class="fa-solid fa-chevron-right text-xs transition-transform duration-200 group-hover:translate-x-0.5"
          aria-hidden="true"
        ></i>
      </button>
      <!-- 패널 상단 헤더 -->
      <div
        class="min-h-[68px] px-6 pt-4 pb-2.5 flex items-center justify-between gap-3 bg-white shrink-0"
      >
        <div v-if="property" class="min-w-0">
          <div class="mb-1.5 flex items-center gap-1.5">
            <span
              class="inline-flex items-center rounded-md bg-[#eef1ff] px-2 py-1 text-[11px] font-bold text-[#4767f7]"
            >
              {{ property.buildingType === 3 ? '오피스텔' : '빌라/연립' }}
            </span>
            <span
              class="inline-flex items-center gap-1 rounded-md px-2 py-1 text-[11px] font-bold"
              :class="safetyScoreClass"
            >
              <i class="fa-solid fa-shield-halved text-[10px]" aria-hidden="true"></i>
              {{ hasSafetyScore ? `${property.safetyScore}점` : '점수 없음' }}
            </span>
          </div>
          <p class="truncate text-[15px] font-bold text-slate-800">
            {{ property.address }}
          </p>
        </div>

        <h2 class="hidden font-bold text-lg text-slate-900 flex items-center gap-2">
          <span>🏠</span>
          <span>매물 상세 리포트</span>
        </h2>

        <div class="flex shrink-0 items-center gap-0.5">
          <!-- 찜 버튼 (하트) -->
          <button
            type="button"
            class="inline-flex h-9 w-9 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-[#dc4b5d] disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="isBookmarkPending"
            @click="property && emit('toggle-bookmark', property.propertyId)"
          >
            <svg
              viewBox="0 0 24 24"
              class="h-5 w-5 transition-colors"
              :class="property?.isBookmarked ? 'fill-[#dc4b5d] text-[#dc4b5d]' : 'fill-none'"
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

          <!-- 🔗 매물 URL 주소 복사/공유 버튼 -->
          <button
            type="button"
            class="inline-flex h-9 w-9 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-[#4058f5] active:scale-95"
            title="매물 주소 공유하기 (링크 복사)"
            @click="handleCopyShareLink"
          >
            <svg
              class="h-5 w-5"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"></path>
              <polyline points="16 6 12 2 8 6"></polyline>
              <line x1="12" y1="2" x2="12" y2="15"></line>
            </svg>
          </button>

          <!-- 닫기 버튼 -->
          <button
            type="button"
            class="inline-flex h-9 w-9 items-center justify-center rounded-full text-[19px] font-normal leading-none text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700"
            @click="emit('close')"
          >
            ✕
          </button>
        </div>
      </div>

      <!-- 패널 메인 스크롤 콘텐츠 -->
      <div
        v-if="property"
        ref="detailScrollRef"
        class="property-detail-scroll min-h-0 flex-1 overflow-x-hidden overflow-y-auto"
      >
        <div class="flex min-h-full flex-col gap-4 p-4 py-0">
          <!-- 매물 대표 사진 + 보증금/월세/평수/층수 -->
          <div class="flex gap-4">
            <button
              type="button"
              class="group relative h-28 w-28 flex-shrink-0 overflow-hidden rounded-2xl border border-slate-200 bg-slate-100 text-left"
              aria-label="매물 이미지 크게 보기"
              @click="isImagePreviewOpen = true"
            >
              <img
                :src="detailImageUrl"
                :alt="property.title"
                class="h-full w-full object-cover transition duration-200 group-hover:scale-105"
              />
              <span
                class="absolute inset-0 flex items-center justify-center bg-slate-900/0 text-sm text-white opacity-0 transition group-hover:bg-slate-900/30 group-hover:opacity-100"
              >
                <i class="fa-solid fa-magnifying-glass-plus" aria-hidden="true"></i>
              </span>
            </button>

            <div class="flex flex-1 flex-col justify-center gap-1.5">
              <div class="flex items-baseline gap-1.5">
                <span class="text-[13px] font-medium text-slate-500">보증금</span>
                <span class="text-[16px] font-extrabold text-slate-800">{{ depositLabel }}</span>
              </div>
              <div v-if="property.monthlyRent" class="flex items-baseline gap-1.5">
                <span class="text-[13px] font-medium text-slate-500">월세</span>
                <span class="text-[16px] font-extrabold text-slate-800"
                  >{{ property.monthlyRent }}만원</span
                >
              </div>
              <p class="text-[13px] font-medium text-slate-500">
                {{ property.area || 24.5 }}m² · {{ property.floor || 3 }}층
              </p>
            </div>
          </div>

          <!-- 건물 안전 정보 및 대장 상세 (울트라 슬림 레이아웃) -->
          <section class="border-t border-slate-200/80 pt-3">
            <h3 class="mb-2 flex items-center gap-1.5 text-[13px] font-bold text-slate-800">
              <span aria-hidden="true">🏢</span>
              건물 정보 및 대장 안전
            </h3>
            <div class="grid grid-cols-2 gap-1.5">
              <div
                class="flex min-h-[64px] items-center gap-2.5 rounded-xl border px-3 py-1.5"
                :class="
                  property.isIllegalBuilding
                    ? 'border-rose-200 bg-rose-50/70'
                    : 'border-emerald-200 bg-emerald-50/50'
                "
              >
                <i
                  class="fa-solid text-[16px] shrink-0"
                  :class="
                    property.isIllegalBuilding
                      ? 'fa-triangle-exclamation text-rose-500'
                      : 'fa-circle-check text-emerald-500'
                  "
                  aria-hidden="true"
                ></i>
                <div class="min-w-0 flex-1">
                  <p
                    class="text-[12px] font-extrabold leading-tight"
                    :class="property.isIllegalBuilding ? 'text-rose-600' : 'text-emerald-700'"
                  >
                    {{ property.isIllegalBuilding ? '위반 건물' : '적법 건물' }}
                  </p>
                  <p class="text-[9.5px] font-medium leading-tight text-slate-500 truncate mt-0.5">
                    {{
                      property.illegalReason ||
                      (property.isIllegalBuilding ? '위반건축물 지정 이력' : '건축물대장 기준 적법')
                    }}
                  </p>
                </div>
              </div>
              <div
                class="relative flex min-h-[64px] flex-col items-center justify-center rounded-xl border border-slate-200 bg-slate-50/80 px-2 py-1.5 text-center"
              >
                <span class="text-[12px] font-bold text-slate-800 leading-tight whitespace-nowrap">
                  {{ formattedUseAprDay || `${property.builtYear}년 준공` }}
                </span>
                <div class="group relative mt-0 inline-block">
                  <span
                    class="inline-flex items-center gap-1 text-[9.5px] font-bold px-2 py-0.5 rounded-md bg-white border border-slate-200/90 shadow-2xs whitespace-nowrap cursor-help transition-all hover:border-slate-300"
                    :class="buildingAgeCategory.class"
                  >
                    <span>{{ buildingAgeCategory.label }}</span>
                    <i
                      class="fa-solid fa-circle-info text-[8.5px] opacity-70 group-hover:opacity-100"
                      aria-hidden="true"
                    ></i>
                  </span>

                  <!-- 🤍 화이트 테마 마우스 호버(Hover) 4단계 연식 기준 안내 팝오버 -->
                  <div
                    class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-48 rounded-xl bg-white p-2.5 text-slate-800 shadow-xl border border-slate-200 z-50 text-[10px] space-y-1.5 leading-tight text-left opacity-0 group-hover:opacity-100 invisible group-hover:visible transition-all duration-200 pointer-events-none"
                  >
                    <div
                      class="font-extrabold text-blue-600 pb-1 border-b border-slate-100 text-[10.5px]"
                    >
                      🏠 건물 연식 분류 기준
                    </div>
                    <div
                      class="flex justify-between items-center py-0.5"
                      :class="{
                        'font-black text-emerald-600 bg-emerald-50/80 px-1 rounded':
                          buildingAge !== null && buildingAge <= 5,
                      }"
                    >
                      <span>✨ 신축</span>
                      <span class="text-slate-500 font-medium">5년 이내</span>
                    </div>
                    <div
                      class="flex justify-between items-center py-0.5"
                      :class="{
                        'font-black text-blue-600 bg-blue-50/80 px-1 rounded':
                          buildingAge > 5 && buildingAge <= 10,
                      }"
                    >
                      <span>🏢 준신축</span>
                      <span class="text-slate-500 font-medium">10년 이내 (5~10년)</span>
                    </div>
                    <div
                      class="flex justify-between items-center py-0.5"
                      :class="{
                        'font-black text-indigo-600 bg-indigo-50/80 px-1 rounded':
                          buildingAge > 10 && buildingAge <= 20,
                      }"
                    >
                      <span>🏠 구축</span>
                      <span class="text-slate-500 font-medium">20년 이내 (10~20년)</span>
                    </div>
                    <div
                      class="flex justify-between items-center py-0.5"
                      :class="{
                        'font-black text-amber-600 bg-amber-50/80 px-1 rounded': buildingAge > 20,
                      }"
                    >
                      <span>🛠️ 노후</span>
                      <span class="text-slate-500 font-medium">20년 초과</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 건축물대장 상세 3대 지표 (구조 / 주용도 / 내진설계) -->
            <div class="mt-1.5 grid grid-cols-3 gap-1.5">
              <div
                class="rounded-lg border border-slate-100 bg-slate-50/60 py-1 px-1.5 text-center"
              >
                <span class="block text-[9.5px] font-medium text-slate-400">구조</span>
                <strong class="block truncate text-[10.5px] font-bold text-slate-700">
                  {{ property.structureName || '철근콘크리트' }}
                </strong>
              </div>
              <div
                class="rounded-lg border border-slate-100 bg-slate-50/60 py-1 px-1.5 text-center"
              >
                <span class="block text-[9.5px] font-medium text-slate-400">용도</span>
                <strong class="block truncate text-[10.5px] font-bold text-slate-700">
                  {{
                    property.mainPurposeName ||
                    (property.buildingType === 3 ? '오피스텔' : '공동주택')
                  }}
                </strong>
              </div>
              <div
                class="rounded-lg border border-slate-100 bg-slate-50/60 py-1 px-1.5 text-center"
              >
                <span class="block text-[9.5px] font-medium text-slate-400">내진</span>
                <strong
                  class="block truncate text-[10.5px] font-bold"
                  :class="
                    property.earthquakeProofYn === '1' ? 'text-emerald-600' : 'text-slate-600'
                  "
                >
                  {{ property.earthquakeProofYn === '1' ? '적용 🟢' : '미적용 ⚪' }}
                </strong>
              </div>
            </div>
          </section>

          <!-- 🛡️ 안심 귀갓길 & 안전 지표 리포트 -->
          <section class="border-t border-slate-200 pt-4">
            <!-- 타이틀과 버튼을 양옆으로 배치 (flex justify-between) -->
            <div class="mb-3 flex items-start justify-between">
              <h3 class="flex items-center gap-1.5 text-[15px] font-bold text-slate-800 mt-1">
                <span aria-hidden="true">💡</span>
                귀갓길 안전 점수
              </h3>

              <!-- image_56c273.png 스타일의 모달 오픈 버튼 -->
              <button
                type="button"
                class="rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-left text-[12px] leading-tight text-slate-500 transition-colors hover:bg-slate-50"
                @click="openSafetyModal"
              >
                <span class="block">안전 점수는 어떻게 산출되나요?</span>
              </button>
            </div>

            <div
              class="safety-report-card"
              :class="`is-${safetyReport.tone}`"
              :style="{
                '--score-color': safetyReport.color,
              }"
            >
              <div class="safety-report-summary">
                <div class="safety-score-chart">
                  <svg class="safety-score-chart__svg" viewBox="0 0 72 72" aria-hidden="true">
                    <circle class="safety-score-chart__track" cx="36" cy="36" r="30" />
                    <circle
                      class="safety-score-chart__progress"
                      cx="36"
                      cy="36"
                      r="30"
                      :stroke="safetyReport.color"
                      :stroke-dasharray="`${safetyScoreValue * 1.885} 188.5`"
                      stroke-linecap="round"
                    />
                    <circle
                      v-if="hasSafetyScore && safetyScoreValue > 0"
                      class="safety-score-chart__cap"
                      cx="66"
                      cy="36"
                      r="3"
                      :fill="safetyReport.color"
                    />
                    <circle
                      v-if="hasSafetyScore && safetyScoreValue > 0"
                      class="safety-score-chart__cap"
                      :cx="safetyScoreEndPoint.x"
                      :cy="safetyScoreEndPoint.y"
                      r="3"
                      :fill="safetyReport.color"
                    />
                  </svg>
                  <div class="safety-score-chart__inner">
                    <strong>{{ hasSafetyScore ? safetyScoreValue : '-' }}</strong>
                    <span>/ 100</span>
                  </div>
                </div>

                <div class="min-w-0 flex-1">
                  <div class="mb-1 flex items-center gap-2">
                    <span class="safety-grade-tag">
                      <i class="fa-solid fa-shield-halved" aria-hidden="true"></i>
                      {{ safetyGradeLabel }}
                    </span>
                  </div>
                  <p class="text-[12px] leading-5 text-slate-500">
                    주변 안전 시설을 종합해 산출한 귀갓길 점수예요.
                  </p>
                </div>
              </div>

              <div class="safety-metric-grid">
                <div class="safety-metric-card safety-metric-card--cctv">
                  <i class="fa-solid fa-video" aria-hidden="true"></i>
                  <span>CCTV</span>
                  <strong>{{ property.cctvCount || 0 }}개</strong>
                </div>
                <div class="safety-metric-card safety-metric-card--light">
                  <i class="fa-solid fa-lightbulb" aria-hidden="true"></i>
                  <span>가로등</span>
                  <strong
                    >{{ property.streetLampCount ?? property.streetlightCount ?? 0 }}개</strong
                  >
                </div>
                <div class="safety-metric-card safety-metric-card--police">
                  <i class="fa-solid fa-user-shield" aria-hidden="true"></i>
                  <span>파출소</span>
                  <strong>{{ property.hasPoliceStation ? '근처' : '확인 필요' }}</strong>
                </div>
              </div>
            </div>
          </section>

          <WalkingTime
            :key="detailSessionKey"
            class="detail-section-flush detail-section-divider"
            :amenities="amenities"
          />
          <section class="finance-section border-t border-slate-200 pt-3">
            <div class="finance-section-header" @click="isLoanOpen = !isLoanOpen">
              <div class="finance-section-title">
                <span aria-hidden="true">🏦</span>
                추천 금융 상품
              </div>
              <button
                type="button"
                class="finance-toggle-button"
                :aria-expanded="isLoanOpen"
                aria-label="맞춤 금융 상품 펼치기"
                @click.stop="isLoanOpen = !isLoanOpen"
              >
                <svg class="finance-toggle-icon" viewBox="0 0 24 24" aria-hidden="true">
                  <path :d="isLoanOpen ? 'M6 15l6-6 6 6' : 'M6 9l6 6 6-6'" />
                </svg>
              </button>
            </div>

            <div v-show="isLoanOpen">
              <p v-if="loanList.length > 0" class="finance-section-hint">
                상품 클릭 시 해당 은행 사이트로 이동합니다
              </p>
              <div v-if="loanListLoading" class="text-gray-400 text-sm text-center py-8">
                상품을 찾고 있어요...
              </div>
              <div v-else-if="loanList.length === 0" class="text-gray-400 text-sm text-center py-8">
                추천 가능한 대출 상품이 없습니다.
              </div>
              <div v-else class="loan-scroll-list overflow-y-auto space-y-3 pr-1">
                <div
                  v-for="item in loanList"
                  :key="item.productName"
                  class="loan-item"
                  :class="{
                    'loan-item--clickable': getBankLinkUrl(item.companyName),
                  }"
                  :role="getBankLinkUrl(item.companyName) ? 'button' : undefined"
                  :tabindex="getBankLinkUrl(item.companyName) ? 0 : undefined"
                  @click="openBankLink(item.companyName)"
                  @keydown.enter="openBankLink(item.companyName)"
                >
                  <div class="loan-item__header">
                    <img
                      v-if="getBankLogoUrl(item.companyName)"
                      :src="getBankLogoUrl(item.companyName)"
                      :alt="item.companyName"
                      class="loan-item__logo"
                    />
                    <span class="loan-bank-tag">{{ item.companyName }}</span>
                  </div>
                  <p class="loan-item__name">{{ item.productName }}</p>
                  <p class="loan-item__details">{{ item.rateInfo }} · {{ item.loanLimit }}</p>
                </div>
              </div>
            </div>
          </section>
          <section class="detail-community-section">
            <CommentSection
              class="detail-section-flush"
              :property-id="property.propertyId"
              :property="property"
            />
          </section>
        </div>
      </div>
    </component>

    <SafetyModal
      v-if="isSafetyModalOpen"
      :property="property"
      :safety-breakdown="safetyDetails?.selectedRoute?.breakdown"
      :safety-route="safetyDetails?.selectedRoute"
      :is-calculating="isSafetyDetailsLoading"
      @close="isSafetyModalOpen = false"
    />
  </div>
</template>

<style scoped>
.property-detail-panel {
  top: var(--app-header-height, 56px) !important;
}

:deep(.detail-section-divider) {
  border-top: 1px solid #e2e8f0;
  padding-top: 12px !important;
}

:deep(.walking-time.detail-section-flush) {
  padding-left: 0 !important;
  padding-right: 0 !important;
  padding-bottom: 0 !important;
}

.detail-community-section {
  width: calc(100% + 24px);
  margin: 0 -12px;
  padding: 14px 12px 16px;
  border-top: 1px solid #e2e8f0;
  background: #f5f7fb;
}

@media (min-width: 1280px) {
  .property-detail-backdrop {
    display: none;
  }

  .detail-community-section {
    margin-top: auto;
  }

  .property-detail-scroll {
    scrollbar-gutter: stable both-edges;
    scrollbar-width: thin;
    scrollbar-color: #d7deea transparent;
  }

  .property-detail-scroll::-webkit-scrollbar {
    width: 5px;
  }

  .property-detail-scroll::-webkit-scrollbar-thumb {
    border-radius: 999px;
    background: #d7deea;
  }
}

:deep(.comment-section.detail-section-flush) {
  border-top: 0 !important;
}

.safety-report-card {
  padding: 0 14px;
  padding-top: 0;
}

.safety-report-summary {
  display: flex;
  align-items: center;
  gap: 14px;
}

.safety-score-chart {
  position: relative;
  display: grid;
  width: 78px;
  height: 78px;
  flex: 0 0 auto;
  place-items: center;
}

.safety-score-chart__svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  overflow: visible;
  transform: rotate(-90deg);
}

.safety-score-chart__track,
.safety-score-chart__progress {
  fill: none;
  stroke-width: 6;
}

.safety-score-chart__track {
  stroke: #e6ebf3;
}

.safety-score-chart__progress {
  stroke-linecap: round;
}

.safety-score-chart__cap {
  pointer-events: none;
}

.safety-score-chart__inner {
  position: relative;
  z-index: 1;
  display: flex;
  width: 56px;
  height: 56px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 4px rgb(15 23 42 / 4%);
}

.safety-score-chart__inner strong {
  color: var(--score-color);
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
}

.safety-score-chart__inner span {
  margin-top: 3px;
  color: #94a3b8;
  font-size: 9px;
  font-weight: 700;
}

.safety-grade-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 22px;
  padding: 0 8px;
  border: 1px solid color-mix(in srgb, var(--score-color) 34%, white);
  border-radius: 999px;
  background: color-mix(in srgb, var(--score-color) 11%, white);
  color: var(--score-color);
  font-size: 11px;
  font-weight: 700;
}

.safety-grade-tag i {
  font-size: 10px;
}

.safety-metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.safety-metric-card {
  display: flex;
  min-width: 0;
  min-height: 76px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  border-radius: 12px;
  text-align: center;
}

.safety-metric-card i {
  margin-bottom: 5px;
  font-size: 13px;
}

.safety-metric-card span {
  color: #8b95a7;
  font-size: 10px;
  font-weight: 600;
}

.safety-metric-card strong {
  max-width: 100%;
  margin-top: 2px;
  overflow: hidden;
  color: #1e293b;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.safety-metric-card--cctv {
  border-color: #dce5ff;
  background: #f1f5ff;
}

.safety-metric-card--cctv i {
  color: #4f64ff;
}

.safety-metric-card--light {
  border-color: #f7e4bd;
  background: #fffaed;
}

.safety-metric-card--light i {
  color: #e69a1d;
}

.safety-metric-card--police {
  border-color: #ccecdc;
  background: #effaf4;
}

.safety-metric-card--police i {
  color: #22a06b;
}

.safety-report-card.is-safe {
  border-color: #c9eadb;
}

.safety-report-card.is-caution {
  border-color: #f5dfb7;
}

.safety-report-card.is-warning {
  border-color: #f4cccc;
}

.finance-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 48px;
  cursor: pointer;
}

.finance-section-hint {
  margin: 0 0 8px;
  color: #94a3b8;
  font-size: 11px;
}

.finance-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #1e293b;
  font-size: 15px;
  font-weight: 700;
}

.finance-section-title > span:last-child {
  line-height: 1.2;
}

.finance-section-title > span:first-child {
  font-size: 16px;
}

.finance-toggle-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #222;
  cursor: pointer;
}

.finance-toggle-icon {
  display: block;
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2.5;
}

.loan-scroll-list {
  max-height: 210px;
  padding: 2px;
}

.loan-item {
  box-sizing: border-box;
  padding: 12px;
  border: 1px solid #e3e9f5;
  border-radius: 14px;
  background: #f7f9fe;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease,
    border-color 0.15s ease;
}

.loan-item--clickable {
  cursor: pointer;
}

.loan-item--clickable:hover {
  transform: translateY(-1px);
  border-color: #c7d2f5;
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.06);
}

.loan-item__header {
  display: flex;
  align-items: center;
  gap: 6px;
}

.loan-item__logo {
  width: 20px;
  height: 20px;
  object-fit: contain;
  border-radius: 4px;
  background: #fff;
}

.loan-bank-tag {
  display: inline-flex;
  align-items: center;
  min-height: 19px;
  padding: 0 6px;
  border-radius: 5px;
  background: #eef1ff;
  color: #4767f7;
  font-size: 10px;
  font-weight: 700;
}

.loan-item__name {
  margin: 5px 0 0;
  color: #1e293b;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.loan-item__details {
  margin: 4px 0 0;
  color: #8b95a7;
  font-size: 11px;
}
</style>
