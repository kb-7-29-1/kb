<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [0, 100],
  },
  min: {
    type: Number,
    default: 0,
  },
  max: {
    type: Number,
    default: 100,
  },
  step: {
    type: Number,
    default: 1,
  },
});

const emit = defineEmits(['update:modelValue', 'change']);

const trackRef = ref(null);
let activeThumb = null; // 'min' | 'max'

const valMin = computed(() => Math.min(props.modelValue[0], props.modelValue[1]));
const valMax = computed(() => Math.max(props.modelValue[0], props.modelValue[1]));

const minPercent = computed(() => {
  if (props.max === props.min) return 0;
  return Math.max(0, Math.min(100, ((valMin.value - props.min) / (props.max - props.min)) * 100));
});

const maxPercent = computed(() => {
  if (props.max === props.min) return 100;
  return Math.max(0, Math.min(100, ((valMax.value - props.min) / (props.max - props.min)) * 100));
});

const handlePointerDown = (e) => {
  if (!trackRef.value) return;
  const rect = trackRef.value.getBoundingClientRect();
  if (!rect.width) return;

  const clickX = e.clientX - rect.left;
  const ratio = Math.max(0, Math.min(1, clickX / rect.width));
  const rawVal = props.min + ratio * (props.max - props.min);
  const steppedVal = Math.round(rawVal / props.step) * props.step;
  const clampedVal = Math.max(props.min, Math.min(props.max, steppedVal));

  const distMin = Math.abs(valMin.value - clampedVal);
  const distMax = Math.abs(valMax.value - clampedVal);

  let newMin = valMin.value;
  let newMax = valMax.value;

  if (distMin <= distMax) {
    newMin = clampedVal;
    activeThumb = 'min';
  } else {
    newMax = clampedVal;
    activeThumb = 'max';
  }

  emit('update:modelValue', [newMin, newMax]);
  emit('change', [newMin, newMax]);

  const handlePointerMove = (moveEvt) => {
    if (!activeThumb || !trackRef.value) return;
    const moveRect = trackRef.value.getBoundingClientRect();
    const moveX = moveEvt.clientX - moveRect.left;
    const moveRatio = Math.max(0, Math.min(1, moveX / moveRect.width));
    const moveRaw = props.min + moveRatio * (props.max - props.min);
    const moveStep = Math.round(moveRaw / props.step) * props.step;
    const moveClamp = Math.max(props.min, Math.min(props.max, moveStep));

    let updatedMin = props.modelValue[0];
    let updatedMax = props.modelValue[1];

    if (activeThumb === 'min') {
      updatedMin = moveClamp;
    } else {
      updatedMax = moveClamp;
    }

    emit('update:modelValue', [updatedMin, updatedMax]);
    emit('change', [updatedMin, updatedMax]);
  };

  const handlePointerUp = () => {
    activeThumb = null;
    window.removeEventListener('pointermove', handlePointerMove);
    window.removeEventListener('pointerup', handlePointerUp);
  };

  window.addEventListener('pointermove', handlePointerMove);
  window.addEventListener('pointerup', handlePointerUp);
};
</script>

<template>
  <div
    ref="trackRef"
    class="relative w-full h-7 flex items-center cursor-pointer select-none touch-none"
    @pointerdown="handlePointerDown"
  >
    <!-- 트랙 회색 배경 -->
    <div class="absolute inset-x-0 h-2 bg-slate-200 rounded-full pointer-events-none"></div>

    <!-- 파란색 활성 범위 트랙 -->
    <div
      class="absolute h-2 bg-blue-600 rounded-full pointer-events-none transition-all duration-75"
      :style="{
        left: `${minPercent}%`,
        width: `${maxPercent - minPercent}%`,
      }"
    ></div>

    <!-- 핸들 1 (Min) -->
    <div
      class="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 h-5 w-5 rounded-full bg-white border-2 border-blue-600 shadow-md pointer-events-none transition-transform hover:scale-125 z-20"
      :style="{ left: `${minPercent}%` }"
    ></div>

    <!-- 핸들 2 (Max) -->
    <div
      class="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 h-5 w-5 rounded-full bg-white border-2 border-blue-600 shadow-md pointer-events-none transition-transform hover:scale-125 z-20"
      :style="{ left: `${maxPercent}%` }"
    ></div>
  </div>
</template>
