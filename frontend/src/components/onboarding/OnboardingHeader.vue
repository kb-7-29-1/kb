<script setup>
defineProps({
  currentStep: {
    type: Number,
    required: true,
  },
});
</script>

<template>
  <header>
    <h1>나에게 맞는 매물 찾기</h1>
    <span>{{ currentStep }} / 5</span>
  </header>
</template>
