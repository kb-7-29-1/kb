import {defineStore} from "pinia";
import { clearMapFilterCache } from '@/utils/mapFilterCache.js';

export const useAuthStore = defineStore('auth',{
    state: () =>({
        token: localStorage.getItem('token') || null,
        user: JSON.parse(localStorage.getItem('user')) || null
    }),
    getters: {
        isLoggedIn: (state) => !! state.token
    },
    actions: {
        setAuth(token, user){
            this.token = token
            this.user = user
            localStorage.setItem('token', token)
            localStorage.setItem('user',JSON.stringify(user))
        },
        updateToken(token){
            this.token = token
            localStorage.setItem('token', token)
        },
        // 다른 탭에서 storage 이벤트로 전달받은 토큰을 반영할 때 사용.
        // localStorage에 다시 쓰지 않음(이미 그 탭이 써서 이 이벤트가 발생한 것이므로, 재작성 시 불필요한 이벤트 재발생을 피함).
        syncToken(token){
            this.token = token
        },
        logout() {
            clearMapFilterCache(this.user);
            this.clearAuthState();
            localStorage.removeItem('token');
            localStorage.removeItem('user');
        },
        clearAuthState() {
            this.token = null;
            this.user = null;
        }
    }
})