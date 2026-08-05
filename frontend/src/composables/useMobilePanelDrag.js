import { ref } from 'vue';

/**
 * 모바일/데스크톱 하단 사이드바 실시간 마우스 및 터치 드래그 리사이즈 Composable
 */
export function useMobilePanelDrag(targetSelector = '.mobile-aside-panel') {
  const mobilePanelHeight = ref('HALF');
  const isDragging = ref(false);
  const dragPixelHeight = ref(null);

  let dragStartY = 0;
  let dragStartHeightPx = 0;

  const toggleMobilePanel = () => {
    if (isDragging.value) return;
    const nextPanelHeight = {
      HALF: 'EXPANDED',
      EXPANDED: 'COLLAPSED',
      COLLAPSED: 'HALF',
    };
    mobilePanelHeight.value = nextPanelHeight[mobilePanelHeight.value] ?? 'HALF';
    dragPixelHeight.value = null;
  };

  // 마우스 / 터치 드래그 시작
  const startDrag = (e) => {
    isDragging.value = true;
    const clientY = e.touches ? e.touches[0].clientY : e.clientY;
    dragStartY = clientY;

    const asideEl = document.querySelector(targetSelector);
    dragStartHeightPx = asideEl
      ? asideEl.getBoundingClientRect().height
      : window.innerHeight * 0.33;

    window.addEventListener('mousemove', onDragMove);
    window.addEventListener('mouseup', stopDrag);
    window.addEventListener('touchmove', onDragMove);
    window.addEventListener('touchend', stopDrag);
  };

  // 마우스 / 터치 드래그 이동
  const onDragMove = (e) => {
    if (!isDragging.value) return;
    const clientY = e.touches ? e.touches[0].clientY : e.clientY;
    const deltaY = dragStartY - clientY; // 위로 끌어올리면 양수
    const newHeight = dragStartHeightPx + deltaY;

    const minHeight = window.innerHeight * 0.13;
    const maxHeight = window.innerHeight * 0.95;

    dragPixelHeight.value = Math.max(minHeight, Math.min(maxHeight, newHeight));
  };

  // 마우스 / 터치 드래그 종료 (스냅 이동)
  const stopDrag = () => {
    if (!isDragging.value) return;
    isDragging.value = false;

    window.removeEventListener('mousemove', onDragMove);
    window.removeEventListener('mouseup', stopDrag);
    window.removeEventListener('touchmove', onDragMove);
    window.removeEventListener('touchend', stopDrag);

    if (dragPixelHeight.value) {
      if (dragPixelHeight.value > window.innerHeight * 0.45) {
        mobilePanelHeight.value = 'EXPANDED';
      } else if (dragPixelHeight.value < window.innerHeight * 0.28) {
        mobilePanelHeight.value = 'COLLAPSED';
      } else {
        mobilePanelHeight.value = 'HALF';
      }
    }
    dragPixelHeight.value = null;
  };

  return {
    mobilePanelHeight,
    isDragging,
    dragPixelHeight,
    toggleMobilePanel,
    startDrag,
  };
}
