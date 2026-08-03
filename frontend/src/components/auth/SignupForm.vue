<script setup>
import {useRouter} from "vue-router";
import {ref} from "vue";
import {checkId, signup} from "@/api/authService.js";

const router = useRouter()

const form = ref({
  loginId:'',
  password:'',
  passwordConfirm:'',
  name:'',
  email:'',
  birthDate:'',
  gender: 'M'
})

const idChecked = ref(false)
const idCheckMessage = ref('')
const errorMessage = ref('')

const handleCheckId = async () => {
  if(!form.value.loginId){
    idCheckMessage.value = '아이디를 입력해주세요.'
    return
  }
  try{
    const response = await checkId(form.value.loginId)
    if(response.data === true){
      idCheckMessage.value = '사용 가능한 아이디 입니다.'
      idChecked.value = true
    }else {
      idCheckMessage.value = '이미 사용중인 아이디 입니다.'
      idChecked.value = false;
    }
  }catch(error) {
    idCheckMessage.value = '중복 확인 중 오류가 발생 했습니다.'
   }
}

const handleSignup = async () => {
  errorMessage.value = ''
  if(!idChecked.value){
    errorMessage.value = '아이디 중복확인을 해주세요.'
    return
  }
  if (form.value.password !== form.value.passwordConfirm){
    errorMessage.value = '비밀번호가 일치하지 않습니다.'
    return
  }
  try{
    await signup({
      loginId: form.value.loginId,
      password: form.value.password,
      name: form.value.name,
      email: form.value.email,
      birthDate: form.value.birthDate,
      gender: form.value.gender
    })
    alert('회원가입이 완료되었습니다.')
    router.push({name:'login'})
  }catch (error){
    errorMessage.value = '회원가입 중 오류가 발생했습니다.'
    console.log(error)
  }
}
</script>
<template>
  <form @submit.prevent="handleSignup" class="flex flex-col gap-4">
    <div>
      <label class="block text-sm text-gray-600 mb-1">이름</label>
      <input v-model="form.name" type="text" placeholder="홍길동" class="w-full border rounded-lg px-4 py-3" required />
    </div>

    <div>
      <label class="block text-sm text-gray-600 mb-1">아이디</label>
      <div class="flex gap-2">
        <input
            v-model="form.loginId"
            type="text"
            placeholder="ID를 입력하세요"
            class="flex-1 border rounded-lg px-4 py-3"
            @input="idChecked = false"
            required
        />
        <button type="button" @click="handleCheckId" class="border rounded-lg px-4 py-3 whitespace-nowrap">
          중복 확인
        </button>
      </div>
      <p v-if="idCheckMessage" class="text-sm mt-1" :class="idChecked ? 'text-green-600' : 'text-red-500'">
        {{ idCheckMessage }}
      </p>
    </div>

    <div>
      <label class="block text-sm text-gray-600 mb-1">이메일</label>
      <input v-model="form.email" type="email" placeholder="example@email.com" class="w-full border rounded-lg px-4 py-3" required />
    </div>

    <div class="flex gap-4">
      <div class="flex-1">
        <label class="block text-sm text-gray-600 mb-1">출생연도</label>
        <input v-model="form.birthDate" type="date" class="w-full border rounded-lg px-4 py-3" required />
      </div>
      <div class="flex-1">
        <label class="block text-sm text-gray-600 mb-1">성별</label>
        <div class="flex gap-2 mt-2">
          <button
              type="button"
              @click="form.gender = 'M'"
              :class="form.gender === 'M' ? 'bg-indigo-600 text-white' : 'bg-gray-100'"
              class="flex-1 rounded-lg py-3"
          >
            남
          </button>
          <button
              type="button"
              @click="form.gender = 'F'"
              :class="form.gender === 'F' ? 'bg-indigo-600 text-white' : 'bg-gray-100'"
              class="flex-1 rounded-lg py-3"
          >
            여
          </button>
        </div>
      </div>
    </div>

    <div>
      <label class="block text-sm text-gray-600 mb-1">비밀번호</label>
      <input v-model="form.password" type="password" placeholder="8자 이상" class="w-full border rounded-lg px-4 py-3" required />
    </div>

    <div>
      <label class="block text-sm text-gray-600 mb-1">비밀번호 확인</label>
      <input v-model="form.passwordConfirm" type="password" placeholder="비밀번호를 다시 입력하세요" class="w-full border rounded-lg px-4 py-3" required />
    </div>

    <p v-if="errorMessage" class="text-red-500 text-sm">{{ errorMessage }}</p>

    <button type="submit" class="w-full bg-indigo-600 text-white rounded-lg py-3 font-semibold">
      회원가입 완료
    </button>
  </form>
</template>