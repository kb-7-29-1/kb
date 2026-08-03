import axios from 'axios'
import { useAuthStore } from '@/stores/useAuthStore'
import router from '@/router'

const api = axios.create({
    baseURL: 'http://localhost:8080/api',
    headers:{
        'Content-Type':'application/json'
    }
})

// 요청 보낼 때마다 토큰 자동으로 헤더에 실어줌
api.interceptors.request.use((config) =>{
    const authStore = useAuthStore()
    if(authStore.token){
        config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config;
})

// 응답 인터셉터 - 토큰 만료(401) 시 자동 로그아웃 처리
api.interceptors.response.use(
    (response) => response,
    (error) =>{
        if(error.response?.status === 401){
            const authStore = useAuthStore()
            authStore.logout()
            router.push({name:'login'})
        }
        return Promise.reject(error);
    }
)

export default api