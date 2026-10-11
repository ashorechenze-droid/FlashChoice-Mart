<template>
  <div class="member-center">
    <div class="container mc-layout">
      <!-- 左侧菜单 -->
      <aside class="mc-sidebar">
        <div class="mc-user-card">
          <div class="mc-avatar">{{ (user.info?.name || user.info?.account || 'U').slice(0, 1).toUpperCase() }}</div>
          <div class="mc-user-info">
            <p class="mc-user-name">{{ user.info?.name || user.info?.account || '会员' }}</p>
            <p class="mc-user-level"><el-icon><Medal /></el-icon> 黄金会员</p>
          </div>
          <div class="mc-user-stats">
            <div class="mc-stat"><span class="mc-stat-num">{{ orders.length }}</span><span class="mc-stat-label">订单</span></div>
            <div class="mc-stat-divider"></div>
            <div class="mc-stat"><span class="mc-stat-num">{{ fav.items.length }}</span><span class="mc-stat-label">收藏</span></div>
            <div class="mc-stat-divider"></div>
            <div class="mc-stat"><span class="mc-stat-num">{{ addresses.length }}</span><span class="mc-stat-label">地址</span></div>
          </div>
        </div>
        <nav class="mc-menu">
          <button v-for="m in menus" :key="m.key" class="mc-menu-item"
                  :class="{ active: activeTab === m.key }" @click="activeTab = m.key">
            <span class="mc-menu-icon"><el-icon><component :is="m.icon" /></el-icon></span>
            <span class="mc-menu-label">{{ m.label }}</span>
            <el-badge v-if="m.key === 'cart' && cart.count > 0" :value="cart.count" class="mc-menu-badge" />
            <el-badge v-else-if="m.key === 'notices' && notice.unreadCount > 0" :value="notice.unreadCount" class="mc-menu-badge" />
            <el-icon class="mc-menu-arrow"><ArrowRight /></el-icon>
          </button>
        </nav>
        <button class="mc-logout" @click="onLogout">
          <el-icon><SwitchButton /></el-icon> 退出登录
        </button>
      </aside>

      <!-- 右侧内容 -->
      <main class="mc-content">
        <!-- 我的订单 -->
        <div v-if="activeTab === 'orders'" class="mc-panel">
          <div class="panel-head">
            <h2>我的订单</h2>
            <div class="order-filters">
              <button v-for="f in orderFilters" :key="f.value" class="filter-chip"
                      :class="{ active: orderStatus === f.value }" @click="filterOrders(f.value)">
                {{ f.label }}
              </button>
            </div>
          </div>
          <div v-if="orders.length" class="order-list">
            <div v-for="order in orders" :key="order.id" class="order-card">
              <div class="order-head">
                <span class="order-no">订单号：{{ order.orderNo }}</span>
                <span class="order-status" :class="statusClass(order.status)">{{ order.status }}</span>
              </div>
              <div class="order-body">
                <div class="order-items">
                  <div v-for="item in order.items" :key="item.goodId" class="order-item">
                    <img :src="goodPic(item)" :alt="item.goodName" @error="($event.target.style.visibility='hidden')">
                    <div class="item-info">
                      <p class="item-name">{{ item.goodName }}</p>
                      <p class="item-price">¥{{ item.dealPrice }} × {{ item.count }}</p>
                    </div>
                  </div>
                  <p v-if="!order.items?.length" class="order-empty">暂无商品明细（后端下单明细链路待完善）</p>
                </div>
                <div class="order-summary">
                  <p class="order-total">合计：<b>¥{{ Number(order.totalPay || 0).toFixed(2) }}</b></p>
                  <p class="order-time">下单：{{ order.checkoutTime }}</p>
                  <div class="order-actions">
                    <el-button v-if="order.status === '待付款'" size="small" type="primary" @click="onPay(order)">立即支付</el-button>
                    <el-button v-if="order.status === '待付款'" size="small" @click="onCancel(order)">取消订单</el-button>
                    <el-button v-if="order.status === '待收货'" size="small" type="primary" @click="onConfirm(order)">确认收货</el-button>
                    <el-button v-if="order.status === '已确认'" size="small" @click="onRebuy(order)">再次购买</el-button>
                    <el-button size="small" @click="viewDetail(order)">订单详情</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无订单" :image-size="120" />
        </div>

        <!-- 购物车 -->
        <div v-else-if="activeTab === 'cart'" class="mc-panel">
          <div class="panel-head">
            <h2>购物车</h2>
            <span class="cart-count">共 {{ cart.count }} 件商品</span>
          </div>
          <div v-if="cart.items.length" class="cart-list">
            <div v-for="it in cart.items" :key="it.good.id" class="cart-row">
              <img class="cart-pic" :src="it.good.pic" :alt="it.good.name" @click="goGood(it.good.id)">
              <div class="cart-info">
                <p class="cart-name" @click="goGood(it.good.id)">{{ it.good.name }}</p>
                <p class="cart-price">¥{{ it.good.price }}</p>
              </div>
              <el-input-number v-model="it.qty" :min="1" :max="99" size="small" @change="cart.changeQty(it, it.qty)" />
              <p class="cart-subtotal">¥{{ (it.good.price * it.qty).toFixed(2) }}</p>
              <button class="cart-del" aria-label="移除" @click="cart.remove(it)">
                <el-icon><Delete /></el-icon>
              </button>
            </div>
            <div class="cart-foot">
              <span>合计：<b class="cart-total">¥{{ cart.totalPrice.toFixed(2) }}</b></span>
              <el-button type="primary" round @click="onCheckout">去结算</el-button>
            </div>
          </div>
          <el-empty v-else description="购物车还是空的" :image-size="120">
            <el-button type="primary" round @click="$router.push('/goods')">去逛逛</el-button>
          </el-empty>
        </div>

        <!-- 我的收藏 -->
        <div v-else-if="activeTab === 'favorites'" class="mc-panel">
          <div class="panel-head">
            <h2>我的收藏</h2>
            <span class="cart-count">共 {{ fav.count }} 件商品</span>
          </div>
          <div v-if="fav.items.length" class="cart-list">
            <div v-for="it in fav.items" :key="it.good.id" class="cart-row">
              <img class="cart-pic" :src="goodPic(it.good)" :alt="it.good.name" @click="goGood(it.good.id)"
                   @error="($event.target.style.visibility='hidden')">
              <div class="cart-info">
                <p class="cart-name" @click="goGood(it.good.id)">{{ it.good.name }}</p>
                <p class="cart-price">¥{{ it.good.price }}</p>
              </div>
              <el-button type="primary" size="small" round @click="cart.add(it.good)">加入购物车</el-button>
              <button class="cart-del" aria-label="取消收藏" @click="fav.remove(it)">
                <el-icon><Delete /></el-icon>
              </button>
            </div>
          </div>
          <el-empty v-else description="还没有收藏的商品，点击商品卡片上的星标即可收藏" :image-size="120">
            <el-button type="primary" round @click="$router.push('/goods')">去逛逛</el-button>
          </el-empty>
        </div>

        <!-- 消息中心 -->
        <div v-else-if="activeTab === 'notices'" class="mc-panel">
          <div class="panel-head">
            <h2>消息中心</h2>
            <div class="notice-head-actions">
              <span class="cart-count">未读 {{ notice.unreadCount }} 条</span>
              <el-button v-if="notice.unreadCount > 0" size="small" plain round @click="notice.markAllRead()">全部已读</el-button>
            </div>
          </div>
          <div v-if="notice.items.length" class="cart-list">
            <div v-for="n in notice.items" :key="n.id" class="cart-row notice-row" :class="{ unread: !n.isRead }"
                 @click="notice.markRead(n)">
              <el-tag :type="n.type === 1 ? 'danger' : 'warning'" size="small" effect="light" round class="notice-tag">
                {{ n.type === 1 ? '降价' : '秒杀' }}
              </el-tag>
              <div class="cart-info">
                <p class="notice-title">{{ n.title }}<span v-if="!n.isRead" class="notice-dot"></span></p>
                <p class="notice-content">{{ n.content }}</p>
                <p class="notice-time">{{ n.createdTime }}</p>
              </div>
              <el-button size="small" round plain @click.stop="goGood(n.goodId)">去看看</el-button>
            </div>
          </div>
          <el-empty v-else description="暂无消息，收藏的商品降价或进入秒杀后会在这里提醒你" :image-size="120">
            <el-button type="primary" round @click="$router.push('/goods')">去逛逛</el-button>
          </el-empty>
        </div>

        <!-- 地址管理 -->
        <div v-else-if="activeTab === 'address'" class="mc-panel">
          <div class="panel-head">
            <h2>收货地址</h2>
            <el-button type="primary" round size="small" @click="openAddrDialog()">
              <el-icon><Plus /></el-icon> 新增地址
            </el-button>
          </div>
          <div v-if="addresses.length" class="addr-list">
            <div v-for="addr in addresses" :key="addr.id" class="addr-card" :class="{ default: addr.isDefault }">
              <div class="addr-main">
                <p class="addr-receiver">
                  {{ addr.receiver }} <span class="addr-phone">{{ addr.phone }}</span>
                  <el-tag v-if="addr.isDefault" type="primary" size="small" effect="light" round>默认地址</el-tag>
                </p>
                <p class="addr-detail"><el-icon class="addr-loc-icon"><Location /></el-icon>{{ addrText(addr) }}</p>
              </div>
              <div class="addr-actions">
                <el-button v-if="!addr.isDefault" size="small" type="primary" plain round @click="setDefault(addr.id)"><el-icon><Star /></el-icon>设为默认</el-button>
                <el-button size="small" plain round @click="openAddrDialog(addr)"><el-icon><Edit /></el-icon>编辑</el-button>
                <el-button size="small" type="danger" plain round @click="delAddress(addr.id)"><el-icon><Delete /></el-icon>删除</el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="还没有收货地址" :image-size="120" />
        </div>

        <!-- 个人资料 -->
        <div v-else-if="activeTab === 'profile'" class="mc-panel">
          <div class="panel-head"><h2>个人资料</h2></div>
          <div class="profile-grid">
            <div class="profile-card">
              <div class="pc-icon"><el-icon><User /></el-icon></div>
              <div class="pc-body"><span class="pc-label">账号</span><span class="pc-value">{{ user.info?.account || '-' }}</span></div>
            </div>
            <div class="profile-card">
              <div class="pc-icon"><el-icon><Avatar /></el-icon></div>
              <div class="pc-body"><span class="pc-label">昵称</span><span class="pc-value">{{ user.info?.name || user.info?.account || '-' }}</span></div>
            </div>
            <div class="profile-card">
              <div class="pc-icon"><el-icon><Medal /></el-icon></div>
              <div class="pc-body"><span class="pc-label">会员等级</span><span class="pc-value">黄金会员</span></div>
            </div>
            <div class="profile-card">
              <div class="pc-icon"><el-icon><Calendar /></el-icon></div>
              <div class="pc-body"><span class="pc-label">注册时间</span><span class="pc-value">2026-09-01</span></div>
            </div>
            <div class="profile-card">
              <div class="pc-icon"><el-icon><List /></el-icon></div>
              <div class="pc-body"><span class="pc-label">订单数</span><span class="pc-value">{{ orders.length }} 单</span></div>
            </div>
            <div class="profile-card">
              <div class="pc-icon"><el-icon><Star /></el-icon></div>
              <div class="pc-body"><span class="pc-label">收藏数</span><span class="pc-value">{{ fav.items.length }} 件</span></div>
            </div>
          </div>
          <p class="profile-tip">资料编辑功能开发中，演示环境展示基础信息。</p>
        </div>
      </main>
    </div>

    <!-- 地址编辑弹窗 -->
    <el-dialog v-model="addrDialogVisible" :title="editingAddr?.id ? '编辑地址' : '新增地址'" width="480px" align-center>
      <el-form :model="addrForm" label-width="80px" size="default">
        <el-form-item label="收货人"><el-input v-model="addrForm.receiver" placeholder="请输入收货人姓名" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="addrForm.phone" placeholder="请输入手机号" /></el-form-item>
        <!-- mock 模式：省/市/区文本输入；真实模式：行政区划级联选择（/api/regions/tree） -->
        <template v-if="USE_MOCK">
          <el-form-item label="省份"><el-input v-model="addrForm.province" placeholder="如：北京市" /></el-form-item>
          <el-form-item label="城市"><el-input v-model="addrForm.city" placeholder="如：北京市" /></el-form-item>
          <el-form-item label="区县"><el-input v-model="addrForm.district" placeholder="如：朝阳区" /></el-form-item>
        </template>
        <el-form-item v-else label="所在地区">
          <el-cascader v-model="addrForm.regionIds" :options="regionOptions" :props="regionProps"
                       placeholder="请选择省 / 市 / 区" style="width: 100%" clearable />
        </el-form-item>
        <el-form-item label="详细地址"><el-input v-model="addrForm.addrDetail" type="textarea" :rows="2" placeholder="街道、门牌号等" /></el-form-item>
        <el-form-item label="设为默认"><el-switch v-model="addrForm.isDefault" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addrDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ShoppingCart, List, Location, User, SwitchButton, Delete, Plus, Medal, ArrowRight, Star, Edit, Avatar, Calendar, Bell } from '@element-plus/icons-vue'
import { USE_MOCK } from '../config'
import { useUserStore } from '../stores/user'
import { useCartStore } from '../stores/cart'
import { useFavoriteStore } from '../stores/favorite'
import { useNoticeStore } from '../stores/notice'
import { listMyOrders, cancelOrder, confirmReceive, payOrder } from '../api/order'
import { listAddresses, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '../api/address'
import { getRegionTree } from '../api/region'

const router = useRouter()
const user = useUserStore()
const cart = useCartStore()
const fav = useFavoriteStore()
const notice = useNoticeStore()

const activeTab = ref('orders')
const menus = [
  { key: 'orders', label: '我的订单', icon: List },
  { key: 'cart', label: '购物车', icon: ShoppingCart },
  { key: 'favorites', label: '我的收藏', icon: Star },
  { key: 'notices', label: '消息中心', icon: Bell },
  { key: 'address', label: '地址管理', icon: Location },
  { key: 'profile', label: '个人资料', icon: User },
]

// 订单
const orders = ref([])
const orderStatus = ref('all')
const orderFilters = [
  { label: '全部', value: 'all' },
  { label: '待付款', value: '待付款' },
  { label: '已支付', value: '已支付' },
  { label: '待收货', value: '待收货' },
  { label: '已确认', value: '已确认' },
]

async function loadOrders() {
  // status=all 时后端按字面值过滤会查不到数据，故"全部"不传该参数
  const params = orderStatus.value === 'all' ? {} : { status: orderStatus.value }
  const data = await listMyOrders(params)
  let orderList = data.list || []
  // 补充订单商品图片：订单项可能不含 goodPic 字段，批量从商品详情补全
  const missingPicIds = []
  orderList.forEach((order) => {
    ;(order.items || []).forEach((item) => {
      if (!item.goodPic && !item.pic && !item.image) missingPicIds.push(item.goodId)
    })
  })
  if (missingPicIds.length > 0) {
    try {
      const { getGoodsByIds } = await import('../api/goods')
      const goodsDetail = await getGoodsByIds([...new Set(missingPicIds)])
      const picMap = {}
      ;(goodsDetail || []).forEach((g) => { picMap[g.id] = g.pic || g.goodPic || '' })
      orderList = orderList.map((order) => ({
        ...order,
        items: (order.items || []).map((item) => ({
          ...item,
          goodPic: item.goodPic || picMap[item.goodId] || '',
        })),
      }))
    } catch (e) { /* 补充图片失败不影响订单展示 */ }
  }
  orders.value = orderList
}

function filterOrders(status) {
  orderStatus.value = status
  loadOrders()
}

function statusClass(status) {
  const map = { '待付款': 'warn', '已支付': 'info', '待收货': 'primary', '已确认': 'success', '已取消': 'danger' }
  return map[status] || ''
}

async function onPay(order) {
  await payOrder(order.id)
  ElMessage.success('支付成功')
  loadOrders()
}

async function onCancel(order) {
  await ElMessageBox.confirm('确定取消该订单？', '提示', { type: 'warning' })
  await cancelOrder(order.id)
  ElMessage.success('订单已取消')
  loadOrders()
}

async function onConfirm(order) {
  await ElMessageBox.confirm('确认已收到商品？', '提示', { type: 'info' })
  await confirmReceive(order.id)
  ElMessage.success('已确认收货')
  loadOrders()
}

// 再次购买：真实模式按订单明细重新加购（商品按 goodId 批量回查）
async function onRebuy(order) {
  if (USE_MOCK || !order.items || !order.items.length) {
    ElMessage.info('已加入购物车（演示）')
    activeTab.value = 'cart'
    return
  }
  const { getGoodsByIds } = await import('../api/goods')
  const goods = await getGoodsByIds(order.items.map((i) => i.goodId))
  for (const g of goods) {
    const it = order.items.find((i) => i.goodId === g.id)
    await cart.add(g, it ? it.count : 1)
  }
  activeTab.value = 'cart'
}

function viewDetail(order) {
  ElMessageBox.alert(
    `订单号：${order.orderNo}\n收货人：${order.receiverName || '-'} ${order.receiverPhone || ''}\n地址：${order.receiverAddrDetail || '（未快照）'}\n金额：¥${Number(order.totalPay || 0).toFixed(2)}\n状态：${order.status}`,
    '订单详情',
    { confirmButtonText: '关闭', customClass: 'detail-dialog' }
  )
}

// 地址
const addresses = ref([])
const addrDialogVisible = ref(false)
const editingAddr = ref(null)
const addrForm = ref({ receiver: '', phone: '', province: '', city: '', district: '', addrDetail: '', regionIds: [], isDefault: false })

// 行政区划（真实模式：三级树 + id→名称映射，用于级联选择与地址回显）
const regionOptions = ref([])
const regionMap = ref({})
const regionProps = { value: 'id', label: 'name', children: 'children' }

async function loadRegions() {
  if (USE_MOCK) return
  try {
    const tree = await getRegionTree()
    regionOptions.value = tree || []
    const map = {}
    const walk = (nodes, parentId) => {
      for (const n of nodes || []) {
        map[n.id] = { name: n.name, parentId: n.parentId != null ? n.parentId : parentId }
        if (n.children) walk(n.children, n.id)
      }
    }
    walk(regionOptions.value, null)
    regionMap.value = map
  } catch (e) { /* 区域加载失败不影响其它功能 */ }
}

// 地址展示文本：mock 用省市区文本；真实模式用 addrId 反查区划名称
function addrText(addr) {
  const part = (addr.province || '') + (addr.city || '') + (addr.district || '')
  if (part) return `${part} ${addr.detail || ''}`.trim()
  let cur = addr.addrId != null ? regionMap.value[addr.addrId] : null
  const names = []
  while (cur) {
    names.unshift(cur.name)
    cur = cur.parentId != null ? regionMap.value[cur.parentId] : null
  }
  return `${names.join('')} ${addr.addrDetail || ''}`.trim()
}

// 由 addrId 反推级联路径 [省id, 市id, 区id]
function regionIdsOf(addrId) {
  if (addrId == null) return []
  const ids = []
  let cur = regionMap.value[addrId]
  while (cur) {
    ids.unshift(addrId)
    addrId = cur.parentId
    cur = addrId != null ? regionMap.value[addrId] : null
  }
  return ids
}

async function loadAddresses() {
  addresses.value = await listAddresses()
}

function openAddrDialog(addr) {
  if (addr) {
    editingAddr.value = addr
    addrForm.value = {
      receiver: addr.receiver || '',
      phone: addr.phone || '',
      province: addr.province || '',
      city: addr.city || '',
      district: addr.district || '',
      addrDetail: addr.addrDetail || addr.detail || '',
      regionIds: regionIdsOf(addr.addrId),
      isDefault: !!addr.isDefault,
    }
  } else {
    editingAddr.value = null
    addrForm.value = { receiver: '', phone: '', province: '', city: '', district: '', addrDetail: '', regionIds: [], isDefault: false }
  }
  addrDialogVisible.value = true
}

async function saveAddress() {
  if (!addrForm.value.receiver || !addrForm.value.phone) {
    ElMessage.warning('请填写收货人和手机号')
    return
  }
  const f = addrForm.value
  // mock：省市区文本；真实：addrId（区划末级 id）+ addrDetail
  const payload = USE_MOCK
    ? { receiver: f.receiver, phone: f.phone, province: f.province, city: f.city, district: f.district, detail: f.addrDetail, isDefault: f.isDefault }
    : { receiver: f.receiver, phone: f.phone, addrId: f.regionIds.length ? f.regionIds[f.regionIds.length - 1] : undefined, addrDetail: f.addrDetail, isDefault: f.isDefault }
  if (!USE_MOCK && !payload.addrId) {
    ElMessage.warning('请选择所在地区')
    return
  }
  if (editingAddr.value) {
    await updateAddress(editingAddr.value.id, payload)
    ElMessage.success('地址已更新')
  } else {
    await addAddress(payload)
    ElMessage.success('地址已添加')
  }
  addrDialogVisible.value = false
  loadAddresses()
}

async function delAddress(id) {
  await ElMessageBox.confirm('确定删除该地址？', '提示', { type: 'warning' })
  await deleteAddress(id)
  ElMessage.success('已删除')
  loadAddresses()
}

async function setDefault(id) {
  await setDefaultAddress(id)
  loadAddresses()
}

// 购物车
function goGood(id) {
  router.push(`/goods/${id}`)
}

// 商品图片容错：收藏接口返回的 good 对象可能 pic 字段为空或字段名不同
function goodPic(good) {
  if (!good) return ''
  return good.pic || good.goodPic || good.image || good.cover || good.picUrl || ''
}

// 去结算：真实模式提交订单（POST /api/orders {cartIds, addrId}）
async function onCheckout() {
  if (!user.requireLogin()) return
  const addresses = await listAddresses()
  if (!addresses || !addresses.length) {
    ElMessage.warning('请先在地址管理中添加上送收货地址')
    return
  }
  const addr = addresses.find((a) => a.isDefault) || addresses[0]
  const order = await cart.checkout(addr.id)
  if (order) ElMessage.success(`订单提交成功：${order.orderNo}`)
}

function onLogout() {
  user.logout()
  router.push('/')
}

onMounted(() => {
  loadOrders()
  loadAddresses()
  loadRegions()
  if (user.isLoggedIn) {
    cart.load()
    fav.load()
    notice.load()
  }
})
</script>

<style scoped>
.member-center { padding: 28px 0 64px; min-height: 60vh; }
.mc-layout { display: grid; grid-template-columns: 260px 1fr; gap: 24px; align-items: start; }

.mc-sidebar {
  background: var(--surface); border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm); overflow: hidden;
  position: sticky; top: calc(var(--nav-h) + 16px);
}

/* 用户卡：渐变横幅 */
.mc-user-card {
  background: linear-gradient(135deg, #1e3a8a 0%, #2f6bff 55%, #5a9eff 100%);
  padding: 24px 20px 18px; color: #fff;
}
.mc-avatar {
  width: 60px; height: 60px; border-radius: 50%;
  display: grid; place-items: center;
  background: rgba(255,255,255,.22); backdrop-filter: blur(4px);
  border: 2px solid rgba(255,255,255,.5);
  color: #fff; font-size: 24px; font-weight: 800; flex-shrink: 0;
  margin-bottom: 12px;
}
.mc-user-name { font-size: 18px; font-weight: 800; line-height: 1.3; }
.mc-user-level {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 12px; margin-top: 4px;
  background: rgba(255,215,0,.18); color: #ffd700;
  padding: 2px 10px; border-radius: 10px; font-weight: 600;
}
.mc-user-stats {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 16px; padding-top: 14px;
  border-top: 1px solid rgba(255,255,255,.18);
}
.mc-stat { display: flex; flex-direction: column; align-items: center; flex: 1; }
.mc-stat-num { font-size: 18px; font-weight: 800; }
.mc-stat-label { font-size: 11px; opacity: .8; margin-top: 2px; }
.mc-stat-divider { width: 1px; height: 24px; background: rgba(255,255,255,.2); }

/* 菜单：现代侧边栏风格 */
.mc-menu { display: flex; flex-direction: column; gap: 2px; padding: 12px 12px; }
.mc-menu-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; border-radius: 12px;
  font-size: 14.5px; font-weight: 600; color: var(--ink-2);
  transition: all .25s ease; text-align: left; width: 100%;
  position: relative; border: 1px solid transparent;
}
.mc-menu-item:hover { background: var(--bg); color: var(--ink); }
.mc-menu-item.active {
  background: linear-gradient(135deg, rgba(47,107,255,.1), rgba(47,107,255,.04));
  color: var(--brand); border-color: rgba(47,107,255,.18);
}
.mc-menu-item.active::before {
  content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%);
  width: 3px; height: 20px; border-radius: 0 3px 3px 0;
  background: linear-gradient(180deg, var(--brand-deep), var(--brand));
}
.mc-menu-icon { font-size: 19px; display: grid; place-items: center; width: 22px; }
.mc-menu-label { flex: 1; }
.mc-menu-arrow { font-size: 14px; opacity: .35; transition: transform .25s; }
.mc-menu-item:hover .mc-menu-arrow, .mc-menu-item.active .mc-menu-arrow { opacity: .8; transform: translateX(2px); }
.mc-menu-badge { margin-right: 2px; }

.mc-logout {
  display: flex; align-items: center; gap: 8px; justify-content: center;
  margin: 4px 12px 16px; padding: 11px;
  border-radius: 12px; font-size: 14px; font-weight: 600;
  color: var(--price); background: rgba(255,77,79,.06);
  border: 1px solid rgba(255,77,79,.15);
  transition: all .25s ease;
}
.mc-logout:hover { background: rgba(255,77,79,.12); border-color: rgba(255,77,79,.3); }

.mc-content { min-width: 0; }
.mc-panel {
  background: var(--surface); border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm); padding: 24px 28px;
}
.panel-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid var(--line); }
.panel-head h2 { font-size: 20px; font-weight: 800; }

/* 订单 */
.order-filters { display: flex; gap: 8px; }
.filter-chip {
  padding: 6px 14px; border-radius: var(--radius-pill);
  font-size: 13px; font-weight: 600; color: var(--ink-2);
  background: var(--bg); transition: all .3s ease;
}
.filter-chip.active { background: var(--brand); color: #fff; }

.order-list { display: flex; flex-direction: column; gap: 16px; }
.order-card { border: 1px solid var(--line); border-radius: 14px; overflow: hidden; transition: box-shadow .3s ease; }
.order-card:hover { box-shadow: var(--shadow-sm); }
.order-head {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 18px; background: var(--bg);
  font-size: 13px;
}
.order-no { color: var(--ink-2); font-family: monospace; }
.order-status { font-weight: 700; }
.order-status.warn { color: var(--accent); }
.order-status.info { color: var(--brand); }
.order-status.primary { color: var(--brand-deep); }
.order-status.success { color: var(--ok); }
.order-status.danger { color: var(--price); }

.order-body { display: flex; padding: 16px 18px; gap: 20px; }
.order-items { flex: 1; display: flex; flex-direction: column; gap: 12px; }
.order-item { display: flex; gap: 12px; align-items: center; }
.order-item img { width: 60px; height: 60px; border-radius: 10px; object-fit: cover; }
.item-name { font-size: 14px; font-weight: 600; }
.item-price { font-size: 13px; color: var(--ink-2); margin-top: 2px; }
.order-empty { font-size: 13px; color: var(--ink-2); padding: 8px 0; }

.order-summary { width: 180px; flex-shrink: 0; display: flex; flex-direction: column; align-items: flex-end; gap: 6px; border-left: 1px dashed var(--line); padding-left: 20px; }
.order-total { font-size: 14px; color: var(--ink-2); }
.order-total b { font-size: 20px; color: var(--price); }
.order-time { font-size: 12px; color: var(--ink-2); }
.order-actions { display: flex; flex-wrap: wrap; gap: 6px; justify-content: flex-end; margin-top: 4px; }

/* 购物车 */
.cart-count { font-size: 13px; color: var(--ink-2); }
.cart-list { display: flex; flex-direction: column; gap: 12px; }
.cart-row {
  display: flex; align-items: center; gap: 16px;
  padding: 14px; border: 1px solid var(--line); border-radius: 14px;
  transition: box-shadow .3s ease;
}
.cart-row:hover { box-shadow: var(--shadow-sm); }
.cart-pic { width: 72px; height: 72px; border-radius: 12px; object-fit: cover; cursor: pointer; }
.cart-info { flex: 1; min-width: 0; }
.cart-name { font-size: 15px; font-weight: 600; cursor: pointer; }
.cart-name:hover { color: var(--brand); }
.cart-price { font-size: 16px; font-weight: 800; color: var(--price); margin-top: 4px; }
.cart-subtotal { width: 90px; text-align: right; font-size: 16px; font-weight: 700; color: var(--price); }
.cart-del {
  width: 36px; height: 36px; border-radius: 10px;
  display: grid; place-items: center; color: var(--ink-2);
  transition: all .3s ease;
}
.cart-del:hover { color: var(--price); background: rgba(255,77,79,.08); }
.cart-foot {
  display: flex; align-items: center; justify-content: flex-end; gap: 20px;
  padding-top: 16px; border-top: 1px dashed var(--line); margin-top: 8px;
  font-size: 14px; color: var(--ink-2);
}
.cart-total { font-size: 24px; color: var(--price); font-weight: 800; }

/* 消息中心 */
.notice-head-actions { display: flex; align-items: center; gap: 12px; }
.notice-row { cursor: pointer; align-items: flex-start; }
.notice-row.unread { border-color: rgba(47,107,255,.25); background: linear-gradient(135deg, rgba(47,107,255,.04), rgba(47,107,255,.01)); }
.notice-tag { flex-shrink: 0; margin-top: 3px; }
.notice-title { font-size: 15px; font-weight: 600; display: flex; align-items: center; }
.notice-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: var(--price); margin-left: 8px; }
.notice-content { font-size: 13.5px; color: var(--ink-2); margin-top: 4px; line-height: 1.65; }
.notice-time { font-size: 12px; color: var(--ink-2); opacity: .75; margin-top: 6px; }

/* 地址 */
.addr-list { display: flex; flex-direction: column; gap: 14px; }
.addr-card {
  display: flex; justify-content: space-between; align-items: center;
  padding: 18px 22px; border: 1px solid var(--line); border-radius: 14px;
  transition: all .25s ease; position: relative; overflow: hidden;
}
.addr-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,.06); border-color: #d8e0ee; }
.addr-card.default {
  border-color: rgba(47,107,255,.35);
  background: linear-gradient(135deg, rgba(47,107,255,.05), rgba(47,107,255,.01));
}
.addr-card.default::before {
  content: ''; position: absolute; left: 0; top: 0; bottom: 0;
  width: 4px; background: linear-gradient(180deg, var(--brand-deep), var(--brand));
}
.addr-receiver { font-size: 16px; font-weight: 700; display: flex; align-items: center; gap: 10px; }
.addr-phone { font-size: 13.5px; color: var(--ink-2); font-weight: 400; }
.addr-detail { font-size: 13.5px; color: var(--ink-2); margin-top: 8px; display: flex; align-items: center; gap: 6px; }
.addr-loc-icon { color: var(--brand); font-size: 15px; }
.addr-actions { display: flex; gap: 8px; flex-shrink: 0; margin-left: 16px; }

/* 个人资料：卡片式 */
.profile-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.profile-card {
  display: flex; align-items: center; gap: 14px;
  padding: 18px 20px; border: 1px solid var(--line); border-radius: 14px;
  transition: all .25s ease; background: var(--surface);
}
.profile-card:hover { box-shadow: 0 4px 14px rgba(0,0,0,.06); transform: translateY(-2px); border-color: #d8e0ee; }
.pc-icon {
  width: 44px; height: 44px; border-radius: 12px; flex-shrink: 0;
  display: grid; place-items: center; font-size: 20px;
  background: linear-gradient(135deg, rgba(47,107,255,.12), rgba(47,107,255,.04));
  color: var(--brand);
}
.pc-body { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.pc-label { font-size: 12.5px; color: var(--ink-2); }
.pc-value { font-size: 15.5px; font-weight: 700; color: var(--ink); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.profile-tip { margin-top: 24px; font-size: 13px; color: var(--ink-2); text-align: center; padding: 12px; background: var(--bg); border-radius: 10px; }

@media (max-width: 1100px) {
  .mc-layout { grid-template-columns: 1fr; gap: 16px; }
  .mc-sidebar {
    position: static;
    display: flex;
    flex-direction: column;
    gap: 0;
    padding: 0;
    overflow: hidden;
  }
  /* 用户卡：全宽横幅 */
  .mc-user-card {
    width: 100%;
    margin-right: 0;
    padding: 20px 16px 16px;
  }
  .mc-avatar { width: 52px; height: 52px; font-size: 20px; margin-bottom: 10px; }
  .mc-user-name { font-size: 16px; }
  /* 菜单：横向滚动标签栏 */
  .mc-menu {
    flex-direction: row;
    flex-wrap: nowrap;
    overflow-x: auto;
    padding: 8px 12px;
    gap: 6px;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
    position: relative;
  }
  .mc-menu::-webkit-scrollbar { display: none; }
  .mc-menu-item {
    padding: 6px 12px;
    font-size: 12.5px;
    flex-shrink: 0;
    white-space: nowrap;
    width: auto;
    border-radius: 20px;
    background: var(--bg);
    color: var(--ink-2);
    transition: all .2s ease;
  }
  .mc-menu-item.active {
    background: var(--brand);
    color: #fff;
    font-weight: 600;
  }
  .mc-menu-item.active::before { display: none; }
  .mc-menu-arrow { display: none; }
  .mc-logout {
    width: calc(100% - 24px);
    margin: 0 12px 12px;
    padding: 10px;
  }
  .mc-panel { padding: 16px; }
  .panel-head { flex-wrap: wrap; gap: 10px; }
  .panel-head h2 { font-size: 18px; }
  .order-filters { overflow-x: auto; flex-wrap: nowrap; width: 100%; }
  .filter-chip { flex-shrink: 0; }
  .order-body { flex-direction: column; gap: 12px; }
  .order-summary { width: 100%; align-items: flex-start; border-left: none; border-top: 1px dashed var(--line); padding-left: 0; padding-top: 12px; }
  .order-actions { justify-content: flex-start; }
  .profile-grid { grid-template-columns: 1fr; }
  /* 购物车行：手机端紧凑布局 */
  .cart-row { flex-wrap: wrap; gap: 10px; }
  .cart-pic { width: 56px; height: 56px; }
  .cart-info { min-width: 0; flex: 1; }
  .cart-subtotal { width: auto; }
  .cart-foot { flex-wrap: wrap; justify-content: space-between; }
  /* 地址卡：手机端纵向 */
  .addr-card { flex-direction: column; align-items: flex-start; gap: 10px; }
  .addr-actions { margin-left: 0; width: 100%; justify-content: flex-start; flex-wrap: wrap; }
}
</style>
