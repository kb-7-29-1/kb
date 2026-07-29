# 🧪 [살고싶오] E2E(End-to-End) 전용 통합 테스트 가이드 (Markdown)

본 문서는 **살고싶오(29반 1팀) - 빌라촌 안심 주거 매칭 플랫폼**의 프론트엔드(Vue 3)와 백엔드(Spring), DB(MySQL) 및 공공 API 연동 전체 흐름을 자동화하여 검증하기 위한 **E2E(End-to-End) 전용 시나리오 테스트 가이드**입니다.

---

## 📌 0. E2E 테스트 환경 & 실행 스펙

- **E2E Framework**: **Playwright** (또는 Cypress)
- **Target App URL**:
  - Vue 3 Dev Server: `http://localhost:5173`
  - Spring Backend API: `http://localhost:8080`
- **Testing Viewports**:
  - 📱 **Mobile Viewport**: 375 x 812px (iPhone 13 / Galaxy S22)
  - 🖥️ **PC Widescreen Viewport**: 1920 x 1080px (Desktop Full HD)

---

## 🎯 1. 5대 핵심 E2E 시나리오 테스트 매트릭스 (Scenario Test Matrix)

---

### 🧪 TC-01: [인증 & 회원] 회원가입 (이름, ID, 이메일, 생년월일, 성별, 비밀번호) ➔ 로그인 ➔ JWT 토큰 수령

- **목적**: 사용자가 회원가입 폼(ID 중복 확인 포함)을 작성하고 가입 후, 해당 계정으로 로그인하여 JWT 토큰을 정상 발급받는지 검증. (주민등록번호/휴대폰 인증 없음)
- **E2E 테스트 스텝**:
  1. `http://localhost:5173/auth/signup` 접속.
  2. 이름(`홍길동`), 아이디(`testuser01`) 입력 후 `[중복확인]` 클릭 ➔ "사용 가능한 아이디입니다" 확인.
  3. 이메일(`test@example.com`), 생년월일(`2001-05-15`), 성별(`[남]`), 비밀번호(`password123!`) 입력.
  4. `[회원가입 완료]` 버튼 클릭 ➔ 로그인 페이지`/auth/login` 이동 확인.
  5. 아이디(`testuser01`), 비밀번호(`password123!`) 입력 후 `[로그인]` 클릭.
  6. **검증(Assertion)**:
     - `localStorage.getItem('token')` 토큰 생성 여부 확인.
     - 메인 페이지 또는 온보딩 페이지로 정상 이탈 확인.

---

### 🧪 TC-02: [온보딩] 초개인화 조건 입력 (Step 1~5) ➔ DB 저장 검증 (중요도 5/5)

- **목적**: 목적지(직장/학교), 이동수단, 예산 듀얼 슬라이더, 안전 가중치를 입력하고 DB `onboardings` 테이블에 올바르게 저장되는지 검증.
- **E2E 테스트 스텝**:
  1. `/onboarding` 페이지 진입.
  2. **Step 1 (목적지)**: `[🎓 학교]` 태그 선택 ➔ 검색창에 "연세대학교" 입력 ➔ 검색 결과 팝업 첫 번째 항목 클릭.
  3. **Step 2 (이동수단)**: `[🚌 대중교통]` 탭 선택 ➔ 소요시간 슬라이더 `15분`으로 이동.
  4. **Step 3 (예산)**: 보증금 범위 슬라이더 `5,000만원`, 월세 범위 슬라이더 `75만원` 설정.
  5. **Step 4 (안전가중치)**: 🏢 건물안전 `30%`, 💡 거리안전 `50%`, 📜 등기부권리 `20%` 슬라이더 조정.
  6. **Step 5 (종합확인)**: 🛡️ 요약 카드 확인 후 `[지도에서 매물 확인하기]` 클릭.
- **검증(Assertion)**:
  - URL이 `/map`으로 정상 이동하는지 확인.
  - 백엔드 DB `onboardings` 테이블의 해당 `user_id` 레코드 값이 온보딩 입력값과 100% 일치하는지 확인.

---

### 🧪 TC-03: [지도 & 필터] 15분 이소크론 지도 렌더링 ➔ 편의시설 7종 필터 ➔ 마커 뱃지 검증 (중요도 4/5, 3/5)

- **목적**: 지도 상에 15분 통근 이소크론 반원이 렌더링되고, 편의시설 필터 조절 시 지도 마커에 충족 개수 뱃지(`4/5`)가 업데이트되는지 검증.
- **E2E 테스트 스텝**:
  1. `/map` 메인 지도 페이지 진입.
  2. 지도 상단에 반투명 이소크론 영역(`SVG/Canvas Polygon`) 및 지도 핀 뱃지(🟢 88점, 🟡 72점) 표시 확인.
  3. 좌측 사이드바 `[편의시설 커스텀 필터]` 버튼 클릭 ➔ 필터 모달 오픈.
  4. 7개 편의시설 중 `[🛍️ 다이소]` (도보 5분 이내), `[💄 올리브영]` (도보 10분 이내) 슬라이더 조절 후 `[필터 적용]` 클릭.
- **검증(Assertion)**:
  - 지도 매물 마커 아이콘 상단에 퍼플 뱃지(예: `4/5` 충족)가 실시간 재렌더링되는지 확인.
  - 조건 미충족 매물은 지도에서 반투명 오파시티(`opacity: 0.3`)로 처리되는지 확인.

---

### 🧪 TC-04: [매물 상세] 슬라이딩 도어 ➔ 위반건축물 OX ➔ 5축 레이더 차트 ➔ 귀갓길 댓글/후기 (중요도 4/5)

- **목적**: 매물 선택 시 우측 슬라이딩 도어가 열리고, 건물 안전 OX, CPTED 귀갓길 점수, 댓글 등록 및 후기 투표가 작동하는지 검증.

```markdown

```

---

### 🧪 TC-05: [마이페이지 & 와이드 비교] 프로필 수정 ➔ 관심 매물 3열 비교 (중요도 1/5)

- **목적**: 마이페이지에서 내 주거 조건을 수정하고, 찜한 관심 매물 3개를 3열 비교 테이블로 시각화하는지 검증.

```markdown

```

---

## 🛠️ 2. Playwright E2E 자동화 코드 예시 (`tests/e2e/signup-to-map.spec.ts`)

```typescript
import { test, expect } from '@playwright/test';

test.describe('살고싶오 가입 및 매칭 E2E 테스트', () => {
  test('회원가입 -> 로그인 -> 온보딩 -> 지도 매칭 검증', async ({ page }) => {
    // 1. 회원가입 (주민번호/휴대폰인증 없음)
    await page.goto('http://localhost:5173/auth/signup');
    await page.fill('input[name="name"]', '홍길동');
    await page.fill('input[name="login_id"]', 'user2026');
    await page.click('button:has-text("중복확인")');
    await page.fill('input[name="email"]', 'user2026@test.com');
    await page.fill('input[name="birth_date"]', '2001-05-15');
    await page.click('text=남');
    await page.fill('input[name="password"]', 'pass1234!');
    await page.fill('input[name="confirm_password"]', 'pass1234!');
    await page.click('button:has-text("회원가입 완료")');

    // 2. 로그인
    await expect(page).toHaveURL('http://localhost:5173/auth/login');
    await page.fill('input[name="login_id"]', 'user2026');
    await page.fill('input[name="password"]', 'pass1234!');
    await page.click('button:has-text("로그인")');

    // 3. 토큰 검증
    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeTruthy();
  });
});
```
