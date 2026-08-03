<script setup>
import { ref } from 'vue'
import { useRouter } from "vue-router";
import { login } from '@/api/authService'
import {useAuthStore} from "@/stores/useAuthStore.js";

const router = useRouter();
const authStore = useAuthStore();

const loginId = ref('')
const password = ref('')
const errorMessage = ref('')

const handleLogin = async () => {
  errorMessage.value = ''
  try{
    const response = await login({
      loginId:loginId.value,
      password:password.value
    })
    authStore.setAuth(response.data.token, response.data.user)
    router.push({name:'map'})
  }catch (error){
    errorMessage.value = '아이디 또는 비밀번호가 일치하지 않습니다.'
  }
}
</script>
<template>
  <form @submit.prevent="handleLogin" class="flex flex-col gap-4">
    <div>
      <label class="block text-sm text-gray-600 mb-1">아이디</label>
      <input
          v-model="loginId"
          type="text"
          placeholder="아이디를 입력하세요"
          class="w-full border rounded-lg px-4 py-3"
          required
      />
    </div>

    <div>
      <label class="block text-sm text-gray-600 mb-1">비밀번호</label>
      <input
          v-model="password"
          type="password"
          placeholder="비밀번호를 입력하세요"
          class="w-full border rounded-lg px-4 py-3"
          required
      />
    </div>

    <p v-if="errorMessage" class="text-red-500 text-sm">{{ errorMessage }}</p>

    <button
        type="submit"
        class="w-full bg-indigo-600 text-white rounded-lg py-3 font-semibold"
    >
      로그인
    </button>
  </form>
</template>