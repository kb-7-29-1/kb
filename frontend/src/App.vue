<script setup>
import { RouterView } from 'vue-router';
import { hasFatalError } from '@/utils/globalError.js';
import ErrorFallback from '@/components/common/ErrorFallback.vue';
</script>

<template>
  <ErrorFallback v-if="hasFatalError" />
  <RouterView v-else />
</template>
