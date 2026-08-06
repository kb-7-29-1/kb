/**
 * 초고속 격자 클러스터링 (Grid Marker Clustering) 및 바운즈 연산 유틸리티
 */

// 매물 마커 핀 HTML 렌더러 (순수 초고속 HTML 스트링 템플릿)
export const renderPropertyPinHTML = (prop, isSelected) => {
  const safetyScore = prop.safetyScore || 85;
  let theme = {
    border: 'border-emerald-500',
    badge: 'bg-emerald-500/10 text-emerald-600',
    background: 'bg-emerald-500',
    pointer: 'bg-emerald-500',
  };

  if (safetyScore < 60) {
    theme = {
      border: 'border-rose-500',
      badge: 'bg-rose-500/10 text-rose-600',
      background: 'bg-rose-500',
      pointer: 'bg-rose-500',
    };
  } else if (safetyScore < 80) {
    theme = {
      border: 'border-amber-500',
      badge: 'bg-amber-500/10 text-amber-600',
      background: 'bg-amber-500',
      pointer: 'bg-amber-500',
    };
  }

  const deposit = Number(prop.deposit || 0);
  const rent = Number(prop.monthlyRent || 0);
  const depositText =
    deposit >= 10000
      ? `${(Math.floor((deposit / 10000) * 10) / 10).toFixed(1).replace(/\.0$/, '')}억`
      : `${deposit.toLocaleString()}만`;

  const priceText = rent === 0 ? `전세 ${depositText}` : `${depositText}/${rent}`;
  const selectedStyle = isSelected
    ? `${theme.background} text-white z-30`
    : 'bg-white text-slate-800 hover:-translate-y-0.5 z-10';
  const badgeStyle = isSelected ? 'bg-white/20 text-white' : theme.badge;

  return `
    <div class="inline-flex w-max -translate-x-1/2 -translate-y-full flex-col items-center cursor-pointer select-none transform">
      <div class="flex w-max items-center gap-1.5 whitespace-nowrap rounded-full border px-2.5 py-1.5 text-xs font-bold shadow-lg transition-all ${theme.border} ${selectedStyle}">
        <span class="shrink-0">${priceText}</span>
        <span class="shrink-0 rounded-md px-1.5 py-0.5 text-[10px] font-bold ${badgeStyle}">
          ${safetyScore}점
        </span>
      </div>
      <div class="-mt-1.5 h-2.5 w-2.5 rotate-45 ${theme.pointer}"></div>
      <div class="mt-1 h-2 w-6 rounded-full bg-black/20 blur-sm"></div>
    </div>
  `;
};

// 주 목적지 마커 핀 HTML 렌더러 (순수 초고속 HTML 스트링 템플릿)
export const renderDestinationPinHTML = (destination) => {
  const name = destination?.name || '주 목적지';
  return `
    <div class="inline-flex w-max flex-col items-center pointer-events-auto cursor-pointer transform -translate-x-1/2 -translate-y-full select-none" title="${name}">
      <div class="flex w-max items-center gap-1.5 whitespace-nowrap rounded-full border border-blue-400 bg-blue-600 px-3.5 py-1.5 text-xs font-bold text-white shadow-xl transition-all hover:bg-blue-700">
        <span class="inline-block shrink-0 animate-bounce">🚩</span>
        <span class="shrink-0">${name}</span>
      </div>
      <div class="w-3 h-3 bg-blue-600 rotate-45 -mt-1.5"></div>
      <div class="w-8 h-3 bg-black/20 rounded-full blur-xs mt-1"></div>
    </div>
  `;
};

// 클러스터 마커 핀 HTML (축소 상태 시 매물 묶음 핀)
export const renderClusterPinHTML = (count) => {
  return `
    <div class="flex h-[50px] w-[50px] items-center justify-center rounded-full border border-[#aebcff] bg-[#dfe5ff] text-[#3852e8] shadow-lg transition-all transform -translate-x-1/2 -translate-y-full hover:-translate-y-[calc(100%+3px)] hover:bg-[#d2dcff] cursor-pointer z-20 select-none">
      <span class="text-[16px] font-extrabold leading-none">${count.toLocaleString()}<span class="ml-px text-[10px] font-semibold">개</span></span>
    </div>
  `;
};

/**
 * 사용자 세세 조절 커스텀 옵션 세팅
 */
export const DEFAULT_CLUSTER_OPTIONS = {
  minClusterZoom: 18, // 🔍 18 레벨 이상 극도로 확대했을 때만 클러스터 해제
  gridSizeFactor: 0.012, // 📐 클러스터 반경 범위 크기
  minClusterCount: 2, // 🔢 최소 묶음 개수 (2개부터 무조건 클러스터 핀으로 묶음)
};

/**
 * 초고속 격자 클러스터링 알고리즘
 */
export const getClusteredMarkers = (properties, currentZoom, bounds, customOptions = {}) => {
  if (!properties || properties.length === 0) return [];

  const options = { ...DEFAULT_CLUSTER_OPTIONS, ...customOptions };

  let minLat = -90, maxLat = 90, minLng = -180, maxLng = 180;
  if (bounds) {
    const sw = bounds.getSW();
    const ne = bounds.getNE();
    minLat = sw.lat();
    maxLat = ne.lat();
    minLng = sw.lng();
    maxLng = ne.lng();
  }

  // 1. 줌 레벨이 minClusterZoom 이상으로 확대되면 개별 매물 핀 표출
  if (currentZoom >= options.minClusterZoom) {
    return properties
      .filter((p) => p.latitude && p.longitude && p.latitude >= minLat && p.latitude <= maxLat && p.longitude >= minLng && p.longitude <= maxLng)
      .map((p) => ({ isCluster: false, item: p }));
  }

  // 2. 줌 레벨이 낮을(축소) 때 격자 클러스터링 계산
  const gridSize = options.gridSizeFactor * Math.pow(2, 14 - currentZoom);
  const clusters = new Map();

  properties.forEach((p) => {
    if (!p.latitude || !p.longitude) return;
    if (p.latitude < minLat || p.latitude > maxLat || p.longitude < minLng || p.longitude > maxLng) return;

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
