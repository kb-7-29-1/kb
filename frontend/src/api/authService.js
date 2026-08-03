import api from './api'

// 회원가입
export const signup = (data) => api.post('/user/signup', data)

// 아이디 종복확인
export const checkId = (loginId) => api.get('/user/check-id', {params:{loginId}})

// 로그인
export const login = (data) => api.post('/auth/login',data)

// 아이디 찾기
export const findId = (email) => api.post('/auth/find-id',{email})

// 비밀번호 찾기 - 본인확인
export const verifyForPasswordReset = (data) => api.post('/auth/find-password', data)

// 비밀번호 찾기 - 재설정
export const resetPassword = (data) => api.post('/auth/reset-password',data)

// 프로필 조회
export const getProfile = () => api.get('/user/profile')

// 프로필 수정
export const updateProfile = (data) => api.put('/user/profile', data)

// 비밀번호 변경
export const changePassword = (data) => api.put('/user/password', data)

// 회원 탈퇴
export const withdraw = (password) => api.delete('/user', {data:{password}})

// 로그아웃 (백엔드 API 없이 프론트에서 토큰만 삭제)
export const logout = () =>{
    // 별도 서버 호출 없음, useAuthStore.logout() 사용
}