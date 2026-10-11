import request from './request'
import { USE_MOCK } from '../config'
import { mockGoods, delay } from './mock'

// 商品模块（接口文档 2.3：GET /api/goods 商品分页 / 详情）
// 排序映射：default 综合 / hot 销量 / priceAsc 价格升 / priceDesc 价格降
function sortGoods(list, sort) {
  const arr = [...list]
  if (sort === 'priceAsc') return arr.sort((a, b) => a.price - b.price)
  if (sort === 'priceDesc') return arr.sort((a, b) => b.price - a.price)
  if (sort === 'hot' || sort === 'sales') return arr.sort((a, b) => (b.isHot || 0) - (a.isHot || 0))
  return arr.sort((a, b) => (b.isHot || 0) - (a.isHot || 0) || a.id - b.id)
}

function mockQuery(params = {}) {
  let list = mockGoods.filter((g) => !g.isTakeDown)
  const { keyword, categoryId, brandId, priceLow, priceHigh, sort, isHot, pageNo = 1, pageSize = 12 } = params
  if (keyword) list = list.filter((g) => g.name.includes(keyword) || (g.alias || '').includes(keyword))
  if (categoryId) list = list.filter((g) => g.categoryId === Number(categoryId))
  if (brandId) list = list.filter((g) => g.brandId === Number(brandId))
  if (priceLow != null) list = list.filter((g) => g.price >= Number(priceLow))
  if (priceHigh != null) list = list.filter((g) => g.price <= Number(priceHigh))
  if (isHot) list = list.filter((g) => g.isHot === 1)
  const sorted = sortGoods(list, sort)
  const total = sorted.length
  const start = (pageNo - 1) * pageSize
  return delay({ list: sorted.slice(start, start + pageSize), total, pageNum: Number(pageNo), pageSize: Number(pageSize) })
}

// 前台公开检索默认过滤：上架(isTakeDown=0) + 未删除(isDel=0)
function publicParams(params = {}) {
  const p = { ...params }
  if (p.isTakeDown == null) p.isTakeDown = 0
  if (p.isDel == null) p.isDel = 0
  return p
}

// 商品分页检索：GET /api/goods（前台自动附加 isTakeDown=0&isDel=0）
export function listGoods(params) {
  if (USE_MOCK) return mockQuery(params)
  return request.get('/goods', { params: publicParams(params) })
}

// 首页热销：GET /api/goods?isHot=1&isTakeDown=0&isDel=0（返回当前页数组）
export async function listHotGoods() {
  if (USE_MOCK) {
    const d = await mockQuery({ isHot: 1, pageSize: 8 })
    return d.list
  }
  const d = await request.get('/goods', { params: { isHot: 1, isTakeDown: 0, isDel: 0, pageSize: 8 } })
  return d.list || d
}

// 管理端全量商品：不过滤上下架/删除状态
export function listAllGoods(params = {}) {
  return request.get('/goods', { params: { pageSize: 100, ...params } })
}

// 商品详情：GET /api/goods/id/{id}?full=true（含品牌 / 分类 / 详情图）
export function getGoodsDetail(id) {
  if (USE_MOCK) {
    const good = mockGoods.find((g) => g.id === Number(id))
    return delay(good ? { ...good } : Promise.reject({ msg: '商品不存在' }), 200)
  }
  return request.get(`/goods/id/${id}`, { params: { full: true } })
}

// 批量查询商品（购物车/秒杀回显用）：GET /api/goods/ids?ids=1,2,3
export function getGoodsByIds(ids) {
  if (!ids || !ids.length) return Promise.resolve([])
  return request.get('/goods/ids', { params: { ids: ids.join(',') } })
}
