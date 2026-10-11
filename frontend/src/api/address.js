import request from './request'
import { USE_MOCK } from '../config'
import { delay } from './mock'
import { useUserStore } from '../stores/user'

// 会员地址模块（接口文档 3.2：/api/member-addresses）
// mock 地址数据（对齐测试数据.sql 的 member_address）
let mockAddresses = [
  { id: 1, memberAccount: 'demo', receiver: '张三', phone: '13800138001', province: '北京市', city: '北京市', district: '朝阳区', detail: '建国路88号院3号楼501', isDefault: 1 },
  { id: 2, memberAccount: 'demo', receiver: '张太太', phone: '13900138001', province: '北京市', city: '北京市', district: '海淀区', detail: '中关村大街1号', isDefault: 0 },
  { id: 3, memberAccount: 'demo', receiver: '李四', phone: '13800138002', province: '上海市', city: '上海市', district: '浦东新区', detail: '陆家嘴环路1000号A座20层', isDefault: 0 },
]

let nextId = 4

// 地址列表（当前会员；管理端查他人地址时传 { account }）
export function listAddresses(params = {}) {
  if (USE_MOCK) return delay([...mockAddresses])
  const cfg = { params: {} }
  if (params.account) {
    // 管理端查看指定会员地址：不携带当前身份头，改用 account 参数
    cfg._identity = 'none'
    cfg.params.account = params.account
  }
  return request.get('/member-addresses', cfg)
}

// 新增地址：{memberAccount(可省，真实模式下由前端补账号——后端 insert 未注入身份头), receiver, phone, addrId(地区ID), addrDetail, isDefault}
export function addAddress(data) {
  if (USE_MOCK) {
    const item = { id: nextId++, memberAccount: 'demo', ...data, isDefault: data.isDefault ? 1 : 0 }
    if (item.isDefault) {
      mockAddresses.forEach((a) => { a.isDefault = 0 })
    }
    mockAddresses.push(item)
    return delay(item)
  }
  const user = useUserStore()
  const body = { ...data }
  if (!body.memberAccount) body.memberAccount = user.info?.account || ''
  return request.post('/member-addresses', body)
}

// 更新地址：PUT /api/member-addresses（Body 含 id，空值不更新）
export function updateAddress(id, data) {
  if (USE_MOCK) {
    const idx = mockAddresses.findIndex((a) => a.id === Number(id))
    if (idx >= 0) {
      mockAddresses[idx] = { ...mockAddresses[idx], ...data, isDefault: data.isDefault ? 1 : 0 }
      if (data.isDefault) {
        mockAddresses.forEach((a, i) => { if (i !== idx) a.isDefault = 0 })
      }
    }
    return delay({ success: true })
  }
  return request.put('/member-addresses', { id, ...data })
}

// 删除地址：DELETE /api/member-addresses/{id}
export function deleteAddress(id) {
  if (USE_MOCK) {
    mockAddresses = mockAddresses.filter((a) => a.id !== Number(id))
    return delay({ success: true })
  }
  return request.delete(`/member-addresses/${id}`)
}

// 设为默认：PUT /api/member-addresses/{id}/default
export function setDefaultAddress(id) {
  if (USE_MOCK) {
    mockAddresses.forEach((a) => { a.isDefault = a.id === Number(id) ? 1 : 0 })
    return delay({ success: true })
  }
  return request.put(`/member-addresses/${id}/default`)
}
