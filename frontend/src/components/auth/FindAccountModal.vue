<script setup>
import {findId, resetPassword, verifyForPasswordReset} from "@/api/authService.js";
import {ref} from "vue";

const emit = defineEmits(['close'])

const tab = ref('findId')

// 아이디 찾기
const email = ref('')
const maskedLoginId = ref('')
const findIdError = ref('')

const handleFindId = async () => {
  findIdError.value = ''
  maskedLoginId.value = ''
  try{
    const response = await findId(email.value)
    maskedLoginId.value = response.data.maskedLoginId
  }catch (error){
    findIdError.value = '해당 이메일로 가입된 계정이 없습니다.'
  }
}

// 비밀번호 찾기
const pwLoginId = ref('')
const pwName = ref('')
const pwEmail = ref('')
const verifiedUserId =ref(null)
const verifyError = ref('')

const handleVerify = async () => {
  verifyError.value = ''
  try {
    const response = await verifyForPasswordReset({
      loginId: pwLoginId.value,
      name: pwName.value,
      email:pwEmail.value
    })
    verifiedUserId.value = response.data
  }catch (error){
    verifyError.value = '일치하는 계정 정보가 없습니다.'
  }
}

// 비밀번호 찾기 - 재설정 단계
const newPassword = ref('')
const newPasswordConfirm = ref('')
const resetError = ref('')
const resetSuccess = ref(false)

const handleResetPassword = async () => {
  resetError.value = ''
  if(newPassword.value !== newPasswordConfirm.value){
    resetError.value = '비밀번호가 일치하지않습니다'
    return
  }

  try{
    await resetPassword({
      userId:verifiedUserId.value,
      newPassword:newPassword.value
    })
    resetSuccess.value = true
  }catch (error){
    resetError.value ='비밀번호 변경 중 오류가 발생했습니다.'
  }
}

const switchTab = (target) => {
  tab.value = target
  // 탭 전환시 상태 초기화
  maskedLoginId.value = ''
  findIdError.value =''
  verifiedUserId.value = null
  verifyError.value = ''
  resetError.value = ''
  resetSuccess.value = false
}
</script>
<template>
  <div class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
    <div class="bg-white rounded-2xl w-full max-w-md p-8 relative">
      <button @click="emit('close')" class="absolute top-6 left-6 text-gray-400">
        &lt;
      </button>
      <h2 class="text-xl font-bold text-center mb-6">계정 / 비밀번호 찾기</h2>

      <div class="flex rounded-full bg-gray-100 p-1 mb-6">
        <button
            @click="switchTab('findId')"
            :class="tab === 'findId' ? 'bg-white shadow font-semibold' : 'text-gray-500'"
            class="flex-1 rounded-full py-2 text-sm"
        >
          아이디 찾기
        </button>
        <button
            @click="switchTab('findPassword')"
            :class="tab === 'findPassword' ? 'bg-white shadow font-semibold' : 'text-gray-500'"
            class="flex-1 rounded-full py-2 text-sm"
        >
          비밀번호 찾기 & 즉시 변경
        </button>
      </div>

      <!-- 아이디 찾기 -->
      <div v-if="tab === 'findId'">
        <p class="text-sm text-gray-500 mb-4">
          가입 시 등록한 <span class="font-semibold">이메일 주소</span>를 입력하시면 마스킹된 아이디를 즉시 안내해드립니다.
        </p>
        <label class="block text-sm text-gray-600 mb-1">이메일</label>
        <input v-model="email" type="email" placeholder="example@email.com" class="w-full border rounded-lg px-4 py-3 mb-4" />

        <p v-if="maskedLoginId" class="text-indigo-600 font-semibold mb-4">
          회원님의 아이디는 <span class="font-bold">{{ maskedLoginId }}</span> 입니다.
        </p>
        <p v-if="findIdError" class="text-red-500 text-sm mb-4">{{ findIdError }}</p>

        <button @click="handleFindId" class="w-full bg-indigo-600 text-white rounded-lg py-3 font-semibold">
          아이디 찾기
        </button>
      </div>

      <!-- 비밀번호 찾기 -->
      <div v-else>
        <template v-if="!verifiedUserId">
          <p class="text-sm text-gray-500 mb-4">아이디, 이름, 이메일을 입력해주세요.</p>

          <label class="block text-sm text-gray-600 mb-1">아이디</label>
          <input v-model="pwLoginId" type="text" class="w-full border rounded-lg px-4 py-3 mb-3" />

          <label class="block text-sm text-gray-600 mb-1">이름</label>
          <input v-model="pwName" type="text" class="w-full border rounded-lg px-4 py-3 mb-3" />

          <label class="block text-sm text-gray-600 mb-1">이메일</label>
          <input v-model="pwEmail" type="email" class="w-full border rounded-lg px-4 py-3 mb-4" />

          <p v-if="verifyError" class="text-red-500 text-sm mb-4">{{ verifyError }}</p>

          <button @click="handleVerify" class="w-full bg-indigo-600 text-white rounded-lg py-3 font-semibold">
            본인 확인
          </button>
        </template>

        <template v-else-if="!resetSuccess">
          <p class="text-sm text-gray-500 mb-4">새 비밀번호를 입력해주세요.</p>

          <label class="block text-sm text-gray-600 mb-1">새 비밀번호</label>
          <input v-model="newPassword" type="password" class="w-full border rounded-lg px-4 py-3 mb-3" />

          <label class="block text-sm text-gray-600 mb-1">새 비밀번호 재확인</label>
          <input v-model="newPasswordConfirm" type="password" class="w-full border rounded-lg px-4 py-3 mb-4" />

          <p v-if="resetError" class="text-red-500 text-sm mb-4">{{ resetError }}</p>

          <button @click="handleResetPassword" class="w-full bg-indigo-600 text-white rounded-lg py-3 font-semibold">
            비밀번호 변경 완료
          </button>
        </template>

        <template v-else>
          <p class="text-center text-green-600 font-semibold py-8">
            비밀번호가 변경되었습니다.<br />새 비밀번호로 로그인해주세요.
          </p>
          <button @click="emit('close')" class="w-full bg-indigo-600 text-white rounded-lg py-3 font-semibold">
            확인
          </button>
        </template>
      </div>
    </div>
  </div>
</template>