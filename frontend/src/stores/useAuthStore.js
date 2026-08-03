import {defineStore} from "pinia";

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
        logout(){
            this.token = null
            this.user = null
            localStorage.removeItem('token')
            localStorage.removeItem('user')
        }
    }
})