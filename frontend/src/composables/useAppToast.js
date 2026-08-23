import { ref } from 'vue';

export const toastState = ref({
  visible: false,
  message: '',
  type: 'info', // 'info' | 'success' | 'warning' | 'error'
  icon: '',
});

let toastTimer = null;

export const showToast = (message, { type = 'info', duration = 3000, icon = '' } = {}) => {
  let defaultIcon = '💡';
  if (type === 'success') defaultIcon = '✅';
  if (type === 'warning') defaultIcon = '⚠️';
  if (type === 'error') defaultIcon = '🚨';

  toastState.value = {
    visible: true,
    message,
    type,
    icon: icon || defaultIcon,
  };

  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toastState.value.visible = false;
  }, duration);
};

export const hideToast = () => {
  if (toastTimer) clearTimeout(toastTimer);
  toastState.value.visible = false;
};

if (typeof window !== 'undefined') {
  window.showToast = showToast;
}

export function useAppToast() {
  return {
    toastState,
    showToast,
    hideToast,
  };
}
