import request from './request'
import { USE_MOCK } from '../config'
import { delay } from './mock'
import { getOrdersByAccount } from './order'

// ============================================================
// 管理端 API（对齐「FlashChoice-Mart - 接口文档.md」）
// USE_MOCK=true  返回本地 mock 数据（演示/答辩）
// USE_MOCK=false 走真实后端网关 /api/**
// ============================================================

// ---------- mock 数据 ----------
const mockMembers = [
  { id: 1, account: 'demo', name: '优选会员', phone: '13800138000', email: 'demo@example.com', sex: '男', enabled: 1, createdTime: '2026-09-01 10:00:00' },
  { id: 2, account: 'zhangsan', name: '张三', phone: '13800138001', email: 'zhangsan@example.com', sex: '男', enabled: 1, createdTime: '2026-09-02 11:00:00' },
  { id: 3, account: 'lisi', name: '李四', phone: '13800138002', email: 'lisi@example.com', sex: '女', enabled: 1, createdTime: '2026-09-03 14:00:00' },
  { id: 4, account: 'wangwu', name: '王五', phone: '13800138003', email: 'wangwu@example.com', sex: '男', enabled: 1, createdTime: '2026-09-04 09:00:00' },
]

const mockSalesTrend = [
  { day: '09-02', value: 3299 },
  { day: '09-03', value: 5499 },
  { day: '09-04', value: 2199 },
  { day: '09-05', value: 9999 },
  { day: '09-06', value: 3299 },
  { day: '09-07', value: 6499 },
  { day: '09-08', value: 17998 },
]

// ---------- 管理员图形验证码 ----------
// GET /api/users/captcha（接口文档 6.1：后台用户独立验证码，与会员互不串用）
// 返回 { captchaId, imageBase64 }
export function getAdminCaptcha() {
  if (USE_MOCK) return Promise.resolve({ captchaId: 'mock-' + Date.now(), imageBase64: '' })
  return request.get('/users/captcha')
}

// ---------- 管理员登录 ----------
// POST /api/users/login（接口文档 6.1：返回 { user, accessToken, refreshToken, tokenType, expiresIn }）
export async function adminLogin(data) {
  if (USE_MOCK) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        if (data.username === 'admin' && data.password === '123456') {
          resolve({
            user: { id: 1, username: 'admin', name: '系统管理员', role: '超级管理员' },
            accessToken: 'admin_mock_token_' + Date.now(),
            refreshToken: 'admin_mock_refresh_' + Date.now(),
          })
        } else {
          reject({ msg: '账号或密码错误' })
        }
      }, 500)
    })
  }
  const res = await request.post('/users/login', data)
  // 后端返回 { user, accessToken, refreshToken, ... }，用户信息在 res.user 里
  const u = res.user || res
  return {
    id: u.id,
    username: u.username || u.account,
    name: u.name,
    role: u.role || '管理员',
    token: res.accessToken || '',
    refreshToken: res.refreshToken || '',
  }
}

// ---------- 会员列表 ----------
// GET /api/members（keyword 匹配 account/name/phone，分页）
export function listMembers(params = {}) {
  if (USE_MOCK) {
    let list = [...mockMembers]
    if (params.keyword) {
      list = list.filter((m) => m.account.includes(params.keyword) || m.name.includes(params.keyword) || m.phone.includes(params.keyword))
    }
    return delay({ list, total: list.length })
  }
  return request.get('/members', { params: { pageNo: 1, pageSize: 100, ...params } })
}

// ---------- 会员详情 ----------
// GET /api/members/id/{id}
export function getMemberDetail(id) {
  if (USE_MOCK) {
    const m = mockMembers.find((x) => x.id === Number(id))
    return delay(m || Promise.reject({ msg: '会员不存在' }))
  }
  return request.get(`/members/id/${id}`)
}

// ---------- 启用/停用会员 ----------
// PUT /api/members/{id}/enabled?enabled=true|false
export function updateMemberEnabled(id, enabled) {
  if (USE_MOCK) {
    const m = mockMembers.find((x) => x.id === Number(id))
    if (m) m.enabled = enabled ? 1 : 0
    return delay({ success: true })
  }
  return request.put(`/members/${id}/enabled`, null, { params: { enabled } })
}

// ---------- 会员订单记录 ----------
// GET /api/orders?memberAccount=xxx（后台全量视角，按会员账号过滤）
export function getMemberOrders(account) {
  if (USE_MOCK) {
    const memberId = Number(account) || (account === 'demo' ? 1 : 0)
    const allOrders = [
      { id: 1, orderNo: 'ORD202609070001', status: '已支付', totalPay: 6499.00, checkoutTime: '2026-09-07 10:00:00', items: [{ goodId: 1, goodName: '华为Mate 60 Pro', count: 1, dealPrice: 6499 }] },
      { id: 2, orderNo: 'ORD202609070002', status: '待收货', totalPay: 17998.00, checkoutTime: '2026-09-07 11:00:00', items: [{ goodId: 4, goodName: '联想拯救者Y9000X', count: 1, dealPrice: 9999 }, { goodId: 5, goodName: '华为MateBook X Pro', count: 1, dealPrice: 7999 }] },
      { id: 3, orderNo: 'ORD202609070003', status: '已确认', totalPay: 999.00, checkoutTime: '2026-09-07 09:00:00', items: [{ goodId: 6, goodName: '小米手表S3', count: 1, dealPrice: 999 }] },
      { id: 4, orderNo: 'ORD202609060001', status: '待付款', totalPay: 3299.00, checkoutTime: '2026-09-06 16:00:00', items: [{ goodId: 7, goodName: '格力空调·云锦', count: 1, dealPrice: 3299 }] },
      { id: 5, orderNo: 'ORD202609050001', status: '已确认', totalPay: 9999.00, checkoutTime: '2026-09-05 14:00:00', items: [{ goodId: 3, goodName: 'iPhone 15 Pro Max', count: 1, dealPrice: 8999 }, { goodId: 6, goodName: '小米手表S3', count: 1, dealPrice: 999 }] },
    ]
    const result = memberId === 1 ? allOrders : allOrders.filter((o) => o.id === memberId)
    return delay(result)
  }
  return getOrdersByAccount(account)
}

// ---------- 订单发货 ----------
// PUT /api/orders/{id}/ship
export function shipOrder(id) {
  if (USE_MOCK) return delay({ success: true, shipTime: new Date().toISOString().slice(0, 19).replace('T', ' ') })
  return request.put(`/orders/${id}/ship`)
}

// ---------- 商品上下架 ----------
// PUT /api/goods/take-down?ids={id}&isTakeDown={true|false}
export function updateGoodsStatus(id, isTakeDown) {
  if (USE_MOCK) return delay({ success: true })
  return request.put('/goods/take-down', null, { params: { ids: id, isTakeDown } })
}

// ---------- 商品新增/编辑 ----------
// POST /api/goods（新增，id 为空）；PUT /api/goods（编辑，Body 带 id）
export function saveGoods(data) {
  if (USE_MOCK) return delay({ success: true, id: data.id || Date.now() })
  return data.id ? request.put('/goods', data) : request.post('/goods', data)
}

// ---------- 品牌新增/编辑/删除 ----------
// POST/PUT /api/brands；DELETE /api/brands（Body: [id]）
export function saveBrand(data) {
  if (USE_MOCK) return delay({ success: true })
  return data.id ? request.put('/brands', data) : request.post('/brands', data)
}

export function deleteBrand(id) {
  if (USE_MOCK) return delay({ success: true })
  return request.delete('/brands', { data: [id] })
}

// ---------- 秒杀活动管理 ----------
// GET /api/seckills（keyword/enabled/pageNo/pageSize 分页）
export async function listSeckills() {
  if (USE_MOCK) {
    return delay([
      { id: 1, name: '今晚八点场', enabled: 1, startTime: '2026-09-08 20:00:00', endTime: '2026-09-08 22:00:00', description: '爆款直降到底，手快有手慢无', goodCount: 3 },
      { id: 2, name: '明日午间场', enabled: 1, startTime: '2026-09-09 12:00:00', endTime: '2026-09-09 17:00:00', description: '限时5小时，秒杀价先到先得', goodCount: 2 },
      { id: 3, name: '周末大促', enabled: 1, startTime: '2026-09-13 00:00:00', endTime: '2026-09-14 23:59:59', description: '精选好物低至5折', goodCount: 2 },
      { id: 4, name: '国庆特惠', enabled: 0, startTime: '2026-10-01 00:00:00', endTime: '2026-10-07 23:59:59', description: '国庆七天乐，全场特惠', goodCount: 0 },
    ])
  }
  const data = await request.get('/seckills', { params: { pageNo: 1, pageSize: 100 } })
  return data.list || data
}

// POST /api/seckills
export function createSeckill(data) {
  if (USE_MOCK) return delay({ id: Date.now(), ...data, success: true })
  return request.post('/seckills', data)
}

// PUT /api/seckills（Body 带 id）
export function updateSeckill(id, data) {
  if (USE_MOCK) return delay({ success: true })
  return request.put('/seckills', { id, ...data })
}

// DELETE /api/seckills/{id}
export function deleteSeckill(id) {
  if (USE_MOCK) return delay({ success: true })
  return request.delete(`/seckills/${id}`)
}

// 秒杀商品列表 GET /api/seckills/{seckillId}/goods
export function listSeckillGoods(seckillId) {
  if (USE_MOCK) {
    const items = [
      { id: 101, seckillId: 1, goodId: 2, goodName: '小米14 Ultra', seckillPrice: 999, stock: 20, sold: 12, limitPerUser: 1 },
      { id: 102, seckillId: 1, goodId: 8, goodName: '华为FreeBuds Pro 3', seckillPrice: 199, stock: 30, sold: 24, limitPerUser: 2 },
      { id: 103, seckillId: 1, goodId: 12, goodName: '小米手环8 Pro', seckillPrice: 299, stock: 25, sold: 9, limitPerUser: 1 },
      { id: 201, seckillId: 2, goodId: 9, goodName: 'iPad Air 5', seckillPrice: 3499, stock: 10, sold: 0, limitPerUser: 1 },
      { id: 202, seckillId: 2, goodId: 6, goodName: '小米手表S3', seckillPrice: 399, stock: 40, sold: 3, limitPerUser: 2 },
      { id: 301, seckillId: 3, goodId: 4, goodName: '联想拯救者Y9000X', seckillPrice: 499, stock: 60, sold: 5, limitPerUser: 2 },
      { id: 302, seckillId: 3, goodId: 10, goodName: '索尼WH-1000XM5', seckillPrice: 199, stock: 35, sold: 2, limitPerUser: 2 },
    ]
    return delay(items.filter((i) => i.seckillId === Number(seckillId)))
  }
  return request.get(`/seckills/${seckillId}/goods`)
}

// 添加秒杀商品 POST /api/seckills/{seckillId}/goods
export function addSeckillGood(seckillId, data) {
  if (USE_MOCK) return delay({ success: true })
  return request.post(`/seckills/${seckillId}/goods`, data)
}

// 删除秒杀商品 DELETE /api/seckills/goods/{id}（id 为秒杀商品行 id）
export function removeSeckillGood(id) {
  if (USE_MOCK) return delay({ success: true })
  return request.delete(`/seckills/goods/${id}`)
}
