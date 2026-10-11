import request from './request'
import { USE_MOCK } from '../config'
import { delay } from './mock'

// 商品收藏模块（GET/POST/DELETE /api/collects）
// 说明：收藏状态由 stores/favorite.js 管理（mock 模式本地持久化）；
// 联调模式（USE_MOCK=false）可在此处对接网关收藏接口，本文件提供完整封装。
export function getCollects() {
  if (USE_MOCK) return delay([])
  return request.get('/collects')
}

export function checkCollected(goodId) {
  if (USE_MOCK) return delay(false)
  return request.get('/collects/check', { params: { goodId } })
}

export function collectGood(data) {
  if (USE_MOCK) return delay({ ok: true })
  return request.post('/collects', data)
}

export function removeCollectByGood(goodId) {
  if (USE_MOCK) return delay({ ok: true })
  return request.delete(`/collects/good/${goodId}`)
}

export function removeCollects(ids) {
  if (USE_MOCK) return delay({ ok: true })
  return request.delete('/collects', { data: ids })
}
