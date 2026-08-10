<script setup>
// [임시/테스트 전용] 세종대 도보 15분권 매물 안전점수 알고리즘을 눈으로 검증하기 위한
// 독립 디버그 페이지입니다. 10개 개별 On/Off 토글 & 실시간 영향권 렌더링 지원.
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

const FACILITY_RADIUS = {
  CCTV: 50,
  STREET_LIGHT: 50,
  POLICE: 300,
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

const isLoading = ref(false);
const loadError = ref('');
const selectedDetail = ref(null);

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

function renderFacilitiesForActiveItems() {
  clearFacilityOverlays();
  if (!allFacilities.value || allFacilities.value.length === 0) return;

  // 현재 활성화(ON)된 매물들만 수집
  const activeItems = currentItems.value.filter((_, idx) =>
    activeItemIndices.value.includes(idx),
  );
  if (activeItems.length === 0) return;

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

  relevantFacilities.forEach((facility) => {
    const color = FACILITY_COLOR[facility.facilityType] || '#94a3b8';
    const radius = FACILITY_RADIUS[facility.facilityType] || 50;

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
        content: `<div style="width:8px;height:8px;background:${color};border:1.5px solid #fff;border-radius:50%;box-shadow:0 1px 3px rgba(0,0,0,0.4);"></div>`,
        anchor: new window.naver.maps.Point(4, 4),
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

async function fetchPage(page) {
  if (isLoading.value) return;
  isLoading.value = true;
  loadError.value = '';
  selectedDetail.value = null;
  clearMapOverlays();

  const offset = (page - 1) * PAGE_SIZE;

  try {
    const response = await api.get('/safety/test/linestrings', {
      params: {
        destinationId: DESTINATION_ID,
        offset,
        limit: PAGE_SIZE,
      },
    });
    const { totalCount: total, items } = response.data;
    totalCount.value = total;
    currentPage.value = page;
    currentItems.value = items || [];
    activeItemIndices.value = currentItems.value.map((_, idx) => idx); // 기본 전체 ON

    renderRoutes(currentItems.value);

    if (currentItems.value.length > 0) {
      selectedDetail.value = { ...currentItems.value[0], displayNumber: 1 };
    }
  } catch (error) {
    loadError.value =
      '경로를 불러오지 못했습니다. 로그인 상태와 서버 상태를 확인해주세요.';
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
    const {
      destinationLatitude,
      destinationLongitude,
      destinationName,
      facilities,
    } = facilityResponse.data;

    allFacilities.value = facilities || [];

    mapInstance = new window.naver.maps.Map(mapEl.value, {
      center: new window.naver.maps.LatLng(
        destinationLatitude,
        destinationLongitude,
      ),
      zoom: 15,
    });

    destinationMarker = new window.naver.maps.Marker({
      map: mapInstance,
      position: new window.naver.maps.LatLng(
        destinationLatitude,
        destinationLongitude,
      ),
      icon: {
        content: `<div style="background:#111827;color:#fff;font-size:12px;font-weight:900;padding:6px 10px;border-radius:8px;white-space:nowrap;box-shadow:0 3px 8px rgba(0,0,0,0.4);border:2px solid #3b82f6;">📍 ${destinationName || '목적지(세종대)'}</div>`,
        anchor: new window.naver.maps.Point(0, 0),
      },
    });

    await fetchPage(1);
  } catch (error) {
    loadError.value =
      '지도를 초기화하지 못했습니다. 로그인 상태와 서버 상태를 확인해주세요.';
  }
}

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
      <p class="safety-debug-subtitle">세종대 도보 15분권 · 10개 단위 개별 ON/OFF 검증</p>

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
        <span class="safety-debug-page-num">{{ currentPage }} P</span>
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
          <label><input v-model="showStreetLight" type="checkbox" /> 가로등 (50m 반경)</label>
          <label><input v-model="showPolice" type="checkbox" /> 파출소 (300m 반경)</label>
        </div>
      </div>

      <div class="safety-debug-legend">
        <span><i style="background: #22a06b"></i> SAFE (80~100점)</span>
        <span><i style="background: #e69a1d"></i> WARNING (60~79점)</span>
        <span><i style="background: #dc4b5d"></i> DANGER (~59점)</span>
      </div>

      <div v-if="selectedDetail" class="safety-debug-detail">
        <h3>매물 #{{ selectedDetail.displayNumber }} (ID: {{ selectedDetail.propertyId }})</h3>
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

.safety-debug-map {
  flex: 1;
  min-width: 0;
  height: 100%;
}
</style>
