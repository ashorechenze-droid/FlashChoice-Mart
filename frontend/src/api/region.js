import request from './request'
import { USE_MOCK } from '../config'
import { delay } from './mock'

// 行政区划模块（接口文档：GET /api/regions/tree 省/市/区三级树；GET /api/regions?parentId= 级联）
export function getRegionTree() {
  if (USE_MOCK) return delay([])
  return request.get('/regions/tree')
}

export function getRegionsByParent(parentId = 0) {
  if (USE_MOCK) return delay([])
  return request.get('/regions', { params: { parentId } })
}
