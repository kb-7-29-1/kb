<script setup>
defineProps({
  currentStep: {
    type: Number,
    required: true,
  },
});

defineEmits(['previous', 'next']);
</script>

<template>
  <nav>
    <a v-if="currentStep > 1" href="#" @click.prevent="$emit('previous')"> 이전 </a>
    <a v-if="currentStep < 5" href="#" @click.prevent="$emit('next')"> 다음 </a>
    <a v-else href="#" @click.prevent> 완료 </a>
  </nav>
</template>
