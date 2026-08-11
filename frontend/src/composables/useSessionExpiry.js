import {onUnmounted, ref, watch} from 'vue';
import {useAuthStore} from '@/stores/useAuthStore.js';
import {refreshToken} from '@/api/authService.js';
import router from '@/router';

const WARNING_BEFORE_MS = 60 * 1000; // 만료 60초 전 경고
const GRACE_MS = 5 * 60 * 1000; // 서버 refresh 유예기간과 동일하게(JwtProcessor.REFRESH_GRACE_MILISECOND)

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

        // 토큰이 만료돼도 서버가 유예기간(GRACE_MS) 안에서는 연장을 받아주므로,
        // 그 안에는 강제로그아웃하지 않고 모달을 띄운 채로 연장 기회를 준다.
        const hardLogoutAt = expiry + GRACE_MS;
        const hardLogoutDelay = hardLogoutAt - Date.now();
        if (hardLogoutDelay <= 0) {
            forceLogout();
            return;
        }

        const warnDelay = expiry - WARNING_BEFORE_MS - Date.now();
        if (warnDelay <= 0) {
            openModal(expiry);
        } else {
            warnTimer = setTimeout(() => openModal(expiry), warnDelay);
        }
        logoutTimer = setTimeout(forceLogout, hardLogoutDelay);
    };

    const extendSession = async () => {
        try {
            const { data } = await refreshToken();
            authStore.updateToken(data.token);
        } catch (e) {
            forceLogout();
        }
    };

    const handleVisibilityChange = () => {
        // 백그라운드 탭에서는 브라우저가 setTimeout을 지연시키므로,
        // 포그라운드로 돌아온 시점에 현재 시각 기준으로 다시 계산한다.
        if (document.visibilityState === 'visible') {
            scheduleTimers(authStore.token);
        }
    };
    document.addEventListener('visibilitychange', handleVisibilityChange);

    watch(() => authStore.token, scheduleTimers, { immediate: true });
    onUnmounted(() => {
        clearTimers();
        document.removeEventListener('visibilitychange', handleVisibilityChange);
    });

    return { showModal, remainingSeconds, extendSession, forceLogout };
}