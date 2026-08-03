<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/useAuthStore';
import {changePassword, withdraw} from '@/api/authService';
import onboardingApi from '@/api/onboardingApi';
import ProfileCard from '@/components/mypage/ProfileCard.vue';
import BookmarkList from '@/components/mypage/BookmarkList.vue';
import OnboardingPanel from '@/components/mypage/OnboardingPanel.vue';

const router = useRouter();
const authStore = useAuthStore();

// --- 온보딩 ---
const onboarding = ref(null);

const formatAmount = (amount) => {
  const value = Number(amount);

  if (!Number.isFinite(value)) return '설정 정보 없음';
  if (value >= 10000)
    return value % 10000 === 0 ? `${value / 10000}억원 이하` : `${value.toLocaleString()}만원 이하`;

  return `${value.toLocaleString()}만원 이하`;
};

const destination = computed(() => onboarding.value?.destination?.destName ?? '설정 정보 없음');
const transport = computed(() => {
  if (!onboarding.value) return '설정 정보 없음';

  const label = onboarding.value.transportMode === 'WALK' ? '도보' : '대중교통';
  return `${label} (최대 ${onboarding.value.maxTravelTime}분)`;
});
const deposit = computed(() => formatAmount(onboarding.value?.budgetDeposit));
const rent = computed(() => {
  const value = Number(onboarding.value?.budgetRent);
  if (!Number.isFinite(value)) return '설정 정보 없음';
  return value === 0 ? '전세' : `${value.toLocaleString()}만원 이하`;
});
const safety = computed(() => {
  const score = onboarding.value?.minSafetyScore;
  return Number.isFinite(Number(score)) ? `${score}점 이상` : '설정 정보 없음';
});

const loadOnboarding = async () => {
  try {
    onboarding.value = await onboardingApi.getOnboarding();
  } catch (error) {
    if (error.response?.status !== 404) {
      console.error('ONBOARDING GET ERROR: ', error);
    }
  }
};

// --- 비밀번호 변경 ---
const showPasswordModal = ref(false);
const passwordForm = ref({ currentPassword: '', newPassword: '', newPasswordConfirm: '' });
const passwordError = ref('');

const handleChangePassword = async () => {
  passwordError.value = '';
  if (passwordForm.value.newPassword !== passwordForm.value.newPasswordConfirm) {
    passwordError.value = '새 비밀번호가 일치하지 않습니다.';
    return;
  }
  try {
    await changePassword({
      currentPassword: passwordForm.value.currentPassword,
      newPassword: passwordForm.value.newPassword
    });
    alert('비밀번호가 변경되었습니다.');
    showPasswordModal.value = false;
    passwordForm.value = { currentPassword: '', newPassword: '', newPasswordConfirm: '' };
  } catch (error) {
    passwordError.value = '현재 비밀번호가 일치하지 않습니다.';
  }
};

// --- 로그아웃 / 탈퇴 ---
const handleLogout = () => {
  authStore.logout();
  router.push({ name: 'login' });
};

const showWithdrawModal = ref(false);
const withdrawPassword = ref('');
const withdrawError = ref('');

const handleWithdraw = async () => {
  withdrawError.value = '';
  try {
    await withdraw(withdrawPassword.value);
    authStore.logout();
    router.push({ name: 'login' });
  } catch (error) {
    withdrawError.value = '비밀번호가 일치하지 않습니다.';
  }
};

onMounted(loadOnboarding);
</script>

<template>
  <div class="max-w-md mx-auto px-4 py-6">
    <div class="relative flex items-center justify-center mb-6">
      <button @click="router.push({ name: 'home' })" class="absolute left-0 text-gray-600 text-xl">
        ←
      </button>
      <h1 class="text-xl font-bold">마이페이지</h1>
    </div>

    <ProfileCard />

    <OnboardingPanel
        :destination="destination"
        :transport="transport"
        :deposit="deposit"
        :rent="rent"
        :safety="safety"
    />

    <BookmarkList />

    <div class="mt-6 space-y-3">
      <button @click="showPasswordModal = true" class="w-full text-left text-gray-600 flex items-center gap-2">
        🔒 비밀번호 변경
      </button>
      <button @click="handleLogout" class="w-full text-left text-gray-600 flex items-center gap-2">
        ↪ 로그아웃
      </button>
      <button @click="showWithdrawModal = true" class="w-full text-left text-red-500 flex items-center gap-2">
        🗑 회원 탈퇴
      </button>
    </div>

    <div v-if="showPasswordModal" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white rounded-2xl w-full max-w-md p-6">
        <h3 class="font-bold text-lg mb-4">비밀번호 변경</h3>
        <label class="block text-sm text-gray-600 mb-1">현재 비밀번호</label>
        <input v-model="passwordForm.currentPassword" type="password" class="w-full border rounded-lg px-4 py-3 mb-3" />
        <label class="block text-sm text-gray-600 mb-1">새 비밀번호</label>
        <input v-model="passwordForm.newPassword" type="password" class="w-full border rounded-lg px-4 py-3 mb-3" />
        <label class="block text-sm text-gray-600 mb-1">새 비밀번호 확인</label>
        <input v-model="passwordForm.newPasswordConfirm" type="password" class="w-full border rounded-lg px-4 py-3 mb-3" />
        <p v-if="passwordError" class="text-red-500 text-sm mb-3">{{ passwordError }}</p>
        <div class="flex gap-2">
          <button @click="showPasswordModal = false" class="flex-1 border rounded-lg py-3">취소</button>
          <button @click="handleChangePassword" class="flex-1 bg-indigo-600 text-white rounded-lg py-3">변경하기</button>
        </div>
      </div>
    </div>

    <div v-if="showWithdrawModal" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white rounded-2xl w-full max-w-md p-6">
        <h3 class="font-bold text-lg mb-4">회원 탈퇴</h3>
        <p class="text-sm text-gray-500 mb-4">탈퇴하려면 비밀번호를 입력해주세요.</p>
        <input v-model="withdrawPassword" type="password" class="w-full border rounded-lg px-4 py-3 mb-3" />
        <p v-if="withdrawError" class="text-red-500 text-sm mb-3">{{ withdrawError }}</p>
        <div class="flex gap-2">
          <button @click="showWithdrawModal = false" class="flex-1 border rounded-lg py-3">취소</button>
          <button @click="handleWithdraw" class="flex-1 bg-red-500 text-white rounded-lg py-3">탈퇴하기</button>
        </div>
      </div>
    </div>
  </div>
</template>
