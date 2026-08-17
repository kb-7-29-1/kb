/**
 * [도커 라우팅 전용 오버레이 모듈: hybridRouteOverlay.js]
 * 발할라(도보) 및 그래프호퍼(대중교통) 경로와 스마트 칩 네임카드를 네이버 지도 위에 렌더링합니다.
 */

export const TRANSIT_COLORS = {
  BUS: '#2563eb',          // 간선버스 코발트 블루
  SUBWAY: '#7c3aed',       // 지하철 로열 바이올렛
  WALK_SUPPORTED: '#059669', // 지원 구역 에메랄드 그린
  WALK_UNSUPPORTED: '#94a3b8', // 미지원 구역 슬레이트 그레이
  LABEL_BG_MISSING: '#64748b',
};

/**
 * 대중교통/도보 구간 칩 HTML 생성기
 */
export function createTransitChipHTML({ type, routeName, durationMinutes, isDataMissing, strokeColor }) {
  let chipEmoji = '🚌';
  let chipText = routeName || '대중교통';

  if (type === 'SUBWAY' || type === 'METRO') {
    chipEmoji = '🚇';
  } else if (type === 'BUS') {
    chipEmoji = '🚌';
  } else {
    chipEmoji = '🚶';
    chipText = isDataMissing
      ? `도보 ${durationMinutes ? `${durationMinutes}분` : ''} (데이터 부족)`
      : `도보 ${durationMinutes ? `${durationMinutes}분` : ''}`;
  }

  const bg = type === 'WALK'
    ? (isDataMissing ? TRANSIT_COLORS.LABEL_BG_MISSING : TRANSIT_COLORS.WALK_SUPPORTED)
    : strokeColor;

  const durationStr = durationMinutes && type !== 'WALK' ? ` | ${durationMinutes}분` : '';

  return `
    <div style="
      background: ${bg};
      color: #ffffff;
      font-size: 11.5px;
      font-weight: 800;
      padding: 3.5px 9px;
      border-radius: 999px;
      white-space: nowrap;
      box-shadow: 0 2px 6px rgba(0,0,0,0.35);
      border: 1.5px solid rgba(255,255,255,0.9);
      display: flex;
      align-items: center;
      gap: 3.5px;
      user-select: none;
    ">
      <span style="font-size: 10px;">${chipEmoji}</span>
      <span>${chipText}${durationStr}</span>
    </div>
  `;
}

/**
 * 안전점수 단일 뱃지 HTML 생성기
 */
export function createSafetyScoreChipHTML({ score, color, isDataMissing }) {
  if (isDataMissing) {
    return `
      <div style="
        background: ${TRANSIT_COLORS.LABEL_BG_MISSING};
        color: #ffffff;
        font-size: 11px;
        font-weight: 700;
        padding: 3px 8px;
        border-radius: 999px;
        white-space: nowrap;
        box-shadow: 0 2px 6px rgba(0,0,0,0.35);
        border: 1.5px solid rgba(255,255,255,0.85);
        display: flex;
        align-items: center;
        gap: 3px;
        user-select: none;
      ">
        <span style="font-size: 9.5px;">🛡️</span> 데이터 부족
      </div>
    `;
  }

  return `
    <div style="
      background: ${color};
      color: #ffffff;
      font-size: 12px;
      font-weight: 800;
      padding: 3px 9px;
      border-radius: 999px;
      white-space: nowrap;
      box-shadow: 0 2px 5px rgba(0,0,0,0.3);
      border: 1.5px solid rgba(255,255,255,0.85);
      user-select: none;
    ">
      ${score}점
    </div>
  `;
}

/**
 * 발할라 & 호퍼 하이브리드 경로 지도 렌더러
 * @returns {object} { polylines: Array, markers: Array, lastRouteKey: string }
 */
export function renderHybridRouteOverlays({
  mapInstance,
  safetyRoute,
  gradeColors = {},
}) {
  if (!mapInstance || !window.naver?.maps) return { polylines: [], markers: [], lastRouteKey: '' };

  const overlays = {
    polylines: [],
    markers: [],
    lastRouteKey: '',
  };

  // 1. [대중교통 다중 구간(도보1 + 대중교통2 + 도보3) 호퍼 렌더링]
  const transitSegments = safetyRoute?.transitSegments;
  if (Array.isArray(transitSegments) && transitSegments.length > 0) {
    transitSegments.forEach((seg) => {
      const segPoints = (seg.routePoints || [])
        .map((p) => ({ lat: Number(p.latitude), lng: Number(p.longitude) }))
        .filter((p) => Number.isFinite(p.lat) && Number.isFinite(p.lng));
      if (segPoints.length < 2) return;

      const path = segPoints.map((p) => new window.naver.maps.LatLng(p.lat, p.lng));
      const type = String(seg.type || 'WALK').toUpperCase();
      let strokeColor = seg.lineColor || TRANSIT_COLORS.BUS;
      let strokeStyle = 'shortdash'; // 🎯 촘촘하고 조밀한 점선 스타일!
      let strokeWeight = 6;
      let strokeOpacity = 0.95;

      const isSegDataMissing = seg.isSupportedDistrict === false || safetyRoute?.safetyScore == null;

      if (type === 'SUBWAY' || type === 'METRO') {
        strokeColor = seg.lineColor || TRANSIT_COLORS.SUBWAY;
        strokeStyle = 'shortdash';
        strokeWeight = 6.5;
      } else if (type === 'BUS') {
        strokeColor = seg.lineColor || TRANSIT_COLORS.BUS;
        strokeStyle = 'shortdash';
        strokeWeight = 6;
      } else {
        // 🚶 도보 구간: 우리의 핵심 서비스이므로 또렷한 실선(solid, 7px) 적용!
        strokeColor = isSegDataMissing ? TRANSIT_COLORS.WALK_UNSUPPORTED : TRANSIT_COLORS.WALK_SUPPORTED;
        strokeStyle = 'solid';
        strokeWeight = 7;
        strokeOpacity = 0.95;
      }

      const polyline = new window.naver.maps.Polyline({
        map: mapInstance,
        path,
        strokeColor,
        strokeWeight,
        strokeOpacity,
        strokeStyle,
        zIndex: type === 'WALK' ? 19 : 17,
      });
      overlays.polylines.push(polyline);

      // 중앙 네임카드 칩
      const midPoint = path[Math.floor(path.length / 2)];
      const chipMarker = new window.naver.maps.Marker({
        map: mapInstance,
        position: midPoint,
        icon: {
          content: createTransitChipHTML({
            type,
            routeName: seg.routeName,
            durationMinutes: seg.durationMinutes,
            isDataMissing: isSegDataMissing,
            strokeColor,
          }),
          anchor: new window.naver.maps.Point(36, 12),
        },
        zIndex: 20,
      });
      overlays.markers.push(chipMarker);
    });

    return overlays;
  }

  // 2. [단일 도보/안전 경로 발할라 렌더링]
  const rawPoints = safetyRoute?.routePoints;
  if (!Array.isArray(rawPoints) || rawPoints.length < 2) {
    return overlays;
  }

  const points = rawPoints
    .map((point) => ({
      lat: Number(point.latitude),
      lng: Number(point.longitude),
    }))
    .filter(
      (point) =>
        Number.isFinite(point.lat) &&
        Number.isFinite(point.lng) &&
        point.lat >= -90 &&
        point.lat <= 90 &&
        point.lng >= -180 &&
        point.lng <= 180,
    );

  if (points.length < 2) return overlays;

  const first = points[0];
  const last = points[points.length - 1];
  const routeKey = `${safetyRoute?.routeId || 'route'}_${points.length}_${first.lat}_${first.lng}_${last.lat}_${last.lng}`;
  overlays.lastRouteKey = routeKey;

  const path = points.map((point) => new window.naver.maps.LatLng(point.lat, point.lng));
  const score = safetyRoute?.safetyScore;
  const grade = safetyRoute?.safetyGrade;
  const isDataMissing = score == null;
  const color = isDataMissing ? TRANSIT_COLORS.WALK_UNSUPPORTED : (gradeColors[grade] || '#4058f5');

  const polyline = new window.naver.maps.Polyline({
    map: mapInstance,
    path,
    strokeColor: color,
    strokeWeight: isDataMissing ? 6 : 7,
    strokeOpacity: isDataMissing ? 0.82 : 0.92,
    strokeStyle: isDataMissing ? 'dash' : 'solid',
    zIndex: 18,
  });
  overlays.polylines.push(polyline);

  const midPoint = path[Math.floor(path.length / 2)];
  const scoreMarker = new window.naver.maps.Marker({
    map: mapInstance,
    position: midPoint,
    icon: {
      content: createSafetyScoreChipHTML({ score, color, isDataMissing }),
      anchor: new window.naver.maps.Point(isDataMissing ? 38 : 24, 12),
    },
    zIndex: 19,
  });
  overlays.markers.push(scoreMarker);

  return overlays;
}

/**
 * 생성된 모든 하이브리드 라우트 오버레이 정리
 */
export function clearHybridRouteOverlays(overlayState) {
  if (!overlayState) return;
  if (Array.isArray(overlayState.polylines)) {
    overlayState.polylines.forEach((p) => p && p.setMap(null));
  }
  if (Array.isArray(overlayState.markers)) {
    overlayState.markers.forEach((m) => m && m.setMap(null));
  }
}
