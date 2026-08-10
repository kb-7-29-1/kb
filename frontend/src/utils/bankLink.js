// 대출 API 응답에 상품 상세 페이지 URL이 없어서, bankLogo.js와 같은 방식으로
// 회사명(companyName) 문자열에 포함된 키워드로 은행 공식 홈페이지 URL을 매칭한다.
// 정확한 상품 페이지가 아니라 은행 홈페이지로 연결되는 점 참고.
const BANK_LINK_RULES = [
  { keywords: ['주택도시기금'], url: 'https://nhuf.molit.go.kr' },
  { keywords: ['국민은행', 'KB'], url: 'https://www.kbstar.com' },
  { keywords: ['신한은행'], url: 'https://www.shinhan.com' },
  { keywords: ['우리은행'], url: 'https://www.wooribank.com' },
  { keywords: ['하나은행'], url: 'https://www.kebhana.com' },
  { keywords: ['농협은행'], url: 'https://banking.nonghyup.com' },
  { keywords: ['중소기업은행', 'IBK'], url: 'https://www.ibk.co.kr' },
  { keywords: ['스탠다드차타드', 'SC제일'], url: 'https://www.standardchartered.co.kr' },
  { keywords: ['아이엠뱅크', 'iM뱅크', 'IM뱅크', 'DGB'], url: 'https://www.imbank.co.kr' },
  { keywords: ['경남은행'], url: 'https://www.knbank.co.kr' },
  { keywords: ['부산은행'], url: 'https://www.busanbank.co.kr' },
  { keywords: ['광주은행'], url: 'https://www.kjbank.com' },
  { keywords: ['전북은행'], url: 'https://www.jbbank.co.kr' },
  { keywords: ['제주은행'], url: 'https://www.jejubank.co.kr' },
  { keywords: ['수협은행'], url: 'https://www.suhyup-bank.com' },
  { keywords: ['케이뱅크'], url: 'https://www.kbanknow.com' },
  { keywords: ['카카오뱅크'], url: 'https://www.kakaobank.com' },
  { keywords: ['토스뱅크'], url: 'https://www.tossbank.com' },
  { keywords: ['새마을금고'], url: 'https://www.kfcc.co.kr' },
  { keywords: ['신협'], url: 'https://www.cu.co.kr' },
  { keywords: ['SBI저축은행'], url: 'https://www.sbisb.co.kr' },
  { keywords: ['씨티은행', '씨티뱅크'], url: 'https://www.citibank.co.kr' },
  { keywords: ['우체국'], url: 'https://www.epostbank.go.kr' },
  { keywords: ['산업은행'], url: 'https://www.kdb.co.kr' },
  { keywords: ['국민연금공단'], url: 'https://www.nps.or.kr' },
];

// 은행명 문자열을 넣으면 매칭되는 홈페이지 URL을, 없으면 null을 반환한다.
export function getBankLinkUrl(companyName) {
  if (!companyName) return null;
  const rule = BANK_LINK_RULES.find(({ keywords }) => keywords.some((kw) => companyName.includes(kw)));
  return rule ? rule.url : null;
}