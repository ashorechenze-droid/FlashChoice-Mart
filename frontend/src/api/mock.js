// ============================================================
// Mock 数据层（USE_MOCK = true 时使用）
// 字段结构对齐《详细设计说明书》：good / brand / category / seckill / seckill_good / member
// 后端就绪后切换 config.js 的 USE_MOCK = false 即可走真实网关接口
// ============================================================

// 模拟网络延迟
export function delay(data, ms = 220) {
  return new Promise((resolve) => setTimeout(() => resolve(data), ms))
}

// ---------- 分类（category：parent_id 自关联树，一级 + 二级） ----------
// 对齐后端 /api/categories/tree 返回结构：根分类带 children 嵌套
export const mockCategories = [
  { id: 1, parentId: 0, name: '数码影音', icon: 'Headset', summary: '耳机 / 音箱 / 键盘', children: [
    { id: 11, parentId: 1, name: '耳机音响', icon: 'Headset', summary: '降噪耳机 / 蓝牙音箱' },
    { id: 12, parentId: 1, name: '键盘鼠标', icon: 'Keyboard', summary: '机械键盘 / 游戏鼠标' },
    { id: 13, parentId: 1, name: '手机数码', icon: 'Iphone', summary: '智能手机 / 配件' },
  ]},
  { id: 2, parentId: 0, name: '智能穿戴', icon: 'Watch', summary: '手表 / 手环', children: [
    { id: 21, parentId: 2, name: '智能手表', icon: 'Watch', summary: '运动手表 / 健康监测' },
    { id: 22, parentId: 2, name: '智能手环', icon: 'Timer', summary: '运动手环 / 心率带' },
  ]},
  { id: 3, parentId: 0, name: '品质箱包', icon: 'Backpack', summary: '双肩包 / 通勤包', children: [
    { id: 31, parentId: 3, name: '双肩背包', icon: 'Backpack', summary: '通勤 / 旅行背包' },
    { id: 32, parentId: 3, name: '手提箱包', icon: 'Briefcase', summary: '公文包 / 行李箱' },
  ]},
  { id: 4, parentId: 0, name: '潮流运动', icon: 'Basketball', summary: '跑鞋 / 运动装备', children: [
    { id: 41, parentId: 4, name: '跑步鞋服', icon: 'Shoes', summary: '跑鞋 / 运动服' },
    { id: 42, parentId: 4, name: '运动器材', icon: 'Basketball', summary: '球类 / 健身器材' },
  ]},
  { id: 5, parentId: 0, name: '居家生活', icon: 'House', summary: '灯具 / 杯具 / 小家电', children: [
    { id: 51, parentId: 5, name: '家居灯具', icon: 'Sunny', summary: '台灯 / 氛围灯' },
    { id: 52, parentId: 5, name: '水具杯具', icon: 'CoffeeCup', summary: '保温杯 / 咖啡杯' },
    { id: 53, parentId: 5, name: '厨房小电', icon: 'Oven', summary: '咖啡机 / 空气炸锅' },
  ]},
  { id: 6, parentId: 0, name: '美妆个护', icon: 'MagicStick', summary: '护肤 / 香氛', children: [
    { id: 61, parentId: 6, name: '护肤身体', icon: 'MagicStick', summary: '护手霜 / 身体乳' },
    { id: 62, parentId: 6, name: '香氛香水', icon: 'Cherry', summary: '香水 / 香薰' },
  ]},
  { id: 7, parentId: 0, name: '家用电器', icon: 'Oven', summary: '厨电 / 生活电器', children: [
    { id: 71, parentId: 7, name: '厨房电器', icon: 'Oven', summary: '空气炸锅 / 电饭煲' },
    { id: 72, parentId: 7, name: '生活电器', icon: 'Fan', summary: '风扇 / 净化器' },
  ]},
]

// ---------- 品牌（brand） ----------
export const mockBrands = [
  { id: 1, name: '极光 AURORA', company: '极光智能科技有限公司', logo: '', site: '', description: '专注影音与智能硬件的科技品牌，为品质生活提供极致声学体验。' },
  { id: 2, name: 'NOVAPLUS', company: '诺瓦普拉斯创新科技', logo: '', site: '', description: '新锐智能穿戴品牌，以健康监测与运动科学见长。' },
  { id: 3, name: '山林集', company: '山林集实业有限公司', logo: '', site: '', description: '东方极简生活方式品牌，取材天然，克制而耐看。' },
  { id: 4, name: '栖屿', company: '栖屿家居设计有限公司', logo: '', site: '', description: '以「安静的舒适」为理念的家居生活品牌。' },
  { id: 5, name: '流光 LUMI', company: '流光照明科技有限公司', logo: '', site: '', description: '专注健康照明的设计品牌，让每一束光都有温度。' },
  { id: 6, name: 'MOTIVE', company: '慕提运动用品有限公司', logo: '', site: '', description: '为城市跑者而生的运动装备品牌，轻量、缓震、可靠。' },
]

// ---------- 商品（good SPU） ----------
export const mockGoods = [
  { id: 1, name: '降噪无线耳机 Pro', alias: 'AURORA ANC Pro', summary: '40dB 深度主动降噪，Hi-Res 金标音质',
    price: 899, markPrice: 1299, qty: 120, pic: 'https://aka.doubaocdn.com/s/H6jXqs7H47',
    categoryId: 1, brandId: 1, isHot: 1, isSeckill: 0, isTakeDown: 0,
    desc: '40dB 深度主动降噪，Hi-Res 金标音质，单次续航 36 小时，通勤差旅沉浸享受。',
    tags: ['主动降噪', 'Hi-Res 金标', '36h 续航', '蓝牙 5.4'] },
  { id: 2, name: '智能手表 S8 运动版', alias: 'NOVAPLUS S8 Sport', summary: 'AMOLED 高清屏，全天候健康监测',
    price: 1499, markPrice: 1999, qty: 80, pic: 'https://aka.doubaocdn.com/s/DRHgi9vRS8',
    categoryId: 2, brandId: 2, isHot: 1, isSeckill: 1, isTakeDown: 0,
    desc: 'AMOLED 高清屏，血氧 / 心率 / 睡眠监测，双频 GPS 精准定位，全天候守护健康。',
    tags: ['AMOLED 屏', '血氧监测', '双频 GPS', '5ATM 防水'] },
  { id: 3, name: '轻奢通勤双肩包', alias: '山林集 Wanderer', summary: '头层牛皮，15.6 英寸电脑仓',
    price: 459, markPrice: 0, qty: 60, pic: 'https://aka.doubaocdn.com/s/BECn5F65Xo',
    categoryId: 3, brandId: 3, isHot: 0, isSeckill: 0, isTakeDown: 0,
    desc: '头层牛皮拼接，15.6 英寸电脑仓，通勤差旅两相宜，低调而有质感。',
    tags: ['头层牛皮', '15.6 寸电脑仓', '防泼水', 'USB 外接'] },
  { id: 4, name: '轻弹缓震跑鞋', alias: 'MOTIVE Float 2', summary: '全掌缓震，单只 238g',
    price: 629, markPrice: 0, qty: 150, pic: 'https://aka.doubaocdn.com/s/LoL3pn25Br',
    categoryId: 4, brandId: 6, isHot: 1, isSeckill: 0, isTakeDown: 0,
    desc: '全掌缓震中底，透气飞织鞋面，轻若无物，越跑越轻松。',
    tags: ['全掌缓震', '飞织鞋面', '单只 238g', '防滑大底'] },
  { id: 5, name: '便携意式咖啡机', alias: '栖屿 Brew One', summary: '15Bar 高压萃取，一键出杯',
    price: 1099, markPrice: 0, qty: 40, pic: 'https://aka.doubaocdn.com/s/F80KxEA0BS',
    categoryId: 5, brandId: 4, isHot: 0, isSeckill: 0, isTakeDown: 0,
    desc: '15Bar 高压萃取，一键出杯，随行也能喝到一杯好咖啡。',
    tags: ['15Bar 萃取', '一键出杯', '奶泡一体', 'Type-C 供电'] },
  { id: 6, name: '客制化机械键盘', alias: 'AURORA Key 75', summary: 'Gasket 结构，全键热插拔',
    price: 529, markPrice: 0, qty: 90, pic: 'https://aka.doubaocdn.com/s/MDzpfC9JsU',
    categoryId: 1, brandId: 1, isHot: 1, isSeckill: 0, isTakeDown: 0,
    desc: 'Gasket 结构，全键热插拔，三模连接，办公游戏通吃。',
    tags: ['Gasket 结构', '全键热插拔', '三模连接', 'PBT 键帽'] },
  { id: 7, name: '植萃香氛护手霜', alias: '山林集 Cedrus', summary: '雪松白茶香调，植萃修护',
    price: 89, markPrice: 0, qty: 300, pic: 'https://aka.doubaocdn.com/s/c1iCaNhqEI',
    categoryId: 6, brandId: 3, isHot: 0, isSeckill: 0, isTakeDown: 0,
    desc: '雪松与白茶香调，三重植萃修护，留香持久不黏腻。',
    tags: ['雪松白茶', '植萃修护', '留香 6h', '便携装'] },
  { id: 8, name: '无线蓝牙音箱 Mini', alias: 'AURORA Bead', summary: '360° 环绕声场，IPX7 防水',
    price: 399, markPrice: 0, qty: 110, pic: 'https://aka.doubaocdn.com/s/4VBzOfpH7T',
    categoryId: 1, brandId: 1, isHot: 0, isSeckill: 1, isTakeDown: 0,
    desc: '360° 环绕声场，IPX7 防水，24 小时续航，户外聚会氛围担当。',
    tags: ['360° 声场', 'IPX7 防水', '24h 续航', 'TWS 组队'] },
  { id: 9, name: '5G 智能手机 星野', alias: 'NOVAPLUS Starfield', summary: '旗舰影像，超长续航',
    price: 3999, markPrice: 4599, qty: 50, pic: 'https://aka.doubaocdn.com/s/ZRNRXXCUx1',
    categoryId: 1, brandId: 2, isHot: 1, isSeckill: 0, isTakeDown: 0,
    desc: '旗舰影像系统，5000mAh 超长续航，轻至 189g，握持如星。',
    tags: ['旗舰影像', '5000mAh', '189g', '120Hz 屏'] },
  { id: 10, name: '简约护眼台灯', alias: '流光 LUMI Soft', summary: '无频闪护眼，多档色温',
    price: 269, markPrice: 0, qty: 140, pic: 'https://aka.doubaocdn.com/s/2rYaFWhnOR',
    categoryId: 5, brandId: 5, isHot: 0, isSeckill: 0, isTakeDown: 0,
    desc: '无频闪护眼光源，三档色温调节，读书工作久看不累。',
    tags: ['无频闪', '三档色温', 'Ra95 显色', '触控调光'] },
  { id: 11, name: '316L 保温杯', alias: '栖屿 Thermo', summary: '12 小时长效保温',
    price: 129, markPrice: 0, qty: 200, pic: 'https://aka.doubaocdn.com/s/jmbDLponJ0',
    categoryId: 5, brandId: 4, isHot: 0, isSeckill: 0, isTakeDown: 0,
    desc: '316L 不锈钢内胆，12 小时长效保温，哑光质感，久用如新。',
    tags: ['316L 内胆', '12h 保温', '哑光漆面', '一键弹盖'] },
  { id: 12, name: '大容量空气炸锅', alias: '栖屿 Air Crisp', summary: '5.5L 大容量，无油低脂',
    price: 459, markPrice: 599, qty: 70, pic: 'https://aka.doubaocdn.com/s/MVWWxBz4ua',
    categoryId: 7, brandId: 4, isHot: 0, isSeckill: 1, isTakeDown: 0,
    desc: '5.5L 大容量，360° 热风循环，无油低脂，一键脆炸。',
    tags: ['5.5L 大容量', '360° 热风', '无油低脂', '可拆洗'] },
]

// ---------- 秒杀活动（seckill + seckill_good） ----------
// 活动时间用相对当前时间生成，保证任何时候打开演示都有「进行中 / 即将开始」状态
function seckillTime() {
  const now = Date.now()
  const H = 3600 * 1000
  return {
    running: { start: new Date(now - 2 * H), end: new Date(now + 3 * H) },
    upcoming: { start: new Date(now + 1 * H), end: new Date(now + 6 * H) },
    weekend: { start: new Date(now + 48 * H), end: new Date(now + 52 * H) },
  }
}

export const mockSeckills = (() => {
  const t = seckillTime()
  const fmt = (d) => d.toISOString().slice(0, 19).replace('T', ' ')
  return [
    { id: 1, name: '今晚八点场', enabled: 1, startTime: fmt(t.running.start), endTime: fmt(t.running.end), description: '爆款直降到底，手快有手慢无' },
    { id: 2, name: '明日午间场', enabled: 1, startTime: fmt(t.upcoming.start), endTime: fmt(t.upcoming.end), description: '限时 5 小时，秒杀价先到先得' },
    { id: 3, name: '周末大促', enabled: 1, startTime: fmt(t.weekend.start), endTime: fmt(t.weekend.end), description: '精选好物低至 5 折' },
  ]
})()

export const mockSeckillGoods = [
  { seckillId: 1, goodId: 2, seckillPrice: 999, stock: 20, sold: 12, limitPerUser: 1 },
  { seckillId: 1, goodId: 8, seckillPrice: 199, stock: 30, sold: 24, limitPerUser: 2 },
  { seckillId: 1, goodId: 12, seckillPrice: 299, stock: 25, sold: 9, limitPerUser: 1 },
  { seckillId: 2, goodId: 9, seckillPrice: 3499, stock: 10, sold: 0, limitPerUser: 1 },
  { seckillId: 2, goodId: 6, seckillPrice: 399, stock: 40, sold: 3, limitPerUser: 2 },
  { seckillId: 3, goodId: 4, seckillPrice: 499, stock: 60, sold: 5, limitPerUser: 2 },
  { seckillId: 3, goodId: 10, seckillPrice: 199, stock: 35, sold: 2, limitPerUser: 2 },
]

// ---------- 会员（member：mock 登录/注册） ----------
// 演示账号：demo / 123456；其他任意账号密码在 mock 模式也可注册后登录
export function mockLogin({ account, password }) {
  if (account === 'demo' && password !== '123456') {
    return delay(Promise.reject({ code: 400, msg: '密码错误（演示账号：demo / 123456）' }))
  }
  const user = { id: 1, account, name: account === 'demo' ? '优选会员' : account, portrait: '' }
  return delay({ token: `mock-token-${Date.now()}`, user })
}

export function mockRegister({ account, password }) {
  return delay({ token: `mock-token-${Date.now()}`, user: { id: 2, account, name: account, portrait: '' } })
}
