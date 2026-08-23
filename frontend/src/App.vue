<script setup>
import { RouterView } from 'vue-router';
import { hasFatalError } from '@/utils/globalError.js';
import ErrorFallback from '@/components/common/ErrorFallback.vue';
import SessionExpiryModal from "@/components/common/SessionExpiryModal.vue";
import AppToast from '@/components/common/AppToast.vue';
import { useSessionExpiry } from '@/composables/useSessionExpiry.js';

const { showModal, remainingSeconds, extendSession, forceLogout } = useSessionExpiry();
</script>

<template>
  <ErrorFallback v-if="hasFatalError" />
  <RouterView v-else/>
  <AppToast />
  <SessionExpiryModal
      v-if="showModal"
      :remaining-seconds="remainingSeconds"
      @extend="extendSession"
      @logout="forceLogout"
  />
</template>
