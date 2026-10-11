import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { refresh as apiRefresh } from './member'
import router from '../router'

// Axios 实例：统一 baseURL 为网关前缀（接口文档 1.1：/api/** -> 网关 :8888）
// 开发环境由 vite proxy 将 /api 反代到网关 :8888
const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

const TOKEN_KEY = 'mall_token'
const REFRESH_TOKEN_KEY = 'mall_refresh_token'
const ADMIN_KEY = 'mall_admin'

// 当前是否管理端会话（管理端登录态存 mall_admin，见 AdminDashboard.vue）
function isAdminSession() {
  return !!localStorage.getItem(ADMIN_KEY)
}

// 管理端会话详情（管理端令牌独立存放于 mall_admin：{ id, username, name, role, token, refreshToken }）
function getAdminSession() {
  try {
    return JSON.parse(localStorage.getItem(ADMIN_KEY) || 'null')
  } catch (e) {
    return null
  }
}

// 管理端令牌刷新：独立 axios 直连 /api/users/refresh（网关白名单放行），
// 不走 service 实例以避免与 api/admin.js 的循环依赖
function refreshAdminToken(refreshToken) {
  return axios.post('/api/users/refresh', { refreshToken }).then((res) => {
    const body = res.data
    if (body && (body.code === 200 || body.success === true)) return body.data
    throw new Error((body && body.msg) || '刷新失败')
  })
}

// 身份请求头（接口文档 1.2：网关校验通过后注入 X-User-Id / X-User-Name / X-Audience）
// - 默认：已登录会员自动携带会员身份头（订单/购物车/地址等「我的数据」接口依赖）
// - config._identity = 'none'：管理端查全量数据等场景，不携带任何身份头
function identityHeaders(config) {
  const headers = {}
  if (config._identity === 'none') return headers
  const user = useUserStore()
  if (user.info && user.info.id != null) {
    headers['X-User-Id'] = String(user.info.id)
    headers['X-User-Name'] = user.info.account || user.info.name || ''
    headers['X-Audience'] = 'member'
  }
  return headers
}

// 请求拦截器：自动携带 JWT（Authorization: Bearer {accessToken}）+ 身份头
service.interceptors.request.use((config) => {
  const user = useUserStore()
  const adminSession = getAdminSession()
  // 管理端会话优先（令牌存 mall_admin，与会员令牌槽位分离，见 AdminDashboard.vue）
  const token = adminSession && adminSession.token ? adminSession.token : user.token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  Object.assign(config.headers, identityHeaders(config))
  return config
})

// ===== 401 令牌自动刷新机制 =====
// 接口文档 1.2：Access Token 过期返回 401，用 Refresh Token 换新
// 会员端走 /api/members/refresh；管理端走 /api/users/refresh（按 mall_admin 登录态区分）
// Refresh Token 一次性轮换，并发 401 请求排队等待同一次刷新完成后重试
let isRefreshing = false
let refreshSubscribers = []

function subscribeTokenRefresh(cb) {
  refreshSubscribers.push(cb)
}
function onRefreshed(token) {
  refreshSubscribers.forEach((cb) => cb(token))
  refreshSubscribers = []
}
function handleAuthFailure() {
  const adminSession = isAdminSession()
  const user = useUserStore()
  user.token = ''
  user.refreshToken = ''
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  if (adminSession) {
    // 管理端会话失效：清除管理端登录态并回到管理端登录页（强制刷新以重置组件内状态）
    localStorage.removeItem(ADMIN_KEY)
    ElMessage.warning('登录已过期，请重新登录')
    if (!window.location.hash.startsWith('#/admin')) window.location.hash = '#/admin'
    window.location.reload()
    return
  }
  user.info = null
  localStorage.removeItem('mall_user')
  ElMessage.warning('登录已过期，请重新登录')
  router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
}

// 响应拦截器：统一处理业务码（接口文档 1.3：code=200 或 success=true 表示成功）
service.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body === 'object') {
      // 兼容两种成功格式：{code: 200, data: ...} 和 {success: true, data: ...}
      const isSuccess = body.code === 200 || body.success === true
      if (isSuccess) return body.data
      if (body.code === 401) {
        const config = res.config
        // refresh 请求本身 401（Refresh Token 也过期了），直接跳转登录
        if (config._isRefresh) {
          handleAuthFailure()
          return Promise.reject(new Error(body.msg || '登录已过期'))
        }
        const user = useUserStore()
        const adminSession = getAdminSession()
        // 按当前会话取对应 Refresh Token（管理端存 mall_admin，会员存 mall_token）
        const currentRefreshToken = isAdminSession() ? (adminSession && adminSession.refreshToken) : user.refreshToken
        // 无 Refresh Token 可换：直接判定会话失效
        if (!currentRefreshToken) {
          handleAuthFailure()
          return Promise.reject(new Error(body.msg || '登录已过期'))
        }
        // 有 Refresh Token 且未在刷新中 → 发起刷新（管理端 /api/users/refresh，会员端 /api/members/refresh）
        if (!isRefreshing) {
          isRefreshing = true
          const adminFlow = isAdminSession()
          const doRefresh = adminFlow ? refreshAdminToken(currentRefreshToken) : apiRefresh(currentRefreshToken)
          doRefresh
            .then((data) => {
              if (adminFlow) {
                // 管理端令牌轮换：写回 mall_admin，不污染会员令牌槽位
                const session = getAdminSession() || {}
                session.token = data.accessToken
                session.refreshToken = data.refreshToken
                localStorage.setItem(ADMIN_KEY, JSON.stringify(session))
              } else {
                user.token = data.accessToken
                user.refreshToken = data.refreshToken
                localStorage.setItem(TOKEN_KEY, data.accessToken)
                localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
              }
              onRefreshed(data.accessToken)
            })
            .catch(() => {
              onRefreshed(null)
              handleAuthFailure()
            })
            .finally(() => {
              isRefreshing = false
            })
        }
        // 并发 401 请求排队：等刷新完成后用新 Token 重试
        const retryOriginal = new Promise((resolve, reject) => {
          subscribeTokenRefresh((token) => {
            if (!token) {
              reject(new Error('登录已过期'))
              return
            }
            config.headers.Authorization = `Bearer ${token}`
            resolve(service(config))
          })
        })
        return retryOriginal
      }
      // 明确失败的情况：code 非 200 或 success === false
      const isFailure = (body.code != null && body.code !== 200) || body.success === false
      if (isFailure) {
        // _silent: true 时不弹出全局错误提示（非关键接口如通知、统计等，由调用方自行处理）
        if (!res.config._silent) ElMessage.error(body.msg || '请求失败')
        return Promise.reject(new Error(body.msg || '请求失败'))
      }
      // 无标准业务码字段（如纯数据响应），直接返回
      return body
    }
    return body
  },
  (err) => {
    const msg = err.response?.data?.msg || err.message || '网络异常，请稍后重试'
    if (!err.config?._silent) ElMessage.error(msg)
    return Promise.reject(err)
  },
)

export default service
