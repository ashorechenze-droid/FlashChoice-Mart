import request from './request'
import { USE_MOCK } from '../config'
import { mockCategories, delay } from './mock'

// 把分类树拍平为平铺列表（保留 parentId 关系，兼容后端树状结构返回）
// 后端 /api/categories/tree 返回 [{ id, name, parentId, children: [...] }]
// 拍平后每个节点都带 parentId，前端现有筛选逻辑可直接使用
function flattenTree(nodes, parentId = 0, result = []) {
  for (const n of nodes || []) {
    result.push({ ...n, parentId: n.parentId != null ? n.parentId : parentId })
    if (n.children && n.children.length) {
      flattenTree(n.children, n.id, result)
    }
  }
  return result
}

// 分类列表（前台用 tree 接口拍平，兼容后端树状结构）
export async function getCategories() {
  if (USE_MOCK) return delay(flattenTree(mockCategories))
  const tree = await request.get('/categories/tree')
  return flattenTree(tree)
}

// 分类树（原始树结构，需要层级展示时用）
export function getCategoryTree() {
  if (USE_MOCK) return delay(mockCategories)
  return request.get('/categories/tree')
}
