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
    if (e.key === 'token' && e.newValue === null) {
        const authStore = useAuthStore();
        authStore.clearAuthState();
        if (router.currentRoute.value.name !== 'login') {
            router.push({ name: 'login' });
        }
    }
});
window.addEventListener('unhandledrejection', (e) => reportFatalError(e.reason, 'unhandledrejection'));
window.addEventListener('error', (e) => reportFatalError(e.error || e.message, 'window.onerror'));
app.mount('#app');
