<script setup>
// [임시/테스트 전용] 특정 목적지 도보권 매물 안전점수 알고리즘을 눈으로 검증하기 위한
// 독립 디버그 페이지입니다. 10개 개별 On/Off 토글 & 실시간 영향권 렌더링 지원.
// 목적지 ID를 화면에서 바꿀 수 있고, 실제 조회된 목적지 이름을 항상 같이 표시합니다.
import { onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '@/api/api.js';

const route = useRoute();
const router = useRouter();

// destinations 테이블은 고정 시드 없이 동적 생성되는 테이블이라 ID가 항상
// 같은 장소를 가리킨다고 가정하면 안 됨. URL 쿼리(?destinationId=)로 지정하고,
// 없으면 1을 기본값으로 시도하되 실제 목적지 이름은 API 응답으로 화면에 표시해 확인한다.
const destinationId = ref(Number(route.query.destinationId) || 1);
const destinationIdInput = ref(String(destinationId.value));
const resolvedDestinationName = ref('');

const GRADE_COLOR = {
  SAFE: '#22a06b',
  WARNING: '#e69a1d',
  DANGER: '#dc4b5d',
};

const FACILITY_COLOR = {
  CCTV: '#2a60f7',
  STREET_LIGHT: '#f5b301',
  POLICE: '#7c3aed',
};

// SafetyScoreCalculator.java의 반경 상수와 반드시 일치시켜야 함
const FACILITY_RADIUS = {
  CCTV: 50,
  STREET_LIGHT: 20,
  POLICE: 500,
};

const FACILITY_LABEL = {
  CCTV: '📷 CCTV',
  STREET_LIGHT: '💡 보안등',
  POLICE: '👮 파출소',
};

const mapEl = ref(null);
let mapInstance = null;
let destinationMarker = null;

const routeOverlays = []; // { polyline, startMarker, label, item, index }
const facilityOverlays = []; // { circle, dot, type }

const showRoutes = ref(true);
const showScoreLabel = ref(true);
const showCctv = ref(true);
const showStreetLight = ref(true);
const showPolice = ref(true);
const showInfluenceRadius = ref(true);

const totalCount = ref(0);
const currentPage = ref(1);
const PAGE_SIZE = 10;
const currentItems = ref([]);
const allFacilities = ref([]);
const activeItemIndices = ref([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]);
// 지도에 실제로 찍힌 시설 지점수 vs 대수 합계를 보여줘서 breakdown 숫자와 직접 대조하기 위한 값
const mapFacilitySummary = ref({
  CCTV: { locations: 0, totalCount: 0 },
  STREET_LIGHT: { locations: 0, totalCount: 0 },
  POLICE: { locations: 0, totalCount: 0 },
});

const isLoading = ref(false);
const loadError = ref('');
const selectedDetail = ref(null);

const isStatsModalOpen = ref(false);
const isStatsLoading = ref(false);
const statsData = ref(null);

async function openStatsModal() {
  isStatsModalOpen.value = true;
  if (statsData.value) return;

  isStatsLoading.value = true;
  try {
    const res = await api.get('/safety/test/linestrings', {
      params: {
        destinationId: destinationId.value,
        offset: 0,
        limit: 1000,
      },
    });

    const items = res.data.items || [];
    if (!items.length) return;

    const scores = items.map((i) => Number(i.safetyScore)).sort((a, b) => a - b);
    const totalCnt = scores.length;
    const sum = scores.reduce((acc, v) => acc + v, 0);
    const avg = (sum / totalCnt).toFixed(1);

    const mid = Math.floor(totalCnt / 2);
    const median =
      totalCnt % 2 !== 0
        ? scores[mid]
        : ((scores[mid - 1] + scores[mid]) / 2).toFixed(1);

    const min = scores[0];
    const max = scores[scores.length - 1];

    const variance =
      scores.reduce((acc, v) => acc + Math.pow(v - Number(avg), 2), 0) / totalCnt;
    const stdDev = Math.sqrt(variance).toFixed(1);

    const safeCnt = items.filter((i) => i.safetyGrade === 'SAFE').length;
    const warningCnt = items.filter((i) => i.safetyGrade === 'WARNING').length;
    const dangerCnt = items.filter((i) => i.safetyGrade === 'DANGER').length;

    const histogram = Array(10).fill(0);
    items.forEach((i) => {
      const score = Math.max(0, Math.min(100, Number(i.safetyScore)));
      const bucket = Math.min(9, Math.floor(score / 10));
      histogram[bucket]++;
    });
    const maxBucketCnt = Math.max(...histogram, 1);

    const avgCctvPenalty = (
      items.reduce(
        (acc, i) =>
          acc +
          (i.breakdown?.cctvCoveragePenalty || 0) +
          (i.breakdown?.cctvDensityPenalty || 0),
        0,
      ) / totalCnt
    ).toFixed(1);

    const avgStreetLightPenalty = (
      items.reduce(
        (acc, i) => acc + (i.breakdown?.streetLightCoveragePenalty || 0),
        0,
      ) / totalCnt
    ).toFixed(1);

    const policeMissingCnt = items.filter(
      (i) => !i.breakdown?.hasPoliceStation,
    ).length;
    const policeMissingRatio = ((policeMissingCnt / totalCnt) * 100).toFixed(1);

    statsData.value = {
      totalCnt,
      avg,
      median,
      min,
      max,
      stdDev,
      safeCnt,
      safePct: ((safeCnt / totalCnt) * 100).toFixed(1),
      warningCnt,
      warningPct: ((warningCnt / totalCnt) * 100).toFixed(1),
      dangerCnt,
      dangerPct: ((dangerCnt / totalCnt) * 100).toFixed(1),
      histogram,
      maxBucketCnt,
      avgCctvPenalty,
      avgStreetLightPenalty,
      policeMissingCnt,
      policeMissingRatio,
    };
  } catch (err) {
    console.error('Failed to compute stats:', err);
  } finally {
    isStatsLoading.value = false;
  }
}

function getDistanceMeters(lat1, lon1, lat2, lon2) {
  const R = 6371000;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function loadNaverSdk() {
  return new Promise((resolve, reject) => {
    if (window.naver && window.naver.maps) {
      resolve();
      return;
    }
    const scriptId = 'naver-map-sdk-safety-debug';
    const existing = document.getElementById(scriptId);
    if (existing) {
      existing.addEventListener('load', () => resolve());
      existing.addEventListener('error', reject);
      return;
    }
    const clientId = import.meta.env.VITE_NAVER_CLIENT_ID;
    const script = document.createElement('script');
    script.id = scriptId;
    script.type = 'text/javascript';
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${clientId}`;
    script.onload = () => resolve();
    script.onerror = reject;
    document.head.appendChild(script);
  });
}

function clearFacilityOverlays() {
  facilityOverlays.forEach(({ circle, dot }) => {
    if (circle) circle.setMap(null);
    if (dot) dot.setMap(null);
  });
  facilityOverlays.length = 0;
}

function clearMapOverlays() {
  routeOverlays.forEach(({ polyline, startMarker, label }) => {
    if (polyline) polyline.setMap(null);
    if (startMarker) startMarker.setMap(null);
    if (label) label.setMap(null);
  });
  routeOverlays.length = 0;
  clearFacilityOverlays();
}

function resetMapFacilitySummary() {
  mapFacilitySummary.value = {
    CCTV: { locations: 0, totalCount: 0 },
    STREET_LIGHT: { locations: 0, totalCount: 0 },
    POLICE: { locations: 0, totalCount: 0 },
  };
}

function renderFacilitiesForActiveItems() {
  clearFacilityOverlays();
  if (!allFacilities.value || allFacilities.value.length === 0) {
    resetMapFacilitySummary();
    return;
  }

  // 현재 활성화(ON)된 매물들만 수집
  const activeItems = currentItems.value.filter((_, idx) =>
    activeItemIndices.value.includes(idx),
  );
  if (activeItems.length === 0) {
    resetMapFacilitySummary();
    return;
  }

  const routePoints = [];
  activeItems.forEach((item) => {
    if (item.routePoints) {
      item.routePoints.forEach((pt) => routePoints.push(pt));
    }
  });

  // 활성화된 경로 영향권 내의 시설물만 추출
  const relevantFacilities = allFacilities.value.filter((facility) => {
    const radiusThreshold = FACILITY_RADIUS[facility.facilityType] || 50;
    return routePoints.some(
      (pt) =>
        getDistanceMeters(
          pt.latitude,
          pt.longitude,
          facility.latitude,
          facility.longitude,
        ) <= radiusThreshold,
    );
  });

  // 지점수 vs 대수(facility_count) 합계 집계 — breakdown의 cctvCount 등과 직접 비교용
  const summary = {
    CCTV: { locations: 0, totalCount: 0 },
    STREET_LIGHT: { locations: 0, totalCount: 0 },
    POLICE: { locations: 0, totalCount: 0 },
  };
  relevantFacilities.forEach((facility) => {
    const bucket = summary[facility.facilityType];
    if (!bucket) return;
    bucket.locations += 1;
    bucket.totalCount += Math.max(1, Number(facility.facilityCount) || 1);
  });
  mapFacilitySummary.value = summary;

  relevantFacilities.forEach((facility) => {
    const color = FACILITY_COLOR[facility.facilityType] || '#94a3b8';
    const radius = FACILITY_RADIUS[facility.facilityType] || 50;
    const labelText = FACILITY_LABEL[facility.facilityType] || facility.facilityType;
    const unitCount = Math.max(1, Number(facility.facilityCount) || 1);
    const countBadge = unitCount > 1 ? ` ×${unitCount}` : '';

    const circle = new window.naver.maps.Circle({
      map: mapInstance,
      center: new window.naver.maps.LatLng(facility.latitude, facility.longitude),
      radius: radius,
      strokeWeight: 1.5,
      strokeColor: color,
      strokeOpacity: 0.8,
      fillColor: color,
      fillOpacity: 0.15,
    });

    const dot = new window.naver.maps.Marker({
      map: mapInstance,
      position: new window.naver.maps.LatLng(facility.latitude, facility.longitude),
      icon: {
        content: `<div style="background:${color};color:#fff;font-size:9px;font-weight:900;padding:1.5px 5px;border-radius:4px;white-space:nowrap;box-shadow:0 1.5px 4px rgba(0,0,0,0.35);border:1px solid #fff;">${labelText}${countBadge}</div>`,
        anchor: new window.naver.maps.Point(20, 8),
      },
    });

    facilityOverlays.push({ circle, dot, type: facility.facilityType });
  });

  applyFacilityVisibility();
}

function renderRoutes(items) {
  const bounds = new window.naver.maps.LatLngBounds();

  items.forEach((item, index) => {
    const displayNumber = index + 1;
    const path = item.routePoints.map(
      (point) => new window.naver.maps.LatLng(point.latitude, point.longitude),
    );
    path.forEach((pt) => bounds.extend(pt));

    const color = GRADE_COLOR[item.safetyGrade] || '#94a3b8';

    const polyline = new window.naver.maps.Polyline({
      map: mapInstance,
      path,
      strokeColor: color,
      strokeWeight: 5,
      strokeOpacity: 0.85,
    });

    const startPoint = path[0];
    const startMarker = new window.naver.maps.Marker({
      map: mapInstance,
      position: startPoint,
      icon: {
        content: `<div class="marker-btn-${index}" style="background:${color};color:#fff;font-size:11px;font-weight:900;width:24px;height:24px;line-height:24px;text-align:center;border-radius:50%;box-shadow:0 2px 5px rgba(0,0,0,0.4);border:2px solid #fff;cursor:pointer;">${displayNumber}</div>`,
        anchor: new window.naver.maps.Point(12, 12),
      },
    });

    const midPoint = path[Math.floor(path.length / 2)];
    const label = new window.naver.maps.Marker({
      map: mapInstance,
      position: midPoint,
      icon: {
        content: `<div style="background:${color};color:#fff;font-size:11px;font-weight:800;padding:2px 7px;border-radius:999px;white-space:nowrap;box-shadow:0 2px 4px rgba(0,0,0,0.3);border:1px solid rgba(255,255,255,0.8);cursor:pointer;">#${displayNumber} · ${item.safetyScore}점</div>`,
        anchor: new window.naver.maps.Point(20, 10),
      },
    });

    const selectThisItem = () => {
      selectedDetail.value = { ...item, displayNumber };
    };

    window.naver.maps.Event.addListener(polyline, 'click', selectThisItem);
    window.naver.maps.Event.addListener(startMarker, 'click', selectThisItem);
    window.naver.maps.Event.addListener(label, 'click', selectThisItem);

    routeOverlays.push({ polyline, startMarker, label, item, index });
  });

  if (items.length > 0 && mapInstance) {
    mapInstance.fitBounds(bounds, { top: 50, right: 50, bottom: 50, left: 50 });
  }

  updateItemVisibilities();
}

function updateItemVisibilities() {
  routeOverlays.forEach(({ polyline, startMarker, label, item, index }) => {
    const isActive = activeItemIndices.value.includes(index);
    const color = GRADE_COLOR[item.safetyGrade] || '#94a3b8';
    const displayNumber = index + 1;

    if (isActive) {
      // ON 상태: 정상 선명한 컬러 & 라벨 표시
      if (polyline) {
        polyline.setOptions({
          strokeColor: color,
          strokeOpacity: 0.9,
          strokeWeight: 5,
        });
        polyline.setMap(showRoutes.value ? mapInstance : null);
      }
      if (startMarker) {
        startMarker.setIcon({
          content: `<div style="background:${color};color:#fff;font-size:11px;font-weight:900;width:24px;height:24px;line-height:24px;text-align:center;border-radius:50%;box-shadow:0 2px 5px rgba(0,0,0,0.4);border:2px solid #fff;cursor:pointer;">${displayNumber}</div>`,
          anchor: new window.naver.maps.Point(12, 12),
        });
        startMarker.setMap(mapInstance);
      }
      if (label) {
        label.setMap(showScoreLabel.value ? mapInstance : null);
      }
    } else {
      // OFF 상태: 은은한 회색 투명선 & 어두운 마커 & 라벨 숨김
      if (polyline) {
        polyline.setOptions({
          strokeColor: '#cbd5e1',
          strokeOpacity: 0.2,
          strokeWeight: 2.5,
        });
        polyline.setMap(showRoutes.value ? mapInstance : null);
      }
      if (startMarker) {
        startMarker.setIcon({
          content: `<div style="background:#94a3b8;color:#fff;font-size:10px;font-weight:700;width:20px;height:20px;line-height:20px;text-align:center;border-radius:50%;opacity:0.4;border:1px solid #fff;cursor:pointer;">${displayNumber}</div>`,
          anchor: new window.naver.maps.Point(10, 10),
        });
        startMarker.setMap(mapInstance);
      }
      if (label) {
        label.setMap(null);
      }
    }
  });

  renderFacilitiesForActiveItems();
}

function applyFacilityVisibility() {
  facilityOverlays.forEach(({ circle, dot, type }) => {
    const typeVisible =
      (type === 'CCTV' && showCctv.value) ||
      (type === 'STREET_LIGHT' && showStreetLight.value) ||
      (type === 'POLICE' && showPolice.value);

    if (circle) circle.setMap(typeVisible && showInfluenceRadius.value ? mapInstance : null);
    if (dot) dot.setMap(typeVisible ? mapInstance : null);
  });
}

function toggleItemIndex(idx) {
  if (activeItemIndices.value.includes(idx)) {
    activeItemIndices.value = activeItemIndices.value.filter((i) => i !== idx);
  } else {
    activeItemIndices.value = [...activeItemIndices.value, idx].sort((a, b) => a - b);
  }
  updateItemVisibilities();
}

function selectAllItems() {
  activeItemIndices.value = currentItems.value.map((_, idx) => idx);
  updateItemVisibilities();
}

function deselectAllItems() {
  activeItemIndices.value = [];
  updateItemVisibilities();
}

async function fetchPage(page, updateUrl = true) {
  if (isLoading.value) return;
  isLoading.value = true;
  loadError.value = '';
  selectedDetail.value = null;
  clearMapOverlays();

  const targetPage = Math.max(1, Number(page) || 1);
  const offset = (targetPage - 1) * PAGE_SIZE;

  try {
    const response = await api.get('/safety/test/linestrings', {
      params: {
        destinationId: destinationId.value,
        offset,
        limit: PAGE_SIZE,
      },
    });
    const { totalCount: total, items } = response.data;
    totalCount.value = total;
    currentPage.value = targetPage;
    currentItems.value = items || [];
    activeItemIndices.value = currentItems.value.map((_, idx) => idx); // 기본 전체 ON

    renderRoutes(currentItems.value);

    if (currentItems.value.length > 0) {
      selectedDetail.value = { ...currentItems.value[0], displayNumber: 1 };
    }

    if (updateUrl && Number(route.query.page) !== targetPage) {
      router.replace({ query: { ...route.query, page: targetPage } });
    }
  } catch (error) {
    loadError.value =
      '경로를 불러오지 못했습니다. 로그인 상태와 서버 상태를 확인해주세요.';
  } finally {
    isLoading.value = false;
  }
}

async function loadDestination({ resetPage = true } = {}) {
  const facilityResponse = await api.get('/safety/test/facilities', {
    params: { destinationId: destinationId.value },
  });
  const {
    destinationLatitude,
    destinationLongitude,
    destinationName,
    facilities,
  } = facilityResponse.data;

  resolvedDestinationName.value = destinationName || '(이름 없음)';
  allFacilities.value = facilities || [];

  const center = new window.naver.maps.LatLng(
    destinationLatitude,
    destinationLongitude,
  );
  mapInstance.setCenter(center);
  mapInstance.setZoom(15);

  if (destinationMarker) destinationMarker.setMap(null);
  destinationMarker = new window.naver.maps.Marker({
    map: mapInstance,
    position: center,
    icon: {
      content: `<div style="background:#111827;color:#fff;font-size:12px;font-weight:900;padding:6px 10px;border-radius:8px;white-space:nowrap;box-shadow:0 3px 8px rgba(0,0,0,0.4);border:2px solid #3b82f6;">📍 ${destinationName || `목적지 #${destinationId.value}`}</div>`,
      anchor: new window.naver.maps.Point(0, 0),
    },
  });

  const initialPage = resetPage ? 1 : Math.max(1, Number(route.query.page) || 1);
  await fetchPage(initialPage, false);
}

async function initMap() {
  try {
    await loadNaverSdk();

    mapInstance = new window.naver.maps.Map(mapEl.value, {
      center: new window.naver.maps.LatLng(37.5502, 127.0764),
      zoom: 15,
    });

    await loadDestination({ resetPage: false });
  } catch (error) {
    loadError.value =
      '지도를 초기화하지 못했습니다. 로그인 상태와 서버 상태를 확인해주세요.';
  }
}

async function applyDestinationId() {
  const parsed = Number(destinationIdInput.value);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    loadError.value = '유효한 목적지 ID를 입력해주세요.';
    return;
  }

  destinationId.value = parsed;
  router.replace({ query: { ...route.query, destinationId: parsed, page: 1 } });

  statsData.value = null; // 목적지가 바뀌면 이전 목적지 통계 캐시를 버림
  loadError.value = '';
  try {
    await loadDestination();
  } catch (error) {
    loadError.value =
      '목적지를 불러오지 못했습니다. destinationId가 존재하는지 확인해주세요.';
  }
}

watch(
  () => route.query.page,
  (newPage) => {
    const parsedPage = Math.max(1, Number(newPage) || 1);
    if (parsedPage !== currentPage.value) {
      fetchPage(parsedPage, false);
    }
  },
);

watch(showRoutes, updateItemVisibilities);
watch(showScoreLabel, updateItemVisibilities);
watch([showCctv, showStreetLight, showPolice, showInfluenceRadius], applyFacilityVisibility);

onMounted(initMap);

onUnmounted(() => {
  clearMapOverlays();
  if (destinationMarker) destinationMarker.setMap(null);
});
</script>

<template>
  <div class="safety-debug-page">
    <aside class="safety-debug-panel">
      <h2>안전점수 디버그 지도</h2>
      <p class="safety-debug-subtitle">
        도보 15분권 · 10개 단위 개별 ON/OFF 검증 ·
        목적지: <strong>{{ resolvedDestinationName || '로딩중...' }}</strong> (ID {{ destinationId }})
      </p>

      <div class="safety-debug-destination-input">
        <input
          v-model="destinationIdInput"
          type="number"
          min="1"
          placeholder="destinationId"
          @keydown.enter="applyDestinationId"
        />
        <button type="button" :disabled="isLoading" @click="applyDestinationId">
          목적지 변경
        </button>
      </div>
      <p class="safety-debug-hint">
        destinations 테이블은 검색/선택할 때마다 동적으로 생성되는 테이블이라
        ID가 항상 같은 장소를 가리키지 않습니다. 위 목적지 이름으로 실제 조회된 곳이
        맞는지 꼭 확인하세요.
      </p>

      <div class="safety-debug-progress">
        총 {{ totalCount }}개 중 <strong>{{ (currentPage - 1) * PAGE_SIZE + 1 }} ~ {{ Math.min(currentPage * PAGE_SIZE, totalCount) }}번</strong> 표시
      </div>

      <!-- 페이지 이동 버튼 -->
      <div class="safety-debug-pagination">
        <button
          type="button"
          class="safety-debug-page-button"
          :disabled="isLoading || currentPage === 1"
          @click="fetchPage(currentPage - 1)"
        >
          ◀ 이전 10개
        </button>
        <div class="flex items-center gap-1">
          <input
            type="number"
            min="1"
            :max="Math.ceil(totalCount / PAGE_SIZE) || 1"
            :value="currentPage"
            class="w-12 h-8 text-center font-black border border-slate-300 rounded-lg text-xs bg-white text-slate-800 focus:outline-none focus:border-blue-600 shadow-xs"
            @change="fetchPage(Number($event.target.value))"
          />
          <span class="text-xs font-bold text-slate-500">P</span>
        </div>
        <button
          type="button"
          class="safety-debug-page-button"
          :disabled="isLoading || currentPage * PAGE_SIZE >= totalCount"
          @click="fetchPage(currentPage + 1)"
        >
          다음 10개 ▶
        </button>
      </div>

      <p v-if="loadError" class="safety-debug-error">{{ loadError }}</p>

      <!-- 10개 매물 개별 ON/OFF 및 전체 제어 -->
      <div v-if="currentItems.length > 0" class="space-y-2 pt-1">
        <div class="flex items-center justify-between">
          <span class="text-xs font-bold text-slate-700">매물 개별 토글 (#1~#10)</span>
          <div class="flex items-center gap-1">
            <button
              type="button"
              class="text-[11px] font-extrabold text-blue-600 hover:underline px-1"
              @click="selectAllItems"
            >
              전체 ON
            </button>
            <span class="text-slate-300 text-[10px]">|</span>
            <button
              type="button"
              class="text-[11px] font-bold text-slate-400 hover:text-slate-600 hover:underline px-1"
              @click="deselectAllItems"
            >
              전체 OFF
            </button>
          </div>
        </div>

        <div class="safety-debug-item-list">
          <button
            v-for="(item, idx) in currentItems"
            :key="item.propertyId"
            type="button"
            class="safety-debug-item-chip"
            :class="{
              active: activeItemIndices.includes(idx),
              selected: selectedDetail?.propertyId === item.propertyId,
            }"
            @click="
              toggleItemIndex(idx);
              selectedDetail = { ...item, displayNumber: idx + 1 };
            "
          >
            <span class="chip-num">#{{ idx + 1 }}</span>
            <span
              class="chip-score"
              :style="{ color: activeItemIndices.includes(idx) ? GRADE_COLOR[item.safetyGrade] : '#94a3b8' }"
            >
              {{ item.safetyScore }}점
            </span>
          </button>
        </div>
      </div>

      <div class="safety-debug-toggle-group">
        <label><input v-model="showRoutes" type="checkbox" /> 경로선 표시</label>
        <label><input v-model="showScoreLabel" type="checkbox" /> 점수 라벨</label>
        <label><input v-model="showInfluenceRadius" type="checkbox" /> 안전 시설 영향권 반경 원형</label>
        <div class="toggle-sub-group">
          <label><input v-model="showCctv" type="checkbox" /> CCTV (50m 반경)</label>
          <label><input v-model="showStreetLight" type="checkbox" /> 가로등 (15m 반경)</label>
          <label><input v-model="showPolice" type="checkbox" /> 파출소 (500m 반경)</label>
        </div>
      </div>

      <div class="safety-debug-legend">
        <div class="font-bold text-slate-700 text-[11px] mb-1">범례 (Legend)</div>
        <span><i style="background: #22a06b"></i> SAFE (80~100점)</span>
        <span><i style="background: #e69a1d"></i> WARNING (60~79점)</span>
        <span><i style="background: #dc4b5d"></i> DANGER (~59점)</span>
        <div class="border-t pt-1 mt-1 space-y-0.5 text-[11px]">
          <span><i style="background: #2a60f7"></i> 📷 파란색 = CCTV (50m 반경)</span>
          <span><i style="background: #f5b301"></i> 💡 노란색 = 보안등/가로등 (15m 반경)</span>
          <span><i style="background: #7c3aed"></i> 👮 보라색 = 파출소 (500m 반경)</span>
        </div>
      </div>

      <div v-if="selectedDetail" class="safety-debug-detail">
        <h3>매물 #{{ selectedDetail.displayNumber }} (ID: {{ selectedDetail.propertyId }})</h3>
        <p class="safety-debug-detail-score">
          {{ selectedDetail.safetyScore }}점 · {{ selectedDetail.safetyGrade }}
        </p>
        <ul>
          <li>CCTV {{ selectedDetail.breakdown.cctvCount }}개 (커버리지 {{ selectedDetail.breakdown.cctvCoveragePercent }}%)</li>
          <li>가로등 {{ selectedDetail.breakdown.streetLightCount }}개 (커버리지 {{ selectedDetail.breakdown.streetLightCoveragePercent }}%)</li>
          <li>파출소 500m 이내: {{ selectedDetail.breakdown.hasPoliceStation ? 'O' : 'X' }}</li>
          <li>총 페널티: {{ selectedDetail.breakdown.totalPenalty }}</li>
        </ul>
      </div>

      <div class="safety-debug-map-summary">
        <div class="font-bold text-slate-700 text-[11px] mb-1">
          지도에 표시된 시설 (현재 켜진 매물 전체 기준)
        </div>
        <p class="safety-debug-map-summary-hint">
          매물을 1개만 켠 상태에서 봐야 좌측 breakdown 숫자와 정확히 비교됩니다.
        </p>
        <ul>
          <li>
            📷 CCTV: {{ mapFacilitySummary.CCTV.locations }}개 지점 · 총
            {{ mapFacilitySummary.CCTV.totalCount }}대
          </li>
          <li>
            💡 가로등: {{ mapFacilitySummary.STREET_LIGHT.locations }}개 지점 · 총
            {{ mapFacilitySummary.STREET_LIGHT.totalCount }}대
          </li>
          <li>
            👮 파출소: {{ mapFacilitySummary.POLICE.locations }}개 지점 · 총
            {{ mapFacilitySummary.POLICE.totalCount }}대
          </li>
        </ul>
      </div>

      <!-- 📊 안전점수 통계 분석 모달 버튼 -->
      <button
        type="button"
        class="w-full mt-3 py-2.5 px-3 rounded-xl bg-gradient-to-r from-slate-900 to-slate-800 hover:from-blue-700 hover:to-indigo-700 text-white text-xs font-black shadow-md transition-all active:scale-95 flex items-center justify-center gap-2 border border-slate-700"
        @click="openStatsModal"
      >
        <span class="text-sm">📊</span>
        <span>세종대 540여개 전체 통계 분석 모달</span>
      </button>
    </aside>

    <div ref="mapEl" class="safety-debug-map"></div>

    <!-- 📊 세종대 540개 매물 안전점수 통계 분석 모달 -->
    <Transition name="fade">
      <div
        v-if="isStatsModalOpen"
        class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 backdrop-blur-md p-4 pointer-events-auto overflow-y-auto"
        @click.self="isStatsModalOpen = false"
      >
        <div
          class="relative w-full max-w-2xl bg-white rounded-3xl shadow-2xl border border-slate-200 overflow-hidden my-auto max-h-[90vh] flex flex-col"
        >
          <!-- 헤더 -->
          <div class="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/80">
            <div class="flex items-center gap-2">
              <span class="text-lg">📊</span>
              <div>
                <h3 class="text-base font-black text-slate-900 leading-tight">세종대 도보 15분권 안전점수 수치 통계</h3>
                <p class="text-xs text-slate-500 font-medium mt-0.5">전체 {{ statsData?.totalCnt || 540 }}개 매물의 분포 및 정규분포 히스토그램</p>
              </div>
            </div>
            <button
              type="button"
              class="w-8 h-8 flex items-center justify-center rounded-full text-slate-400 hover:text-slate-700 hover:bg-slate-200/60 font-bold transition-all text-sm"
              @click="isStatsModalOpen = false"
            >
              ✕
            </button>
          </div>

          <!-- 로딩 상태 -->
          <div v-if="isStatsLoading" class="p-12 text-center space-y-3">
            <i class="fa-solid fa-spinner animate-spin text-2xl text-blue-600"></i>
            <p class="text-xs font-bold text-slate-600">540여개 매물의 점수를 실시간 집계 계산 중이에요...</p>
          </div>

          <!-- 메인 데이터 콘텐츠 -->
          <div v-else-if="statsData" class="p-6 overflow-y-auto space-y-6">
            <!-- 1. 핵심 수치 요약 카운터 (4 컬럼) -->
            <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div class="p-3.5 rounded-2xl bg-slate-50 border border-slate-100 text-center">
                <span class="text-[11px] font-bold text-slate-400 block">평균 점수 (Mean)</span>
                <strong class="text-xl font-black text-blue-600 mt-1 block">{{ statsData.avg }}점</strong>
              </div>
              <div class="p-3.5 rounded-2xl bg-slate-50 border border-slate-100 text-center">
                <span class="text-[11px] font-bold text-slate-400 block">중앙값 (Median)</span>
                <strong class="text-xl font-black text-indigo-600 mt-1 block">{{ statsData.median }}점</strong>
              </div>
              <div class="p-3.5 rounded-2xl bg-slate-50 border border-slate-100 text-center">
                <span class="text-[11px] font-bold text-slate-400 block">최저 / 최고점</span>
                <strong class="text-xl font-black text-slate-800 mt-1 block">{{ statsData.min }}~{{ statsData.max }}점</strong>
              </div>
              <div class="p-3.5 rounded-2xl bg-slate-50 border border-slate-100 text-center">
                <span class="text-[11px] font-bold text-slate-400 block">표준 편차 (σ)</span>
                <strong class="text-xl font-black text-slate-700 mt-1 block">±{{ statsData.stdDev }}점</strong>
              </div>
            </div>

            <!-- 2. 점수 구간별 정규분포 히스토그램 바 차트 -->
            <div class="p-4 rounded-2xl border border-slate-200 bg-slate-50/50 space-y-3">
              <div class="flex items-center justify-between">
                <h4 class="text-xs font-black text-slate-800 flex items-center gap-1.5">
                  <span>📈</span>
                  <span>점수 구간별 분포 (정규분포 히스토그램)</span>
                </h4>
                <span class="text-[11px] text-slate-400 font-medium">10점 단위 구간</span>
              </div>

              <!-- Bar Chart -->
              <div class="flex items-end gap-1.5 h-36 pt-4 pb-2 px-2 bg-white rounded-xl border border-slate-100">
                <div
                  v-for="(count, idx) in statsData.histogram"
                  :key="idx"
                  class="flex-1 flex flex-col items-center gap-1 group relative h-full justify-end"
                >
                  <!-- Tooltip -->
                  <div class="absolute -top-7 opacity-0 group-hover:opacity-100 transition-opacity bg-slate-900 text-white text-[10px] font-bold px-1.5 py-0.5 rounded shadow pointer-events-none whitespace-nowrap z-10">
                    {{ idx * 10 }}~{{ idx * 10 + 9 }}점: {{ count }}개
                  </div>
                  <!-- Bar Value -->
                  <span class="text-[9px] font-extrabold text-slate-500 leading-none mb-0.5">{{ count }}</span>
                  <!-- Bar Fill -->
                  <div
                    class="w-full rounded-t-md transition-all duration-300 group-hover:brightness-110"
                    :style="{
                      height: `${Math.max(6, (count / statsData.maxBucketCnt) * 100)}%`,
                      backgroundColor: idx >= 8 ? '#22a06b' : idx >= 6 ? '#e69a1d' : '#dc4b5d',
                    }"
                  ></div>
                  <!-- Axis Label -->
                  <span class="text-[9px] font-bold text-slate-400 leading-none shrink-0 mt-1">{{ idx * 10 }}대</span>
                </div>
              </div>
            </div>

            <!-- 3. 안전 등급별 비율 (SAFE / WARNING / DANGER) -->
            <div class="space-y-2">
              <h4 class="text-xs font-black text-slate-800 flex items-center gap-1.5">
                <span>🏷️</span>
                <span>안전 등급별 분포 비율</span>
              </h4>
              <div class="grid grid-cols-3 gap-2">
                <div class="p-3 rounded-xl bg-emerald-50/70 border border-emerald-100 text-center">
                  <div class="text-[11px] font-black text-emerald-700">🟢 SAFE (80점~)</div>
                  <div class="text-base font-black text-emerald-800 mt-1">{{ statsData.safeCnt }}개 <span class="text-xs opacity-75">({{ statsData.safePct }}%)</span></div>
                </div>
                <div class="p-3 rounded-xl bg-amber-50/70 border border-amber-100 text-center">
                  <div class="text-[11px] font-black text-amber-700">🟡 WARNING (60~79점)</div>
                  <div class="text-base font-black text-amber-800 mt-1">{{ statsData.warningCnt }}개 <span class="text-xs opacity-75">({{ statsData.warningPct }}%)</span></div>
                </div>
                <div class="p-3 rounded-xl bg-rose-50/70 border border-rose-100 text-center">
                  <div class="text-[11px] font-black text-rose-700">🔴 DANGER (~59점)</div>
                  <div class="text-base font-black text-rose-800 mt-1">{{ statsData.dangerCnt }}개 <span class="text-xs opacity-75">({{ statsData.dangerPct }}%)</span></div>
                </div>
              </div>
            </div>

            <!-- 4. 3대 주요 감점 요인 평균 -->
            <div class="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-2.5">
              <h4 class="text-xs font-black text-slate-800 flex items-center gap-1.5">
                <span>⚠️</span>
                <span>3대 치안 요소별 평균 감점 분석</span>
              </h4>
              <ul class="text-xs space-y-1.5 text-slate-700 font-medium">
                <li class="flex items-center justify-between bg-white p-2 rounded-lg border border-slate-100">
                  <span>💡 가로등(보안등) 사각지대 평균 감점</span>
                  <strong class="text-amber-600 font-black">-{{ statsData.avgStreetLightPenalty }}점</strong>
                </li>
                <li class="flex items-center justify-between bg-white p-2 rounded-lg border border-slate-100">
                  <span>📷 CCTV 사각지대 평균 감점</span>
                  <strong class="text-blue-600 font-black">-{{ statsData.avgCctvPenalty }}점</strong>
                </li>
                <li class="flex items-center justify-between bg-white p-2 rounded-lg border border-slate-100">
                  <span>👮 파출소 100m 이내 미존재 비율</span>
                  <strong class="text-rose-600 font-black">{{ statsData.policeMissingRatio }}% <span class="text-[11px] text-slate-400 font-normal">({{ statsData.policeMissingCnt }}개 매물)</span></strong>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.safety-debug-page {
  display: flex;
  width: 100%;
  height: 100dvh;
}

.safety-debug-panel {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 320px;
  flex: 0 0 320px;
  padding: 16px;
  overflow-y: auto;
  border-right: 1px solid #e2e8f0;
  background: #fff;
}

.safety-debug-panel h2 {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
  font-weight: 800;
}

.safety-debug-subtitle {
  margin: 0;
  color: #64748b;
  font-size: 12px;
}

.safety-debug-destination-input {
  display: flex;
  gap: 6px;
}

.safety-debug-destination-input input {
  flex: 1;
  height: 32px;
  padding: 0 8px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 12px;
}

.safety-debug-destination-input button {
  height: 32px;
  padding: 0 10px;
  border: 0;
  border-radius: 8px;
  background: #0f172a;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.safety-debug-destination-input button:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}

.safety-debug-hint {
  margin: 0;
  color: #94a3b8;
  font-size: 11px;
  line-height: 1.5;
}

.safety-debug-progress {
  color: #334155;
  font-size: 13px;
}

.safety-debug-pagination {
  display: flex;
  align-items: center;
  gap: 8px;
}

.safety-debug-page-button {
  flex: 1;
  height: 36px;
  border: 0;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.safety-debug-page-button:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}

.safety-debug-page-num {
  font-size: 12px;
  font-weight: 800;
  color: #475569;
  min-width: 40px;
  text-align: center;
}

.safety-debug-item-list {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 4px;
}

.safety-debug-item-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 5px 2px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f1f5f9;
  opacity: 0.55;
  cursor: pointer;
  transition: all 0.15s ease;
}

.safety-debug-item-chip:hover {
  opacity: 0.85;
  border-color: #94a3b8;
}

.safety-debug-item-chip.active {
  background: #ffffff;
  border-color: #2563eb;
  box-shadow: 0 1px 3px rgba(37, 99, 235, 0.2);
  opacity: 1;
}

.safety-debug-item-chip.selected {
  outline: 2px solid #2563eb;
  outline-offset: -1px;
}

.chip-num {
  font-size: 11px;
  font-weight: 900;
  color: #1e293b;
}

.chip-score {
  font-size: 10px;
  font-weight: 700;
}

.safety-debug-error {
  margin: 0;
  color: #dc4b5d;
  font-size: 12px;
}

.safety-debug-toggle-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  font-size: 12px;
  font-weight: 600;
}

.toggle-sub-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 6px;
  margin-top: 4px;
  border-top: 1px dashed #cbd5e1;
  padding-left: 8px;
}

.safety-debug-legend {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 11px;
  color: #475569;
}

.safety-debug-legend i {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 6px;
  border-radius: 50%;
}

.safety-debug-detail {
  padding: 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #eff6ff;
}

.safety-debug-detail h3 {
  margin: 0 0 6px;
  color: #1e3a8a;
  font-size: 13px;
}

.safety-debug-detail-score {
  margin: 0 0 8px;
  color: #1e3a8a;
  font-size: 15px;
  font-weight: 700;
}

.safety-debug-detail ul {
  margin: 0;
  padding-left: 16px;
  color: #334155;
  font-size: 12px;
  line-height: 1.6;
}

.safety-debug-map-summary {
  padding: 10px 12px;
  border: 1px solid #fde68a;
  border-radius: 8px;
  background: #fffbeb;
}

.safety-debug-map-summary-hint {
  margin: 0 0 6px;
  color: #92400e;
  font-size: 10px;
  line-height: 1.4;
}

.safety-debug-map-summary ul {
  margin: 0;
  padding-left: 16px;
  color: #334155;
  font-size: 12px;
  line-height: 1.6;
}

.safety-debug-map {
  flex: 1;
  min-width: 0;
  height: 100%;
}
</style>
