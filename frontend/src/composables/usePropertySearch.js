import { ref } from 'vue';
import api from '@/api/api.js';
import { DEFAULT_DEPOSIT, DEFAULT_RENT, LOAN_PRODUCTS } from '@/utils/budget';
import { getSearchRadiusKm, getMinSearchRadiusKm } from '@/utils/isochroneFilter.js';
import { calculateDistanceKm } from '@/utils/geo.js';
import safetyService from '@/api/safetyService.js';
import { UNSUPPORTED_SAFETY_DISTRICTS, isSeoulServiceArea } from '@/utils/districtPolygonOverlay.js';

/**
 * 자치구 및 지역 주소를 검사하여 안전점수 계산 가능 대상인지 사전에 고속 판별합니다.
 */
function isEligibleForSafetyCalculation(item) {
  if (!item) return false;
  const address = item.address || item.roadAddress || item.jibunAddress || item.title || '';
  if (!isSeoulServiceArea(address)) return false;
  return !UNSUPPORTED_SAFETY_DISTRICTS.some((gu) => address.includes(gu));
}

/**
 * 백엔드 REST API 매물 조회 및 무한 스크롤 페이징, 대출 상한 계산을 전담 관리하는 Composable입니다.
 */
export function usePropertySearch() {
  const properties = ref([]);
  const serverTotalCount = ref(0);
  const currentPage = ref(1);
  const isPropertyLoading = ref(true);
  const isPropertyApiError = ref(false);
  const isMoreLoading = ref(false);
  const showAllLoadedToast = ref(false);

  let propertyRequestSequence = 0;
  let toastTimer = null;

  const triggerAllLoadedToast = () => {
    showAllLoadedToast.value = true;
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      showAllLoadedToast.value = false;
    }, 2500);
  };

  const getEffectiveMaxDeposit = (filters) => {
    let maxDeposit = Number(filters.maxDeposit) || DEFAULT_DEPOSIT;

    if (filters.selectedLoanId && filters.selectedLoanId !== 'NONE') {
      const loan = LOAN_PRODUCTS.find((item) => item.id === filters.selectedLoanId);
      if (loan?.ratio > 0) {
        maxDeposit = Math.round(maxDeposit * (1 + loan.ratio));
      }
    }

    return maxDeposit;
  };

  const buildPropertySearchParams = (appliedFilterState, destinationConfig, authStore) => {
    const filters = appliedFilterState.value;
    const params = {
      destinationId: filters.destinationId || undefined,
      lat: destinationConfig.value.lat,
      lng: destinationConfig.value.lng,
      swLat: filters.swLat || undefined,
      swLng: filters.swLng || undefined,
      neLat: filters.neLat || undefined,
      neLng: filters.neLng || undefined,
      radius: getSearchRadiusKm(filters),
      minRadius: getMinSearchRadiusKm(filters),
      maxDeposit: getEffectiveMaxDeposit(filters),
      userId: authStore.user?.userId || authStore.user?.id || undefined,
    };

    if (filters.tradeType === 'JEONSE') {
      params.maxMonthlyRent = 0;
    } else if (filters.tradeType === 'MONTHLY') {
      // 월세 단독 선택 시 전세 매물(월세 0) 제외
      const minRent = Number(filters.minRent);
      const maxRent = Number(filters.maxRent);

      params.minMonthlyRent = Number.isFinite(minRent) && minRent > 0 ? minRent : 1;

      if (Number.isFinite(maxRent) && maxRent >= params.minMonthlyRent && maxRent < 200) {
        params.maxMonthlyRent = maxRent;
      }
    } else if (Number.isFinite(Number(filters.maxRent)) && Number(filters.maxRent) < 200) {
      const maxRent = Number(filters.maxRent);
      if (maxRent > 0) {
        params.maxMonthlyRent = maxRent;
      }
    }

    // 최솟값 > 최댓값 역전 방어
    if (
      params.minMonthlyRent != null &&
      params.maxMonthlyRent != null &&
      params.minMonthlyRent > params.maxMonthlyRent
    ) {
      delete params.minMonthlyRent;
    }

    return params;
  };

  const doFetchPropertiesFromBackend = async ({
    appliedFilterState,
    destinationConfig,
    authStore,
    isMapMoved,
    pendingBounds,
    clearBoundsFromFilters,
    updateLastFetchedCenter,
    isAppend = false,
  }) => {
    const requestId = ++propertyRequestSequence;
    isPropertyLoading.value = true;
    isPropertyApiError.value = false;

    if (!isAppend) {
      properties.value = [];
      currentPage.value = 1;
      if (!isMapMoved?.value && !pendingBounds?.value) {
        clearBoundsFromFilters(appliedFilterState.value);
      }
    }

    try {
      const searchParams = buildPropertySearchParams(
        appliedFilterState,
        destinationConfig,
        authStore,
      );
      const propertyResponse = await api.get('/properties', {
        params: searchParams,
        timeout: 5000,
      });

      if (requestId !== propertyRequestSequence) return;

      const resData = propertyResponse.data;
      serverTotalCount.value = Number(resData?.totalCount) || 0;

      const rawCandidates = Array.isArray(resData?.items)
        ? resData.items
        : Array.isArray(resData)
          ? resData
          : [];

      const searchRadiusKm = getSearchRadiusKm(appliedFilterState.value);
      const centerLat = searchParams.lat || destinationConfig.value.lat;
      const centerLng = searchParams.lng || destinationConfig.value.lng;

      const seenIds = new Set();
      const candidates = rawCandidates.filter((item) => {
        const id = Number(item.propertyId);
        if (!id || seenIds.has(id)) return false;

        if (
          item.latitude != null &&
          item.longitude != null &&
          centerLat != null &&
          centerLng != null &&
          searchRadiusKm > 0
        ) {
          const dist = calculateDistanceKm(
            centerLat,
            centerLng,
            Number(item.latitude),
            Number(item.longitude),
          );
          if (dist > searchRadiusKm) return false;
        }

        seenIds.add(id);
        return true;
      });

      properties.value = candidates.map((item) => {
        const isEligible = isEligibleForSafetyCalculation(item);
        return {
          ...item,
          isSafetyLoading: isEligible && item.safetyScore == null,
        };
      });
      updateLastFetchedCenter(centerLat, centerLng);

      // 목적지별 안전점수를 백그라운드(non-blocking)로 일괄 준비 및 병합합니다.
      const dest = destinationConfig.value;
      const uncachedPropertyIds = candidates
        .filter((item) => isEligibleForSafetyCalculation(item) && item.safetyScore == null)
        .map((item) => Number(item.propertyId))
        .filter(Boolean);

      if (
        uncachedPropertyIds.length > 0 &&
        (dest.lat != null || appliedFilterState.value.destinationId != null)
      ) {
        safetyService
          .getScoresForProperties({
            propertyIds: uncachedPropertyIds,
            destinationId: appliedFilterState.value.destinationId || null,
            destinationName: dest.name || appliedFilterState.value.destination || '',
            destinationAddress: appliedFilterState.value.destinationAddress || '',
            destinationLatitude: dest.lat,
            destinationLongitude: dest.lng,
          })
          .then((scoresMap) => {
            if (requestId !== propertyRequestSequence || !scoresMap) return;
            properties.value = properties.value.map((prop) => {
              const score =
                scoresMap?.[prop.propertyId] ?? scoresMap?.[String(prop.propertyId)];
              return {
                ...prop,
                safetyScore: score ?? prop.safetyScore ?? null,
                isSafetyLoading: false,
              };
            });
          })
          .catch((err) => {
            console.warn('Background safety scores batch calculation error:', err);
            properties.value = properties.value.map((prop) => ({
              ...prop,
              isSafetyLoading: false,
            }));
          });
      } else {
        properties.value = properties.value.map((prop) => ({
          ...prop,
          isSafetyLoading: false,
        }));
      }
    } catch (error) {
      if (requestId !== propertyRequestSequence) return;
      isPropertyApiError.value = true;
      properties.value = [];
      serverTotalCount.value = 0;
      console.error('Failed to fetch properties from backend:', error);
    } finally {
      if (requestId === propertyRequestSequence) {
        isPropertyLoading.value = false;
      }
    }
  };

  const loadMoreProperties = async ({ appliedFilterState, destinationConfig, authStore }) => {
    if (isMoreLoading.value || properties.value.length >= serverTotalCount.value) return;

    isMoreLoading.value = true;
    try {
      const nextPage = currentPage.value + 1;
      const searchParams = {
        ...buildPropertySearchParams(appliedFilterState, destinationConfig, authStore),
        page: nextPage,
        size: 200,
      };

      const response = await api.get('/properties', {
        params: searchParams,
        timeout: 5000,
      });

      const resData = response.data;
      if (resData?.totalCount) {
        serverTotalCount.value = Number(resData.totalCount);
      }

      const newItems = Array.isArray(resData?.items)
        ? resData.items
        : Array.isArray(resData)
          ? resData
          : [];

      currentPage.value = nextPage;

      if (newItems.length > 0) {
        const searchRadiusKm = getSearchRadiusKm(appliedFilterState.value);
        const centerLat = searchParams.lat || destinationConfig.value.lat;
        const centerLng = searchParams.lng || destinationConfig.value.lng;

        const existingIds = new Set(properties.value.map((p) => Number(p.propertyId)));

        const filteredNewItems = newItems.filter((item) => {
          const id = Number(item.propertyId);
          if (!id || existingIds.has(id)) return false;

          if (
            item.latitude != null &&
            item.longitude != null &&
            centerLat != null &&
            centerLng != null &&
            searchRadiusKm > 0
          ) {
            const dist = calculateDistanceKm(
              centerLat,
              centerLng,
              Number(item.latitude),
              Number(item.longitude),
            );
            if (dist > searchRadiusKm) return false;
          }

          existingIds.add(id);
          return true;
        });

        if (filteredNewItems.length > 0) {
          const newFormattedItems = filteredNewItems.map((item) => {
            const isEligible = isEligibleForSafetyCalculation(item);
            return {
              ...item,
              isSafetyLoading: isEligible && item.safetyScore == null,
            };
          });

          properties.value = [...properties.value, ...newFormattedItems];

          // 🛡️ 신규 추가된 매물들에 대해서도 목적지별 도커 안전점수를 백그라운드로 일괄 계산 및 병합
          const dest = destinationConfig.value;
          const uncachedPropertyIds = newFormattedItems
            .filter((item) => isEligibleForSafetyCalculation(item) && item.safetyScore == null)
            .map((item) => Number(item.propertyId))
            .filter(Boolean);

          if (
            uncachedPropertyIds.length > 0 &&
            (dest.lat != null || appliedFilterState.value.destinationId != null)
          ) {
            safetyService
              .getScoresForProperties({
                propertyIds: uncachedPropertyIds,
                destinationId: appliedFilterState.value.destinationId || null,
                destinationName: dest.name || appliedFilterState.value.destination || '',
                destinationAddress: appliedFilterState.value.destinationAddress || '',
                destinationLatitude: dest.lat,
                destinationLongitude: dest.lng,
              })
              .then((scoresMap) => {
                if (!scoresMap) return;
                properties.value = properties.value.map((prop) => {
                  const score =
                    scoresMap?.[prop.propertyId] ?? scoresMap?.[String(prop.propertyId)];
                  return {
                    ...prop,
                    safetyScore: score ?? prop.safetyScore ?? null,
                    isSafetyLoading: false,
                  };
                });
              })
              .catch((err) => {
                console.warn('Background safety scores batch calculation error (loadMore):', err);
                properties.value = properties.value.map((prop) => ({
                  ...prop,
                  isSafetyLoading: false,
                }));
              });
          }
        }
      }

      if (nextPage * 200 >= serverTotalCount.value || newItems.length === 0) {
        serverTotalCount.value = properties.value.length;
      }
    } catch (error) {
      console.error('Failed to load more properties:', error);
    } finally {
      isMoreLoading.value = false;
    }
  };

  return {
    properties,
    serverTotalCount,
    currentPage,
    isPropertyLoading,
    isPropertyApiError,
    isMoreLoading,
    showAllLoadedToast,
    triggerAllLoadedToast,
    buildPropertySearchParams,
    doFetchPropertiesFromBackend,
    loadMoreProperties,
  };
}
