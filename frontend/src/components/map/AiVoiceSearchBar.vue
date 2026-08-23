<script setup>
import { computed, onBeforeUnmount, ref } from 'vue';

const props = defineProps({
  loading: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  transcript: {
    type: String,
    default: '',
  },
  message: {
    type: String,
    default: '',
  },
  error: {
    type: String,
    default: '',
  },
});

const emit = defineEmits(['voice-search', 'clear-feedback']);

const isListening = ref(false);
const localError = ref('');
const interimTranscript = ref('');
let recognition = null;
let heardFinalResult = false;
let autoStopTimer = null;

const MAX_LISTENING_MS = 15000;

const SpeechRecognitionConstructor = () =>
  window.SpeechRecognition || window.webkitSpeechRecognition || null;

const isSpeechRecognitionSupported = computed(
  () => typeof window !== 'undefined' && Boolean(SpeechRecognitionConstructor()),
);

const statusText = computed(() => {
  if (props.disabled) return '로그인 후 AI 음성검색을 사용할 수 있어요.';
  if (props.loading) return 'GPT-5 Nano가 검색조건을 분석하는 중이에요.';
  if (isListening.value) {
    return interimTranscript.value
      ? `인식 중: “${interimTranscript.value}”`
      : '듣고 있어요. 말씀을 마치면 자동으로 검색합니다.';
  }
  if (localError.value) return localError.value;
  if (props.error) return props.error;
  if (props.message) return props.message;
  if (!isSpeechRecognitionSupported.value) {
    return '이 브라우저는 음성 인식을 지원하지 않습니다. Chrome 또는 Edge를 사용해 주세요.';
  }
  return '예: “세종대 도보 20분, 월세 70 이하, 안전한 집 찾아줘”';
});

let errorResetTimer = null;

const setErrorWithAutoReset = (msg, delayMs = 3500) => {
  localError.value = msg;
  if (errorResetTimer) clearTimeout(errorResetTimer);
  errorResetTimer = setTimeout(() => {
    localError.value = '';
  }, delayMs);
};

const clearTimer = () => {
  if (autoStopTimer) clearTimeout(autoStopTimer);
  autoStopTimer = null;
};

const stopListening = () => {
  clearTimer();
  if (recognition && isListening.value) {
    try {
      recognition.stop();
    } catch (_) {
      // 이미 종료 중인 경우 무시
    }
  }
};

const cleanupRecognition = () => {
  clearTimer();
  isListening.value = false;
  interimTranscript.value = '';
  recognition = null;
};

const startListening = () => {
  if (errorResetTimer) clearTimeout(errorResetTimer);
  localError.value = '';
  interimTranscript.value = '';
  emit('clear-feedback');

  if (props.disabled) {
    setErrorWithAutoReset('로그인 후 사용할 수 있습니다.');
    return;
  }
  if (props.loading) return;

  const Recognition = SpeechRecognitionConstructor();
  if (!Recognition) {
    setErrorWithAutoReset(
      '이 브라우저는 음성 인식을 지원하지 않습니다. Chrome 또는 Edge를 사용해 주세요.',
    );
    return;
  }

  recognition = new Recognition();
  recognition.lang = 'ko-KR';
  recognition.continuous = false;
  recognition.interimResults = true;
  recognition.maxAlternatives = 1;
  heardFinalResult = false;

  recognition.onstart = () => {
    isListening.value = true;
  };

  recognition.onresult = (event) => {
    let interim = '';
    let finalText = '';

    for (let index = event.resultIndex; index < event.results.length; index += 1) {
      const result = event.results[index];
      const text = String(result?.[0]?.transcript || '').trim();
      if (!text) continue;

      if (result.isFinal) finalText += `${text} `;
      else interim += `${text} `;
    }

    interimTranscript.value = interim.trim();

    const normalizedFinal = finalText.trim();
    if (normalizedFinal) {
      heardFinalResult = true;
      clearTimer();
      isListening.value = false;
      interimTranscript.value = '';
      emit('voice-search', normalizedFinal);
    }
  };

  recognition.onerror = (event) => {
    clearTimer();
    isListening.value = false;
    interimTranscript.value = '';

    const code = event?.error;
    if (code === 'not-allowed' || code === 'service-not-allowed') {
      setErrorWithAutoReset('브라우저의 마이크 권한을 허용해 주세요.');
    } else if (code === 'no-speech') {
      setErrorWithAutoReset('음성을 인식하지 못했습니다. 다시 말씀해 주세요.');
    } else if (code === 'audio-capture') {
      setErrorWithAutoReset('사용 가능한 마이크를 찾지 못했습니다.');
    } else if (code === 'network') {
      setErrorWithAutoReset('브라우저 음성 인식 네트워크 오류가 발생했습니다.');
    } else if (code !== 'aborted') {
      setErrorWithAutoReset(`음성 인식 중 오류가 발생했습니다${code ? ` (${code})` : ''}.`);
    }
  };

  recognition.onend = () => {
    clearTimer();
    isListening.value = false;
    interimTranscript.value = '';

    if (!heardFinalResult && !localError.value) {
      setErrorWithAutoReset('음성을 인식하지 못했습니다. 다시 말씀해 주세요.');
    }
    recognition = null;
  };

  try {
    recognition.start();
    autoStopTimer = setTimeout(stopListening, MAX_LISTENING_MS);
  } catch (error) {
    cleanupRecognition();
    setErrorWithAutoReset('음성 인식을 시작하지 못했습니다.');
  }
};

const toggleListening = () => {
  if (isListening.value) stopListening();
  else startListening();
};

onBeforeUnmount(() => {
  if (errorResetTimer) clearTimeout(errorResetTimer);
  clearTimer();
  if (recognition) {
    recognition.onresult = null;
    recognition.onerror = null;
    recognition.onend = null;
    try {
      recognition.abort();
    } catch (_) {
      // ignore
    }
  }
  recognition = null;
});
</script>

<template>
  <section
    class="ai-voice-search pointer-events-auto w-[min(460px,calc(100vw-2rem))] rounded-2xl border border-white/80 bg-white/95 p-3 shadow-xl shadow-slate-900/10 backdrop-blur-md"
    aria-label="AI 음성 매물 검색"
  >
    <div class="flex items-center gap-3">
      <button
        type="button"
        class="relative flex h-11 w-11 shrink-0 items-center justify-center rounded-full text-white shadow-md transition active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
        :class="
          isListening
            ? 'bg-rose-500 shadow-rose-200'
            : loading
              ? 'bg-slate-400'
              : 'bg-gradient-to-br from-[#5267e8] to-[#7c5ce7] shadow-indigo-200'
        "
        :disabled="loading || disabled || !isSpeechRecognitionSupported"
        :aria-label="isListening ? '음성 인식 종료' : 'AI 음성검색 시작'"
        @click="toggleListening"
      >
        <span
          v-if="isListening"
          class="absolute inset-0 animate-ping rounded-full bg-rose-400 opacity-30"
          aria-hidden="true"
        ></span>
        <i
          v-if="loading"
          class="fa-solid fa-spinner animate-spin text-[17px]"
          aria-hidden="true"
        ></i>
        <i
          v-else-if="isListening"
          class="fa-solid fa-stop relative z-10 text-[15px]"
          aria-hidden="true"
        ></i>
        <i
          v-else
          class="fa-solid fa-microphone text-[17px]"
          aria-hidden="true"
        ></i>
      </button>

      <div class="min-w-0 flex-1">
        <div class="flex items-center gap-2">
          <strong class="text-[13px] font-extrabold text-slate-800">
            AI 음성검색
          </strong>
          <span
            v-if="isListening"
            class="rounded-full bg-rose-50 px-2 py-0.5 text-[10px] font-bold text-rose-600"
          >
            듣는 중 · 최대 15초
          </span>
          <span
            v-else
            class="rounded-full bg-indigo-50 px-2 py-0.5 text-[10px] font-bold text-[#5267e8]"
          >
            GPT-5 Nano
          </span>
        </div>
        <p
          class="mt-0.5 line-clamp-2 text-[11px] leading-4"
          :class="error || localError ? 'text-rose-500' : 'text-slate-500'"
        >
          {{ statusText }}
        </p>
      </div>
    </div>

    <div
      v-if="transcript && !isListening"
      class="mt-2 rounded-xl bg-slate-50 px-3 py-2 text-[11px] leading-4 text-slate-600"
    >
      <span class="font-bold text-slate-700">인식된 말</span>
      <span class="ml-1">“{{ transcript }}”</span>
    </div>
  </section>
</template>
