// 대출 API가 은행 로고 URL을 안 줘서, 응답의 회사명(companyName) 문자열에
// 포함된 키워드로 로컬 로고 파일을 찾아 매칭한다.
const logoFiles = import.meta.glob('../assets/images/banklogo/*.png', {
  eager: true,
  import: 'default',
});

function resolveLogoUrl(fileName) {
  // macOS(Finder)에 저장된 파일명은 NFD(자모 분해형)라서, 여기 하드코딩한
  // NFC(완성형) 문자열과 바이트 단위로 다르다. 정규화 후 비교해야 매치된다.
  const target = fileName.normalize('NFC');
  const entry = Object.entries(logoFiles).find(
    ([path]) => path.normalize('NFC').endsWith(`/${target}`),
  );
  return entry ? entry[1] : null;
}

const BANK_LOGO_RULES = [
  { keywords: ['국민은행', 'KB'], file: 'KB국민은행.png' },
  { keywords: ['신한은행'], file: '신한은행.png' },
  { keywords: ['우리은행'], file: '우리은행.png' },
  { keywords: ['하나은행'], file: '하나은행.png' },
  { keywords: ['농협은행'], file: 'NH농협은행.png' },
  { keywords: ['중소기업은행', 'IBK'], file: 'IBK기업은행.png' },
  { keywords: ['스탠다드차타드', 'SC제일'], file: 'SC제일은행.png' },
  { keywords: ['아이엠뱅크', 'iM뱅크', 'IM뱅크', 'DGB'], file: 'IM뱅크.png' },
  { keywords: ['경남은행'], file: 'BNK경남은행.png' },
  { keywords: ['부산은행'], file: 'BNK부산은행.png' },
  { keywords: ['광주은행'], file: '광주은행.png' },
  { keywords: ['전북은행'], file: '전북은행.png' },
  { keywords: ['제주은행'], file: '제주은행.png' },
  { keywords: ['수협은행'], file: '수협은행.png' },
  { keywords: ['케이뱅크'], file: '케이뱅크.png' },
  { keywords: ['카카오뱅크'], file: '카카오뱅크.png' },
  { keywords: ['토스뱅크'], file: '토스뱅크.png' },
  { keywords: ['새마을금고'], file: 'MG새마을금고.png' },
  { keywords: ['신협'], file: '신협.png' },
  { keywords: ['SBI저축은행'], file: 'SBI저축은행.png' },
  { keywords: ['씨티은행', '씨티뱅크'], file: '씨티뱅크.png' },
  { keywords: ['우체국'], file: '우체국.png' },
  { keywords: ['산업은행'], file: '산업은행.png' },
];

// 은행명 문자열(예: "농협은행주식회사", "주식회사 하나은행")을 넣으면
// 매칭되는 로고 이미지 URL을, 없으면 null을 반환한다.
export function getBankLogoUrl(companyName) {
  if (!companyName) return null;
  const rule = BANK_LOGO_RULES.find(({ keywords }) => keywords.some((kw) => companyName.includes(kw)));
  return rule ? resolveLogoUrl(rule.file) : null;
}