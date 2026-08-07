import { ref } from 'vue';
import onboardingApi from '@/api/onboardingApi.js';
import { DEFAULT_DEPOSIT, DEFAULT_RENT } from '@/utils/budget';

const ONBOARDING_DRAFT_KEY = 'salgosipo-onboarding-draft';

/**
 * 온보딩 저장값(localStorage 및 GET /api/onboarding)을 기반으로
 * 지도 퀵 필터 기본 상태를 로드 및 관리하는 Composable
 */
export function useOnboardingFilter() {
  const filterState = ref({
    destinationId: null,
    destination: '세종대학교',
    destinationAddress: '서울특별시 광진구 능동로 209',
    destinationLat: 37.5502,
    destinationLng: 127.0731,
    tradeType: 'MONTHLY',
    maxDeposit: DEFAULT_DEPOSIT,
    maxRent: DEFAULT_RENT,
    minSafetyScore: 0,
    transportMode: 'WALK',
    travelTime: 15,
    walkPace: 'NORMAL',
    showIsochrone: true,
    selectedAmenities: [],
  });

  const loadOnboardingDefaultFilters = async () => {
    let saved = null;

    try {
      const localResult = localStorage.getItem('salgosipo-onboarding-result');
      const localDraft = localStorage.getItem(ONBOARDING_DRAFT_KEY);
      if (localResult) {
        saved = JSON.parse(localResult);
      } else if (localDraft) {
        saved = JSON.parse(localDraft);
      }
    } catch (error) {
      console.warn('LocalStorage onboarding data load error:', error);
    }

    try {
      const apiData = await onboardingApi.getOnboarding();
      if (apiData && typeof apiData === 'object') {
        saved = { ...saved, ...apiData };
      }
    } catch (error) {
      // API 통신 불가 시 localStorage 값을 유지합니다.
    }

    if (!saved) return;

    const rawDestination =
      saved.destination || saved.destinationName || saved.destName;

    const topLevelDestinationId =
      saved.destinationId ?? saved.destId ?? null;
    if (topLevelDestinationId != null) {
      filterState.value.destinationId = Number(topLevelDestinationId);
    }

    if (rawDestination) {
      if (typeof rawDestination === 'object') {
        const destinationId =
          rawDestination.destinationId ?? rawDestination.destId;
        if (destinationId != null) {
          filterState.value.destinationId = Number(destinationId);
        }

        filterState.value.destination =
          rawDestination.destName ||
          rawDestination.name ||
          rawDestination.destinationName ||
          '세종대학교';
        filterState.value.destinationAddress =
          rawDestination.destAddress || rawDestination.address || '';

        const latitude =
          rawDestination.destLatitude ??
          rawDestination.lat ??
          rawDestination.latitude;
        const longitude =
          rawDestination.destLongitude ??
          rawDestination.lng ??
          rawDestination.longitude;

        if (latitude != null && longitude != null) {
          filterState.value.destinationLat = Number(latitude);
          filterState.value.destinationLng = Number(longitude);
        }
      } else if (typeof rawDestination === 'string') {
        filterState.value.destination = rawDestination;
      }
    }

    if (saved.transport || saved.transportMode) {
      const mode = String(saved.transport || saved.transportMode).toUpperCase();
      filterState.value.transportMode = mode.includes('WALK')
        ? 'WALK'
        : 'TRANSIT';
    }

    const travelTime = saved.maxTravelTime ?? saved.travelTime;
    if (travelTime != null) {
      filterState.value.travelTime = Number(travelTime);
    }

    const maxDeposit =
      saved.budgetDeposit ?? saved.maxDeposit ?? saved.deposit;
    if (maxDeposit != null) {
      filterState.value.maxDeposit = Number(maxDeposit);
    }

    const maxRent =
      saved.budgetRent ?? saved.maxRent ?? saved.monthlyRent;
    if (maxRent != null) {
      filterState.value.maxRent = Number(maxRent);
    }

    if (saved.safety || saved.minSafetyScore !== undefined) {
      if (typeof saved.minSafetyScore === 'number') {
        filterState.value.minSafetyScore = saved.minSafetyScore;
      } else if (saved.safety === 'high') {
        filterState.value.minSafetyScore = 85;
      } else if (saved.safety === 'medium') {
        filterState.value.minSafetyScore = 70;
      }
    }
  };

  return {
    filterState,
    loadOnboardingDefaultFilters,
  };
}
