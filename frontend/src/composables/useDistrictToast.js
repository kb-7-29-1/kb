import { ref } from 'vue';

const toastMessage = ref('');
let toastTimeout = null;

export function useDistrictToast() {
  const showToast = (msg = '선택하신 자치구는 보안등 공공데이터가 구축되지 않아 안전 점수가 제공되지 않습니다.', duration = 5000) => {
    toastMessage.value = msg;
    if (toastTimeout) clearTimeout(toastTimeout);
    toastTimeout = setTimeout(() => {
      toastMessage.value = '';
    }, duration);
  };

  const hideToast = () => {
    if (toastTimeout) clearTimeout(toastTimeout);
    toastMessage.value = '';
  };

  return {
    toastMessage,
    showToast,
    hideToast,
  };
}
