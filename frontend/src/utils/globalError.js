import { ref } from 'vue';

export const hasFatalError = ref(false);

export const reportFatalError = (error, context) => {
    console.error(`[GLOBAL ERROR] ${context}:`, error);
    hasFatalError.value = true;
};