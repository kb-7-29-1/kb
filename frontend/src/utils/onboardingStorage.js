const ONBOARDING_STORAGE_PREFIX = 'salgosipo-onboarding';

// 온보딩 임시값 사용자별 키 사용
export const getOnboardingStorageKeys = (user) => {
  const rawUserId = user?.userId ?? user?.id ?? user;
  const userId = Number(rawUserId);

  if (!Number.isInteger(userId) || userId <= 0) return null;

  const suffix = `-${userId}`;
  return {
    draft: `${ONBOARDING_STORAGE_PREFIX}-draft${suffix}`,
    step: `${ONBOARDING_STORAGE_PREFIX}-draft-step${suffix}`,
    result: `${ONBOARDING_STORAGE_PREFIX}-result${suffix}`,
  };
};
