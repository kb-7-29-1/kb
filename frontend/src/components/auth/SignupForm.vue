<script setup>
import { useRouter } from 'vue-router';
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from 'vue';
import { VueDatePicker } from '@vuepic/vue-datepicker';
import '@vuepic/vue-datepicker/dist/main.css';
import { ko } from 'date-fns/locale';
import { checkId, signup } from '@/api/authService.js';

const router = useRouter();

const form = ref({
  loginId: '',
  password: '',
  passwordConfirm: '',
  name: '',
  email: '',
  birthDate: '',
  gender: 'M',
});

const today = new Date().toISOString().split('T')[0];
const maxBirthDate = new Date();

const idChecked = ref(false);
const idCheckMessage = ref('');
const errorMessage = ref('');
const emailLocal = ref('');
const emailDomain = ref('');
const selectedEmailDomain = ref('SELECT');
const isCustomEmailDomain = computed(
  () => selectedEmailDomain.value === 'CUSTOM',
);
const customEmailDomainInput = ref(null);
const isEmailDomainMenuOpen = ref(false);
const emailDomainMenuRef = ref(null);
const emailDomainOptions = [
  'naver.com',
  'gmail.com',
  'daum.net',
  'nate.com',
  'hanmail.net',
];

const emailAddress = computed(() => {
  const local = emailLocal.value.trim();
  const domain = emailDomain.value.trim();
  return local && domain ? `${local}@${domain}` : '';
});

const handleEmailDomainChange = async () => {
  emailDomain.value =
    selectedEmailDomain.value === 'SELECT' || isCustomEmailDomain.value
      ? ''
      : selectedEmailDomain.value;

  if (isCustomEmailDomain.value) {
    await nextTick();
    customEmailDomainInput.value?.focus();
  }
};

const showEmailDomainOptions = () => {
  selectedEmailDomain.value = 'SELECT';
  emailDomain.value = '';
};

const toggleEmailDomainMenu = () => {
  isEmailDomainMenuOpen.value = !isEmailDomainMenuOpen.value;
};

const selectEmailDomain = async (domain) => {
  selectedEmailDomain.value = domain;
  isEmailDomainMenuOpen.value = false;
  await handleEmailDomainChange();
};

const closeEmailDomainMenuOnOutsideClick = (event) => {
  if (!emailDomainMenuRef.value?.contains(event.target)) {
    isEmailDomainMenuOpen.value = false;
  }
};

onMounted(() =>
  document.addEventListener('click', closeEmailDomainMenuOnOutsideClick),
);
onBeforeUnmount(() =>
  document.removeEventListener('click', closeEmailDomainMenuOnOutsideClick),
);

const NAME_REGEX = /^[가-힣a-zA-Z\s]{2,20}$/;
const LOGIN_ID_REGEX = /^[a-zA-Z0-9]{4,20}$/;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

watch(
  () => form.value.loginId,
  () => {
    idChecked.value = false;
    idCheckMessage.value = '';
  },
);

const handleCheckId = async () => {
  const loginId = form.value.loginId.trim();

  if (!loginId) {
    idCheckMessage.value = '아이디를 입력해주세요.';
    idChecked.value = false;
    return;
  }

  if (!LOGIN_ID_REGEX.test(loginId)) {
    idCheckMessage.value = '아이디는 영문·숫자 4~20자로 입력해주세요.';
    idChecked.value = false;
    return;
  }

  try {
    const response = await checkId(loginId);
    if (response.data === true) {
      idCheckMessage.value = '사용 가능한 아이디 입니다.';
      idChecked.value = true;
    } else {
      idCheckMessage.value = '이미 사용중인 아이디 입니다.';
      idChecked.value = false;
    }
  } catch (error) {
    idCheckMessage.value = '중복 확인 중 오류가 발생 했습니다.';
  }
};

const handleSignup = async () => {
  errorMessage.value = '';
  form.value.email = emailAddress.value;

  if (!NAME_REGEX.test(form.value.name)) {
    errorMessage.value = '이름은 한글/영문 2~20자로 입력해주세요.';
    return;
  }
  if (!LOGIN_ID_REGEX.test(form.value.loginId)) {
    errorMessage.value = '아이디는 영문/숫자 4~20자로 입력해주세요.';
    return;
  }
  if (!idChecked.value) {
    errorMessage.value = '아이디 중복확인을 해주세요.';
    return;
  }
  if (!EMAIL_REGEX.test(form.value.email) || form.value.email.length > 100) {
    errorMessage.value = '올바른 이메일 형식으로 입력해주세요.';
    return;
  }
  if (form.value.password.length < 8 || form.value.password.length > 100) {
    errorMessage.value = '비밀번호는 8자 이상 100자 이하로 입력해주세요.';
    return;
  }
  if (form.value.password !== form.value.passwordConfirm) {
    errorMessage.value = '비밀번호가 일치하지 않습니다.';
    return;
  }

  if (form.value.birthDate > today) {
    errorMessage.value = '출생연도는 오늘 이후로 설정할 수 없습니다.';
    return;
  }

  try {
    const response = await signup({
      loginId: form.value.loginId,
      password: form.value.password,
      name: form.value.name,
      email: form.value.email,
      birthDate: form.value.birthDate,
      gender: form.value.gender,
    });
    if (response.data && response.data.success === false) {
      errorMessage.value =
        response.data.message || '회원가입 중 오류가 발생했습니다.';
      return;
    }
    alert('회원가입이 완료되었습니다.');
    router.push({ name: 'login' });
  } catch (error) {
    errorMessage.value = '회원가입 중 오류가 발생했습니다.';
    console.log(error);
  }
};
</script>

<template>
  <form
    @submit.prevent="handleSignup"
    class="signup-form flex flex-col gap-4"
    autocomplete="off"
  >
    <div>
      <label class="block text-sm text-gray-600 mb-1">아이디</label>
      <i
        class="signup-field-icon signup-field-icon--login-id fa-regular fa-id-card"
        aria-hidden="true"
      ></i>
      <div class="flex gap-2">
        <input
          v-model="form.loginId"
          type="text"
          placeholder="ID를 입력하세요"
          class="flex-1 border rounded-lg px-4 py-3"
          maxlength="20"
          autocomplete="off"
          @input="idChecked = false"
          required
        />
        <button
          type="button"
          @click="handleCheckId"
          class="rounded-xl px-4 py-3 text-sm font-semibold whitespace-nowrap text-[#4058f5] border border-200"
        >
          중복 확인
        </button>
      </div>
      <p
        v-if="idCheckMessage"
        class="text-sm mt-1"
        :class="idChecked ? 'text-green-600' : 'text-red-500'"
      >
        {{ idCheckMessage }}
      </p>
    </div>

    <div>
      <label class="block text-sm text-gray-600 mb-1">이름</label>
      <i class="signup-field-icon fa-regular fa-user" aria-hidden="true"></i>
      <input
        v-model="form.name"
        type="text"
        placeholder="홍길동"
        class="w-full border rounded-lg px-4 py-3"
        maxlength="20"
        autocomplete="off"
        required
      />
    </div>

    <div class="signup-email-field">
      <label class="block text-sm text-gray-600 mb-1">이메일</label>
      <i
        class="signup-field-icon fa-regular fa-envelope"
        aria-hidden="true"
      ></i>
      <div class="email-input-row">
        <i
          class="fa-regular fa-envelope email-envelope-icon"
          aria-hidden="true"
        ></i>
        <input
          v-model="emailLocal"
          type="text"
          inputmode="email"
          autocomplete="email"
          placeholder="이메일 입력"
          class="email-local-input border rounded-lg px-4 py-3"
          maxlength="64"
          required
        />
        <span class="email-at" aria-hidden="true">@</span>
        <span ref="emailDomainMenuRef" class="email-domain-select-wrap">
          <template v-if="isCustomEmailDomain">
            <input
              ref="customEmailDomainInput"
              v-model="emailDomain"
              type="text"
              inputmode="url"
              autocomplete="off"
              placeholder="직접 입력"
              class="email-domain-input"
              maxlength="100"
              required
            />
            <button
              type="button"
              class="email-domain-options-button"
              aria-label="이메일 도메인 목록 열기"
              @click="toggleEmailDomainMenu"
            >
              <i
                class="fa-solid fa-chevron-down email-domain-chevron"
                aria-hidden="true"
              ></i>
            </button>
          </template>
          <button
            v-else
            class="email-domain-select"
            type="button"
            :aria-expanded="isEmailDomainMenuOpen"
            @click="toggleEmailDomainMenu"
          >
            {{
              selectedEmailDomain === 'SELECT' ? '선택' : selectedEmailDomain
            }}
          </button>
          <i
            v-if="!isCustomEmailDomain"
            class="fa-solid fa-chevron-down email-domain-chevron"
            :class="{ 'is-open': isEmailDomainMenuOpen }"
            aria-hidden="true"
          ></i>
          <Transition name="email-domain-menu">
            <div v-if="isEmailDomainMenuOpen" class="email-domain-menu">
              <button
                v-for="domain in emailDomainOptions"
                :key="domain"
                type="button"
                class="email-domain-option"
                :class="{ 'is-selected': selectedEmailDomain === domain }"
                @click="selectEmailDomain(domain)"
              >
                {{ domain }}
              </button>
              <button
                type="button"
                class="email-domain-option"
                :class="{ 'is-selected': isCustomEmailDomain }"
                @click="selectEmailDomain('CUSTOM')"
              >
                직접 입력
              </button>
            </div>
          </Transition>
        </span>
      </div>
    </div>

    <div class="signup-birth-gender-row flex gap-4">
      <div class="signup-birth-field flex-[1.8]">
        <label class="block text-sm text-gray-600 mb-1">출생연도</label>
        <VueDatePicker
          v-model="form.birthDate"
          model-type="yyyy-MM-dd"
          :formats="{ input: 'yyyy-MM-dd' }"
          :time-config="{ enableTimePicker: false }"
          :max-date="maxBirthDate"
          :locale="ko"
          :clearable="false"
          :auto-apply="true"
          :text-input="{ format: 'yyyy-MM-dd' }"
          :floating="{ placement: 'bottom-start', flip: false }"
          :year-first="true"
          placeholder="YYYY-MM-DD"
          class="birth-date-picker"
        />
      </div>
      <div class="signup-gender-field flex-[0.8]">
        <label class="block text-sm text-gray-600 mb-1">성별</label>
        <div class="gender-select flex h-12 gap-2">
          <button
            type="button"
            @click="form.gender = 'M'"
            :class="
              form.gender === 'M'
                ? 'bg-indigo-600 text-white'
                : 'bg-gray-100 text-slate-500'
            "
            class="flex flex-1 items-center justify-center gap-1 rounded-lg py-3 text-xs font-semibold"
          >
            남
          </button>
          <button
            type="button"
            @click="form.gender = 'F'"
            :class="
              form.gender === 'F'
                ? 'bg-indigo-600 text-white'
                : 'bg-gray-100 text-slate-500'
            "
            class="flex flex-1 items-center justify-center gap-1 rounded-lg py-3 text-xs font-semibold"
          >
            여
          </button>
        </div>
      </div>
    </div>

    <div>
      <label class="block text-sm text-gray-600 mb-1">비밀번호</label>
      <i class="signup-field-icon fa-solid fa-lock" aria-hidden="true"></i>
      <input
        v-model="form.password"
        type="password"
        placeholder="8자 이상"
        class="w-full border rounded-lg px-4 py-3"
        maxlength="100"
        autocomplete="new-password"
        required
      />
    </div>

    <div>
      <label class="block text-sm text-gray-600 mb-1">비밀번호 확인</label>
      <i class="signup-field-icon fa-solid fa-lock" aria-hidden="true"></i>
      <input
        v-model="form.passwordConfirm"
        type="password"
        placeholder="비밀번호를 다시 입력하세요"
        class="w-full border rounded-lg px-4 py-3"
        maxlength="100"
        autocomplete="new-password"
        required
      />
    </div>

    <p v-if="errorMessage" class="text-red-500 text-sm">{{ errorMessage }}</p>

    <div class="signup-submit-area">
      <button
        type="submit"
        class="w-full bg-indigo-600 text-white rounded-lg py-3 font-semibold"
      >
        회원가입 완료
      </button>
    </div>
  </form>
</template>

<style scoped>
.birth-date-picker {
  width: 100%;
}

.signup-email-field > .signup-field-icon {
  display: none;
}

.email-input-row {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 24px minmax(0, 1fr);
  align-items: center;
  min-height: 48px;
  overflow: visible;
  border: 1px solid #dce1e9;
  border-radius: 12px;
  background: #fff;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.email-input-row:focus-within {
  border-color: #4058f5;
  box-shadow: 0 0 0 3px rgb(64 88 245 / 12%);
}

.signup-email-field .email-local-input,
.signup-email-field .email-domain-select,
.signup-email-field .email-domain-input {
  height: 46px;
  min-height: 46px;
  border: 0 !important;
  border-radius: 0;
  background: transparent;
  box-shadow: none !important;
  outline: 0;
}

.signup-email-field .email-local-input {
  grid-column: 1;
  padding-left: 38px;
}

.signup-email-field .email-local-input,
.signup-email-field .email-domain-select,
.signup-email-field .email-domain-input {
  color: #475569;
  font-size: 14px;
  font-weight: 400;
}

.email-envelope-icon {
  position: absolute;
  top: 50%;
  left: 14px;
  z-index: 1;
  color: #8b9ab5;
  font-size: 13px;
  pointer-events: none;
  transform: translateY(-50%);
}

.email-domain-select-wrap {
  position: relative;
  display: flex;
  min-width: 0;
  grid-column: 3;
  align-items: center;
}

.signup-email-field .email-domain-select {
  width: 100%;
  padding: 0 34px 0 10px;
  color: #475569;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
}

.signup-email-field .email-domain-input {
  width: 100%;
  padding: 0 34px 0 10px;
}

.email-domain-chevron {
  position: absolute;
  right: 13px;
  color: #8b9ab5;
  font-size: 11px;
  pointer-events: none;
  transition: transform 0.2s ease;
}

.email-domain-chevron.is-open {
  transform: rotate(180deg);
}

.email-domain-options-button {
  position: absolute;
  right: 0;
  display: inline-flex;
  width: 36px;
  height: 46px;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  color: #8b9ab5;
  cursor: pointer;
}

.email-domain-menu {
  position: absolute;
  top: calc(100% + 7px);
  right: 0;
  z-index: 30;
  display: grid;
  width: 100%;
  overflow: hidden;
  padding: 5px;
  border: 1px solid #dbe3f4;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 10px 22px rgb(30 41 59 / 14%);
}

.email-domain-option {
  width: 100%;
  padding: 9px 10px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #475569;
  font-family: inherit;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    color 0.15s ease;
}

.email-domain-option:hover,
.email-domain-option:focus-visible {
  outline: 0;
  background: #eef1ff;
  color: #4058f5;
}

.email-domain-option.is-selected {
  background: #e8edff;
  color: #4058f5;
  font-weight: 700;
}

.email-domain-menu-enter-active,
.email-domain-menu-leave-active {
  transition:
    opacity 0.16s ease,
    transform 0.16s ease;
}

.email-domain-menu-enter-from,
.email-domain-menu-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.email-at {
  grid-column: 2;
  justify-self: center;
  color: #94a3b8;
  font-size: 14px;
  font-weight: 600;
}

@media (min-width: 481px) {
  .signup-form {
    gap: 16px;
  }

  .signup-form > div:has(> .signup-field-icon) {
    position: relative;
  }

  .signup-field-icon {
    position: absolute;
    top: 49px;
    left: 14px;
    z-index: 1;
    color: #9aa6bf;
    font-size: 13px;
    pointer-events: none;
    transform: translateY(-50%);
  }

  .signup-field-icon--login-id {
    top: 49px;
  }

  .signup-form input {
    min-height: 48px;
    padding-left: 38px;
    border-radius: 12px;
    font-size: 14px;
  }

  .signup-email-field .signup-field-icon {
    top: 49px;
  }

  .email-input-row input {
    min-width: 0;
    padding-left: 14px;
  }

  .email-domain-input {
    flex: 1.15;
  }

  .email-at {
    color: #64748b;
    font-size: 15px;
    font-weight: 700;
  }

  .email-domain-select {
    min-width: 0;
    font-family: inherit;
    font-size: 14px;
  }

  .signup-form input:focus {
    border-color: #4058f5;
    outline: none;
    box-shadow: 0 0 0 3px rgb(64 88 245 / 12%);
  }

  .signup-form .flex > button[type='button'] {
    min-height: 48px;
    border-radius: 12px;
  }

  .signup-birth-gender-row {
    align-items: flex-end;
  }

  .signup-birth-field,
  .signup-gender-field {
    min-width: 0;
  }

  .gender-select button {
    border-radius: 12px;
    font-size: 13px;
  }

  .signup-submit-area {
    margin-top: 12px;
  }

  .signup-submit-area button {
    min-height: 60px;
    border-radius: 12px;
  }
}

@media (max-width: 480px) {
  .signup-form {
    flex: 1;
    min-height: 0;
    padding: 22px 24px 110px;
    overflow-y: auto;
    background: #f6f8fc;
  }

  .signup-form label {
    color: #697386;
    font-size: 13px;
    font-weight: 600;
  }

  .signup-form input {
    height: 48px;
    border-color: #e0e5f5;
    border-radius: 10px;
    background: #fff;
    font-size: 14px;
  }

  .signup-email-field .signup-field-icon {
    top: 43px;
    bottom: auto;
  }

  .email-input-row input {
    min-width: 0;
    padding-left: 13px;
  }

  .email-local-input {
    flex: 1;
  }

  .email-domain-input {
    flex: 1.12;
  }

  .email-at {
    color: #64748b;
    font-size: 14px;
    font-weight: 700;
  }

  .email-domain-select {
    min-width: 0;
    font-family: inherit;
    font-size: 14px;
  }

  .signup-form input:focus {
    border-color: #4058f5;
    outline: none;
    box-shadow: 0 0 0 3px rgb(64 88 245 / 12%);
  }

  .signup-form > div:has(> .signup-field-icon) {
    position: relative;
  }

  .signup-field-icon {
    position: absolute;
    bottom: 17px;
    left: 13px;
    z-index: 1;
    color: #9aa6bf;
    font-size: 12px;
    pointer-events: none;
  }

  .signup-field-icon--login-id {
    top: 42px;
    bottom: auto;
  }

  .signup-form input {
    padding-left: 36px;
  }

  .signup-submit-area {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 20;
    box-sizing: border-box;
    width: 100%;
    min-height: 110px;
    padding: 16px 20px max(16px, calc(env(safe-area-inset-bottom) + 10px));
    border-top: 1px solid #e2e8f0;
    background: #f6f8fc;
  }

  .signup-submit-area button {
    height: 65px;
    border-radius: 14px;
    background: #4051db;
    font-size: 16px;
  }
}
</style>

<style>
/* vue-datepicker의 팝업 캘린더는 body로 teleport돼서 scoped 스타일이
   안 먹기 때문에, 폼 색감(#4058f5)에 맞추려면 :root 변수를 전역으로 덮어써야 한다. */
:root {
  --dp-primary-color: #4058f5;
  --dp-border-radius: 12px;
  --dp-font-family: inherit;
  --dp-font-size: 14px;
  --dp-border-color: #d1d5db;
  --dp-border-color-hover: #4058f5;
  --dp-border-color-focus: #4058f5;
  --dp-input-padding: 12px 30px 12px 16px;
}
</style>
