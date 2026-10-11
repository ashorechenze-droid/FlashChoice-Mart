import request from './request'
import { USE_MOCK } from '../config'
import { mockLogin, mockRegister } from './mock'

// 会员模块（接口文档 3.1：/api/members）

// 获取图形验证码（接口文档 3.1：GET /api/members/captcha）
// 返回 { captchaId, imageBase64 }，imageBase64 为 PNG Data URL，可直接用于 <img :src>
export function getCaptcha() {
  if (USE_MOCK) {
    return Promise.resolve({ captchaId: 'mock-' + Date.now(), imageBase64: '' })
  }
  return request.get('/members/captcha')
}

export function login(data) {
  if (USE_MOCK) return mockLogin(data)
  return request.post('/members/login', data)
}

export function register(data) {
  if (USE_MOCK) return mockRegister(data)
  return request.post('/members/register', data)
}

// 令牌刷新（接口文档 3.1：POST /api/members/refresh，Body: {refreshToken}）
// 返回新的一对 Token { accessToken, refreshToken, ... }
// _isRefresh 标记：响应拦截器识别后不再触发二次刷新，避免无限循环
export function refresh(refreshToken) {
  if (USE_MOCK) return Promise.resolve({ accessToken: 'mock_refreshed_' + Date.now(), refreshToken: 'mock_refresh_' + Date.now() })
  return request.post('/members/refresh', { refreshToken }, { _isRefresh: true })
}

// 退出登录（接口文档 3.1：POST /api/members/logout，Body 可省 {refreshToken}）
export function logout(data = {}) {
  if (USE_MOCK) return Promise.resolve({ success: true })
  return request.post('/members/logout', data)
}

// 我的资料（接口文档 3.1：GET /api/members/id/{id}）
export function getMemberInfo(id) {
  if (USE_MOCK) return Promise.resolve({ id, account: 'demo', name: '优选会员' })
  return request.get(`/members/id/${id}`)
}
