import request from './request'
import { USE_MOCK } from '../config'
import { mockSeckills, mockSeckillGoods, mockGoods, delay } from './mock'
import { getGoodsByIds } from './goods'

// 秒杀模块（接口文档 5：活动列表 / 活动商品 / 抢购）
// 前台活动列表：GET /api/seckills/active（启用且未结束，进行中在前）
export function listSeckills() {
  if (USE_MOCK) return delay(mockSeckills)
  return request.get('/seckills/active')
}

// 活动商品：秒杀价 / 剩余库存 / 已售 / 限购 + 商品信息组装
// 后端 good 回显依赖 good-api Feign（暂未接入，返回空），前端按 goodId 二次批量查询 /api/goods/ids
export async function getSeckillGoods(seckillId) {
  if (!seckillId) return []
  if (USE_MOCK) {
    const items = mockSeckillGoods
      .filter((s) => s.seckillId === Number(seckillId))
      .map((s) => {
        const good = mockGoods.find((g) => g.id === s.goodId)
        return { ...s, good }
      })
    return delay(items)
  }
  const items = await request.get(`/seckills/${seckillId}/goods`)
  const ids = [...new Set((items || []).map((it) => it.goodId).filter(Boolean))]
  if (!ids.length) return items || []
  try {
    const goods = await getGoodsByIds(ids)
    const map = Object.fromEntries((goods || []).map((g) => [g.id, g]))
    return items.map((it) => ({ ...it, good: map[it.goodId] || it.good || null }))
  } catch (e) {
    return items || []
  }
}

// 抢购：提交后返回秒杀流水号，订单在消息消费者中异步生成。
export function grabSeckill(data) {
  if (USE_MOCK) {
    return delay({ orderNo: `MOCK${Date.now()}`, status: '处理中', message: '抢购提交成功，请等待结果' })
  }
  return request.post('/seckills/grab', data)
}

export function getGrabResult(seckillNo) {
  if (USE_MOCK) return delay({ orderNo: seckillNo, actualOrderNo: seckillNo, status: '成功' })
  return request.get(`/seckills/grab/${encodeURIComponent(seckillNo)}`)
}
