<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/useAuthStore';
import { changePassword, withdraw } from '@/api/authService';
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

const openPasswordModal = () => {
  passwordForm.value = { currentPassword: '', newPassword: '', newPasswordConfirm: '' };
  passwordError.value = '';
  showPasswordModal.value = true;
};

const closePasswordModal = () => {
  showPasswordModal.value = false;
  passwordForm.value = { currentPassword: '', newPassword: '', newPasswordConfirm: '' };
  passwordError.value = '';
};

const handleChangePassword = async () => {
  passwordError.value = '';
  if (passwordForm.value.newPassword !== passwordForm.value.newPasswordConfirm) {
    passwordError.value = '새 비밀번호가 일치하지 않습니다.';
    return;
  }
  try {
    await changePassword({
      currentPassword: passwordForm.value.currentPassword,
      newPassword: passwordForm.value.newPassword,
    });
    alert('비밀번호가 변경되었습니다.');
    closePasswordModal();
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

const openWithdrawModal = () => {
  withdrawPassword.value = '';
  withdrawError.value = '';
  showWithdrawModal.value = true;
};

const closeWithdrawModal = () => {
  showWithdrawModal.value = false;
  withdrawPassword.value = '';
  withdrawError.value = '';
};

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
  <div class="mypage-page">
    <header class="mypage-header">
      <button
        type="button"
        class="mypage-back-button"
        aria-label="지도로 돌아가기"
        @click="router.push({ name: 'home' })"
      >
        <i class="fa-solid fa-chevron-left" aria-hidden="true"></i>
        <span class="mypage-back-label">지도 화면으로</span>
      </button>
      <h1>마이페이지</h1>
    </header>

    <main class="mypage-content">
      <ProfileCard />

      <OnboardingPanel
        :destination="destination"
        :transport="transport"
        :deposit="deposit"
        :rent="rent"
        :safety="safety"
      />

      <BookmarkList />

      <section class="mypage-actions" aria-labelledby="account-actions-title">
        <h2 id="account-actions-title">계정 관리</h2>
        <button type="button" class="account-action" @click="openPasswordModal">
          <span class="account-action__icon" aria-hidden="true">
            <i class="fa-solid fa-lock"></i>
          </span>
          <span>비밀번호 변경</span>
          <i class="fa-solid fa-chevron-right account-action__chevron" aria-hidden="true"></i>
        </button>
        <button type="button" class="account-action" @click="handleLogout">
          <span class="account-action__icon" aria-hidden="true">
            <i class="fa-solid fa-right-from-bracket"></i>
          </span>
          <span>로그아웃</span>
          <i class="fa-solid fa-chevron-right account-action__chevron" aria-hidden="true"></i>
        </button>
        <button
          type="button"
          class="account-action account-action--danger"
          @click="openWithdrawModal"
        >
          <span class="account-action__icon" aria-hidden="true">
            <i class="fa-regular fa-trash-can"></i>
          </span>
          <span>회원 탈퇴</span>
          <i class="fa-solid fa-chevron-right account-action__chevron" aria-hidden="true"></i>
        </button>
      </section>
    </main>

    <div v-if="showPasswordModal" class="account-modal-overlay">
      <div
        class="account-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="password-modal-title"
      >
        <div class="account-modal-heading">
          <h3 id="password-modal-title">
            <span class="account-modal-icon" aria-hidden="true"
              ><i class="fa-solid fa-lock"></i
            ></span>
            비밀번호 변경
          </h3>
          <button
            type="button"
            class="account-modal-close"
            aria-label="닫기"
            @click="closePasswordModal"
          >
            <i class="fa-solid fa-xmark" aria-hidden="true"></i>
          </button>
        </div>
        <p class="account-modal-description">현재 비밀번호 확인 후 새 비밀번호를 설정해 주세요.</p>

        <label class="account-modal-label">현재 비밀번호</label>
        <input
          v-model="passwordForm.currentPassword"
          type="password"
          class="account-modal-input"
          placeholder="현재 비밀번호를 입력해 주세요"
        />
        <label class="account-modal-label">새 비밀번호</label>
        <input
          v-model="passwordForm.newPassword"
          type="password"
          class="account-modal-input"
          placeholder="새 비밀번호를 입력해 주세요"
        />
        <label class="account-modal-label">새 비밀번호 확인</label>
        <input
          v-model="passwordForm.newPasswordConfirm"
          type="password"
          class="account-modal-input"
          placeholder="새 비밀번호를 다시 입력해 주세요"
        />
        <p v-if="passwordError" class="account-modal-error">{{ passwordError }}</p>
        <div class="account-modal-actions">
          <button type="button" class="account-cancel-button" @click="closePasswordModal">
            취소
          </button>
          <button type="button" class="account-primary-button" @click="handleChangePassword">
            변경하기
          </button>
        </div>
      </div>
    </div>

    <div v-if="showWithdrawModal" class="account-modal-overlay">
      <div
        class="account-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="withdraw-modal-title"
      >
        <div class="account-modal-heading">
          <h3 id="withdraw-modal-title">
            <span class="account-modal-icon account-modal-icon--danger" aria-hidden="true">
              <i class="fa-regular fa-trash-can"></i>
            </span>
            회원 탈퇴
          </h3>
          <button
            type="button"
            class="account-modal-close"
            aria-label="닫기"
            @click="closeWithdrawModal"
          >
            <i class="fa-solid fa-xmark" aria-hidden="true"></i>
          </button>
        </div>
        <p class="account-modal-description">탈퇴를 진행하려면 현재 비밀번호를 입력해 주세요.</p>
        <label class="account-modal-label">현재 비밀번호</label>
        <input
          v-model="withdrawPassword"
          type="password"
          class="account-modal-input"
          placeholder="현재 비밀번호를 입력해 주세요"
        />
        <p v-if="withdrawError" class="account-modal-error">{{ withdrawError }}</p>
        <div class="account-modal-actions">
          <button type="button" class="account-cancel-button" @click="closeWithdrawModal">
            취소
          </button>
          <button type="button" class="account-danger-button" @click="handleWithdraw">
            탈퇴하기
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mypage-page {
  box-sizing: border-box;
  width: 100%;
  height: 100dvh;
  max-width: 28rem;
  margin: 0 auto;
  overflow: hidden;
  background: #f6f8fc;
  display: flex;
  flex-direction: column;
}

.mypage-header {
  flex: 0 0 auto;
  position: relative;
  box-sizing: border-box;
  height: max(100px, calc(env(safe-area-inset-top) + 62px));
  padding: max(60px, calc(env(safe-area-inset-top) + 22px)) 20px 0;
  border-bottom: 1px solid #e7eaf0;
  background: #f6f8fc;
  text-align: center;
}

.mypage-header h1 {
  margin: 0;
  color: #20283a;
  font-size: 18px;
  font-weight: 700;
  line-height: 28px;
}

.mypage-back-button {
  position: absolute;
  top: max(60px, calc(env(safe-area-inset-top) + 22px));
  left: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #6d7480;
  font-size: 16px;
  cursor: pointer;
}

.mypage-back-label {
  display: none;
}

.mypage-content {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  gap: 10px;
  padding: 22px 16px 32px;
  overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
}

.mypage-actions {
  padding: 18px 20px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
  margin-top: 0;
}

.mypage-actions h2 {
  margin: 0 0 10px;
  color: #17191d;
  font-size: 15px;
  font-weight: 700;
}

.account-action {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 48px;
  gap: 10px;
  padding: 7px 0;
  border: 0;
  border-bottom: 1px solid #edf0f5;
  background: transparent;
  color: #4b5565;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
}

.account-action:last-child {
  border-bottom: 0;
}

.account-action__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: #eef1ff;
  color: #4767f7;
  font-size: 12px;
}

.account-action__chevron {
  margin-left: auto;
  color: #a1a8b5;
  font-size: 11px;
}

.account-action--danger {
  color: #dc4b5d;
}

.account-action--danger .account-action__icon {
  background: #fff1f2;
  color: #dc4b5d;
}

.account-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgb(15 23 42 / 40%);
}

.account-modal {
  box-sizing: border-box;
  width: 100%;
  max-width: 400px;
  padding: 28px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 20px 48px rgb(15 23 42 / 22%);
}

.account-modal-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
  margin-bottom: 18px;
}

.account-modal-heading h3 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: #20283a;
  font-size: 16px;
  font-weight: 700;
}

.account-modal-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #eef1ff;
  color: #4058f5;
  font-size: 13px;
}

.account-modal-icon--danger {
  background: #fff1f2;
  color: #dc4b5d;
}

.account-modal-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #9aa3b0;
  font-size: 14px;
  cursor: pointer;
}

.account-modal-close:hover {
  background: #f1f5f9;
  color: #475569;
}

.account-modal-description {
  margin: 0 0 18px;
  color: #7b8797;
  font-size: 13px;
}

.account-modal-label {
  display: block;
  margin: 0 0 6px;
  color: #697386;
  font-size: 13px;
  font-weight: 600;
}

.account-modal-input {
  box-sizing: border-box;
  width: 100%;
  height: 48px;
  margin: 0 0 14px;
  padding: 0 14px;
  border: 1px solid #e0e5f5;
  border-radius: 12px;
  outline: none;
  color: #20283a;
  font: inherit;
  font-size: 14px;
}

.account-modal-input:focus {
  border-color: #4058f5;
  box-shadow: 0 0 0 3px rgb(64 88 245 / 12%);
}

.account-modal-error {
  margin: -2px 0 14px;
  color: #e05263;
  font-size: 12px;
}

.account-modal-actions {
  display: flex;
  gap: 8px;
  margin-top: 6px;
}

.account-modal-actions button {
  flex: 1;
  height: 48px;
  border-radius: 12px;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.account-cancel-button {
  border: 1px solid #e0e5f5;
  background: #fff;
  color: #697386;
}

.account-primary-button {
  border: 1px solid #4051db;
  background: #4051db;
  color: #fff;
}

.account-danger-button {
  border: 1px solid #dc4b5d;
  background: #dc4b5d;
  color: #fff;
}

@media (min-width: 768px) {
  :global(html),
  :global(body),
  :global(#app) {
    height: auto;
    min-height: 100%;
    overflow-y: auto;
  }

  .mypage-page {
    width: min(100%, 720px);
    height: auto;
    min-height: 100dvh;
    max-height: none;
    max-width: 720px;
    padding: 0;
    background: #f8fafc;
    box-shadow: none;
  }

  .mypage-header {
    height: 108px;
    padding: 0 28px;
    margin: 10px 0 0;
    border: 0;
    background: transparent;
    text-align: center;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .mypage-header h1 {
    padding-left: 0;
    font-size: 20px;
  }

  .mypage-back-button {
    top: 50%;
    left: 28px;
    transform: translateY(-50%);
    width: auto;
    height: auto;
    gap: 6px;
    color: #94a3b8;
    font-size: 13px;
    line-height: 1;
  }

  .mypage-back-button i {
    font-size: 11px;
  }

  .mypage-back-label {
    display: inline;
  }

  .mypage-content {
    flex: 1 0 auto;
    min-height: calc(100dvh - 108px);
    overflow: visible;
    justify-content: flex-start;
    gap: 18px;
    padding: 32px 28px 48px;
  }
}

/* 화면 높이가 낮을 때는 첫 카드가 헤더 아래에 가려지지 않도록 위에서부터 배치한다. */
@media (min-width: 768px) and (max-height: 800px) {
  .mypage-header {
    height: 76px;
  }

  .mypage-content {
    justify-content: flex-start;
    min-height: calc(100dvh - 76px);
    padding-top: 16px;
  }
}
</style>
