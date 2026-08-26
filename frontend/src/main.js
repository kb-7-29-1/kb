import './assets/styles/main.css';
import '@fortawesome/fontawesome-free/css/all.min.css';

import { createApp } from 'vue';
import { createPinia } from 'pinia';
import { reportFatalError } from './utils/globalError.js';

import App from './App.vue';
import router from './router';
import {useAuthStore} from "@/stores/useAuthStore.js";
import { showToast } from '@/composables/useAppToast.js';

if (typeof window !== 'undefined') {
  window.showToast = showToast;
}

const app = createApp(App);
app.use(createPinia());
app.use(router);
app.config.errorHandler = (err, instance, info) => {
    reportFatalError(err, `Vue errorHandler (${info})`);
};
window.addEventListener('storage', (e) => {
    if (e.key !== 'token') return;
    const authStore = useAuthStore();
    if (e.newValue === null) {
        // 다른 탭에서 로그아웃(토큰 삭제) -> 이 탭도 로그아웃 처리
        authStore.clearAuthState();
        if (router.currentRoute.value.name !== 'login') {
            router.push({ name: 'login' });
        }
    } else if (e.newValue !== authStore.token) {
        // 다른 탭에서 세션 연장으로 토큰 갱신 -> 이 탭의 상태도 갱신(만료 타이머 재계산은
        // useSessionExpiry.js의 watch(authStore.token)이 반응형으로 처리)
        authStore.syncToken(e.newValue);
    }
});
window.addEventListener('unhandledrejection', (e) => reportFatalError(e.reason, 'unhandledrejection'));
window.addEventListener('error', (e) => reportFatalError(e.error || e.message, 'window.onerror'));
app.mount('#app');
