<template>
  <div class="admin-page">
    <!-- ========== 未登录：管理员登录页（左右分栏） ========== -->
    <div v-if="!admin.isLoggedIn" class="admin-login-page">
      <!-- 左侧品牌展示区 -->
      <div class="login-brand-side">
        <div class="brand-content">
          <div class="brand-logo">
            <svg viewBox="0 0 24 24" width="40" height="40" fill="currentColor"><path d="M13 2 4.5 13.5H11L9.5 22 19 9.5h-6.5L13 2z"/></svg>
          </div>
          <h1 class="brand-name">极速优选</h1>
          <p class="brand-slogan">B2C 秒杀商城 · 管理平台</p>
          <div class="brand-features">
            <div class="brand-feature">
              <div class="bf-icon"><el-icon><DataAnalysis /></el-icon></div>
              <div class="bf-text"><p class="bf-title">实时数据概览</p><p class="bf-desc">销售趋势与经营指标一目了然</p></div>
            </div>
            <div class="brand-feature">
              <div class="bf-icon"><el-icon><Goods /></el-icon></div>
              <div class="bf-text"><p class="bf-title">商品全生命周期</p><p class="bf-desc">上下架、库存、品牌统一管理</p></div>
            </div>
            <div class="brand-feature">
              <div class="bf-icon"><el-icon><List /></el-icon></div>
              <div class="bf-text"><p class="bf-title">订单与会员运营</p><p class="bf-desc">发货履约、会员画像精细化</p></div>
            </div>
          </div>
        </div>
        <div class="brand-decor decor-1"></div>
        <div class="brand-decor decor-2"></div>
        <div class="brand-decor decor-3"></div>
      </div>

      <!-- 右侧登录表单区 -->
      <div class="login-form-side">
        <button class="back-mall-btn" @click="$router.push('/')">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回商城</span>
        </button>
        <div class="login-form-wrap">
          <h2 class="login-title">管理员登录</h2>
          <p class="login-subtitle">请使用管理员账号登录系统</p>
          <el-form :model="loginForm" size="large" @submit.prevent class="login-form">
            <el-form-item>
              <el-input v-model="loginForm.username" placeholder="管理员账号" :prefix-icon="User" clearable />
            </el-form-item>
            <el-form-item>
              <el-input v-model="loginForm.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password @keyup.enter="onAdminLogin" />
            </el-form-item>
            <el-form-item class="admin-captcha-row">
              <el-input v-model="loginForm.captcha" placeholder="验证码" :prefix-icon="Key"
                        maxlength="5" clearable @keyup.enter="onAdminLogin" />
              <img v-if="adminCaptchaImg" :src="adminCaptchaImg" class="admin-captcha-img"
                   alt="验证码" title="点击刷新" @click="refreshAdminCaptcha" />
              <div v-else class="admin-captcha-img admin-captcha-placeholder" @click="refreshAdminCaptcha">
                <span class="captcha-spinner"></span>
              </div>            </el-form-item>
            <el-button type="primary" size="large" class="login-submit" :loading="loginLoading" @click="onAdminLogin">登 录</el-button>
          </el-form>
          <p class="login-demo-tip">演示账号：admin / 123456</p>
        </div>
      </div>
    </div>

    <!-- ========== 已登录：管理后台布局 ========== -->
    <div v-else class="admin-layout">
      <!-- 左侧深色侧边栏 -->
      <aside class="admin-sidebar" :class="{ collapsed: sidebarCollapsed }">
        <div class="sidebar-head">
          <div class="sidebar-logo-icon">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M13 2 4.5 13.5H11L9.5 22 19 9.5h-6.5L13 2z"/></svg>
          </div>
          <span v-if="!sidebarCollapsed" class="sidebar-logo-text">极速优选</span>
        </div>
        <nav class="sidebar-nav">
          <button v-for="m in adminMenus" :key="m.key" class="sidebar-nav-item"
                  :class="{ active: activeModule === m.key }" @click="activeModule = m.key" :title="m.label">
            <el-icon class="sni-icon"><component :is="m.icon" /></el-icon>
            <span v-if="!sidebarCollapsed" class="sni-label">{{ m.label }}</span>
            <span v-if="!sidebarCollapsed && activeModule === m.key" class="sni-indicator"></span>
          </button>
        </nav>
        <div class="sidebar-foot">
          <div class="sidebar-user">
            <div class="su-avatar">{{ admin.info?.username?.slice(0,1) || 'A' }}</div>
            <div v-if="!sidebarCollapsed" class="su-info">
              <p class="su-name">{{ admin.info?.username }}</p>
              <p class="su-role">超级管理员</p>
            </div>
          </div>
          <button class="su-logout" @click="onLogout" title="退出登录">
            <el-icon><SwitchButton /></el-icon>
          </button>
        </div>
      </aside>

      <!-- 右侧主区域 -->
      <div class="admin-main">
        <!-- 顶部白色栏 -->
        <header class="admin-topbar">
          <div class="topbar-left">
            <button class="topbar-collapse" @click="sidebarCollapsed = !sidebarCollapsed">
              <el-icon><Fold v-if="!sidebarCollapsed" /><Expand v-else /></el-icon>
            </button>
            <h3 class="topbar-title">{{ currentModuleLabel }}</h3>
          </div>
          <div class="topbar-right">
            <button class="topbar-action" title="返回商城" @click="$router.push('/')">
              <el-icon><ShoppingCart /></el-icon>
            </button>
            <el-dropdown trigger="click">
              <div class="topbar-user">
                <div class="tu-avatar">{{ admin.info?.username?.slice(0,1) || 'A' }}</div>
                <span class="tu-name">{{ admin.info?.username }}</span>
                <el-icon class="tu-arrow"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="onLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </header>

        <!-- 内容区 -->
        <main class="admin-content">
          <!-- ========== 数据概览 ========== -->
          <div v-if="activeModule === 'dashboard'" class="dashboard-view">
            <div class="stat-grid">
              <div class="stat-card stat-primary">
                <div class="sc-icon-wrap"><el-icon><Money /></el-icon></div>
                <div class="sc-body">
                  <p class="sc-num">¥{{ Number(stats.totalSales || 0).toLocaleString() }}</p>
                  <p class="sc-label">总销售额</p>
                </div>
              </div>
              <div class="stat-card stat-blue">
                <div class="sc-icon-wrap"><el-icon><List /></el-icon></div>
                <div class="sc-body">
                  <p class="sc-num">{{ stats.orderCount || 0 }}</p>
                  <p class="sc-label">订单总数</p>
                </div>
              </div>
              <div class="stat-card stat-orange">
                <div class="sc-icon-wrap"><el-icon><Van /></el-icon></div>
                <div class="sc-body">
                  <p class="sc-num">{{ stats.pendingShip || 0 }}</p>
                  <p class="sc-label">待发货</p>
                </div>
              </div>
              <div class="stat-card stat-green">
                <div class="sc-icon-wrap"><el-icon><Goods /></el-icon></div>
                <div class="sc-body">
                  <p class="sc-num">{{ stats.goodsCount || goods.length }}</p>
                  <p class="sc-label">商品总数</p>
                </div>
              </div>
              <div class="stat-card stat-purple">
                <div class="sc-icon-wrap"><el-icon><UserFilled /></el-icon></div>
                <div class="sc-body">
                  <p class="sc-num">{{ stats.memberCount || members.length }}</p>
                  <p class="sc-label">会员数量</p>
                </div>
              </div>
              <div class="stat-card stat-cyan">
                <div class="sc-icon-wrap"><el-icon><Shop /></el-icon></div>
                <div class="sc-body">
                  <p class="sc-num">{{ stats.brandCount || brands.length }}</p>
                  <p class="sc-label">品牌数量</p>
                </div>
              </div>
            </div>

            <!-- 月销售趋势：全宽 -->
            <div class="panel-card trend-card">
              <div class="panel-head">
                <h4 class="panel-title">月销售趋势</h4>
                <span class="panel-sub">近 30 天销售额（元）</span>
              </div>
              <div ref="monthTrendRef" class="trend-chart"></div>
            </div>

            <div class="panel-row">
              <!-- 分类销售占比饼图 -->
              <div class="panel-card">
                <div class="panel-head"><h4 class="panel-title">分类销售占比</h4></div>
                <div class="pie-wrap">
                  <svg class="pie-chart" viewBox="0 0 200 200">
                    <circle cx="100" cy="100" r="70" fill="none" stroke="#f0f2f7" stroke-width="28"/>
                    <circle v-for="(seg, i) in pieSegments" :key="'pie'+i" cx="100" cy="100" r="70" fill="none"
                            :stroke="seg.color" stroke-width="28"
                            :stroke-dasharray="seg.dash" :stroke-dashoffset="seg.offset"
                            transform="rotate(-90 100 100)" stroke-linecap="butt"/>
                    <text x="100" y="92" text-anchor="middle" font-size="16" font-weight="800" fill="#333">¥{{ Number(stats.totalSales || 0).toLocaleString() }}</text>
                    <text x="100" y="112" text-anchor="middle" font-size="11" fill="#999">总销售额</text>
                  </svg>
                  <div class="pie-legend">
                    <div v-for="(seg, i) in pieSegments" :key="'lg'+i" class="legend-item">
                      <span class="legend-dot" :style="{ background: seg.color }"></span>
                      <span class="legend-label">{{ seg.name }}</span>
                      <span class="legend-value">{{ seg.percentage }}%</span>
                    </div>
                  </div>
                </div>
              </div>
              <!-- 商品分类占比条形图 -->
              <div class="panel-card">
                <div class="panel-head"><h4 class="panel-title">商品分类占比</h4></div>
                <div class="cat-chart">
                  <div v-for="(c, i) in categoryStats" :key="i" class="cat-row">
                    <span class="cat-name">{{ c.name }}</span>
                    <div class="cat-bar-wrap"><div class="cat-bar" :style="{ width: (c.count / maxCat * 100) + '%', opacity: 1 - i * 0.12 }"></div></div>
                    <span class="cat-count">{{ c.count }}件</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="panel-row">
              <!-- 热销排行 -->
              <div class="panel-card">
                <div class="panel-head"><h4 class="panel-title">热销商品 TOP5</h4></div>
                <div class="hot-list">
                  <div v-for="(g, i) in hotGoods" :key="g.id" class="hot-item">
                    <span class="hot-rank" :class="'rank-' + (i+1)">{{ i+1 }}</span>
                    <img :src="g.pic" :alt="g.name" class="hot-img">
                    <span class="hot-name">{{ g.name }}</span>
                    <span class="hot-sales">销量{{ g.sales || 0 }}</span>
                    <span class="hot-price">¥{{ g.price }}</span>
                  </div>
                </div>
              </div>
              <!-- 最近订单 -->
              <div class="panel-card">
                <div class="panel-head"><h4 class="panel-title">最近订单</h4></div>
                <el-table :data="orders.slice(0,5)" stripe size="small" class="admin-table">
                <el-table-column prop="orderNo" label="订单号" width="200" />
                <el-table-column prop="status" label="状态" width="100" />
                <el-table-column prop="totalPay" label="金额" width="100" />
                <el-table-column prop="receiverName" label="收货人" width="100" />
                <el-table-column prop="checkoutTime" label="下单时间" />
              </el-table>
              </div>
            </div>
          </div>

          <!-- ========== 商品管理 ========== -->
          <div v-else-if="activeModule === 'goods'" class="module-view">
            <div class="panel-card">
              <div class="panel-head">
                <h4 class="panel-title">商品管理</h4>
                <el-button type="primary" round @click="openGoodsEdit(null)"><el-icon><Plus /></el-icon> 新增商品</el-button>
              </div>
              <el-table :data="goods" stripe size="small" max-height="560" v-loading="goodsLoading" class="admin-table">
                <el-table-column prop="id" label="ID" width="60" />
                <el-table-column label="商品图" width="70"><template #default="{ row }"><img :src="row.pic" class="tbl-img"></template></el-table-column>
                <el-table-column prop="name" label="商品名称" min-width="160" />
                <el-table-column prop="price" label="价格" width="90" />
                <el-table-column prop="qty" label="库存" width="70" />
                <el-table-column label="状态" width="80">
                  <template #default="{ row }"><el-tag :type="row.isHot ? 'danger' : 'info'" size="small" effect="light">{{ row.isHot ? '热销' : '普通' }}</el-tag></template>
                </el-table-column>
                <el-table-column label="操作" width="170">
                  <template #default="{ row }">
                    <el-button size="small" type="primary" plain round @click="openGoodsEdit(row)">编辑</el-button>
                    <el-button size="small" :type="row.isTakeDown ? 'success' : 'warning'" plain round @click="onToggleGoods(row)">{{ row.isTakeDown ? '上架' : '下架' }}</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>

          <!-- ========== 订单管理 ========== -->
          <div v-else-if="activeModule === 'orders'" class="module-view">
            <div class="panel-card">
              <div class="panel-head"><h4 class="panel-title">订单管理</h4></div>
              <el-table :data="orders" stripe size="small" max-height="560" v-loading="ordersLoading" class="admin-table" style="width: 100%">
                <el-table-column prop="orderNo" label="订单号" width="160" />
                <el-table-column label="商品" min-width="180">
                  <template #default="{ row }">
                    <div class="order-goods-cell">
                      <div v-for="item in (row.items || []).slice(0, 2)" :key="item.goodId" class="og-good">
                        <img v-if="item.goodPic || item.pic" :src="item.goodPic || item.pic" class="og-img" @error="($event.target.style.visibility='hidden')">
                        <span class="og-name">{{ item.goodName }}</span>
                      </div>
                      <span v-if="(row.items || []).length > 2" class="og-more">+{{ (row.items || []).length - 2 }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="80" />
                <el-table-column label="金额" width="80">
                  <template #default="{ row }">¥{{ Number(row.totalPay || 0).toFixed(2) }}</template>
                </el-table-column>
                <el-table-column prop="receiverName" label="收货人" width="80" />
                <el-table-column prop="checkoutTime" label="下单时间" width="150" />
                <el-table-column label="操作" width="180" fixed="right">
                  <template #default="{ row }">
                    <el-button v-if="row.status === '已支付'" size="small" type="success" plain round @click="onShipOrder(row)">发货</el-button>
                    <el-button size="small" type="primary" plain round @click="viewOrder(row)">详情</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>

          <!-- ========== 品牌管理 ========== -->
          <div v-else-if="activeModule === 'brands'" class="module-view">
            <div class="panel-card">
              <div class="panel-head">
                <h4 class="panel-title">品牌管理</h4>
                <el-button type="primary" round @click="openBrandEdit(null)"><el-icon><Plus /></el-icon> 新增品牌</el-button>
              </div>
              <el-table :data="brands" stripe size="small" v-loading="brandsLoading" class="admin-table">
                <el-table-column prop="id" label="ID" width="60" />
                <el-table-column label="Logo" width="70"><template #default="{ row }">
                  <div class="brand-logo-cell">
                    <img v-if="!row._logoErr" :src="row.logo" class="tbl-img" @error="row._logoErr = true">
                    <span v-else class="logo-fallback">{{ (row.name || '?').slice(0, 1) }}</span>
                  </div>
                </template></el-table-column>
                <el-table-column prop="name" label="品牌名称" width="150" />
                <el-table-column prop="company" label="公司" min-width="180" />
                <el-table-column prop="description" label="简介" min-width="180" />
                <el-table-column label="操作" width="170">
                  <template #default="{ row }">
                    <el-button size="small" type="primary" plain round @click="openBrandEdit(row)">编辑</el-button>
                    <el-button size="small" type="danger" plain round @click="onDeleteBrand(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>

          <!-- ========== 会员管理 ========== -->
          <div v-else-if="activeModule === 'members'" class="module-view">
            <div class="panel-card">
              <div class="panel-head"><h4 class="panel-title">会员管理</h4></div>
              <el-table :data="members" stripe size="small" max-height="500" v-loading="membersLoading" class="admin-table">
                <el-table-column prop="id" label="ID" width="60" />
                <el-table-column prop="account" label="账号" width="120" />
                <el-table-column prop="name" label="姓名" width="100" />
                <el-table-column prop="phone" label="手机号" width="140" />
                <el-table-column prop="email" label="邮箱" min-width="160" />
                <el-table-column label="订单数" width="80"><template #default="{ row }">{{ getMemberOrderCount(row) }}</template></el-table-column>
                <el-table-column label="消费总额" width="100"><template #default="{ row }">¥{{ getMemberTotal(row) }}</template></el-table-column>
                <el-table-column label="状态" width="80">
                  <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'danger'" size="small" effect="light">{{ row.enabled ? '正常' : '禁用' }}</el-tag></template>
                </el-table-column>
                <el-table-column label="操作" width="160">
                  <template #default="{ row }">
                    <el-button size="small" type="primary" plain round @click="viewMember(row)">详情</el-button>
                    <el-button size="small" :type="row.enabled ? 'danger' : 'success'" plain round @click="onToggleMember(row)">{{ row.enabled ? '禁用' : '启用' }}</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>

          <!-- ========== 秒杀管理 ========== -->
          <div v-else-if="activeModule === 'seckill'" class="module-view">
            <div class="panel-card">
              <div class="panel-head">
                <h4 class="panel-title">秒杀活动管理</h4>
                <el-button type="primary" round @click="openSeckillEdit(null)"><el-icon><Plus /></el-icon> 新增活动</el-button>
              </div>
              <el-table :data="seckills" stripe size="small" max-height="500" class="admin-table">
                <el-table-column prop="id" label="ID" width="60" />
                <el-table-column prop="name" label="活动名称" min-width="140" />
                <el-table-column label="状态" width="80">
                  <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'" size="small" effect="light">{{ row.enabled ? '启用' : '停用' }}</el-tag></template>
                </el-table-column>
                <el-table-column prop="startTime" label="开始时间" width="170" />
                <el-table-column prop="endTime" label="结束时间" width="170" />
                <el-table-column label="商品数" width="80"><template #default="{ row }">{{ row.goodCount || 0 }}</template></el-table-column>
                <el-table-column prop="description" label="描述" min-width="160" />
                <el-table-column label="操作" width="240">
                  <template #default="{ row }">
                    <el-button size="small" type="primary" plain round @click="openSeckillEdit(row)">编辑</el-button>
                    <el-button size="small" type="success" plain round @click="viewSeckillGoods(row)">商品</el-button>
                    <el-button size="small" type="danger" plain round @click="onDeleteSeckill(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </main>
      </div>
    </div>

    <!-- ===== 商品编辑弹窗 ===== -->
    <el-dialog v-model="goodsDialogVisible" :title="goodsForm.id ? '编辑商品' : '新增商品'" width="560px" align-center class="admin-dialog">
      <el-form :model="goodsForm" label-width="90px" size="default">
        <el-form-item label="商品名称"><el-input v-model="goodsForm.name" /></el-form-item>
        <el-form-item label="商品图片"><el-input v-model="goodsForm.pic" placeholder="图片URL" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="goodsForm.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="划线价"><el-input-number v-model="goodsForm.markPrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="goodsForm.qty" :min="0" /></el-form-item>
        <el-form-item label="是否热销">
          <el-switch v-model="goodsForm.isHot" active-text="热销" inactive-text="普通" />
        </el-form-item>
        <el-form-item label="商品描述"><el-input v-model="goodsForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="goodsDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveGoods">保存</el-button>
      </template>
    </el-dialog>

    <!-- ===== 品牌编辑弹窗 ===== -->
    <el-dialog v-model="brandDialogVisible" :title="brandForm.id ? '编辑品牌' : '新增品牌'" width="500px" align-center class="admin-dialog">
      <el-form :model="brandForm" label-width="80px">
        <el-form-item label="品牌名称"><el-input v-model="brandForm.name" /></el-form-item>
        <el-form-item label="Logo"><el-input v-model="brandForm.logo" placeholder="Logo URL" /></el-form-item>
        <el-form-item label="公司"><el-input v-model="brandForm.company" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="brandForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="brandDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBrand">保存</el-button>
      </template>
    </el-dialog>

    <!-- ===== 秒杀活动编辑弹窗 ===== -->
    <el-dialog v-model="seckillDialogVisible" :title="seckillForm.id ? '编辑秒杀活动' : '新增秒杀活动'" width="520px" align-center class="admin-dialog">
      <el-form :model="seckillForm" label-width="90px">
        <el-form-item label="活动名称"><el-input v-model="seckillForm.name" /></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="seckillForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="seckillForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
        <el-form-item label="是否启用"><el-switch v-model="seckillForm.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="活动描述"><el-input v-model="seckillForm.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="seckillDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSeckill">保存</el-button>
      </template>
    </el-dialog>

    <!-- ===== 秒杀商品管理弹窗 ===== -->
    <el-dialog v-model="seckillGoodsDialogVisible" :title="`秒杀商品管理 - ${currentSeckill?.name || ''}`" width="820px" align-center class="admin-dialog">
      <el-table :data="seckillGoodsList" stripe size="small" max-height="340" class="admin-table">
        <el-table-column label="商品" min-width="220">
          <template #default="{ row }">
            <div class="sg-good-cell">
              <img :src="row.goodPic || getGoodPic(row.goodId)" class="sg-good-img" @error="($event.target.style.display='none')">
              <div class="sg-good-info">
                <span class="sg-good-name">{{ row.goodName || getGoodName(row.goodId) }}</span>
                <span class="sg-good-price">原价 ¥{{ row.originalPrice || getGoodPrice(row.goodId) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="seckillPrice" label="秒杀价" width="100">
          <template #default="{ row }"><span class="sg-seckill-price">¥{{ row.seckillPrice }}</span></template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="70" />
        <el-table-column prop="sold" label="已售" width="70" />
        <el-table-column prop="limitPerUser" label="限购" width="70" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }"><el-button size="small" type="danger" link @click="onRemoveSeckillGood(row)">移除</el-button></template>
        </el-table-column>
      </el-table>
      <div class="sg-add-section">
        <div class="sg-add-title">添加秒杀商品</div>
        <div class="sg-add-row">
          <el-select
            v-model="newSeckillGood.goodId"
            filterable
            placeholder="搜索选择商品"
            class="sg-good-select"
            @change="onSelectGood"
          >
            <el-option
              v-for="g in availableGoods"
              :key="g.id"
              :label="g.name"
              :value="g.id"
            >
              <div class="sg-option">
                <img :src="g.pic" class="sg-option-img" @error="($event.target.style.display='none')">
                <span class="sg-option-name">{{ g.name }}</span>
                <span class="sg-option-price">¥{{ g.price }}</span>
              </div>
            </el-option>
          </el-select>
          <div class="sg-input-group">
            <label class="sg-input-label">秒杀价</label>
            <el-input-number v-model="newSeckillGood.seckillPrice" :min="0" :precision="2" :controls="false" placeholder="秒杀价" class="sg-num-input" />
          </div>
          <div class="sg-input-group">
            <label class="sg-input-label">库存</label>
            <el-input-number v-model="newSeckillGood.stock" :min="0" :controls="false" placeholder="库存" class="sg-num-input" />
          </div>
          <div class="sg-input-group">
            <label class="sg-input-label">限购</label>
            <el-input-number v-model="newSeckillGood.limitPerUser" :min="1" :controls="false" placeholder="限购" class="sg-num-input" />
          </div>
          <el-button type="primary" class="sg-add-btn" @click="onAddSeckillGood">添加</el-button>
        </div>
        <div v-if="selectedGoodPreview" class="sg-preview">
          <img :src="selectedGoodPreview.pic" class="sg-preview-img" @error="($event.target.style.display='none')">
          <div class="sg-preview-info">
            <span class="sg-preview-name">{{ selectedGoodPreview.name }}</span>
            <span class="sg-preview-meta">原价 ¥{{ selectedGoodPreview.price }} · 库存 {{ selectedGoodPreview.qty }}</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- ===== 会员详情弹窗 ===== -->
    <el-dialog v-model="memberDialogVisible" title="会员详情" width="640px" align-center class="admin-dialog">
      <div v-if="currentMember" class="member-detail">
        <div class="member-detail-head">
          <div class="member-avatar">{{ currentMember.name?.slice(0,1) || 'U' }}</div>
          <div class="member-detail-info">
            <h4>{{ currentMember.name }}</h4>
            <p>账号：{{ currentMember.account }}</p>
            <p>{{ currentMember.phone }} · {{ currentMember.email }}</p>
          </div>
          <div class="member-stats">
            <div class="ms-item"><b>{{ getMemberOrderCount(currentMember) }}</b><span>订单数</span></div>
            <div class="ms-item"><b>¥{{ getMemberTotal(currentMember) }}</b><span>消费总额</span></div>
          </div>
        </div>
        <div v-if="memberOrderStats.length" class="member-order-stats">
          <p class="mos-title">订单状态分布</p>
          <div class="mos-bar">
            <div v-for="s in memberOrderStats" :key="s.name" class="mos-seg" :style="{ width: s.pct + '%', background: s.color }" :title="`${s.name}: ${s.count}单`"></div>
          </div>
          <div class="mos-labels">
            <span v-for="s in memberOrderStats" :key="s.name" class="mos-label">
              <i class="mos-dot" :style="{ background: s.color }"></i>{{ s.name }} {{ s.count }}单
            </span>
          </div>
        </div>
        <el-tabs v-model="memberTab" class="member-tabs">
          <el-tab-pane label="基本信息" name="info">
            <div class="info-grid">
              <div class="info-row"><span class="info-label">会员ID</span><span>{{ currentMember.id }}</span></div>
              <div class="info-row"><span class="info-label">账号</span><span>{{ currentMember.account }}</span></div>
              <div class="info-row"><span class="info-label">姓名</span><span>{{ currentMember.name }}</span></div>
              <div class="info-row"><span class="info-label">手机号</span><span>{{ currentMember.phone }}</span></div>
              <div class="info-row"><span class="info-label">邮箱</span><span>{{ currentMember.email }}</span></div>
              <div class="info-row"><span class="info-label">性别</span><span>{{ currentMember.sex || '男' }}</span></div>
              <div class="info-row"><span class="info-label">注册时间</span><span>{{ currentMember.createdTime || '-' }}</span></div>
              <div class="info-row"><span class="info-label">状态</span><el-tag :type="currentMember.enabled ? 'success' : 'danger'" size="small" effect="light">{{ currentMember.enabled ? '正常' : '禁用' }}</el-tag></div>
            </div>
          </el-tab-pane>
          <el-tab-pane :label="`订单记录 (${currentMemberOrders.length})`" name="orders">
            <div v-if="currentMemberOrders.length" class="member-orders">
              <div v-for="o in currentMemberOrders" :key="o.id" class="mo-card">
                <div class="mo-head"><span class="mo-no">{{ o.orderNo }}</span><el-tag size="small" effect="light">{{ o.status }}</el-tag></div>
                <div class="mo-items"><span v-for="item in o.items" :key="item.goodId" class="mo-item">{{ item.goodName }} × {{ item.count }}</span></div>
                <div class="mo-foot"><span>¥{{ Number(o.totalPay || 0).toFixed(2) }}</span><span class="mo-time">{{ o.checkoutTime }}</span></div>
              </div>
            </div>
            <el-empty v-else description="暂无订单" :image-size="80" />
          </el-tab-pane>
          <el-tab-pane label="收货地址" name="address">
            <div v-if="memberAddresses.length" class="member-addrs">
              <div v-for="a in memberAddresses" :key="a.id" class="ma-card">
                <p class="ma-receiver">{{ a.receiver }} <span class="ma-phone">{{ a.phone }}</span><el-tag v-if="a.isDefault" type="primary" size="small" effect="dark">默认</el-tag></p>
                <p class="ma-detail">{{ addrText(a) }}</p>
              </div>
            </div>
            <el-empty v-else description="暂无地址" :image-size="80" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

    <!-- ===== 订单详情弹窗 ===== -->
    <el-dialog v-model="orderDialogVisible" title="订单详情" width="560px" align-center class="admin-dialog">
      <div v-if="currentOrder" class="order-detail">
        <div class="od-row"><span class="od-label">订单号</span><span class="od-value mono">{{ currentOrder.orderNo }}</span></div>
        <div class="od-row"><span class="od-label">状态</span><el-tag size="small" effect="light">{{ currentOrder.status }}</el-tag></div>
        <div class="od-row"><span class="od-label">金额</span><span class="od-value price">¥{{ Number(currentOrder.totalPay || 0).toFixed(2) }}</span></div>
        <div class="od-row"><span class="od-label">收货人</span><span>{{ currentOrder.receiverName }} {{ currentOrder.receiverPhone }}</span></div>
        <div class="od-row"><span class="od-label">收货地址</span><span>{{ currentOrder.receiverAddrDetail }}</span></div>
        <div class="od-row"><span class="od-label">下单时间</span><span>{{ currentOrder.checkoutTime }}</span></div>
        <div class="od-row" v-if="currentOrder.payTime"><span class="od-label">支付时间</span><span>{{ currentOrder.payTime }}</span></div>
        <div class="od-items">
          <p class="od-items-title">商品明细</p>
          <div v-for="item in currentOrder.items" :key="item.goodId" class="od-item">
            <img :src="item.goodPic || item.pic || item.image || ''" :alt="item.goodName" @error="($event.target.style.visibility='hidden')"><span>{{ item.goodName }}</span><span class="od-item-price">¥{{ item.dealPrice }} × {{ item.count }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Lock, Key, Fold, Expand, DataAnalysis, Goods, List, Shop, UserFilled, Money, Van, Timer, ArrowLeft, ArrowDown, ShoppingCart, SwitchButton, Plus } from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])
import { useUserStore } from '../stores/user'
import { listAllGoods, getGoodsByIds } from '../api/goods'
import { getBrands } from '../api/brand'
import { getCategories } from '../api/category'
import { listAllOrders, getOrderDetail } from '../api/order'
import { listAddresses } from '../api/address'
import {
  adminLogin, getAdminCaptcha, listMembers, getMemberOrders, shipOrder as apiShipOrder,
  updateMemberEnabled, updateGoodsStatus, saveGoods as apiSaveGoods,
  saveBrand as apiSaveBrand, deleteBrand as apiDeleteBrand,
  listSeckills, createSeckill, updateSeckill, deleteSeckill,
  listSeckillGoods, addSeckillGood, removeSeckillGood,
} from '../api/admin'

// 管理员登录态
const ADMIN_KEY = 'mall_admin'
const admin = ref({ isLoggedIn: !!localStorage.getItem(ADMIN_KEY), info: JSON.parse(localStorage.getItem(ADMIN_KEY) || 'null') })
const userStore = useUserStore()
const loginForm = ref({ username: '', password: '', captcha: '', captchaId: '' })
const loginLoading = ref(false)
const adminCaptchaImg = ref('')

// 加载管理端图形验证码
async function refreshAdminCaptcha() {
  try {
    const data = await getAdminCaptcha()
    loginForm.value.captchaId = data.captchaId
    adminCaptchaImg.value = data.imageBase64
    loginForm.value.captcha = ''
  } catch (e) { /* 验证码加载失败不阻断 */ }
}
const sidebarCollapsed = ref(false)

const adminMenus = [
  { key: 'dashboard', label: '数据概览', icon: DataAnalysis },
  { key: 'goods', label: '商品管理', icon: Goods },
  { key: 'orders', label: '订单管理', icon: List },
  { key: 'brands', label: '品牌管理', icon: Shop },
  { key: 'members', label: '会员管理', icon: UserFilled },
  { key: 'seckill', label: '秒杀管理', icon: Timer },
]
const activeModule = ref('dashboard')
const currentModuleLabel = computed(() => adminMenus.find((m) => m.key === activeModule.value)?.label || '数据概览')

// 数据
const goods = ref([])
const brands = ref([])
const orders = ref([])
const members = ref([])
const addresses = ref([])
const categories = ref([])
const stats = ref({})
const salesTrend = ref([])
const categoryStats = ref([])
const categorySales = ref([])
const seckills = ref([])
const seckillGoodsList = ref([])

const goodsLoading = ref(false)
const ordersLoading = ref(false)
const brandsLoading = ref(false)
const membersLoading = ref(false)

// 弹窗
const goodsDialogVisible = ref(false)
const goodsForm = ref({})
const brandDialogVisible = ref(false)
const brandForm = ref({})
const seckillDialogVisible = ref(false)
const seckillForm = ref({})
const seckillGoodsDialogVisible = ref(false)
const currentSeckill = ref(null)
const newSeckillGood = ref({ goodId: '', seckillPrice: 0, stock: 0, limitPerUser: 1 })
const selectedGoodPreview = ref(null)

// 可选商品列表（过滤已添加到当前秒杀活动的商品）
const availableGoods = computed(() => {
  const addedIds = new Set(seckillGoodsList.value.map((g) => g.goodId))
  return goods.value.filter((g) => !addedIds.has(g.id) && !g.isTakeDown)
})

function getGoodPic(goodId) {
  const g = goods.value.find((x) => x.id === goodId)
  return g?.pic || ''
}
function getGoodName(goodId) {
  const g = goods.value.find((x) => x.id === goodId)
  return g?.name || `商品${goodId}`
}
function getGoodPrice(goodId) {
  const g = goods.value.find((x) => x.id === goodId)
  return g?.price || '-'
}

function onSelectGood(goodId) {
  const g = goods.value.find((x) => x.id === goodId)
  if (g) {
    selectedGoodPreview.value = g
    if (!newSeckillGood.value.seckillPrice) {
      newSeckillGood.value.seckillPrice = Number((g.price * 0.8).toFixed(2))
    }
    if (!newSeckillGood.value.stock) {
      newSeckillGood.value.stock = g.qty || 10
    }
  } else {
    selectedGoodPreview.value = null
  }
}
const memberDialogVisible = ref(false)
const currentMember = ref(null)
const memberTab = ref('info')
const memberAddresses = ref([])
const currentMemberOrders = ref([])
const orderDialogVisible = ref(false)
const currentOrder = ref(null)

// ===== 图表计算 =====
const maxCat = computed(() => Math.max(...categoryStats.value.map((c) => c.count), 1))

// 月销售趋势（ECharts 曲线图）
const monthTrendRef = ref(null)
let monthChart = null
function renderMonthTrend() {
  if (!monthTrendRef.value) return
  if (monthChart && monthChart.getDom() !== monthTrendRef.value) {
    monthChart.dispose()
    monthChart = null
  }
  if (!monthChart) monthChart = echarts.init(monthTrendRef.value)
  monthChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15,23,42,.92)',
      borderWidth: 0,
      textStyle: { color: '#fff', fontSize: 13 },
      formatter: (params) => {
        const p = params[0]
        return `${p.name}<br/><b>¥${Number(p.value).toLocaleString()}</b>`
      },
    },
    grid: { left: 64, right: 24, top: 28, bottom: 34 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: salesTrend.value.map((d) => d.day),
      axisLine: { lineStyle: { color: '#e4e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#8a94a8', fontSize: 12, interval: 4 },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#eef1f6' } },
      axisLabel: { color: '#8a94a8', fontSize: 12, formatter: (v) => (v >= 10000 ? (v / 10000).toFixed(1) + '万' : v) },
    },
    series: [{
      name: '销售额',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      showSymbol: false,
      data: salesTrend.value.map((d) => d.value),
      lineStyle: { width: 3, color: '#2f6bff' },
      itemStyle: { color: '#2f6bff', borderColor: '#fff', borderWidth: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(47,107,255,.28)' },
          { offset: 1, color: 'rgba(47,107,255,.02)' },
        ]),
      },
    }],
  })
}
watch(salesTrend, () => renderMonthTrend())
watch(activeModule, (val) => {
  if (val === 'dashboard') {
    nextTick(() => renderMonthTrend())
  }
})
function onWindowResize() { if (monthChart) monthChart.resize() }
onMounted(() => {
  window.addEventListener('resize', onWindowResize)
  if (admin.value.isLoggedIn) loadAll()
})
onUnmounted(() => {
  window.removeEventListener('resize', onWindowResize)
  if (monthChart) { monthChart.dispose(); monthChart = null }
})

// 分类销售占比饼图
const pieSegments = computed(() => {
  if (!categorySales.value.length) return []
  const total = categorySales.value.reduce((s, c) => s + c.amount, 0) || 1
  const colors = ['#2f6bff', '#00b4d8', '#06d6a0', '#7209b7', '#ffb703', '#adb5bd']
  let offset = 0
  return categorySales.value.map((c, i) => {
    const pct = c.amount / total
    const seg = { name: c.name, count: c.amount, color: colors[i % colors.length], dash: `${pct * 439.8} 439.8`, offset: -offset * 439.8, percentage: c.percentage }
    offset += pct
    return seg
  })
})

const hotGoods = computed(() => {
  return [...goods.value].map((g, i) => ({ ...g, sales: g.isHot ? 100 - i * 15 : 50 - i * 8 })).sort((a, b) => b.sales - a.sales).slice(0, 5)
})

// 会员订单统计
function memberOrdersOf(row) {
  const acc = row?.account
  if (!acc) return []
  return orders.value.filter((o) => o.memberAccount === acc)
}
function getMemberOrderCount(row) {
  return memberOrdersOf(row).length
}
function getMemberTotal(row) {
  return memberOrdersOf(row).reduce((s, o) => s + (Number(o.totalPay) || 0), 0).toFixed(2)
}

function addrText(a) {
  const part = (a.province || '') + (a.city || '') + (a.district || '')
  if (part) return `${part} ${a.detail || ''}`.trim()
  return `${a.addrDetail || ''}`.trim()
}

const memberOrderStats = computed(() => {
  const map = {}
  currentMemberOrders.value.forEach((o) => { map[o.status] = (map[o.status] || 0) + 1 })
  const total = currentMemberOrders.value.length || 1
  const colors = { '已支付': '#2f6bff', '待收货': '#00c48c', '已确认': '#a55eea', '待付款': '#ff9f43', '已取消': '#999' }
  return Object.entries(map).map(([name, count]) => ({
    name, count, color: colors[name] || '#2f6bff', pct: (count / total * 100).toFixed(0),
  }))
})

// ===== 数据加载 =====
async function loadAll() {
  goodsLoading.value = ordersLoading.value = brandsLoading.value = membersLoading.value = true
  const safe = (p, fallback) => p.catch((e) => { console.warn('加载失败:', e?.message || e); return fallback })
  try {
    const [g, b, o, m, c, sk] = await Promise.all([
      safe(listAllGoods({ pageSize: 100 }), { list: [] }),
      safe(getBrands(), []),
      safe(listAllOrders(), { list: [] }),
      safe(listMembers(), { list: [] }),
      safe(getCategories(), []),
      safe(listSeckills(), []),
    ])
    goods.value = g.list || g || []
    brands.value = b || []
    orders.value = o.list || o || []
    members.value = m.list || m || []
    categories.value = c || []
    seckills.value = sk || []
    await computeSeckillGoodCounts()
    computeStats()
  } catch (e) { console.error('管理端数据加载失败', e) }
  finally { goodsLoading.value = ordersLoading.value = brandsLoading.value = membersLoading.value = false }
}

async function computeSeckillGoodCounts() {
  try {
    const results = await Promise.all(seckills.value.map(async (s) => {
      try { const list = await listSeckillGoods(s.id); return { id: s.id, count: (list || []).length } }
      catch (e) { return { id: s.id, count: 0 } }
    }))
    const map = Object.fromEntries(results.map((r) => [r.id, r.count]))
    seckills.value = seckills.value.map((s) => ({ ...s, goodCount: map[s.id] || 0 }))
  } catch (e) { /* 忽略 */ }
}

function computeStats() {
  const valid = orders.value.filter((o) => o.status !== '已取消')
  stats.value = {
    totalSales: valid.reduce((s, o) => s + (Number(o.totalPay) || 0), 0),
    orderCount: orders.value.length,
    pendingShip: orders.value.filter((o) => o.status === '已支付').length,
    goodsCount: goods.value.length,
    memberCount: members.value.length,
    brandCount: brands.value.length,
  }
  const byDay = {}
  valid.forEach((o) => {
    if (!o.checkoutTime) return
    const day = o.checkoutTime.slice(5, 10)
    byDay[day] = (byDay[day] || 0) + (Number(o.totalPay) || 0)
  })
  const days = []
  for (let i = 29; i >= 0; i--) {
    const d = new Date(Date.now() - i * 86400000)
    const key = `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    days.push({ day: key, value: byDay[key] || 0 })
  }
  salesTrend.value = days
  const catMap = Object.fromEntries(categories.value.map((x) => [x.id, x.name]))
  const catCount = {}
  goods.value.forEach((gd) => {
    if (gd.categoryId == null) return
    const name = catMap[gd.categoryId] || `分类${gd.categoryId}`
    catCount[name] = (catCount[name] || 0) + 1
  })
  categoryStats.value = Object.entries(catCount).map(([name, count]) => ({ name, count }))
  const goodMap = Object.fromEntries(goods.value.map((gd) => [gd.id, gd]))
  const catSales = {}
  valid.forEach((o) => {
    ;(o.items || []).forEach((it) => {
      const gd = goodMap[it.goodId]
      if (!gd || gd.categoryId == null) return
      const name = catMap[gd.categoryId] || `分类${gd.categoryId}`
      catSales[name] = (catSales[name] || 0) + (Number(it.dealPrice) || 0) * (it.count || 1)
    })
  })
  const totalAmt = Object.values(catSales).reduce((s, v) => s + v, 0) || 1
  categorySales.value = Object.entries(catSales).map(([name, amount]) => ({
    name, amount, percentage: Number((amount / totalAmt * 100).toFixed(1)),
  }))
}

// ===== 登录 =====
async function onAdminLogin() {
  if (!loginForm.value.username || !loginForm.value.password) { ElMessage.warning('请输入账号和密码'); return }
  if (!loginForm.value.captcha) { ElMessage.warning('请输入验证码'); return }
  loginLoading.value = true
  try {
    const res = await adminLogin(loginForm.value)
    // 管理端令牌独立存储（mall_admin），不写入会员令牌槽位；同时清空会员会话（同一时刻仅一种身份）
    userStore.token = ''
    userStore.refreshToken = ''
    userStore.info = null
    localStorage.removeItem('mall_token')
    localStorage.removeItem('mall_refresh_token')
    localStorage.removeItem('mall_user')
    admin.value.isLoggedIn = true
    admin.value.info = { id: res.id, username: res.username, name: res.name, role: res.role, token: res.token || '', refreshToken: res.refreshToken || '' }
    localStorage.setItem(ADMIN_KEY, JSON.stringify(admin.value.info))
    ElMessage.success('登录成功')
    loadAll()
  } catch (e) {
    ElMessage.error(e.msg || '账号或密码错误')
    refreshAdminCaptcha()
  }
  finally { loginLoading.value = false }
}
function onLogout() {
  admin.value.isLoggedIn = false; admin.value.info = null
  localStorage.removeItem(ADMIN_KEY)
  userStore.token = ''
  userStore.refreshToken = ''
  userStore.info = null
  localStorage.removeItem('mall_token')
  localStorage.removeItem('mall_refresh_token')
  localStorage.removeItem('mall_user')
  ElMessage.success('已退出管理端')
  // 退出后跳转到首页，统一登录入口
  router.push('/')
}

// ===== 商品编辑 =====
function openGoodsEdit(row) {
  goodsForm.value = row ? { ...row } : { name: '', pic: '', price: 0, markPrice: 0, qty: 0, isHot: false, description: '' }
  goodsDialogVisible.value = true
}
async function saveGoods() {
  if (!goodsForm.value.name) { ElMessage.warning('请输入商品名称'); return }
  try {
    await apiSaveGoods(goodsForm.value)
    ElMessage.success(goodsForm.value.id ? '商品已更新' : '商品已新增')
    goodsDialogVisible.value = false
    const g = await listAllGoods({ pageSize: 100 })
    goods.value = g.list || g || []
    computeStats()
  } catch (e) { /* 拦截器已提示 */ }
}
async function onToggleGoods(row) {
  try {
    await updateGoodsStatus(row.id, !row.isTakeDown)
    row.isTakeDown = row.isTakeDown ? 0 : 1
    ElMessage.success(row.isTakeDown ? '已下架' : '已上架')
  } catch (e) { /* 拦截器已提示 */ }
}

// ===== 品牌编辑 =====
function openBrandEdit(row) {
  brandForm.value = row ? { ...row } : { name: '', logo: '', company: '', description: '' }
  brandDialogVisible.value = true
}
async function saveBrand() {
  if (!brandForm.value.name) { ElMessage.warning('请输入品牌名称'); return }
  try {
    await apiSaveBrand(brandForm.value)
    ElMessage.success(brandForm.value.id ? '品牌已更新' : '品牌已新增')
    brandDialogVisible.value = false
    brands.value = await getBrands()
    computeStats()
  } catch (e) { /* 拦截器已提示 */ }
}
async function onDeleteBrand(row) {
  try {
    await ElMessageBox.confirm(`确定删除品牌「${row.name}」吗？`, '提示', { type: 'warning' })
    await apiDeleteBrand(row.id)
    ElMessage.success('已删除')
    brands.value = await getBrands()
    computeStats()
  } catch (e) { /* 取消 */ }
}

// ===== 会员启停 =====
async function onToggleMember(row) {
  try {
    const enabled = !row.enabled
    await updateMemberEnabled(row.id, enabled)
    row.enabled = enabled ? 1 : 0
    ElMessage.success(enabled ? '已启用' : '已禁用')
  } catch (e) { /* 拦截器已提示 */ }
}

// ===== 订单发货 =====
async function onShipOrder(row) {
  try {
    await apiShipOrder(row.id)
    row.status = '待收货'
    row.shipTime = new Date().toISOString().slice(0, 19).replace('T', ' ')
    ElMessage.success(`订单 ${row.orderNo} 已发货`)
    computeStats()
  } catch (e) { /* 拦截器已提示 */ }
}

// ===== 会员详情 =====
async function viewMember(row) {
  currentMember.value = row
  memberTab.value = 'info'
  memberAddresses.value = []
  currentMemberOrders.value = []
  memberDialogVisible.value = true
  try { currentMemberOrders.value = await getMemberOrders(row.account) || [] } catch (e) { currentMemberOrders.value = [] }
  try { memberAddresses.value = await listAddresses({ account: row.account }) || [] } catch (e) { memberAddresses.value = [] }
}
async function viewOrder(row) {
  currentOrder.value = row
  orderDialogVisible.value = true
  // 列表行数据可能不含明细，异步拉取订单详情（含 orderItems）增强弹窗内容
  try {
    const detail = await getOrderDetail(row.id)
    // 补充订单商品图片：订单项可能不含 goodPic 字段，批量从商品详情补全
    const items = detail.items || []
    const missingPicIds = items.filter((item) => !item.goodPic && !item.pic && !item.image).map((item) => item.goodId)
    if (missingPicIds.length > 0) {
      try {
        const goodsDetail = await getGoodsByIds([...new Set(missingPicIds)])
        const picMap = {}
        ;(goodsDetail || []).forEach((g) => { picMap[g.id] = g.pic || g.goodPic || '' })
        detail.items = items.map((item) => ({
          ...item,
          goodPic: item.goodPic || picMap[item.goodId] || '',
        }))
      } catch (e) { /* 补充图片失败不影响订单详情展示 */ }
    }
    currentOrder.value = detail
  } catch (e) { /* 拉取失败降级用行数据展示 */ }
}

// ===== 秒杀活动 =====
function openSeckillEdit(row) {
  seckillForm.value = row ? { ...row } : { name: '', startTime: '', endTime: '', enabled: 1, description: '' }
  seckillDialogVisible.value = true
}
async function saveSeckill() {
  if (!seckillForm.value.name) { ElMessage.warning('请输入活动名称'); return }
  try {
    if (seckillForm.value.id) {
      await updateSeckill(seckillForm.value.id, seckillForm.value)
      ElMessage.success('活动已更新')
    } else {
      await createSeckill(seckillForm.value)
      ElMessage.success('活动已新增')
    }
    seckillDialogVisible.value = false
    seckills.value = await listSeckills()
    await computeSeckillGoodCounts()
  } catch (e) { /* 拦截器已提示 */ }
}
async function onDeleteSeckill(row) {
  try {
    await ElMessageBox.confirm(`确定删除活动「${row.name}」吗？`, '提示', { type: 'warning' })
    await deleteSeckill(row.id)
    ElMessage.success('已删除')
    seckills.value = await listSeckills()
    await computeSeckillGoodCounts()
  } catch (e) { /* 取消 */ }
}
async function viewSeckillGoods(row) {
  currentSeckill.value = row
  selectedGoodPreview.value = null
  newSeckillGood.value = { goodId: '', seckillPrice: 0, stock: 0, limitPerUser: 1 }
  try {
    const data = await listSeckillGoods(row.id)
    // 后端返回的商品信息嵌套在 good 对象中，拍平到顶层供模板使用
    seckillGoodsList.value = (data || []).map((item) => ({
      ...item,
      goodName: item.goodName || item.good?.name || `商品${item.goodId}`,
      goodPic: item.goodPic || item.good?.pic || '',
      originalPrice: item.good?.price || item.originalPrice || 0,
    }))
  } catch (e) { seckillGoodsList.value = [] }
  seckillGoodsDialogVisible.value = true
}
async function onAddSeckillGood() {
  if (!newSeckillGood.value.goodId) { ElMessage.warning('请选择商品'); return }
  if (!newSeckillGood.value.seckillPrice || newSeckillGood.value.seckillPrice <= 0) { ElMessage.warning('请输入秒杀价'); return }
  if (!newSeckillGood.value.stock || newSeckillGood.value.stock <= 0) { ElMessage.warning('请输入库存'); return }
  try {
    await addSeckillGood(currentSeckill.value.id, newSeckillGood.value)
    const data = await listSeckillGoods(currentSeckill.value.id)
    seckillGoodsList.value = data || []
    currentSeckill.value.goodCount = seckillGoodsList.value.length
    ElMessage.success('已添加')
    newSeckillGood.value = { goodId: '', seckillPrice: 0, stock: 0, limitPerUser: 1 }
    selectedGoodPreview.value = null
  } catch (e) { /* 拦截器已提示 */ }
}
async function onRemoveSeckillGood(row) {
  try {
    await removeSeckillGood(row.id)
    const data = await listSeckillGoods(currentSeckill.value.id)
    seckillGoodsList.value = data || []
    currentSeckill.value.goodCount = seckillGoodsList.value.length
    ElMessage.success('已移除')
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(() => {
  if (admin.value.isLoggedIn) {
    loadAll()
  } else {
    refreshAdminCaptcha()
  }
})
</script>

<style scoped>
.admin-page { min-height: 100vh; background: #f0f2f5; }

/* ========== 登录页 ========== */
.admin-login-page {
  min-height: 100vh;
  display: flex;
}
.login-brand-side {
  flex: 1.2;
  position: relative;
  background: linear-gradient(135deg, #1a237e 0%, #283593 35%, #2f6bff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.brand-content {
  position: relative;
  z-index: 2;
  color: #fff;
  max-width: 480px;
  padding: 40px;
}
.brand-logo {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  display: grid;
  place-items: center;
  background: rgba(255,255,255,.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,.25);
  margin-bottom: 28px;
}
.brand-name {
  font-size: 42px;
  font-weight: 900;
  letter-spacing: 2px;
  margin-bottom: 8px;
}
.brand-slogan {
  font-size: 16px;
  opacity: .85;
  margin-bottom: 48px;
  letter-spacing: 1px;
}
.brand-features {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.brand-feature {
  display: flex;
  align-items: center;
  gap: 16px;
}
.bf-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(255,255,255,.12);
  border: 1px solid rgba(255,255,255,.2);
  font-size: 22px;
  flex-shrink: 0;
}
.bf-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 2px;
}
.bf-desc {
  font-size: 13px;
  opacity: .7;
}
.brand-decor {
  position: absolute;
  border-radius: 50%;
  background: rgba(255,255,255,.06);
}
.decor-1 { width: 400px; height: 400px; top: -120px; right: -80px; }
.decor-2 { width: 300px; height: 300px; bottom: -60px; left: -60px; background: rgba(255,255,255,.04); }
.decor-3 { width: 180px; height: 180px; top: 40%; right: 15%; background: rgba(255,255,255,.05); }

.login-form-side {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  background: #fff;
}
.back-mall-btn {
  position: absolute;
  top: 24px;
  left: 24px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #5a6a85;
  background: #f5f7fa;
  border: 1px solid #e8ecf3;
  transition: all .25s ease;
}
.back-mall-btn:hover {
  color: #2f6bff;
  background: #eef3ff;
  border-color: #c5d8ff;
  transform: translateX(-2px);
}
.back-mall-btn .el-icon { font-size: 16px; }

.login-form-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  max-width: 400px;
  width: 100%;
  margin: 0 auto;
  padding: 40px;
}
.login-title {
  font-size: 28px;
  font-weight: 800;
  color: #1a2332;
  margin-bottom: 6px;
}
.login-subtitle {
  font-size: 14px;
  color: #8a94a8;
  margin-bottom: 32px;
}
.login-form .el-form-item { margin-bottom: 20px; }
.login-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 6px 16px;
  box-shadow: 0 0 0 1px #e0e6f0 inset;
  transition: box-shadow .25s ease;
}
.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #b8c8e8 inset;
}
.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(47,107,255,.35) inset;
}
.login-submit {
  width: 100%;
  margin-top: 8px;
  height: 48px;
  font-size: 16px;
  font-weight: 700;
  border-radius: 12px;
  background: linear-gradient(135deg, #2f6bff, #4a8aff);
  border: none;
  box-shadow: 0 6px 20px rgba(47,107,255,.35);
  transition: all .3s ease;
}
.login-submit:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(47,107,255,.45);
}
.login-demo-tip {
  text-align: center;
  font-size: 13px;
  color: #a0aac0;
  margin-top: 20px;
}

/* 验证码 */
.admin-captcha-row :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
  gap: 10px;
}
.admin-captcha-row :deep(.el-input) { flex: 1; }
.admin-captcha-img {
  width: 110px;
  height: 40px;
  border-radius: 10px;
  border: 1px solid #e0e6f0;
  cursor: pointer;
  object-fit: cover;
  flex-shrink: 0;
  transition: opacity .2s;
}
.admin-captcha-img:hover { opacity: .8; }
.admin-captcha-placeholder {
  display: grid;
  place-items: center;
  background: #f5f7fa;
  color: #a0aac0;
  font-size: 12px;
}
.captcha-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid #e0e6f0;
  border-top-color: #2f6bff;
  border-radius: 50%;
  animation: captcha-spin .8s linear infinite;
}
@keyframes captcha-spin {
  to { transform: rotate(360deg); }
}

/* ========== 管理后台布局 ========== */
.admin-layout {
  min-height: 100vh;
  display: flex;
}

/* 深色侧边栏 */
.admin-sidebar {
  width: 220px;
  background: linear-gradient(180deg, #1a2332 0%, #151c28 100%);
  display: flex;
  flex-direction: column;
  transition: width .3s ease;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  height: 100vh;
}
.admin-sidebar.collapsed { width: 68px; }
.sidebar-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 18px;
  border-bottom: 1px solid rgba(255,255,255,.06);
}
.sidebar-logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #2f6bff, #4a8aff);
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(47,107,255,.4);
}
.sidebar-logo-text {
  font-size: 18px;
  font-weight: 800;
  color: #fff;
  letter-spacing: 1px;
  white-space: nowrap;
}
.sidebar-nav {
  flex: 1;
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
}
.sidebar-nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  color: rgba(255,255,255,.55);
  transition: all .25s ease;
  text-align: left;
  width: 100%;
  white-space: nowrap;
  overflow: hidden;
  position: relative;
  background: transparent;
  border: none;
  cursor: pointer;
  font-family: inherit;
}
.admin-sidebar.collapsed .sidebar-nav-item {
  justify-content: center;
  padding: 12px;
}
.sidebar-nav-item:hover {
  background: rgba(255,255,255,.06);
  color: rgba(255,255,255,.85);
}
.sidebar-nav-item.active {
  background: linear-gradient(135deg, rgba(47,107,255,.9), rgba(74,138,255,.8));
  color: #fff;
  box-shadow: 0 4px 14px rgba(47,107,255,.35);
}
.sni-icon { font-size: 18px; flex-shrink: 0; }
.sni-label { flex: 1; }
.sni-indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fff;
  flex-shrink: 0;
}

.sidebar-foot {
  padding: 14px 10px;
  border-top: 1px solid rgba(255,255,255,.06);
  display: flex;
  align-items: center;
  gap: 8px;
}
.sidebar-user {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.su-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #2f6bff, #4a8aff);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  flex-shrink: 0;
}
.admin-sidebar.collapsed .su-info { display: none; }
.su-info { min-width: 0; flex: 1; }
.su-name {
  font-size: 13.5px;
  font-weight: 700;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.su-role {
  font-size: 11.5px;
  color: rgba(255,255,255,.45);
  margin-top: 1px;
}
.su-logout {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: rgba(255,255,255,.45);
  transition: all .25s ease;
  flex-shrink: 0;
  background: transparent;
  border: none;
  cursor: pointer;
}
.su-logout:hover {
  background: rgba(255,77,79,.15);
  color: #ff6b6b;
}

/* 右侧主区域 */
.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* 顶部白色栏 */
.admin-topbar {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e8ecf3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 10;
}
.topbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.topbar-collapse {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: #5a6a85;
  background: #f5f7fa;
  transition: all .25s ease;
}
.topbar-collapse:hover {
  color: #2f6bff;
  background: #eef3ff;
}
.topbar-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a2332;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.topbar-action {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: #5a6a85;
  background: #f5f7fa;
  transition: all .25s ease;
}
.topbar-action:hover {
  color: #2f6bff;
  background: #eef3ff;
}
.topbar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 14px 6px 6px;
  border-radius: 24px;
  background: #f5f7fa;
  cursor: pointer;
  transition: all .25s ease;
}
.topbar-user:hover { background: #eef3ff; }
.tu-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #2f6bff, #4a8aff);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}
.tu-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a2332;
}
.tu-arrow {
  font-size: 12px;
  color: #8a94a8;
}

/* 内容区 */
.admin-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

/* ========== 数据概览 ========== */
.dashboard-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}
.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 12px rgba(0,0,0,.04);
  transition: all .3s ease;
  border: 1px solid #f0f2f7;
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0,0,0,.08);
}
.sc-icon-wrap {
  width: 50px;
  height: 50px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-size: 24px;
  color: #fff;
  flex-shrink: 0;
}
.stat-primary .sc-icon-wrap { background: linear-gradient(135deg, #2f6bff, #4a8aff); }
.stat-blue .sc-icon-wrap { background: linear-gradient(135deg, #00b4d8, #48cae4); }
.stat-orange .sc-icon-wrap { background: linear-gradient(135deg, #ff9f43, #ffb976); }
.stat-green .sc-icon-wrap { background: linear-gradient(135deg, #00c48c, #4ade80); }
.stat-purple .sc-icon-wrap { background: linear-gradient(135deg, #a55eea, #c084fc); }
.stat-cyan .sc-icon-wrap { background: linear-gradient(135deg, #06b6d4, #22d3ee); }
.sc-body { flex: 1; min-width: 0; }
.sc-num {
  font-size: 24px;
  font-weight: 800;
  color: #1a2332;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sc-label {
  font-size: 13px;
  color: #8a94a8;
  margin-top: 3px;
}

/* 面板卡片 */
.panel-card {
  background: #fff;
  border-radius: 16px;
  padding: 22px;
  box-shadow: 0 2px 12px rgba(0,0,0,.04);
  border: 1px solid #f0f2f7;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f0f2f7;
}
.panel-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a2332;
}
.panel-sub {
  font-size: 13px;
  color: #8a94a8;
}
.trend-card { grid-column: 1 / -1; }
.trend-chart { width: 100%; height: 300px; margin-top: 8px; }
.panel-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

/* 模块视图 */
.module-view { width: 100%; }

/* 表格美化 */
.admin-table {
  --el-table-header-bg-color: #f8fafc;
  --el-table-border-color: #eef1f6;
  --el-table-row-hover-bg-color: #f5f8ff;
  border-radius: 12px;
  overflow: hidden;
  font-size: 13.5px;
}
.admin-table :deep(.el-table th.el-table__cell) {
  background: #f8fafc;
  color: #3d4a5c;
  font-weight: 700;
  font-size: 13.5px;
  padding: 12px 0;
  border-bottom: 1px solid #e8ecf3;
}
.admin-table :deep(.el-table td.el-table__cell) {
  font-size: 13.5px;
  padding: 11px 0;
  border-bottom: 1px solid #f1f4f9;
}
.admin-table :deep(.el-table__row:last-child td.el-table__cell) { border-bottom: none; }
.admin-table :deep(.el-table .cell) { line-height: 1.6; }
.admin-table :deep(.el-table .el-tag) { font-size: 12.5px; }

/* ===== 按钮美化 ===== */
/* 顶部主按钮（新增） */
.panel-head :deep(.el-button--primary) {
  background: linear-gradient(135deg, #2f6bff, #4a8aff);
  border: none;
  border-radius: 10px;
  padding: 12px 24px;
  font-size: 14.5px;
  font-weight: 600;
  color: #fff;
  box-shadow: 0 4px 14px rgba(47,107,255,.3);
  transition: all .25s ease;
}
.panel-head :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #1e5ae6, #3a7aef);
  box-shadow: 0 6px 20px rgba(47,107,255,.45);
  transform: translateY(-1px);
}
.panel-head :deep(.el-button--primary .el-icon) { font-size: 15px; }

/* 表格内操作按钮统一美化 */
.admin-table :deep(.el-button) {
  border-radius: 8px;
  font-size: 13.5px;
  font-weight: 600;
  padding: 8px 18px;
  border: none;
  transition: all .2s ease;
  letter-spacing: .3px;
}
.admin-table :deep(.el-button + .el-button) { margin-left: 8px; }

/* 编辑/详情 - 蓝色 */
.admin-table :deep(.el-button--primary.is-plain) {
  background: rgba(47,107,255,.1);
  color: #2f6bff;
}
.admin-table :deep(.el-button--primary.is-plain:hover) {
  background: rgba(47,107,255,.18);
  color: #1e5ae6;
  transform: translateY(-1px);
  box-shadow: 0 3px 10px rgba(47,107,255,.2);
}

/* 删除/禁用/移除 - 红色 */
.admin-table :deep(.el-button--danger.is-plain) {
  background: rgba(255,77,79,.1);
  color: #ff4d4f;
}
.admin-table :deep(.el-button--danger.is-plain:hover) {
  background: rgba(255,77,79,.18);
  color: #e63946;
  transform: translateY(-1px);
  box-shadow: 0 3px 10px rgba(255,77,79,.2);
}
.admin-table :deep(.el-button--danger.is-link) {
  color: #ff4d4f;
  font-weight: 600;
}
.admin-table :deep(.el-button--danger.is-link:hover) { color: #e63946; }

/* 上架/发货/启用/商品 - 绿色 */
.admin-table :deep(.el-button--success.is-plain) {
  background: rgba(6,214,160,.12);
  color: #06a77d;
}
.admin-table :deep(.el-button--success.is-plain:hover) {
  background: rgba(6,214,160,.2);
  color: #059669;
  transform: translateY(-1px);
  box-shadow: 0 3px 10px rgba(6,214,160,.2);
}

/* 下架 - 橙色 */
.admin-table :deep(.el-button--warning.is-plain) {
  background: rgba(255,165,0,.12);
  color: #e89500;
}
.admin-table :deep(.el-button--warning.is-plain:hover) {
  background: rgba(255,165,0,.2);
  color: #d48806;
  transform: translateY(-1px);
  box-shadow: 0 3px 10px rgba(255,165,0,.2);
}

/* 订单表格内商品展示 */
.order-goods-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.og-good {
  display: flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
}
.og-img {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid #eee;
}
.og-name {
  font-size: 12.5px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100px;
}
.og-more {
  font-size: 12px;
  color: #999;
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
}

/* 弹窗底部按钮 */
.admin-dialog :deep(.el-dialog__footer .el-button) {
  border-radius: 10px;
  font-size: 14.5px;
  font-weight: 600;
  padding: 11px 28px;
  transition: all .2s ease;
}
.admin-dialog :deep(.el-dialog__footer .el-button--primary) {
  background: linear-gradient(135deg, #2f6bff, #4a8aff);
  border: none;
  box-shadow: 0 4px 14px rgba(47,107,255,.3);
}
.admin-dialog :deep(.el-dialog__footer .el-button--primary:hover) {
  background: linear-gradient(135deg, #1e5ae6, #3a7aef);
  box-shadow: 0 6px 20px rgba(47,107,255,.45);
  transform: translateY(-1px);
}
.admin-dialog :deep(.el-dialog__footer .el-button:not(.el-button--primary)) {
  background: #f5f7fa;
  border: 1px solid #e8ecf3;
  color: #5a6a85;
}
.admin-dialog :deep(.el-dialog__footer .el-button:not(.el-button--primary):hover) {
  background: #eef1f6;
  color: #1a2332;
  border-color: #d8deea;
}

/* 弹窗内添加按钮 */
.dialog-add-bar :deep(.el-button--primary) {
  background: linear-gradient(135deg, #2f6bff, #4a8aff);
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  padding: 10px 22px;
  box-shadow: 0 3px 10px rgba(47,107,255,.25);
  transition: all .2s ease;
}
.dialog-add-bar :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #1e5ae6, #3a7aef);
  box-shadow: 0 5px 16px rgba(47,107,255,.4);
  transform: translateY(-1px);
}

.tbl-img { width: 40px; height: 40px; border-radius: 8px; object-fit: cover; }
.brand-logo-cell { width: 40px; height: 40px; border-radius: 8px; display: grid; place-items: center; }
.logo-fallback {
  width: 40px; height: 40px; border-radius: 8px;
  display: grid; place-items: center;
  background: linear-gradient(135deg, rgba(47,107,255,.15), rgba(47,107,255,.05));
  color: #2f6bff; font-size: 16px; font-weight: 800;
}

/* 饼图 */
.pie-wrap { display: flex; align-items: center; gap: 24px; justify-content: center; }
.pie-chart { width: 180px; height: 180px; flex-shrink: 0; }
.pie-legend { display: flex; flex-direction: column; gap: 10px; }
.legend-item { display: flex; align-items: center; gap: 8px; font-size: 13.5px; }
.legend-dot { width: 12px; height: 12px; border-radius: 3px; flex-shrink: 0; }
.legend-label { color: #1a2332; min-width: 60px; }
.legend-value { color: #8a94a8; font-weight: 600; }

/* 分类条形图 */
.cat-chart { display: flex; flex-direction: column; gap: 14px; }
.cat-row { display: flex; align-items: center; gap: 10px; }
.cat-name { width: 84px; font-size: 13.5px; color: #1a2332; flex-shrink: 0; text-align: right; }
.cat-bar-wrap { flex: 1; height: 22px; background: #f5f7fa; border-radius: 11px; overflow: hidden; }
.cat-bar { height: 100%; border-radius: 11px; transition: width .6s ease; background: linear-gradient(90deg, #2f6bff, #4a8aff); }
.cat-count { width: 52px; font-size: 13px; color: #8a94a8; flex-shrink: 0; }

/* 热销排行 */
.hot-list { display: flex; flex-direction: column; gap: 10px; }
.hot-item { display: flex; align-items: center; gap: 10px; padding: 8px; border-radius: 10px; transition: background .3s ease; }
.hot-item:hover { background: #f8fafc; }
.hot-rank { width: 24px; height: 24px; border-radius: 6px; flex-shrink: 0; display: grid; place-items: center; font-size: 13px; font-weight: 800; color: #fff; background: #94a3b8; }
.hot-rank.rank-1 { background: linear-gradient(135deg, #ff6b35, #ff9f43); }
.hot-rank.rank-2 { background: linear-gradient(135deg, #2f6bff, #4a8aff); }
.hot-rank.rank-3 { background: linear-gradient(135deg, #00b4d8, #48cae4); }
.hot-img { width: 40px; height: 40px; border-radius: 8px; object-fit: cover; flex-shrink: 0; }
.hot-name { flex: 1; font-size: 13.5px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #1a2332; }
.hot-sales { font-size: 13px; color: #8a94a8; flex-shrink: 0; }
.hot-price { font-size: 14.5px; font-weight: 700; color: #ff4d4f; flex-shrink: 0; width: 76px; text-align: right; }

/* 弹窗 */
.admin-dialog :deep(.el-dialog) { border-radius: 16px; overflow: hidden; }
.admin-dialog :deep(.el-dialog__title) { font-weight: 700; font-size: 17px; }
.dialog-add-bar { display: flex; gap: 10px; align-items: center; margin-top: 16px; padding-top: 16px; border-top: 1px solid #f0f2f7; flex-wrap: wrap; }

/* ===== 秒杀商品管理弹窗 ===== */
.sg-good-cell { display: flex; align-items: center; gap: 10px; }
.sg-good-img { width: 40px; height: 40px; border-radius: 8px; object-fit: cover; flex-shrink: 0; }
.sg-good-info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.sg-good-name { font-size: 13.5px; font-weight: 600; color: #1a2332; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sg-good-price { font-size: 12px; color: #8a94a8; }
.sg-seckill-price { color: #ff4d4f; font-weight: 700; font-size: 14px; }

.sg-add-section { margin-top: 18px; padding-top: 18px; border-top: 1px solid #f0f2f7; }
.sg-add-title { font-size: 14px; font-weight: 700; color: #1a2332; margin-bottom: 14px; }
.sg-add-row { display: flex; gap: 12px; align-items: flex-end; flex-wrap: wrap; }
.sg-good-select { flex: 1; min-width: 220px; }
.sg-good-select :deep(.el-select__wrapper) { border-radius: 10px; }
.sg-input-group { display: flex; flex-direction: column; gap: 4px; }
.sg-input-label { font-size: 12px; color: #8a94a8; font-weight: 600; }
.sg-num-input { width: 110px; }
.sg-num-input :deep(.el-input__wrapper) { border-radius: 10px; }
.sg-add-btn {
  background: linear-gradient(135deg, #2f6bff, #4a8aff);
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  padding: 10px 28px;
  color: #fff;
  box-shadow: 0 3px 10px rgba(47,107,255,.25);
  transition: all .2s ease;
  height: 40px;
}
.sg-add-btn:hover {
  background: linear-gradient(135deg, #1e5ae6, #3a7aef);
  box-shadow: 0 5px 16px rgba(47,107,255,.4);
  transform: translateY(-1px);
}

/* 商品选择器下拉选项 */
.sg-option { display: flex; align-items: center; gap: 10px; padding: 4px 0; }
.sg-option-img { width: 32px; height: 32px; border-radius: 6px; object-fit: cover; flex-shrink: 0; }
.sg-option-name { flex: 1; font-size: 13.5px; color: #1a2332; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sg-option-price { font-size: 13px; color: #ff4d4f; font-weight: 700; flex-shrink: 0; }

/* 选中商品预览 */
.sg-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(47,107,255,.06), rgba(47,107,255,.02));
  border: 1px solid rgba(47,107,255,.12);
  border-radius: 12px;
}
.sg-preview-img { width: 48px; height: 48px; border-radius: 10px; object-fit: cover; flex-shrink: 0; }
.sg-preview-info { display: flex; flex-direction: column; gap: 3px; }
.sg-preview-name { font-size: 14.5px; font-weight: 700; color: #1a2332; }
.sg-preview-meta { font-size: 12.5px; color: #8a94a8; }

/* 会员详情 */
.member-detail-head { display: flex; align-items: center; gap: 16px; padding-bottom: 18px; border-bottom: 1px solid #f0f2f7; margin-bottom: 16px; }
.member-avatar { width: 56px; height: 56px; border-radius: 50%; display: grid; place-items: center; background: linear-gradient(135deg, #2f6bff, #4a8aff); color: #fff; font-size: 22px; font-weight: 700; flex-shrink: 0; }
.member-detail-info { flex: 1; }
.member-detail-info h4 { font-size: 18px; font-weight: 700; color: #1a2332; }
.member-detail-info p { font-size: 13px; color: #8a94a8; margin-top: 4px; }
.member-stats { display: flex; gap: 24px; }
.ms-item { text-align: center; }
.ms-item b { display: block; font-size: 20px; font-weight: 800; color: #2f6bff; }
.ms-item span { font-size: 12px; color: #8a94a8; }
.member-order-stats { margin-bottom: 16px; }
.mos-title { font-size: 13px; font-weight: 700; color: #1a2332; margin-bottom: 8px; }
.mos-bar { display: flex; height: 16px; border-radius: 8px; overflow: hidden; background: #f5f7fa; }
.mos-seg { height: 100%; transition: width .6s ease; min-width: 2px; }
.mos-labels { display: flex; flex-wrap: wrap; gap: 14px; margin-top: 8px; }
.mos-label { display: flex; align-items: center; gap: 5px; font-size: 12px; color: #8a94a8; }
.mos-dot { width: 8px; height: 8px; border-radius: 2px; display: inline-block; }
.member-tabs { margin-top: 8px; }
.info-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 0; }
.info-row { display: flex; padding: 10px 0; border-bottom: 1px solid #f0f2f7; }
.info-label { width: 90px; color: #8a94a8; font-size: 13px; flex-shrink: 0; }
.member-orders { display: flex; flex-direction: column; gap: 10px; }
.mo-card { border: 1px solid #e8ecf3; border-radius: 12px; padding: 12px 14px; }
.mo-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.mo-no { font-size: 13px; font-family: monospace; color: #8a94a8; }
.mo-items { display: flex; flex-wrap: wrap; gap: 8px; }
.mo-item { font-size: 13px; background: #f5f7fa; padding: 3px 10px; border-radius: 8px; }
.mo-foot { display: flex; justify-content: space-between; margin-top: 8px; font-size: 13px; }
.mo-time { color: #8a94a8; }
.member-addrs { display: flex; flex-direction: column; gap: 10px; }
.ma-card { border: 1px solid #e8ecf3; border-radius: 12px; padding: 12px 14px; }
.ma-receiver { font-size: 14px; font-weight: 600; display: flex; align-items: center; gap: 8px; color: #1a2332; }
.ma-phone { font-size: 13px; color: #8a94a8; font-weight: 400; }
.ma-detail { font-size: 13px; color: #8a94a8; margin-top: 4px; }

/* 订单详情 */
.order-detail { display: flex; flex-direction: column; gap: 10px; }
.od-row { display: flex; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px dashed #e8ecf3; }
.od-label { width: 80px; color: #8a94a8; font-size: 13px; flex-shrink: 0; }
.od-value { font-size: 14px; font-weight: 600; color: #1a2332; }
.od-value.mono { font-family: monospace; }
.od-value.price { color: #ff4d4f; font-size: 18px; }
.od-items { margin-top: 8px; }
.od-items-title { font-size: 14px; font-weight: 700; margin-bottom: 10px; color: #1a2332; }
.od-item { display: flex; align-items: center; gap: 12px; padding: 8px 0; }
.od-item img { width: 48px; height: 48px; border-radius: 8px; object-fit: cover; }
.od-item span { flex: 1; font-size: 14px; color: #1a2332; }
.od-item-price { font-size: 14px; color: #ff4d4f; font-weight: 600; flex: 0 0 auto !important; }

/* 响应式 */
@media (max-width: 1400px) {
  .stat-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 1100px) {
  .login-brand-side { display: none; }
  .login-form-side { flex: 1; }
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .panel-row { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .admin-sidebar { width: 68px; }
  .admin-sidebar .sni-label, .admin-sidebar .sidebar-logo-text, .admin-sidebar .su-info { display: none; }
  .admin-sidebar .sidebar-nav-item { justify-content: center; padding: 12px; }
  .topbar-title { display: none; }
  .tu-name { display: none; }
  .info-grid { grid-template-columns: 1fr; }
  .pie-wrap { flex-direction: column; }
  .admin-content { padding: 16px; }
}
</style>
