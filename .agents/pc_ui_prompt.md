# 🖥️ [디자인 & UI/UX 특화] PC 웹사이트 전 화면 프롬프트 가이드

본 문서는 와이어프레임(`PC웹사이트 Frame.png`) 내 모든 PC 화면에 맞춰 **데스크톱 와이드 스크린(1440px ~ 1920px) 비주얼 디자인, 좌우 스플릿 레이아웃, 사이드바 구조, 타이포그래피, 그래픽 UI/UX 퀄리티** 구현에 100% 집중한 전용 AI 프롬프트 가이드입니다.

---

## 💎 0. PC 웹 데스크톱 디자인 시스템 명세 (Widescreen Spec)

- **Target Resolution**: Widescreen Desktop (최적 1440px, 1920px 대응)
- **Primary Color**: `#2A60F7` (KB Blue), Active: `#1D4ED8`, Banner Bg: `linear-gradient(135deg, #1E40AF 0%, #2A60F7 100%)`
- **Layout Architecture**: 
  - Split-Screen Layout (좌측 브랜드 비주얼 40% + 우측 폼 60%)
  - Map View Layout (좌측 380px 고정 매물 탐색 사이드바 + 우측 지도 100% Full-bleed)
  - Slide-Over Panel (우측 560px 슬라이드 오버 상세 리포트 패널)
- **Status Colors**: `#10B981` (안전 🟢), `#F59E0B` (주의 🟡), `#EF4444` (위험 🔴)
- **Card & Elevation**: `#FFFFFF` Surface, Border `#E2E8F0`, Soft Elevation `0 10px 30px -5px rgba(15, 23, 42, 0.08)`

---

## 💻 PART 1. 회원가입 및 본인인증 (Split-Screen Layout)

### 🎨 화면 01: PC 로그인 스플릿 화면 UI 디자인
```markdown
[디자인 목표] 데스크톱 와이드 스플릿 화면 레이아웃의 로그인 UI 디자인.

[시각적 레이아웃 및 디자인 스펙]
1. Left Visual Banner (40% Width):
   - Background: KB Deep Blue Gradient (`linear-gradient(135deg, #1E3A8A, #2A60F7)`)
   - Content: 대형 브랜드 로고 + "안전한 주거 거래의 시작\nKB 안심 매칭 도우미" (36px Bold White Typography)
   - Decorative Elements: 3D 방패/안전 그래픽 일러스트 배치
2. Right Content Area (60% Width):
   - Center Container: 480px Max-Width 중앙 정렬 백색 카드 (Radius 24px, Shadow `0 20px 40px rgba(0,0,0,0.06)`)
   - Title: "로그인" (28px Bold)
   - Form Inputs: 아이디, 비밀번호 입력 폼
   - Sub Actions: [아이디/비밀번호 찾기], [회원가입] 서브 링크
3. Primary CTA: 56px 높이 Full Width Blue Button (#2A60F7, Radius 14px)
```

### 🎨 화면 02: PC 회원가입 폼 UI 디자인 (이름, ID, 이메일, 생년월일, 성별, 비밀번호)
```markdown
[디자인 목표] 깔끔하고 대형 스크린에 최적화된 회원가입 Form UI 디자인. (주민등록번호/휴대폰 인증 없음)

[시각적 디자인 스펙]
1. Header: 뒤로가기 링크 + "회원가입" (안심 주거 여정을 시작하세요)
2. Form Input Fields (Right Content Area 540px Width Card):
   - 이름 입력창 (Label: 이름)
   - 아이디 입력창 + 우측 `[중복확인]` Outlined 버튼
   - 이메일 입력창 (Label: 이메일)
   - 출생연도(YYYY) & 성별 버튼 (`[남] / [여]`) 2열 배치
   - 비밀번호 (8자 이상) & 비밀번호 확인 입력창
3. Bottom CTA: "회원가입 완료" 대형 Primary Blue 버튼
```

### 🎨 화면 03: PC 아이디 / 비밀번호 찾기 모달 UI 디자인
```markdown
[디자인 목표] 중앙 레이어드 모달 스타일 계정 찾기 UI 디자인.

[시각적 디자인 스펙]
1. Modal Backdrop: Dimmed Overlay (`rgba(15, 23, 42, 0.5)` Blur 8px)
2. PIN/Account Modal Card: 460px Width White Card (Radius 24px, Elevation Shadow)
3. Tabs: `[아이디 찾기]` / `[비밀번호 찾기]` 탭 및 결과 화면 시각화
```

---

## 🧭 PART 2. PC 온보딩 주거 조건 설정 (Widescreen Flow)

### 🎨 화면 04: PC 온보딩 Step 1 - 목적지 입력 UI 디자인
```markdown
[디자인 목표] 데스크톱 스플릿 레이아웃 온보딩 스텝 1 디자인.
```

### 🎨 화면 05: PC 온보딩 Step 2 - 통근 수단 & 와이드 슬라이더 UI 디자인
```markdown
[디자인 목표] 와이드 스크린 환경에 최적화된 소요시간 Range Slider 디자인.
```

### 🎨 화면 06: PC 온보딩 Step 3 - 예산 범위 듀얼 슬라이더 UI 디자인
```markdown
[디자인 목표] 데스크톱 2열 예산 조절 카드 레이아웃 디자인.
```

### 🎨 화면 07: PC 온보딩 Step 4 - 안전 가중치 3열 카드 UI 디자인
```markdown
[디자인 목표] 3열 Grid로 펼쳐지는 안심 가중치 설정 카드 레이아웃 디자인.
```

### 🎨 화면 08: PC 온보딩 Step 5 - 조건 종합 확인 카드 UI 디자인
```markdown
[디자인 목표] 웰컴 애니메이션 및 와이드 요약 리포트 카드 디자인.
```

---

## 🗺️ PART 3. PC 지도 탐색 & 사이드바 필터 (와이드 지도 뷰)

### 🎨 화면 09: PC 지도 메인 & 380px 사이드바 UI 디자인
```markdown
[디자인 목표] 와이드 스크린 전용 지도 + 고정 사이드바 탐색 UI 디자인.
```

### 🎨 화면 10: PC 지도 상단 와이드 필터 바 UI 디자인
```markdown
[디자인 목표] 지도 상단 부유하는 필터 바 & 드롭다운 팝업 디자인.
```

### 🎨 화면 11: PC 지도 사이드바 필터 확장 패널 UI 디자인
```markdown
[디자인 목표] 좌측 사이드바가 480px로 확장되며 나타나는 상세 필터 UI.
```

### 🎨 화면 12: PC 지도 핀 Hover / Click 툴팁 카드 UI 디자인
```markdown
[디자인 목표] 지도 핀 클릭 시 노출되는 미니 팝업 카드 디자인.
```

### 🎨 화면 13: PC 검색 결과 재설정 중앙 모달 UI 디자인
```markdown
[디자인 목표] 대형 스크린용 중앙 경고 모달 UI 디자인.
```

---

## 📊 PART 4. PC 매물 상세 & 5축 분석 리포트 패널

### 🎨 화면 14: PC 매물 상세 Slide-Over 패널 Top UI 디자인
```markdown
[디자인 목표] 지도를 유지하며 우측에서 슬라이드로 열리는 560px Slide-Over 패널 디자인.
```

### 🎨 화면 15: PC 매물 상세 5축 레이더 차트 & 리포트 UI 디자인
```markdown
[디자인 목표] 고해상도 대형 5축 레이더 차트 및 지표 그리드 디자인.
```

### 🎨 화면 16: PC 매물 상세 등기부 리포트 & KB 금융 연계 UI 디자인
```markdown
[디자인 목표] 등기부 안전 검증 리포트 및 KB 금융 자금 대출 연계 카드 디자인.
```

---

## 👤 PART 5. PC 마이페이지 및 와이드 매물 비교 (Widescreen Grid)

### 🎨 화면 17: PC 마이페이지 대시보드 2열 그리드 UI 디자인
```markdown
[디자인 목표] 데스크톱 2열 레이아웃 프로필 및 조건 관리 대시보드 디자인.
```

### 🎨 화면 18: PC 내 주거 조건 수정 패널 UI 디자인
```markdown
[디자인 목표] 조건 재설정 폼 패널 디자인.
```

### 🎨 화면 19: PC 와이드 3열 매물 비교 테이블 UI 디자인
```markdown
[디자인 목표] 3개 매물을 한눈에 수평 비교하는 데스크톱 3열 비교 테이블 UI 디자인.
```

### 🎨 화면 20: PC 계정 설정 및 회원 탈퇴 모달 UI 디자인
```markdown
[디자인 목표] 설정 관리 및 탈퇴 모달 UI 디자인.
```
