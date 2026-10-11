import request from './request'
import { USE_MOCK } from '../config'
import { delay } from './mock'

// 会员通知/站内信模块（GET/PUT /api/notices）
// 触发来源：收藏商品降价（good-api 改价检测）与加入秒杀活动（seckill-api），
// 由收藏微服务通知模块批量下发；mock 模式返回演示数据。
const mockNotices = [
  { id: 2, type: 2, goodId: 4, title: '收藏商品进入秒杀会场', content: '你收藏的「轻弹缓震跑鞋」已加入秒杀活动「周末大促」，秒杀价 ￥499，先到先得', isRead: false, createdTime: '2026-09-14 10:30:00' },
  { id: 1, type: 1, goodId: 2, title: '收藏商品降价提醒', content: '你收藏的「智能手表 S8 运动版」降价了：￥1999 → ￥1499，快来看看吧', isRead: false, createdTime: '2026-09-14 09:12:00' },
]

export function getNotices(params) {
  if (USE_MOCK) return delay(mockNotices.filter((n) => !params?.unreadOnly || !n.isRead))
  return request.get('/notices', { params, _silent: true })
}

export function getUnreadCount() {
  if (USE_MOCK) return delay(mockNotices.filter((n) => !n.isRead).length)
  return request.get('/notices/unread-count', { _silent: true })
}

export function markNoticeRead(id) {
  if (USE_MOCK) {
    const n = mockNotices.find((x) => x.id === id)
    if (n) n.isRead = true
    return delay({ ok: true })
  }
  return request.put(`/notices/${id}/read`)
}

export function markAllNoticesRead() {
  if (USE_MOCK) {
    mockNotices.forEach((n) => { n.isRead = true })
    return delay({ ok: true })
  }
  return request.put('/notices/read-all')
}
