<script setup>
// [임시/테스트 전용] 세종대 도보 15분권 매물 안전점수 알고리즘을 눈으로 검증하기 위한
// 독립 디버그 페이지입니다. 기존 NaverMap.vue/지도 상태와 완전히 분리되어 있어서
// 이 페이지의 렌더링 부하가 실제 서비스 화면에 영향을 주지 않습니다.
// 검증이 끝나면 이 파일과 router의 관련 라우트, 백엔드 test 엔드포인트를 지워도 됩니다.
import { onMounted, onUnmounted, ref, watch } from 'vue';
import api from '@/api/api.js';

const DESTINATION_ID = 1; // 세종대 고정

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

const FACILITY_LABEL = {
  CCTV: 'CCTV',
  STREET_LIGHT: '가로등',
  POLICE: '파출소',
};

const mapEl = ref(null);
let mapInstance = null;
let destinationMarker = null;
const routeOverlays = []; // { polyline, label, grade }
const facilityOverlays = []; // { circle, type }

const showRoutes = ref(true);
const showScoreLabel = ref(true);
const showCctv = ref(true);
const showStreetLight = ref(true);
const showPolice = ref(true);

const totalCount = ref(0);
const loadedCount = ref(0);
const isLoading = ref(false);
const loadError = ref('');
const selectedDetail = ref(null);

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

function renderFacilities(facilities) {
  facilities.forEach((facility) => {
    const circle = new window.naver.maps.Circle({
      map: mapInstance,
      center: new window.naver.maps.LatLng(facility.latitude, facility.longitude),
      radius: 6,
      strokeWeight: 0,
      fillColor: FACILITY_COLOR[facility.facilityType] || '#94a3b8',
      fillOpacity: 0.9,
    });
    facilityOverlays.push({ circle, type: facility.facilityType });
  });
  applyFacilityVisibility();
}

function renderRoute(item) {
  const path = item.routePoints.map(
    (point) => new window.naver.maps.LatLng(point.latitude, point.longitude),
  );
  const color = GRADE_COLOR[item.safetyGrade] || '#94a3b8';

  const polyline = new window.naver.maps.Polyline({
    map: mapInstance,
    path,
    strokeColor: color,
    strokeWeight: 4,
    strokeOpacity: 0.85,
  });

  const midPoint = path[Math.floor(path.length / 2)];
  const label = new window.naver.maps.Marker({
    map: mapInstance,
    position: midPoint,
    icon: {
      content: `<div style="background:${color};color:#fff;font-size:10px;font-weight:700;padding:2px 5px;border-radius:999px;white-space:nowrap;box-shadow:0 1px 3px rgba(0,0,0,0.3);">${item.safetyScore}</div>`,
      anchor: new window.naver.maps.Point(12, 10),
    },
  });

  window.naver.maps.Event.addListener(polyline, 'click', () => {
    selectedDetail.value = item;
  });
  window.naver.maps.Event.addListener(label, 'click', () => {
    selectedDetail.value = item;
  });

  routeOverlays.push({ polyline, label });
  applyRouteVisibility();
}

function applyRouteVisibility() {
  routeOverlays.forEach(({ polyline, label }) => {
    polyline.setMap(showRoutes.value ? mapInstance : null);
    label.setMap(showScoreLabel.value ? mapInstance : null);
  });
}

function applyFacilityVisibility() {
  facilityOverlays.forEach(({ circle, type }) => {
    const visible =
      (type === 'CCTV' && showCctv.value) ||
      (type === 'STREET_LIGHT' && showStreetLight.value) ||
      (type === 'POLICE' && showPolice.value);
    circle.setMap(visible ? mapInstance : null);
  });
}

async function loadNextPage() {
  if (isLoading.value) return;
  if (totalCount.value > 0 && loadedCount.value >= totalCount.value) return;

  isLoading.value = true;
  loadError.value = '';
  try {
    const response = await api.get('/safety/test/linestrings', {
      params: { destinationId: DESTINATION_ID, offset: loadedCount.value, limit: 50 },
    });
    const { totalCount: total, items } = response.data;
    totalCount.value = total;
    items.forEach((item) => renderRoute(item));
    loadedCount.value += items.length;
  } catch (error) {
    loadError.value = '경로를 불러오지 못했습니다. 로그인 상태와 서버 상태를 확인해주세요.';
  } finally {
    isLoading.value = false;
  }
}

async function initMap() {
  try {
    await loadNaverSdk();

    const facilityResponse = await api.get('/safety/test/facilities', {
      params: { destinationId: DESTINATION_ID },
    });
    const { destinationLatitude, destinationLongitude, destinationName, facilities } =
      facilityResponse.data;

    mapInstance = new window.naver.maps.Map(mapEl.value, {
      center: new window.naver.maps.LatLng(destinationLatitude, destinationLongitude),
      zoom: 15,
    });

    destinationMarker = new window.naver.maps.Marker({
      map: mapInstance,
      position: new window.naver.maps.LatLng(destinationLatitude, destinationLongitude),
      icon: {
        content: `<div style="background:#111827;color:#fff;font-size:11px;font-weight:700;padding:4px 8px;border-radius:6px;white-space:nowrap;">${destinationName || '목적지'}</div>`,
        anchor: new window.naver.maps.Point(0, 0),
      },
    });

    renderFacilities(facilities);
    await loadNextPage();
  } catch (error) {
    loadError.value = '지도를 초기화하지 못했습니다. 로그인 상태와 서버 상태를 확인해주세요.';
  }
}

watch(showRoutes, applyRouteVisibility);
watch(showScoreLabel, applyRouteVisibility);
watch([showCctv, showStreetLight, showPolice], applyFacilityVisibility);

onMounted(initMap);

onUnmounted(() => {
  routeOverlays.forEach(({ polyline, label }) => {
    polyline.setMap(null);
    label.setMap(null);
  });
  routeOverlays.length = 0;
  facilityOverlays.forEach(({ circle }) => circle.setMap(null));
  facilityOverlays.length = 0;
  if (destinationMarker) destinationMarker.setMap(null);
});
</script>

<template>
  <div class="safety-debug-page">
    <aside class="safety-debug-panel">
      <h2>안전점수 디버그 지도</h2>
      <p class="safety-debug-subtitle">세종대 도보 15분권 · 임시 검증용</p>

      <div class="safety-debug-progress">
        {{ loadedCount }} / {{ totalCount || '?' }} 개 로드됨
      </div>

      <button
        type="button"
        class="safety-debug-load-button"
        :disabled="isLoading || (totalCount > 0 && loadedCount >= totalCount)"
        @click="loadNextPage"
      >
        {{
          isLoading
            ? '불러오는 중...'
            : totalCount > 0 && loadedCount >= totalCount
              ? '전체 로드 완료'
              : '50개 더 불러오기'
        }}
      </button>

      <p v-if="loadError" class="safety-debug-error">{{ loadError }}</p>

      <div class="safety-debug-toggle-group">
        <label><input v-model="showRoutes" type="checkbox" /> 경로선</label>
        <label><input v-model="showScoreLabel" type="checkbox" /> 점수 라벨</label>
        <label><input v-model="showCctv" type="checkbox" /> CCTV</label>
        <label><input v-model="showStreetLight" type="checkbox" /> 가로등</label>
        <label><input v-model="showPolice" type="checkbox" /> 파출소</label>
      </div>

      <div class="safety-debug-legend">
        <span><i style="background: #22a06b"></i> SAFE (80~100)</span>
        <span><i style="background: #e69a1d"></i> WARNING (60~79)</span>
        <span><i style="background: #dc4b5d"></i> DANGER (~59)</span>
      </div>

      <div v-if="selectedDetail" class="safety-debug-detail">
        <h3>매물 #{{ selectedDetail.propertyId }}</h3>
        <p class="safety-debug-detail-score">
          {{ selectedDetail.safetyScore }}점 · {{ selectedDetail.safetyGrade }}
        </p>
        <ul>
          <li>CCTV {{ selectedDetail.breakdown.cctvCount }}개 (커버리지 {{ selectedDetail.breakdown.cctvCoveragePercent }}%)</li>
          <li>가로등 {{ selectedDetail.breakdown.streetLightCount }}개 (커버리지 {{ selectedDetail.breakdown.streetLightCoveragePercent }}%)</li>
          <li>파출소 300m 이내: {{ selectedDetail.breakdown.hasPoliceStation ? 'O' : 'X' }}</li>
          <li>총 페널티: {{ selectedDetail.breakdown.totalPenalty }}</li>
        </ul>
      </div>
    </aside>

    <div ref="mapEl" class="safety-debug-map"></div>
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
  width: 300px;
  flex: 0 0 300px;
  padding: 20px;
  overflow-y: auto;
  border-right: 1px solid #e2e8f0;
  background: #fff;
}

.safety-debug-panel h2 {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.safety-debug-subtitle {
  margin: 0;
  color: #64748b;
  font-size: 12px;
}

.safety-debug-progress {
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

.safety-debug-load-button {
  height: 40px;
  border: 0;
  border-radius: 8px;
  background: #2a60f7;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.safety-debug-load-button:disabled {
  background: #cbd5e1;
  cursor: default;
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
  font-size: 13px;
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

.safety-debug-map {
  flex: 1;
  min-width: 0;
  height: 100%;
}
</style>
