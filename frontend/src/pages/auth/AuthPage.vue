<script setup>
import {computed, ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import LoginForm from '@/components/auth/LoginForm.vue'
import SignupForm from '@/components/auth/SignupForm.vue'
import FindAccountModal from "@/components/auth/FindAccountModal.vue";

const route = useRoute()
const router = useRouter()
const mode = computed(() => route.name === 'signup' ? 'signup' : 'login')
const showFindModal = ref(false)
</script>
<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 px-6">
    <div class="w-full max-w-md bg-white rounded-2xl p-8">
      <div class="mb-8">
        <h1 class="text-2xl font-bold cursor-pointer" @click="router.push({name:'login'})">🛡 살고싶오</h1>
        <template v-if="mode === 'login'">
          <p class="text-lg mt-4">
            낯선 동네도<br />
            <span class="text-indigo-600 font-bold">안심</span>하고 이사하세요
          </p>
        </template>
        <template v-else>
          <p class="text-xl font-bold mt-4">회원가입</p>
        </template>
      </div>

      <LoginForm v-if="mode === 'login'" />
      <SignupForm v-else />

      <div class="mt-6 text-center text-sm text-gray-500">
        <template v-if="mode === 'login'">
          <button @click="showFindModal = true">아이디 · 비밀번호 찾기</button>
          <p class="mt-2">
            계정이 없으신가요?
            <button @click="router.push({name:'signup'})" class="text-indigo-600 font-semibold">회원가입</button>
          </p>
        </template>
        <template v-else>
          <p>
            이미 계정이 있으신가요?
            <button @click="router.push({name:'login'})" class="text-indigo-600 font-semibold">로그인</button>
          </p>
        </template>
      </div>
    </div>
    <FindAccountModal v-if="showFindModal" @close="showFindModal = false" />
  </div>
</template>
