<script setup>
import {onMounted, ref} from "vue";
import {getProfile, updateProfile} from "@/api/authService.js";

const profile = ref(null)
const isEditing = ref(false)
const isChangingPassword = ref(false)

const editForm = ref({name:'',email:''})
const passwordForm = ref({currentPassword:'', newPassword:'', newPasswordConfirm:''})

const updateError = ref('')
const passwordError = ref('')

const fetchProfile = async () => {
  const response = await getProfile();
  profile.value = response.data
  editForm.value.name = response.data.name
  editForm.value.email = response.data.email
}

onMounted(fetchProfile)

const startEdit = () => {
  isEditing.value = true
  updateError.value=''
}

const cancelEdit = () => {
  isEditing.value = false
  editForm.value.name = profile.value.name
  editForm.value.email = profile.value.email
}

const handleUpdateProfile = async () => {
  updateError.value = ''
  try {
    await updateProfile({
      name:editForm.value.name,
      email:editForm.value.email
    })
    await fetchProfile()
    isEditing.value = false
  }catch(error){
    updateError.value = '프로필 수정 중 오류가 발생했습니다.'
  }
}

</script>
<template>
  <div v-if="profile" class="bg-white rounded-2xl border p-6 mb-4">
    <div class="flex items-center gap-3 mb-4">
      <div class="w-12 h-12 rounded-full bg-indigo-600 text-white flex items-center justify-center font-bold">
        {{ profile.name?.charAt(0) }}
      </div>
      <div>
        <p class="font-bold">{{ profile.name }}</p>
        <p class="text-sm text-gray-500">{{ profile.email }}</p>
      </div>
      <button @click="startEdit" class="ml-auto text-sm border rounded-lg px-3 py-1">
        정보 수정
      </button>
    </div>

    <!-- 정보 수정 모달 -->
    <div v-if="isEditing" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white rounded-2xl w-full max-w-md p-6">
        <h3 class="font-bold text-lg mb-1">프로필 정보 수정</h3>
        <p class="text-sm text-gray-500 mb-4">변경할 계정 정보를 입력해주세요.</p>

        <label class="block text-sm text-gray-600 mb-1">이름</label>
        <input v-model="editForm.name" type="text" class="w-full border rounded-lg px-4 py-3 mb-3" />

        <label class="block text-sm text-gray-600 mb-1">이메일</label>
        <input v-model="editForm.email" type="email" class="w-full border rounded-lg px-4 py-3 mb-4" />

        <p v-if="updateError" class="text-red-500 text-sm mb-3">{{ updateError }}</p>

        <div class="flex gap-2">
          <button @click="cancelEdit" class="flex-1 border rounded-lg py-3">취소</button>
          <button @click="handleUpdateProfile" class="flex-1 bg-indigo-600 text-white rounded-lg py-3">저장하기</button>
        </div>
      </div>
    </div>
    <div v-if="isChangingPassword" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white rounded-2xl w-full max-w-md p-6">
        <h3 class="font-bold text-lg mb-4">비밀번호 변경</h3>

        <label class="block text-sm text-gray-600 mb-1">현재 비밀번호</label>
        <input v-model="passwordForm.currentPassword" type="password" class="w-full border rounded-lg px-4 py-3 mb-3" />

        <label class="block text-sm text-gray-600 mb-1">새 비밀번호</label>
        <input v-model="passwordForm.newPassword" type="password" class="w-full border rounded-lg px-4 py-3 mb-3" />

        <label class="block text-sm text-gray-600 mb-1">새 비밀번호 재확인</label>
        <input v-model="passwordForm.newPasswordConfirm" type="password" class="w-full border rounded-lg px-4 py-3 mb-4" />

        <p v-if="passwordError" class="text-red-500 text-sm mb-3">{{ passwordError }}</p>

        <div class="flex gap-2">
          <button @click="isChangingPassword = false" class="flex-1 border rounded-lg py-3">취소</button>
          <button @click="handleChangePassword" class="flex-1 bg-indigo-600 text-white rounded-lg py-3">변경 완료</button>
        </div>
      </div>
    </div>
  </div>
</template>