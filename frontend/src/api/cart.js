import request from './request'
import { USE_MOCK } from '../config'
import { delay } from './mock'

// 购物车模块（设计文档：GET/POST/PUT/DELETE /api/carts）
// 说明：页面内购物车状态由 stores/cart.js 管理（mock 模式本地持久化）；
// 联调模式（USE_MOCK=false）可在此处对接网关购物车接口，本文件提供完整封装。
export function getCart() {
  if (USE_MOCK) return delay({ list: [] })
  return request.get('/carts')
}

export function addToCart(data) {
  if (USE_MOCK) return delay({ ok: true })
  return request.post('/carts', data)
}

export function updateCart(data) {
  if (USE_MOCK) return delay({ ok: true })
  return request.put('/carts', data)
}

export function removeCart(ids) {
  if (USE_MOCK) return delay({ ok: true })
  return request.delete('/carts', { data: ids })
}
