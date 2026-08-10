import {onUnmounted, ref, watch} from 'vue';
import {useAuthStore} from '@/stores/useAuthStore.js';
import {refreshToken} from '@/api/authService.js';
import router from '@/router';

const WARNING_BEFORE_MS = 60 * 1000; // 만료 60초 전 경고

function getTokenExpiry(token) {
    try {
        const payload = token.split('.')[1];
        const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
        return decoded.exp ? decoded.exp * 1000 : null;
    } catch {
        return null;
    }
}

export function useSessionExpiry() {
    const authStore = useAuthStore();
    const showModal = ref(false);
    const remainingSeconds = ref(0);

    let warnTimer = null;
    let logoutTimer = null;
    let countdownInterval = null;

    const clearTimers = () => {
        clearTimeout(warnTimer);
        clearTimeout(logoutTimer);
        clearInterval(countdownInterval);
    };

    const forceLogout = () => {
        clearTimers();
        showModal.value = false;
        authStore.logout();
        if (router.currentRoute.value.name !== 'login') {
            router.push({name: 'login'});
        }
    };

    const openModal = (expiry) => {
        showModal.value = true;
        remainingSeconds.value = Math.max(0, Math.round((expiry - Date.now()) / 1000));
        countdownInterval = setInterval(() => {
            remainingSeconds.value = Math.max(0, Math.round((expiry - Date.now()) / 1000));
        }, 1000);
    };

    const scheduleTimers = (token) => {
        clearTimers();
        showModal.value = false;
        if (!token) return;

        const expiry = getTokenExpiry(token);
        if (!expiry) return;

        const logoutDelay = expiry - Date.now();
        if (logoutDelay <= 0) {
            forceLogout();
            return;
        }

        const warnDelay = logoutDelay - WARNING_BEFORE_MS;
        if (warnDelay <= 0) {
            openModal(expiry);
        } else {
            warnTimer = setTimeout(() => openModal(expiry), warnDelay);
        }
        logoutTimer = setTimeout(forceLogout, logoutDelay);
    };

    const extendSession = async () => {
        try {
            const { data } = await refreshToken();
            authStore.updateToken(data.token);
        } catch (e) {
            forceLogout();
        }
    };

    watch(() => authStore.token, scheduleTimers, { immediate: true });
    onUnmounted(clearTimers);

    return { showModal, remainingSeconds, extendSession, forceLogout };
}