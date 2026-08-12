import { ref } from 'vue';
import onboardingApi from '@/api/onboardingApi.js';
import { DEFAULT_DEPOSIT, DEFAULT_RENT } from '@/utils/budget';
import { useAuthStore } from '@/stores/useAuthStore';
import { getOnboardingStorageKeys } from '@/utils/onboardingStorage';

// 사용자별로 온보딩 임시저장 값 localStorage 저장
export function useOnboardingFilter() {
  const authStore = useAuthStore();
  const filterState = ref({
    destinationId: null,
    destination: null,
    destinationAddress: null,
    destinationLat: null,
    destinationLng: null,
    tradeType: 'MONTHLY',
    minDeposit: 0,
    maxDeposit: DEFAULT_DEPOSIT,
    minRent: 0,
    maxRent: DEFAULT_RENT,
    minSafetyScore: 0,
    transportMode: 'WALK',
    minTravelTime: 0,
    travelTime: 15,
    walkPace: 'NORMAL',
    showIsochrone: true,
    selectedAmenities: [],
  });

  const loadOnboardingDefaultFilters = async ({ resetDestination = false } = {}) => {
    let saved = null;

    try {
      const storageKeys = getOnboardingStorageKeys(authStore.user);
      const localResult = storageKeys ? localStorage.getItem(storageKeys.result) : null;
      const localDraft = storageKeys ? localStorage.getItem(storageKeys.draft) : null;
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

    // 3. 온보딩 설정값을 filterState 디폴트 값에 필드 매핑
    if (saved) {
      // 사용자가 직접 검색/선택한 목적지가 이미 존재하는 경우 DB 온보딩값(송파구 등)으로 덮어쓰지 않음
      if (
        resetDestination ||
        !filterState.value.destinationLat ||
        !filterState.value.destinationLng
      ) {
        const rawDest = saved.destination || saved.destinationName || saved.destName;
        if (rawDest) {
          if (typeof rawDest === 'object' && rawDest !== null) {
            const destinationId = rawDest.destinationId ?? rawDest.destId ?? saved.destinationId;
            if (destinationId != null) {
              filterState.value.destinationId = Number(destinationId);
            }
            filterState.value.destination =
              rawDest.destName || rawDest.name || rawDest.destinationName || '세종대학교';
            filterState.value.destinationAddress = rawDest.destAddress || rawDest.address || '';

            const latVal = rawDest.destLatitude || rawDest.lat || rawDest.latitude;
            const lngVal = rawDest.destLongitude || rawDest.lng || rawDest.longitude;
            if (latVal && lngVal) {
              filterState.value.destinationLat = Number(latVal);
              filterState.value.destinationLng = Number(lngVal);
            }
          } else if (typeof rawDest === 'string') {
            filterState.value.destination = rawDest;
          }
        }
      }

      // 이동 수단 매핑
      if (saved.transport || saved.transportMode) {
        const mode = String(saved.transport || saved.transportMode).toUpperCase();
        filterState.value.transportMode = mode.includes('WALK') ? 'WALK' : 'TRANSIT';
      }
      // 소요 시간 매핑
      const travelTime = saved.maxTravelTime ?? saved.travelTime;
      if (travelTime !== undefined && travelTime !== null) {
        filterState.value.travelTime = Number(travelTime);
      }
      if (saved.minTravelTime !== undefined && saved.minTravelTime !== null) {
        filterState.value.minTravelTime = Number(saved.minTravelTime);
      }
      // 보증금 한도 매핑
      const maxDeposit = saved.budgetDeposit ?? saved.maxDeposit ?? saved.deposit;
      if (maxDeposit !== undefined && maxDeposit !== null) {
        filterState.value.maxDeposit = Number(maxDeposit);
      }
      if (saved.minDeposit !== undefined && saved.minDeposit !== null) {
        filterState.value.minDeposit = Number(saved.minDeposit);
      }
      // 월세 한도 매핑
      const maxRent = saved.budgetRent ?? saved.maxRent ?? saved.monthlyRent;
      if (maxRent !== undefined && maxRent !== null) {
        filterState.value.maxRent = Number(maxRent);
        // 온보딩에는 거래유형이 없으므로 월세 0원을 전세로 해석
        filterState.value.tradeType = Number(maxRent) === 0 ? 'JEONSE' : 'MONTHLY';
      }
      if (saved.minRent !== undefined && saved.minRent !== null) {
        filterState.value.minRent = Number(saved.minRent);
      }
      // 안심 점수 매핑
      if (saved.safety || saved.minSafetyScore !== undefined) {
        if (typeof saved.minSafetyScore === 'number') {
          filterState.value.minSafetyScore = saved.minSafetyScore;
        } else if (saved.safety === 'high') {
          filterState.value.minSafetyScore = 85;
        } else if (saved.safety === 'medium') {
          filterState.value.minSafetyScore = 70;
        }
      } else if (typeof rawDestination === 'string') {
        filterState.value.destination = rawDestination;
      }
    }

    if (saved.transport || saved.transportMode) {
      const mode = String(saved.transport || saved.transportMode).toUpperCase();
      filterState.value.transportMode = mode.includes('WALK') ? 'WALK' : 'TRANSIT';
    }

    const travelTime = saved.maxTravelTime ?? saved.travelTime;
    if (travelTime != null) {
      filterState.value.travelTime = Number(travelTime);
    }

    const maxDeposit = saved.budgetDeposit ?? saved.maxDeposit ?? saved.deposit;
    if (maxDeposit != null) {
      filterState.value.maxDeposit = Number(maxDeposit);
    }

    const maxRent = saved.budgetRent ?? saved.maxRent ?? saved.monthlyRent;
    if (maxRent != null) {
      filterState.value.maxRent = Number(maxRent);
      // 초기화 시 온보딩의 월세 0원 설정을 전세로 유지
      filterState.value.tradeType = Number(maxRent) === 0 ? 'JEONSE' : 'MONTHLY';
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

    // 온보딩에는 최대 예산만 저장하므로 초기화 시 최소 예산만 기본값으로 되돌림
    if (resetDestination) {
      filterState.value.minDeposit = 0;
      filterState.value.minRent = 0;
    }
  };

  return {
    filterState,
    loadOnboardingDefaultFilters,
  };
}
