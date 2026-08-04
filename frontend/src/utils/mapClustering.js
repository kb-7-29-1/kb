/**
 * 초고속 격자 클러스터링 (Grid Marker Clustering) 및 바운즈 연산 유틸리티
 */

// 클러스터 마커 핀 HTML (축소 상태 시 매물 묶음 핀)
export const renderClusterPinHTML = (count) => {
  return `
    <div class="px-3 py-2 rounded-2xl text-xs font-black shadow-2xl border border-blue-400/50 bg-blue-600/90 text-white backdrop-blur-md transition-all transform -translate-x-1/2 -translate-y-full hover:scale-110 flex items-center gap-1.5 cursor-pointer z-20 select-none">
      <span class="text-sm">🏢</span>
      <span class="text-sm tracking-tight font-extrabold">${count.toLocaleString()}개 매물</span>
    </div>
  `;
};

/**
 * 사용자 세세 조절 커스텀 옵션 세팅
 */
export const DEFAULT_CLUSTER_OPTIONS = {
  minClusterZoom: 16,    // 🔍 15 레벨 이상 확대 시 클러스터 해제 후 개별 매물 핀 표출 (조정가능: 13 ~ 16)
  gridSizeFactor: 0.01, // 📐 클러스터 반경 범위 크기 (작을수록 촘촘하게 쪼개짐, 클수록 크게 뭉침: 0.004 ~ 0.015)
  minClusterCount: 3,    // 🔢 최소 묶음 개수 (2개 이상 뭉쳤을 때만 🏢 클러스터 핀 적용)
};

/**
 * 초고속 격자 클러스터링 알고리즘
 * @param {Array} properties - 매물 목록
 * @param {Number} currentZoom - 현재 네이버 지도 줌 레벨
 * @param {Object} bounds - 현재 화면 가시 영역 (naver.maps.LatLngBounds)
 * @param {Object} customOptions - 사용자 세부 조정 옵션 { minClusterZoom, gridSizeFactor, minClusterCount }
 * @returns {Array} 클러스터 노드 및 개별 매물 노드 리스트
 */
export const getClusteredMarkers = (properties, currentZoom, bounds, customOptions = {}) => {
  if (!properties || properties.length === 0) return [];

  const options = { ...DEFAULT_CLUSTER_OPTIONS, ...customOptions };

  // 1. 줌 레벨이 minClusterZoom 이상으로 확대되면 개별 매물 핀 표출
  if (currentZoom >= options.minClusterZoom) {
    return properties
      .filter((p) => p.latitude && p.longitude)
      .filter((p) =>
        bounds ? bounds.hasLatLng(new window.naver.maps.LatLng(p.latitude, p.longitude)) : true,
      )
      .map((p) => ({ isCluster: false, item: p }));
  }

  // 2. 줌 레벨이 낮을(축소) 때 격자 클러스터링 계산
  const gridSize = options.gridSizeFactor * Math.pow(2, 14 - currentZoom);
  const clusters = new Map();

  properties.forEach((p) => {
    if (!p.latitude || !p.longitude) return;
    const latLng = new window.naver.maps.LatLng(p.latitude, p.longitude);
    if (bounds && !bounds.hasLatLng(latLng)) return;

    const gridX = Math.floor(p.longitude / gridSize);
    const gridY = Math.floor(p.latitude / gridSize);
    const key = `${gridX}_${gridY}`;

    if (!clusters.has(key)) {
      clusters.set(key, {
        isCluster: true,
        count: 0,
        sumLat: 0,
        sumLng: 0,
        items: [],
      });
    }

    const c = clusters.get(key);
    c.count++;
    c.sumLat += p.latitude;
    c.sumLng += p.longitude;
    c.items.push(p);
  });

  const result = [];
  clusters.forEach((c) => {
    if (c.count < options.minClusterCount) {
      c.items.forEach((item) => result.push({ isCluster: false, item }));
    } else {
      result.push({
        isCluster: true,
        count: c.count,
        lat: c.sumLat / c.count,
        lng: c.sumLng / c.count,
        items: c.items,
      });
    }
  });

  return result;
};
