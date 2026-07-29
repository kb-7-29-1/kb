# 🎨 [디자인 & UI/UX 특화] 모바일 UI 전 화면(20개) 프롬프트 가이드

본 문서는 로직이나 백엔드 연동 지시 대신 **모바일 화면의 비주얼 디자인, 레이아웃 구획, 컬러 팔레트, 타이포그래피, 애니메이션 및 그래픽 UI/UX 퀄리티를 극대화**하기 위한 전용 프롬프트 가이드입니다.

---

## 💎 0. 모바일 디자인 시스템 명세 (Visual Design System)

AI 디자인 및 코드 생성 도구(Antigravity, Figma AI, Cursor)에 기본 전달되는 **프리미엄 모바일 UI 디자인 토큰**입니다:

- **Primary Color**: `#2A60F7` (KB Blue 트렌디 블루), Active: `#1D4ED8`, Subtle: `#EFF6FF`
- **Safety Status Color**: `#10B981` (안전 🟢), `#F59E0B` (주의 🟡), `#EF4444` (위험 🔴)
- **Background & Card**: App Bg `#F8FAFC`, Card Surface `#FFFFFF`, Glass Overlay `rgba(255, 255, 255, 0.85)` (Blur 12px)
- **Border & Shadow**: Soft Border `#E2E8F0`, Card Elevation `0 4px 20px -2px rgba(15, 23, 42, 0.06)`, Floating Sheet Shadow `0 -6px 30px rgba(0,0,0,0.1)`
- **Radius**: Large Cards `20px`, Inputs/Buttons `14px`, Small Badges `8px`
- **Typography Hierarchy**: Display Title 24px Bold, Section Title 18px SemiBold, Body 14px Regular, Caption 12px Medium

---

## 📱 PART 1. 회원가입 및 본인인증 (3개 화면)

### 🎨 화면 01: 로그인 UI 디자인
```markdown
[디자인 목표] 신뢰감을 주는 깔끔하고 간결한 모바일 로그인 UI를 디자인해 줘.

[시각적 레이아웃 및 디자인 스펙]
1. Header Visual: 브랜드 로고 "살고싶오 🛡️" + "낮선 동네도 안심하고 이사하세요" (Bold 22px, #0F172A)
2. Input Form Fields:
   - 로그인 아이디 입력창 (Placeholder: "아이디를 입력하세요")
   - 비밀번호 입력창 (Placeholder: "비밀번호를 입력하세요")
   - Focus 시 Border #2A60F7 + Subtle Blue Box-Shadow (0 0 0 3px #EFF6FF) 효과
3. Action Links & CTA:
   - [아이디 찾기 / 비밀번호 찾기] 하단 서브 텍스트 링크
   - 56px 높이 Full-Width 대형 Primary Blue 버튼 ("로그인")
   - "계정이 없으신가요? 회원가입" 하단 가입 전환 링크
```

### 🎨 화면 02: 회원가입 폼 UI 디자인 (이름, ID, 이메일, 생년월일, 성별, 비밀번호)
```markdown
[디자인 목표] 깔끔하고 접근성 높은 회원가입 폼 UI 디자인. (주민등록번호/휴대폰 인증 없음)

[시각적 디자인 스펙]
1. Header: 뒤로가기 버튼 + "회원가입"
2. Input Form Fields:
   - 이름 입력창 (Placeholder: "홍길동")
   - 로그인 아이디 입력창 + 우측 44px 높이 `[중복확인]` Outlined 버튼
   - 이메일 입력창 (Placeholder: "example@email.com")
   - 생년월일 입력창 (YYYY-MM-DD) & 성별 선택 Segmented Control (`[남] / [여]`)
   - 비밀번호 (8자 이상) & 비밀번호 확인 입력창
3. Bottom Fixed CTA: 모든 필수 항목 입력 시 활성화되는 56px 대형 Primary Blue 버튼 ("회원가입 완료")
```

### 🎨 화면 03: 아이디 / 비밀번호 찾기 모달 UI 디자인
```markdown
[디자인 목표] 중앙 레이어드 모달 스타일 계정 찾기 UI 디자인.

[시각적 디자인 스펙]
1. Modal Backdrop: Dimmed Overlay (`rgba(15, 23, 42, 0.5)` Blur 8px)
2. Tab Control: `[아이디 찾기]` / `[비밀번호 찾기]` 상단 탭
3. Input & Result:
   - 아이디 찾기: 이름 + 이메일 입력 시 마스킹된 아이디(`ahs0***`) 결과 출력
   - 비밀번호 찾기: 비밀번호 재설정 폼 노출
```

---

## 🧭 PART 2. 온보딩 주거 조건 설정 (5개 화면)

### 🎨 화면 04: 온보딩 Step 1 - 목적지 설정 UI 디자인
```markdown
[디자인 목표] 트렌디한 Segmented Tag와 주소 검색 인터페이스 디자인.

[시각적 디자인 스펙]
1. Step Progress Bar: 상단 4px 높이 블루 스텝 바 (Step 1/3 진행률 33% 애니메이션)
2. Type Selector Chips: [🏢 직장], [🎓 학교], [📍 기타] 3개 세그먼트 태그
3. Address Search Result Box:
   - 검색 입력창 내부 돋보기 아이콘 및 Clear(X) 버튼
   - 검색 결과 드롭다운: 도로명 뱃지(Blue background pill)와 지번 주소 계층적 타이포그래피 구분
```

### 🎨 화면 05: 온보딩 Step 2 - 통근 수단 및 소요시간 UI 디자인
```markdown
[디자인 목표] 직관적인 이동수단 선택 탭과 시각적 강조 슬라이더 디자인.

[시각적 디자인 스펙]
1. Transport Toggle Buttons: [🚌 대중교통] / [🚶 도보] 2개 대형 탭 버튼 (Icon + Text 조합)
2. Travel Time Visual Highlight: "최대 15분 이내" 32px Extra Bold 타이포그래피 (#2A60F7)
3. Custom Range Slider: Track 두께 8px (#2A60F7), Thumb 28px 흰색 원형 노브 + 4px Drop Shadow
```

### 🎨 화면 06: 온보딩 Step 3 - 예산 범위 듀얼 슬라이더 UI 디자인
```markdown
[디자인 목표] 고급스러운 듀얼 노브 Range Slider 및 가격 뱃지 디자인.

[시각적 디자인 스펙]
1. Price Display Badges: "보증금 1,000만 ~ 5,000만원", "월세 30만 ~ 75만원" 하이라이트 Card Box
2. Dual Knob Sliders: 범위를 조절하는 슬라이더 및 Floating Tooltip 뱃지
```

### 🎨 화면 07: 온보딩 Step 4 - 안전 가중치 슬라이더 UI 디자인
```markdown
[디자인 목표] 감각적인 지표 가중치 조절 카드 레이아웃 디자인.
```

### 🎨 화면 08: 온보딩 Step 5 - 설정 조건 요약 카드 UI 디자인
```markdown
[디자인 목표] 안심 주거 매칭 완료 요약 카드 및 웰컴 디자인.
```

---

## 🗺️ PART 3. 지도 기반 탐색 및 필터 바텀시트 (5개 화면)

### 🎨 화면 09: 메인 이소크론 지도 탐색 UI 디자인
```markdown
[디자인 목표] 세련된 지도 오버레이와 디자인 핀(Pin) UI.
```

### 🎨 화면 10: 지도 필터 바텀시트 Step 1 UI 디자인
```markdown
[디자인 목표] 부드러운 라운딩의 슬라이딩 바텀시트 패널 디자인.
```

### 🎨 화면 11: 지도 필터 바텀시트 Step 2 (안전 상세 필터) UI 디자인
```markdown
[디자인 목표] 직관적인 스위치 토글과 필터 선택 칩 디자인.
```

### 📄 화면 12: 지도 내 매물 카드 리스트 바텀시트 UI 디자인
```markdown
[디자인 목표] 가독성 높은 부동산 수평/수직 매물 카드 UI.
```

### 🎨 화면 13: 조건 재설정 모달 팝업 UI 디자인
```markdown
[디자인 목표] 경고 및 안내용 중앙 팝업 모달 디자인.
```

---

## 📊 PART 4. 매물 상세 및 안전 분석 리포트 (3개 화면)

### 🎨 화면 14: 매물 상세 Top (갤러리/가격/점수) UI 디자인
```markdown
[디자인 목표] 고급 주거 매물 상세 상단 히어로 영역 디자인.
```

### 🎨 화면 15: 매물 상세 Middle (5축 레이더 차트) UI 디자인
```markdown
[디자인 목표] 금융 리포트 스타일의 5축 레이더 차트 및 지표 디자인.
```

### 🎨 화면 16: 매물 상세 Bottom (등기부 리포트 & 금융 연계) UI 디자인
```markdown
[디자인 목표] 신뢰감 있는 등기부 분석 및 KB 대출 연계 카드 디자인.
```

---

## 👤 PART 5. 마이페이지 및 내 설정 (4개 화면)

### 🎨 화면 17: 마이페이지 메인 UI 디자인
```markdown
[디자인 목표] 깔끔한 사용자 프로필 및 요약 대시보드 디자인.
```

### 🎨 화면 18: 내 주거 조건 수정 UI 디자인
```markdown
[디자인 목표] 온보딩 조건 수정용 Form 재구성 디자인.
```

### 🎨 화면 19: 관심 매물 비교 UI 디자인
```markdown
[디자인 목표] 2개 매물 다이렉트 2열 비교 카드 테이블 디자인.
```

### 🎨 화면 20: 계정 설정 및 탈퇴 모달 UI 디자인
```markdown
[디자인 목표] 설정 리스트 및 Danger Zone 탈퇴 확인 모달 디자인.
```
