import request from './request'
import { USE_MOCK } from '../config'
import { delay } from './mock'

// 订单模块（接口文档 4.2：/api/orders）
// 后端明细字段为 orderItems，统一归一化为 items 供页面使用
function normalizeOrders(list) {
  return (list || []).map((o) => ({ ...o, items: o.orderItems || o.items || [] }))
}

// mock 订单数据（对齐测试数据.sql 的 order / order_item）
const mockOrders = [
  {
    id: 1, orderNo: 'ORD202609070001', status: '已支付', totalPay: 6499.00,
    payType: '模拟支付', checkoutTime: '2026-09-07 10:00:00', payTime: '2026-09-07 10:05:00',
    shipTime: '2026-09-07 14:00:00', acceptTime: null,
    receiverName: '张三', receiverPhone: '13800138001',
    receiverAddrDetail: '北京市朝阳区建国路88号院3号楼501',
    orderComment: '请放快递柜', refundStatus: '无退款',
    items: [
      { goodId: 1, goodName: '华为Mate 60 Pro', goodPic: 'https://picsum.photos/seed/SPU001/200/200', dealPrice: 6499.00, count: 1 },
    ],
  },
  {
    id: 2, orderNo: 'ORD202609070002', status: '待收货', totalPay: 17998.00,
    payType: '模拟支付', checkoutTime: '2026-09-07 11:00:00', payTime: '2026-09-07 11:10:00',
    shipTime: null, acceptTime: null,
    receiverName: '李四', receiverPhone: '13800138002',
    receiverAddrDetail: '上海市浦东新区陆家嘴环路1000号A座20层',
    orderComment: null, refundStatus: '无退款',
    items: [
      { goodId: 4, goodName: '联想拯救者Y9000X', goodPic: 'https://picsum.photos/seed/SPU004/200/200', dealPrice: 9999.00, count: 1 },
      { goodId: 5, goodName: '华为MateBook X Pro', goodPic: 'https://picsum.photos/seed/SPU005/200/200', dealPrice: 7999.00, count: 1 },
    ],
  },
  {
    id: 3, orderNo: 'ORD202609070003', status: '已确认', totalPay: 999.00,
    payType: '模拟支付', checkoutTime: '2026-09-07 09:00:00', payTime: '2026-09-07 09:05:00',
    shipTime: '2026-09-07 13:00:00', acceptTime: '2026-09-07 17:00:00',
    receiverName: '王五', receiverPhone: '13800138003',
    receiverAddrDetail: '广州市天河区天河路385号1201室',
    orderComment: '谢谢', refundStatus: '无退款',
    items: [
      { goodId: 6, goodName: '小米手表S3', goodPic: 'https://picsum.photos/seed/SPU006/200/200', dealPrice: 999.00, count: 1 },
    ],
  },
  {
    id: 4, orderNo: 'ORD202609060001', status: '待付款', totalPay: 3299.00,
    payType: null, checkoutTime: '2026-09-06 16:00:00', payTime: null,
    shipTime: null, acceptTime: null,
    receiverName: '赵六', receiverPhone: '13800138004',
    receiverAddrDetail: '重庆市渝中区中山四路1号附1号',
    orderComment: null, refundStatus: '无退款',
    items: [
      { goodId: 7, goodName: '格力空调·云锦', goodPic: 'https://picsum.photos/seed/SPU007/200/200', dealPrice: 3299.00, count: 1 },
    ],
  },
  {
    id: 5, orderNo: 'ORD202609050001', status: '已确认', totalPay: 9999.00,
    payType: '模拟支付', checkoutTime: '2026-09-05 14:00:00', payTime: '2026-09-05 14:20:00',
    shipTime: '2026-09-06 09:00:00', acceptTime: '2026-09-06 18:00:00',
    receiverName: '孙七', receiverPhone: '13800138005',
    receiverAddrDetail: '南京市玄武区长江路2号301室',
    orderComment: null, refundStatus: '无退款',
    items: [
      { goodId: 3, goodName: 'iPhone 15 Pro Max', goodPic: 'https://picsum.photos/seed/SPU003/200/200', dealPrice: 8999.00, count: 1 },
      { goodId: 6, goodName: '小米手表S3', goodPic: 'https://picsum.photos/seed/SPU006/200/200', dealPrice: 999.00, count: 1 },
    ],
  },
]

// 我的订单列表：GET /api/orders（身份头 X-User-Name 自动过滤本人）
export async function listMyOrders(params = {}) {
  if (USE_MOCK) {
    let list = [...mockOrders]
    if (params.status && params.status !== 'all') {
      list = list.filter((o) => o.status === params.status)
    }
    return delay({ list, total: list.length })
  }
  const data = await request.get('/orders', { params: { pageNo: 1, pageSize: 50, ...params } })
  return { ...data, list: normalizeOrders(data.list) }
}

// 订单详情：GET /api/orders/id/{id}?withItems=true
export async function getOrderDetail(id) {
  if (USE_MOCK) {
    const order = mockOrders.find((o) => o.id === Number(id))
    return delay(order || Promise.reject({ msg: '订单不存在' }))
  }
  const data = await request.get(`/orders/id/${id}`, { params: { withItems: true } })
  return { ...data, items: data.orderItems || data.items || [] }
}

// 提交订单：POST /api/orders
// 方式一（购物车结算）{cartIds, addrId, comment}；方式二（立即购买）{goodId, qty, addrId, comment}
export function createOrder(data) {
  if (USE_MOCK) {
    return delay({ id: Date.now(), orderNo: `ORD${Date.now()}`, status: '待付款', totalPay: 0 })
  }
  return request.post('/orders', data)
}

// 发起支付并模拟支付确认：PUT /api/orders/{id}/pay → POST /api/orders/{id}/pay/confirm
export async function payOrder(id) {
  if (USE_MOCK) {
    const order = mockOrders.find((o) => o.id === Number(id))
    if (order) {
      order.status = '已支付'
      order.payTime = new Date().toISOString().slice(0, 19).replace('T', ' ')
      order.payType = '模拟支付'
    }
    return delay({ success: true, tradeNo: 'ALI' + Date.now() })
  }
  const cashier = await request.put(`/orders/${id}/pay`)
  await request.post(`/orders/${id}/pay/confirm`, { payType: '模拟支付' })
  return { ...cashier, success: true }
}

// 取消订单（仅待付款）：PUT /api/orders/{id}/cancel
export function cancelOrder(id) {
  if (USE_MOCK) {
    const order = mockOrders.find((o) => o.id === Number(id))
    if (order) order.status = '已取消'
    return delay({ success: true })
  }
  return request.put(`/orders/${id}/cancel`)
}

// 确认收货（仅待收货）：PUT /api/orders/{id}/confirm
export function confirmReceive(id) {
  if (USE_MOCK) {
    const order = mockOrders.find((o) => o.id === Number(id))
    if (order) {
      order.status = '已确认'
      order.acceptTime = new Date().toISOString().slice(0, 19).replace('T', ' ')
    }
    return delay({ success: true })
  }
  return request.put(`/orders/${id}/confirm`)
}

// 全部订单（管理端用）：GET /api/orders（不携带身份头 = 全量）
export async function listAllOrders(params = {}) {
  if (USE_MOCK) {
    let list = [...mockOrders]
    if (params.status && params.status !== 'all') {
      list = list.filter((o) => o.status === params.status)
    }
    return delay({ list, total: list.length })
  }
  const data = await request.get('/orders', { params: { pageNo: 1, pageSize: 100, ...params }, _identity: 'none' })
  return { ...data, list: normalizeOrders(data.list) }
}

// 按会员账号查订单（管理端会员详情）：GET /api/orders?memberAccount=xxx
export async function getOrdersByAccount(account) {
  if (USE_MOCK) return delay(mockOrders)
  const data = await request.get('/orders', { params: { memberAccount: account, pageSize: 100 }, _identity: 'none' })
  return normalizeOrders(data.list)
}
