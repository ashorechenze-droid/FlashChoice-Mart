<template>
  <div class="seckill">
    <div class="container">
      <!-- 页头 -->
      <div class="seckill-head reveal">
        <span class="head-icon" aria-hidden="true"><el-icon><Lightning /></el-icon></span>
        <h2>秒杀会场</h2>
        <p>限时好价，数量有限</p>
      </div>

      <!-- 活动切换 -->
      <div class="seckill-tabs no-scrollbar reveal">
        <button v-for="s in seckills" :key="s.id" class="sk-tab"
                :class="{ active: activeId === s.id }" @click="activeId = s.id">
          {{ s.name }}
          <em :class="statusOf(s).cls">{{ statusOf(s).label }}</em>
        </button>
      </div>

      <!-- 倒计时 + 商品 -->
      <template v-if="current">
        <div class="countdown reveal">
          <span class="cd-label">{{ statusOf(current).label }}倒计时</span>
          <span class="cd-time">{{ countdown }}</span>
        </div>

        <div class="sk-grid">
          <div v-for="(it, i) in items" :key="it.id" class="sk-card reveal"
               :style="{ transitionDelay: (i % 4) * 0.06 + 's' }">
            <img v-if="it.good" :src="it.good.pic" :alt="it.good.name" loading="lazy">
            <div class="sk-body">
              <h3>{{ it.good?.name || `商品 ${it.goodId}` }}</h3>
              <div class="sk-price">
                <span class="now">秒杀价 ¥{{ it.seckillPrice }}</span>
                <span class="mark" v-if="it.good">¥{{ it.good.price }}</span>
              </div>
              <div class="sk-progress">
                <el-progress :percentage="Math.round((it.sold || 0) / Math.max(1, (it.sold || 0) + (it.stock || 0)) * 100)" :show-text="false"
                             color="#FF4D4F" :stroke-width="8" />
                <span class="sk-stock">已抢 {{ it.sold }} / 剩余 {{ it.stock }}</span>
                <span class="sk-stock">每人限购 {{ it.limitPerUser || 1 }} 件</span>
              </div>
              <el-button class="sk-grab" size="large" round :disabled="!isRunning || it.stock <= 0 || grabbing"
                         @click="onGrab(it)">
                {{ grabbing ? '提交中' : it.stock <= 0 ? '已售罄' : isRunning ? '立即秒杀' : '未开始' }}
              </el-button>
            </div>
          </div>
        </div>
      </template>
      <p v-if="!seckills.length" class="sk-empty">暂无可参与的秒杀活动</p>
    </div>
    <el-dialog v-model="addressDialog" title="选择收货地址" width="520px" align-center>
      <p v-if="!addresses.length">请先在个人中心添加收货地址。</p>
      <el-radio-group v-else v-model="selectedAddress" class="sk-addresses">
        <el-radio v-for="address in addresses" :key="address.id" :value="address.id">
          {{ address.receiver }} · {{ address.phone }} · {{ address.addrDetail }}
        </el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="addressDialog = false">取消</el-button>
        <el-button v-if="!addresses.length" type="primary" @click="goToAddresses">去添加地址</el-button>
        <el-button v-else type="primary" :loading="grabbing" :disabled="!selectedAddress" @click="submitGrab">确认抢购</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { listSeckills, getSeckillGoods, grabSeckill, getGrabResult } from '../api/seckill'
import { listAddresses } from '../api/address'
import { useUserStore } from '../stores/user'
import { initReveal } from '../utils/reveal'

const user = useUserStore()
const router = useRouter()

const seckills = ref([])
const items = ref([])
const activeId = ref(null)
const now = ref(Date.now())
const grabbing = ref(false)
const addressDialog = ref(false)
const addresses = ref([])
const selectedAddress = ref(null)
const pendingGood = ref(null)

const current = computed(() => seckills.value.find((s) => s.id === activeId.value))
const isRunning = computed(() => {
  const s = current.value
  return s && now.value >= new Date(s.startTime).getTime() && now.value < new Date(s.endTime).getTime()
})

// 活动状态：未开始 / 进行中 / 已结束
function statusOf(s) {
  const start = new Date(s.startTime).getTime()
  const end = new Date(s.endTime).getTime()
  if (now.value < start) return { label: '未开始', cls: 'st-up' }
  if (now.value >= end) return { label: '已结束', cls: 'st-end' }
  return { label: '进行中', cls: 'st-run' }
}

// 倒计时文本（进行中→距结束；未开始→距开始）
const countdown = computed(() => {
  const s = current.value
  if (!s) return '--:--:--'
  const start = new Date(s.startTime).getTime()
  const end = new Date(s.endTime).getTime()
  const target = now.value < start ? start : end
  const diff = Math.max(0, target - now.value)
  const h = String(Math.floor(diff / 3600000)).padStart(2, '0')
  const m = String(Math.floor((diff % 3600000) / 60000)).padStart(2, '0')
  const sec = String(Math.floor((diff % 60000) / 1000)).padStart(2, '0')
  return `${h}:${m}:${sec}`
})

async function load() {
  seckills.value = await listSeckills()
  if (seckills.value.length) activeId.value = seckills.value[0].id
}

async function loadItems() {
  if (!activeId.value) {
    items.value = []
    return
  }
  items.value = await getSeckillGoods(activeId.value)
  requestAnimationFrame(() => initReveal())
}

async function onGrab(it) {
  if (!user.requireLogin()) return
  if (grabbing.value) return
  pendingGood.value = it
  try {
    addresses.value = await listAddresses()
    selectedAddress.value = addresses.value.find((a) => a.isDefault)?.id || addresses.value[0]?.id || null
    addressDialog.value = true
  } catch (e) {
    // The request interceptor displays the API error.
  }
}

function goToAddresses() {
  addressDialog.value = false
  router.push('/member')
}

async function submitGrab() {
  if (!pendingGood.value || !selectedAddress.value || grabbing.value) return
  grabbing.value = true
  try {
    const res = await grabSeckill({ seckillGoodId: pendingGood.value.id, addrId: selectedAddress.value })
    addressDialog.value = false
    ElMessage.info('抢购已提交，正在创建订单…')
    for (let attempt = 0; attempt < 15; attempt++) {
      await new Promise((resolve) => setTimeout(resolve, 1500))
      const result = await getGrabResult(res.orderNo)
      if (result.status === '成功') {
        ElMessageBox.confirm(`抢购成功，订单号：${result.actualOrderNo}。请在 30 分钟内支付。`, '秒杀成功',
          { confirmButtonText: '查看我的订单', cancelButtonText: '稍后查看' })
          .then(() => router.push('/member')).catch(() => {})
        await loadItems()
        return
      }
      if (result.status === '失败') {
        ElMessage.error('抢购未成功，名额已释放')
        await loadItems()
        return
      }
    }
    ElMessage.info('订单仍在处理中，请稍后到我的订单查看')
  } catch (e) {
    // The request interceptor displays the API error.
  } finally {
    grabbing.value = false
  }
}

// 倒计时每秒刷新
let timer = null
onMounted(async () => {
  await load()
  await loadItems()
  timer = setInterval(() => { now.value = Date.now() }, 1000)
})
onBeforeUnmount(() => clearInterval(timer))

// 切换活动时重新加载商品
watch(activeId, () => activeId.value && loadItems())
</script>

<style scoped>
.seckill { padding: 36px 0 64px; }

.seckill-head { text-align: center; margin-bottom: 30px; }
.head-icon {
  display: inline-grid; place-items: center;
  width: 64px; height: 64px; border-radius: 20px;
  background: linear-gradient(135deg, #F04E23, #FF6B35);
  color: #fff; font-size: 34px;
  box-shadow: 0 14px 32px rgba(240, 78, 35, .35);
  margin-bottom: 14px;
}
.seckill-head h2 { font-size: 32px; font-weight: 900; }
.seckill-head p { margin-top: 6px; font-size: 14px; color: var(--ink-2); }

.seckill-tabs { display: flex; gap: 12px; overflow-x: auto; padding-bottom: 8px; margin-bottom: 24px; }
.sk-tab {
  min-height: 46px;
  padding: 0 22px;
  border-radius: var(--radius-pill);
  background: var(--surface);
  border: 1px solid var(--line);
  font-size: 15px; font-weight: 600; color: var(--ink-2);
  display: inline-flex; align-items: center; gap: 10px;
  white-space: nowrap;
  transition: all .3s ease;
}
.sk-tab em { font-style: normal; font-size: 12px; padding: 2px 8px; border-radius: var(--radius-pill); }
.sk-tab .st-run { background: rgba(255, 77, 79, .12); color: var(--price); }
.sk-tab .st-up { background: var(--brand-soft); color: var(--brand-deep); }
.sk-tab .st-end { background: var(--bg); color: var(--ink-2); }
.sk-tab.active {
  color: #fff;
  background: linear-gradient(135deg, #F04E23, #FF6B35);
  border-color: transparent;
  box-shadow: 0 8px 20px rgba(240, 78, 35, .32);
}
.sk-tab.active em { background: rgba(255, 255, 255, .25); color: #fff; }

.countdown {
  display: flex; align-items: center; justify-content: center; gap: 14px;
  margin-bottom: 26px;
  background: var(--surface);
  border: 1px dashed rgba(255, 77, 79, .4);
  border-radius: var(--radius-pill);
  padding: 12px 26px;
  width: max-content; margin-inline: auto;
}
.cd-label { font-size: 14px; font-weight: 600; color: var(--ink-2); }
.cd-time {
  font-size: 24px; font-weight: 900; color: var(--price);
  font-variant-numeric: tabular-nums; letter-spacing: 2px;
}

.sk-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}
.sk-card {
  background: var(--surface);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition: transform .5s var(--elastic), box-shadow .45s ease;
}
.sk-card:hover { transform: translateY(-5px); box-shadow: var(--shadow-md); }
.sk-card img { width: 100%; aspect-ratio: 1 / 1; object-fit: cover; }
.sk-body { padding: 16px; display: flex; flex-direction: column; gap: 10px; }
.sk-body h3 { font-size: 17px; font-weight: 600; }
.sk-price { display: flex; align-items: baseline; gap: 10px; }
.now { font-size: 24px; font-weight: 900; color: var(--price); font-variant-numeric: tabular-nums; }
.mark { font-size: 14px; color: var(--ink-2); text-decoration: line-through; }
.sk-progress { display: flex; flex-direction: column; gap: 4px; }
.sk-stock { font-size: 12px; color: var(--ink-2); }
.sk-grab {
  background: linear-gradient(135deg, #F04E23, #FF6B35);
  border: none; color: #fff; font-weight: 700;
}
.sk-grab.is-disabled { background: var(--ink-2); }
.sk-empty { text-align: center; padding: 60px 0; color: var(--ink-2); }
.sk-addresses { display: flex; flex-direction: column; align-items: flex-start; gap: 12px; }

@media (max-width: 768px) {
  .sk-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .sk-body { padding: 12px; }
  .now { font-size: 18px; }
  .sk-body h3 { font-size: 15px; }
  .countdown { width: 100%; }
}
</style>
