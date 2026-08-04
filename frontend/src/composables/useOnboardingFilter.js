import { ref } from 'vue';
import onboardingApi from '@/api/onboardingApi.js';

const ONBOARDING_DRAFT_KEY = 'salgosipo-onboarding-draft';

/**
 * 온보딩 저장값(localStorage 및 GET /api/onboarding)을 기반으로
 * 지도 퀵 필터 기본 상태를 로드 및 관리하는 Composable
 */
export function useOnboardingFilter() {
  const filterState = ref({
    destination: '세종대학교',
    destinationLat: 37.5502,
    destinationLng: 127.0731,
    tradeType: 'MONTHLY',
    maxDeposit: 5000,
    maxRent: 100,
    minSafetyScore: 0,
    transportMode: 'WALK',
    travelTime: 15,
    walkPace: 'NORMAL',
    showIsochrone: true,
    selectedAmenities: [],
  });

  const loadOnboardingDefaultFilters = async () => {
    let saved = null;

    // 1. localStorage 완료 결과 및 드래프트 저장값 확인
    try {
      const localResult = localStorage.getItem('salgosipo-onboarding-result');
      const localDraft = localStorage.getItem(ONBOARDING_DRAFT_KEY);
      if (localResult) {
        saved = JSON.parse(localResult);
      } else if (localDraft) {
        saved = JSON.parse(localDraft);
      }
    } catch (err) {
      console.warn('LocalStorage onboarding data load error:', err);
    }

    // 2. 백엔드 GET /api/onboarding 연동 (로그인 유저 기준)
    try {
      const apiData = await onboardingApi.getOnboarding();
      if (apiData && typeof apiData === 'object') {
        saved = { ...saved, ...apiData };
      }
    } catch (err) {
      // API 통신 불가 시 localStorage 데이터 유지
    }

    // 3. 온보딩 설정값을 filterState 디폴트 값에 필드 매핑
    if (saved) {
      // 목적지 및 실제 주소/좌표 (address, lat, lng) 매핑
      const rawDest = saved.destination || saved.destinationName || saved.destName;
      if (rawDest) {
        if (typeof rawDest === 'object' && rawDest !== null) {
          filterState.value.destination =
            rawDest.destName || rawDest.name || rawDest.destinationName || '세종대학교';
          filterState.value.destinationAddress =
            rawDest.destAddress || rawDest.address || '';

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

      // 이동 수단 매핑
      if (saved.transport || saved.transportMode) {
        const mode = String(saved.transport || saved.transportMode).toUpperCase();
        filterState.value.transportMode = mode.includes('WALK') ? 'WALK' : 'TRANSIT';
      }
      // 소요 시간 매핑
      if (saved.travelTime || saved.maxTravelTime) {
        filterState.value.travelTime = Number(saved.travelTime || saved.maxTravelTime);
      }
      // 보증금 한도 매핑
      if (saved.deposit || saved.maxDeposit || saved.budgetDeposit) {
        filterState.value.maxDeposit = Number(
          saved.deposit || saved.maxDeposit || saved.budgetDeposit,
        );
      }
      // 월세 한도 매핑
      if (saved.monthlyRent || saved.maxRent || saved.budgetRent) {
        filterState.value.maxRent = Number(
          saved.monthlyRent || saved.maxRent || saved.budgetRent,
        );
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
      }
    }
  };

  return {
    filterState,
    loadOnboardingDefaultFilters,
  };
}
