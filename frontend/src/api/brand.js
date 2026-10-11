import request from './request'
import { USE_MOCK } from '../config'
import { mockBrands, delay } from './mock'

// 品牌模块（接口文档 2.1：GET /api/brands 品牌分页）
// 后端返回 PageInfo {list, total, ...}，此处统一拍平为数组供页面直接使用
export async function getBrands() {
  if (USE_MOCK) return delay(mockBrands)
  const data = await request.get('/brands', { params: { pageNo: 1, pageSize: 0 } })
  return data.list || data
}
