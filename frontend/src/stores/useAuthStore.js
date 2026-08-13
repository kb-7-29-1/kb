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