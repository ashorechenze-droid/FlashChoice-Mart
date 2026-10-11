-- =====================================================================
-- FlashChoice-Mart 测试数据（仅插入，不建表）
-- 请确保所有表已存在且结构匹配
-- 图片说明：
--   商品图使用 picsum.photos（seed 固定，800x800，稳定可访问）
--   品牌Logo/用户头像使用 ui-avatars.com（文字头像，品牌色背景）
--   如需替换为真实产品图，直接修改对应 URL 即可
-- =====================================================================

USE `flashchoice-mart`;

-- 1. 品牌
INSERT INTO `brand` (`id`, `name`, `company`, `logo`, `site`, `description`, `created_time`, `created_by`, `updated_time`, `updated_by`)
VALUES
(1, '华为', '华为技术有限公司', 'https://ui-avatars.com/api/?name=%E5%8D%8E%E4%B8%BA&background=CF0A2C&color=fff&size=128&bold=true', 'https://www.huawei.com', '全球领先的ICT基础设施和智能终端提供商', NOW(), 1, NOW(), 1),
(2, '小米', '小米科技有限责任公司', 'https://ui-avatars.com/api/?name=%E5%B0%8F%E7%B1%B3&background=FF6700&color=fff&size=128&bold=true', 'https://www.mi.com', '专注于智能硬件和电子产品研发', NOW(), 1, NOW(), 1),
(3, '苹果', '苹果公司', 'https://ui-avatars.com/api/?name=Apple&background=000000&color=fff&size=128&bold=true', 'https://www.apple.com', '全球知名消费电子品牌', NOW(), 1, NOW(), 1),
(4, '联想', '联想集团有限公司', 'https://ui-avatars.com/api/?name=%E8%81%94%E6%83%B3&background=E2231A&color=fff&size=128&bold=true', 'https://www.lenovo.com', '全球智能设备及解决方案提供商', NOW(), 1, NOW(), 1),
(5, '格力', '珠海格力电器股份有限公司', 'https://ui-avatars.com/api/?name=%E6%A0%BC%E5%8A%9B&background=00A0E9&color=fff&size=128&bold=true', 'https://www.gree.com', '国际知名空调制造商', NOW(), 1, NOW(), 1);

-- 2. 分类
INSERT INTO `category` (`id`, `parent_id`, `name`, `title`, `tag`, `icon`, `summary`, `sort`, `description`, `created_time`, `created_by`, `updated_time`, `updated_by`)
VALUES
(1, 0, '电子产品', '电子数码', '电子', 'fa-electronics', '各种电子产品', 1, '电子产品分类', NOW(), 1, NOW(), 1),
(2, 1, '手机', '智能手机', '手机', 'fa-mobile', '智能手机', 1, '手机分类', NOW(), 1, NOW(), 1),
(3, 1, '电脑', '笔记本电脑', '电脑', 'fa-laptop', '笔记本电脑', 2, '电脑分类', NOW(), 1, NOW(), 1),
(4, 1, '智能穿戴', '智能手表/手环', '穿戴', 'fa-watch', '智能穿戴设备', 3, '穿戴分类', NOW(), 1, NOW(), 1),
(5, 2, '华为手机', '华为系列', '华为', 'fa-huawei', '华为手机', 1, '华为手机', NOW(), 1, NOW(), 1),
(6, 2, '小米手机', '小米系列', '小米', 'fa-xiaomi', '小米手机', 2, '小米手机', NOW(), 1, NOW(), 1),
(7, 2, '苹果手机', 'iPhone系列', '苹果', 'fa-apple', '苹果手机', 3, '苹果手机', NOW(), 1, NOW(), 1),
(8, 3, '联想笔记本', '联想系列', '联想', 'fa-lenovo', '联想笔记本', 1, '联想笔记本', NOW(), 1, NOW(), 1),
(9, 3, '华为笔记本', 'MateBook系列', '华为', 'fa-huawei', '华为笔记本', 2, '华为笔记本', NOW(), 1, NOW(), 1),
(10, 4, '小米手表', '小米手表', '小米', 'fa-xiaomi', '小米智能手表', 1, '小米手表', NOW(), 1, NOW(), 1);

-- 3. 商品
INSERT INTO `good` (`id`, `spu_no`, `name`, `alias`, `summary`, `category_id`, `brand_id`, `mark_price`, `price`, `qty`, `pic`, `pic2`, `detail_pics`, `detail`, `is_take_down`, `is_hot`, `is_del`, `is_seckill`, `description`, `created_time`, `created_by`, `updated_time`, `updated_by`)
VALUES
(1, 'SPU001', '华为Mate 60 Pro', 'Mate60Pro', '高端旗舰手机', 2, 1, 6999.00, 6499.00, 100, 'https://picsum.photos/seed/SPU001/800/800', 'https://picsum.photos/seed/SPU001b/800/800', 'https://picsum.photos/seed/SPU001d1/800/800,https://picsum.photos/seed/SPU001d2/800/800', '<p>华为Mate 60 Pro 搭载麒麟芯片，支持卫星通话</p>', 0, 1, 0, 0, '华为旗舰', NOW(), 1, NOW(), 1),
(2, 'SPU002', '小米14 Ultra', 'Xiaomi14Ultra', '全能影像旗舰', 2, 2, 5999.00, 5499.00, 200, 'https://picsum.photos/seed/SPU002/800/800', 'https://picsum.photos/seed/SPU002b/800/800', 'https://picsum.photos/seed/SPU002d1/800/800,https://picsum.photos/seed/SPU002d2/800/800', '<p>小米14 Ultra 配备徕卡光学镜头，骁龙8Gen3</p>', 0, 1, 0, 0, '小米旗舰', NOW(), 1, NOW(), 1),
(3, 'SPU003', 'iPhone 15 Pro Max', 'iPhone15PM', '苹果旗舰', 2, 3, 9999.00, 8999.00, 50, 'https://picsum.photos/seed/SPU003/800/800', 'https://picsum.photos/seed/SPU003b/800/800', 'https://picsum.photos/seed/SPU003d1/800/800,https://picsum.photos/seed/SPU003d2/800/800', '<p>iPhone 15 Pro Max 钛金属边框，A17 Pro芯片</p>', 0, 1, 0, 0, '苹果旗舰', NOW(), 1, NOW(), 1),
(4, 'SPU004', '联想拯救者Y9000X', 'Legion Y9000X', '高性能游戏本', 3, 4, 10999.00, 9999.00, 30, 'https://picsum.photos/seed/SPU004/800/800', 'https://picsum.photos/seed/SPU004b/800/800', 'https://picsum.photos/seed/SPU004d1/800/800,https://picsum.photos/seed/SPU004d2/800/800', '<p>联想拯救者Y9000X 配备RTX 4080，32GB内存</p>', 0, 1, 0, 0, '游戏本', NOW(), 1, NOW(), 1),
(5, 'SPU005', '华为MateBook X Pro', 'MateBook X Pro', '超轻薄商务本', 3, 1, 9999.00, 8999.00, 40, 'https://picsum.photos/seed/SPU005/800/800', 'https://picsum.photos/seed/SPU005b/800/800', 'https://picsum.photos/seed/SPU005d1/800/800,https://picsum.photos/seed/SPU005d2/800/800', '<p>华为MateBook X Pro 超轻薄，3K触控屏</p>', 0, 1, 0, 0, '商务本', NOW(), 1, NOW(), 1),
(6, 'SPU006', '小米手表S3', 'Xiaomi Watch S3', '智能运动手表', 4, 2, 1299.00, 999.00, 150, 'https://picsum.photos/seed/SPU006/800/800', 'https://picsum.photos/seed/SPU006b/800/800', 'https://picsum.photos/seed/SPU006d1/800/800,https://picsum.photos/seed/SPU006d2/800/800', '<p>小米手表S3 支持eSIM，AMOLED屏幕</p>', 0, 0, 0, 0, '小米手表', NOW(), 1, NOW(), 1),
(7, 'SPU007', '格力空调·云锦', 'Gree YunJin', '家用变频空调', 1, 5, 3999.00, 3299.00, 80, 'https://picsum.photos/seed/SPU007/800/800', 'https://picsum.photos/seed/SPU007b/800/800', 'https://picsum.photos/seed/SPU007d1/800/800,https://picsum.photos/seed/SPU007d2/800/800', '<p>格力云锦系列 1.5匹变频空调</p>', 0, 1, 0, 0, '格力空调', NOW(), 1, NOW(), 1),
(8, 'SPU008', '华为Pocket 2', 'Pocket2', '折叠屏手机', 2, 1, 7999.00, 7499.00, 60, 'https://picsum.photos/seed/SPU008/800/800', 'https://picsum.photos/seed/SPU008b/800/800', 'https://picsum.photos/seed/SPU008d1/800/800,https://picsum.photos/seed/SPU008d2/800/800', '<p>华为Pocket 2 竖向折叠屏，灵犀通信</p>', 0, 0, 0, 0, '折叠屏', NOW(), 1, NOW(), 1);

-- 4. 商品详情图片
INSERT INTO `good_detail_pics` (`good_id`, `url`, `sort`, `description`)
VALUES
(1, 'https://picsum.photos/seed/SPU001d1/1200/1200', 1, '正面'),
(1, 'https://picsum.photos/seed/SPU001d2/1200/1200', 2, '背面'),
(1, 'https://picsum.photos/seed/SPU001d3/1200/1200', 3, '细节'),
(2, 'https://picsum.photos/seed/SPU002d1/1200/1200', 1, '正面'),
(2, 'https://picsum.photos/seed/SPU002d2/1200/1200', 2, '相机'),
(2, 'https://picsum.photos/seed/SPU002d3/1200/1200', 3, '细节'),
(3, 'https://picsum.photos/seed/SPU003d1/1200/1200', 1, '正面'),
(3, 'https://picsum.photos/seed/SPU003d2/1200/1200', 2, '背面'),
(3, 'https://picsum.photos/seed/SPU003d3/1200/1200', 3, '细节'),
(4, 'https://picsum.photos/seed/SPU004d1/1200/1200', 1, 'A面'),
(4, 'https://picsum.photos/seed/SPU004d2/1200/1200', 2, '键盘'),
(4, 'https://picsum.photos/seed/SPU004d3/1200/1200', 3, '接口'),
(5, 'https://picsum.photos/seed/SPU005d1/1200/1200', 1, '正面'),
(5, 'https://picsum.photos/seed/SPU005d2/1200/1200', 2, '键盘'),
(6, 'https://picsum.photos/seed/SPU006d1/1200/1200', 1, '表盘'),
(6, 'https://picsum.photos/seed/SPU006d2/1200/1200', 2, '表带'),
(7, 'https://picsum.photos/seed/SPU007d1/1200/1200', 1, '整机'),
(7, 'https://picsum.photos/seed/SPU007d2/1200/1200', 2, '细节'),
(8, 'https://picsum.photos/seed/SPU008d1/1200/1200', 1, '展开'),
(8, 'https://picsum.photos/seed/SPU008d2/1200/1200', 2, '折叠');

-- 5. 行政区划
INSERT INTO `t_china_region` (`id`, `name`, `parent_id`, `sort_order`, `level`, `description`)
VALUES
(1, '北京市', 0, 1, 1, '直辖市'),
(2, '上海市', 0, 2, 1, '直辖市'),
(3, '广东省', 0, 3, 1, '省份'),
(4, '重庆市', 0, 4, 1, '直辖市'),
(5, '江苏省', 0, 5, 1, '省份'),
(6, '朝阳区', 1, 1, 2, '北京辖区'),
(7, '海淀区', 1, 2, 2, '北京辖区'),
(8, '浦东新区', 2, 1, 2, '上海辖区'),
(9, '天河区', 3, 1, 2, '广州辖区'),
(10, '渝中区', 4, 1, 2, '重庆辖区'),
(11, '玄武区', 5, 1, 2, '南京辖区');

-- 6. 会员（密码统一为 BCrypt("123456")，可直接登录）
INSERT INTO `member` (`id`, `account`, `password`, `enabled`, `name`, `pinyin`, `pinyin_untoned`, `sex`, `birthday`, `height`, `weight`, `iq`, `qq`, `wechat`, `phone`, `email`, `native_place_id`, `card_id`, `wedlock`, `political_orientation`, `address_id`, `address_detail`, `race`, `religion`, `nationality`, `portrait`, `description`, `extra_info`, `last_login_time`, `last_login_ip`, `version`, `created_time`, `created_by`, `updated_time`, `updated_by`)
VALUES
(1, 'zhangsan', '$2b$10$zHboHB1LbKvuCUYkWlEqvezkdMLJxk8.BUNtBOtVPusrnzsFyyu3C', 1, '张三', 'Zhang San', 'zhangsan', '男', '1995-05-10', 175, 70.5, 110, '12345678', 'zhangsan_wx', '13800138001', 'zhangsan@example.com', 1, '110101199505101234', '未婚', '群众', 1, '北京市朝阳区建国路88号', '汉族', '无', '中国', 'https://ui-avatars.com/api/?name=%E5%BC%A0%E4%B8%89&background=2F6BFF&color=fff&size=128', '普通会员', '{"level":"gold"}', '2026-09-07 10:00:00', '192.168.1.1', 0, NOW(), 1, NOW(), 1),
(2, 'lisi', '$2b$10$zHboHB1LbKvuCUYkWlEqvezkdMLJxk8.BUNtBOtVPusrnzsFyyu3C', 1, '李四', 'Li Si', 'lisi', '女', '1998-12-01', 165, 55.0, 115, '87654321', 'lisi_wx', '13800138002', 'lisi@example.com', 2, '310101199812010987', '已婚', '党员', 2, '上海市浦东新区陆家嘴环路1000号', '汉族', '无', '中国', 'https://ui-avatars.com/api/?name=%E6%9D%8E%E5%9B%9B&background=10B981&color=fff&size=128', '高级会员', '{"level":"platinum"}', '2026-09-06 15:30:00', '192.168.1.2', 0, NOW(), 1, NOW(), 1),
(3, 'wangwu', '$2b$10$zHboHB1LbKvuCUYkWlEqvezkdMLJxk8.BUNtBOtVPusrnzsFyyu3C', 1, '王五', 'Wang Wu', 'wangwu', '男', '1990-03-15', 180, 80.0, 120, '11223344', 'wangwu_wx', '13800138003', 'wangwu@example.com', 3, '440101199003151111', '离异', '群众', 3, '广州市天河区天河路385号', '汉族', '佛教', '中国', 'https://ui-avatars.com/api/?name=%E7%8E%8B%E4%BA%94&background=F59E0B&color=fff&size=128', '超级会员', '{"level":"diamond"}', '2026-09-05 20:00:00', '192.168.1.3', 0, NOW(), 1, NOW(), 1),
(4, 'zhaoliu', '$2b$10$zHboHB1LbKvuCUYkWlEqvezkdMLJxk8.BUNtBOtVPusrnzsFyyu3C', 1, '赵六', 'Zhao Liu', 'zhaoliu', '女', '2000-07-20', 170, 60.0, 105, '99887766', 'zhaoliu_wx', '13800138004', 'zhaoliu@example.com', 4, '500101200007202222', '未婚', '团员', 4, '重庆市渝中区中山四路1号', '土家族', '无', '中国', 'https://ui-avatars.com/api/?name=%E8%B5%B5%E5%85%AD&background=EF4444&color=fff&size=128', '普通会员', '{"level":"silver"}', '2026-09-04 18:00:00', '192.168.1.4', 0, NOW(), 1, NOW(), 1),
(5, 'sunqi', '$2b$10$zHboHB1LbKvuCUYkWlEqvezkdMLJxk8.BUNtBOtVPusrnzsFyyu3C', 1, '孙七', 'Sun Qi', 'sunqi', '男', '1988-11-11', 172, 72.0, 108, '55443322', 'sunqi_wx', '13800138005', 'sunqi@example.com', 5, '320101198811113333', '已婚', '群众', 5, '南京市玄武区长江路2号', '汉族', '无', '中国', 'https://ui-avatars.com/api/?name=%E5%AD%99%E4%B8%83&background=8B5CF6&color=fff&size=128', '普通会员', '{"level":"gold"}', '2026-09-03 12:00:00', '192.168.1.5', 0, NOW(), 1, NOW(), 1);

-- 7. 会员收货地址
INSERT INTO `member_address` (`member_account`, `receiver`, `phone`, `addr_id`, `addr_detail`, `is_default`)
VALUES
('zhangsan', '张三', '13800138001', 1, '北京市朝阳区建国路88号院3号楼501', 1),
('zhangsan', '张太太', '13900138001', 2, '北京市海淀区中关村大街1号', 0),
('lisi', '李四', '13800138002', 3, '上海市浦东新区陆家嘴环路1000号A座20层', 1),
('wangwu', '王五', '13800138003', 4, '广州市天河区天河路385号1201室', 1),
('zhaoliu', '赵六', '13800138004', 5, '重庆市渝中区中山四路1号附1号', 1),
('sunqi', '孙七', '13800138005', 6, '南京市玄武区长江路2号301室', 1);

-- 8. 购物车
INSERT INTO `cart` (`member_id`, `good_id`, `qty`)
VALUES
(1, 1, 1),
(1, 3, 2),
(2, 2, 1),
(2, 5, 1),
(3, 6, 1),
(4, 7, 2),
(5, 4, 1);

-- 9. 订单
INSERT INTO `order` (`id`, `order_no`, `seckill_no`, `member_account`, `total_pay`, `pay_type`, `alipay_trade_no`, `checkout_time`, `pay_time`, `ship_time`, `accept_time`, `status`, `receiver_addr_id`, `receiver_name`, `receiver_phone`, `receiver_addr_detail`, `order_comment`, `refund_status`, `is_del`, `description`, `created_time`, `created_by`, `updated_time`, `updated_by`)
VALUES
(1, 'ORD202609070001', NULL, 'zhangsan', 6499.00, '模拟支付', 'ALI202609071234567', '2026-09-07 10:00:00', '2026-09-07 10:05:00', '2026-09-07 14:00:00', NULL, '已支付', 1, '张三', '13800138001', '北京市朝阳区建国路88号院3号楼501', '请放快递柜', '无退款', 0, NULL, NOW(), 1, NOW(), 1),
(2, 'ORD202609070002', NULL, 'lisi', 17998.00, '模拟支付', 'ALI202609071234568', '2026-09-07 11:00:00', '2026-09-07 11:10:00', NULL, NULL, '待收货', 3, '李四', '13800138002', '上海市浦东新区陆家嘴环路1000号A座20层', NULL, '无退款', 0, NULL, NOW(), 1, NOW(), 1),
(3, 'ORD202609070003', NULL, 'wangwu', 999.00, '模拟支付', 'ALI202609071234569', '2026-09-07 09:00:00', '2026-09-07 09:05:00', '2026-09-07 13:00:00', '2026-09-07 17:00:00', '已确认', 4, '王五', '13800138003', '广州市天河区天河路385号1201室', '谢谢', '无退款', 0, NULL, NOW(), 1, NOW(), 1),
(4, 'ORD202609060001', NULL, 'zhaoliu', 3299.00, '模拟支付', 'ALI202609061234567', '2026-09-06 16:00:00', NULL, NULL, NULL, '待付款', 5, '赵六', '13800138004', '重庆市渝中区中山四路1号附1号', NULL, '无退款', 0, NULL, NOW(), 1, NOW(), 1),
(5, 'ORD202609050001', NULL, 'sunqi', 9999.00, '模拟支付', 'ALI202609051234567', '2026-09-05 14:00:00', '2026-09-05 14:20:00', '2026-09-06 09:00:00', '2026-09-06 18:00:00', '已确认', 6, '孙七', '13800138005', '南京市玄武区长江路2号301室', NULL, '无退款', 0, NULL, NOW(), 1, NOW(), 1);

-- 10. 订单明细
INSERT INTO `order_item` (`order_id`, `good_id`, `deal_price`, `count`, `good_name`, `good_pic`, `good_desc`)
VALUES
(1, 1, 6499.00, 1, '华为Mate 60 Pro', 'https://picsum.photos/seed/SPU001/800/800', '高端旗舰手机'),
(2, 4, 9999.00, 1, '联想拯救者Y9000X', 'https://picsum.photos/seed/SPU004/800/800', '高性能游戏本'),
(2, 5, 7999.00, 1, '华为MateBook X Pro', 'https://picsum.photos/seed/SPU005/800/800', '超轻薄商务本'),
(3, 6, 999.00, 1, '小米手表S3', 'https://picsum.photos/seed/SPU006/800/800', '智能运动手表'),
(4, 7, 3299.00, 1, '格力空调·云锦', 'https://picsum.photos/seed/SPU007/800/800', '家用变频空调'),
(5, 3, 8999.00, 1, 'iPhone 15 Pro Max', 'https://picsum.photos/seed/SPU003/800/800', '苹果旗舰'),
(5, 6, 999.00, 1, '小米手表S3', 'https://picsum.photos/seed/SPU006/800/800', '智能运动手表');

-- 11. 秒杀活动
INSERT INTO `seckill` (`id`, `name`, `enabled`, `start_time`, `end_time`, `description`, `created_time`, `created_by`, `updated_time`, `updated_by`)
VALUES
(1, '开学季秒杀', 1, '2026-09-01 00:00:00', '2026-09-10 23:59:59', '开学季大促，手机电脑特价', NOW(), 1, NOW(), 1),
(2, '双十一预热', 0, '2026-10-20 00:00:00', '2026-11-11 23:59:59', '双十一预热秒杀', NOW(), 1, NOW(), 1);

-- 12. 秒杀活动商品
INSERT INTO `seckill_good` (`seckill_id`, `good_id`, `seckill_price`, `stock`, `sold`, `limit_per_user`, `description`)
VALUES
(1, 1, 5999.00, 20, 5, 1, '华为Mate 60 Pro 限时秒杀'),
(1, 2, 4999.00, 30, 8, 2, '小米14 Ultra 限时秒杀'),
(1, 4, 8999.00, 10, 3, 1, '联想拯救者Y9000X 限时秒杀'),
(2, 3, 7999.00, 15, 0, 1, 'iPhone 15 Pro Max 双十一预热'),
(2, 5, 7999.00, 20, 0, 1, '华为MateBook X Pro 双十一预热');

-- 13. 后台用户
INSERT INTO `user` (`id`, `username`, `password`, `avatar_url`, `enabled`, `status`, `locked`, `user_expire_time`, `credential_expire_time`, `login_times`, `last_login_time`, `last_login_ip`, `op_mode`, `description`, `created_time`, `created_by`, `updated_time`, `updated_by`)
VALUES
(1, 'admin', '$2b$10$zHboHB1LbKvuCUYkWlEqvezkdMLJxk8.BUNtBOtVPusrnzsFyyu3C', 'https://ui-avatars.com/api/?name=%E7%AE%A1&background=1B4DF5&color=fff&size=128&bold=true', 1, 0, 0, NULL, NULL, 100, NOW(), '192.168.1.100', 1, '系统管理员', NOW(), 1, NOW(), 1),
(2, 'manager', '$2b$10$zHboHB1LbKvuCUYkWlEqvezkdMLJxk8.BUNtBOtVPusrnzsFyyu3C', 'https://ui-avatars.com/api/?name=%E8%BF%90&background=2F6BFF&color=fff&size=128&bold=true', 1, 0, 0, NULL, NULL, 50, NOW(), '192.168.1.101', 0, '运营经理', NOW(), 1, NOW(), 1);

-- 14. 用户组
INSERT INTO `t_rbac_group` (`id`, `name`, `enabled`, `description`, `created_time`, `created_by`, `updated_time`, `updated_by`)
VALUES
(1, '管理员组', 1, '拥有所有权限', NOW(), 1, NOW(), 1),
(2, '运营组', 1, '负责商品和订单管理', NOW(), 1, NOW(), 1);

-- 15. 角色
INSERT INTO `t_rbac_role` (`id`, `name`, `enabled`, `description`, `created_time`, `created_by`, `updated_time`, `updated_by`)
VALUES
(1, 'ROLE_ADMIN', 1, '管理员角色', NOW(), 1, NOW(), 1),
(2, 'ROLE_MANAGER', 1, '运营经理角色', NOW(), 1, NOW(), 1);

-- 16. 权限（按后台模块划分；系统管理覆盖用户/用户组/角色/权限/资源/菜单）
INSERT INTO `t_rbac_perm` (`id`, `name`, `enabled`, `description`, `created_time`, `created_by`, `updated_time`, `updated_by`)
VALUES
(1, 'PERM_BRAND_MANAGE', 1, '品牌管理', NOW(), 1, NOW(), 1),
(2, 'PERM_CATEGORY_MANAGE', 1, '分类管理', NOW(), 1, NOW(), 1),
(3, 'PERM_GOOD_MANAGE', 1, '商品管理（含按钮）', NOW(), 1, NOW(), 1),
(4, 'PERM_ORDER_MANAGE', 1, '订单管理', NOW(), 1, NOW(), 1),
(5, 'PERM_MEMBER_MANAGE', 1, '会员管理', NOW(), 1, NOW(), 1),
(6, 'PERM_SECKILL_MANAGE', 1, '秒杀管理', NOW(), 1, NOW(), 1),
(7, 'PERM_SYSTEM_MANAGE', 1, '系统管理（用户/用户组/角色/权限/资源/菜单）', NOW(), 1, NOW(), 1),
(8, 'PERM_REGION_MANAGE', 1, '地区管理', NOW(), 1, NOW(), 1),
(9, 'PERM_UPLOAD', 1, '文件上传', NOW(), 1, NOW(), 1);

-- 17. 资源（按钮 + 接口；接口 value 为 ant 路径，与 RbacService.checkPermission 的匹配规则一致，对应真实控制器路径）
INSERT INTO `t_rbac_resource` (`id`, `name`, `type`, `value`, `description`, `created_time`, `created_by`, `updated_time`, `updated_by`)
VALUES
(1, '新增商品', '按钮', 'good:add', '新增商品按钮', NOW(), 1, NOW(), 1),
(2, '编辑商品', '按钮', 'good:edit', '编辑商品按钮', NOW(), 1, NOW(), 1),
(3, '删除商品', '按钮', 'good:delete', '删除商品按钮', NOW(), 1, NOW(), 1),
(4, '品牌管理', '接口', '/api/brands/**', '品牌管理接口', NOW(), 1, NOW(), 1),
(5, '分类管理', '接口', '/api/categories/**', '分类管理接口', NOW(), 1, NOW(), 1),
(6, '商品管理', '接口', '/api/goods/**', '商品管理接口', NOW(), 1, NOW(), 1),
(7, '订单管理', '接口', '/api/orders/**', '订单管理接口', NOW(), 1, NOW(), 1),
(8, '会员管理', '接口', '/api/members/**', '会员管理接口', NOW(), 1, NOW(), 1),
(9, '秒杀管理', '接口', '/api/seckills/**', '秒杀管理接口', NOW(), 1, NOW(), 1),
(10, '用户管理', '接口', '/api/users/**', '后台用户管理接口', NOW(), 1, NOW(), 1),
(11, '用户组管理', '接口', '/api/groups/**', '用户组管理接口', NOW(), 1, NOW(), 1),
(12, '角色管理', '接口', '/api/roles/**', '角色管理接口', NOW(), 1, NOW(), 1),
(13, '权限管理', '接口', '/api/perms/**', '权限管理接口', NOW(), 1, NOW(), 1),
(14, '资源管理', '接口', '/api/resources/**', '资源管理接口', NOW(), 1, NOW(), 1),
(15, '菜单管理', '接口', '/api/menus/**', '菜单管理接口', NOW(), 1, NOW(), 1),
(16, '用户组分配', '接口', '/api/rbac/**', '用户-用户组分配接口', NOW(), 1, NOW(), 1),
(17, '地区管理', '接口', '/api/regions/**', '地区管理接口', NOW(), 1, NOW(), 1),
(18, '文件上传', '接口', '/upload/**', '商品图/头像上传接口', NOW(), 1, NOW(), 1);

-- 18. 菜单（resource_id 挂钩资源，/api/menus 按当前用户资源动态过滤返回）
INSERT INTO `t_rbac_menu` (`id`, `parent_id`, `resource_id`, `name`, `icon`, `url`, `sort`, `description`, `created_time`, `created_by`, `updated_time`, `updated_by`)
VALUES
(1, 0, 4, '品牌管理', 'Collection', '/brands', 1, '品牌管理菜单', NOW(), 1, NOW(), 1),
(2, 0, 5, '分类管理', 'Grid', '/categories', 2, '分类管理菜单', NOW(), 1, NOW(), 1),
(3, 0, 6, '商品管理', 'Goods', '/goods', 3, '商品管理菜单', NOW(), 1, NOW(), 1),
(4, 0, 7, '订单管理', 'List', '/orders', 4, '订单管理菜单', NOW(), 1, NOW(), 1),
(5, 0, 8, '会员管理', 'User', '/members', 5, '会员管理菜单', NOW(), 1, NOW(), 1),
(6, 0, 9, '秒杀管理', 'Clock', '/seckills', 6, '秒杀管理菜单', NOW(), 1, NOW(), 1),
(7, 0, NULL, '系统管理', 'Setting', NULL, 7, '系统管理目录（无资源挂钩，按子菜单过滤）', NOW(), 1, NOW(), 1),
(8, 7, 10, '用户管理', 'UserFilled', '/system/users', 1, '后台用户管理', NOW(), 1, NOW(), 1),
(9, 7, 11, '用户组管理', 'OfficeBuilding', '/system/groups', 2, '用户组管理', NOW(), 1, NOW(), 1),
(10, 7, 12, '角色管理', 'Avatar', '/system/roles', 3, '角色管理', NOW(), 1, NOW(), 1),
(11, 7, 13, '权限管理', 'Key', '/system/perms', 4, '权限管理', NOW(), 1, NOW(), 1),
(12, 7, 14, '资源管理', 'Box', '/system/resources', 5, '资源管理', NOW(), 1, NOW(), 1),
(13, 7, 15, '菜单管理', 'Menu', '/system/menus', 6, '菜单管理', NOW(), 1, NOW(), 1),
(14, 0, 17, '地区管理', 'Location', '/regions', 8, '地区管理菜单', NOW(), 1, NOW(), 1);

-- 19. 用户-组关联
INSERT INTO `t_rbac_user_group` (`user_id`, `group_id`)
VALUES
(1, 1),
(2, 2);

-- 20. 组-角色关联
INSERT INTO `t_rbac_group_role` (`group_id`, `role_id`)
VALUES
(1, 1),
(2, 2);

-- 21. 角色-权限关联（管理员全量；运营经理：品牌/分类/商品/订单/会员/秒杀/上传）
INSERT INTO `t_rbac_role_perm` (`role_id`, `perm_id`)
VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9),
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 9);

-- 22. 权限-资源关联
INSERT INTO `t_rbac_perm_resource` (`resource_id`, `perm_id`)
VALUES
(1, 3), (2, 3), (3, 3),
(4, 1), (5, 2), (6, 3),
(7, 4), (8, 5), (9, 6),
(10, 7), (11, 7), (12, 7), (13, 7), (14, 7), (15, 7), (16, 7),
(17, 8), (18, 9);

-- 23. 商品收藏
INSERT INTO `collect` (`member_id`, `good_id`)
VALUES
(1, 2),
(1, 4),
(2, 1),
(2, 3),
(3, 5),
(3, 6),
(4, 8),
(5, 1);

-- 24. 会员通知（站内信：收藏商品降价/进入秒杀）
INSERT INTO `notice` (`member_id`, `type`, `good_id`, `title`, `content`, `is_read`, `created_time`)
VALUES
(1, 1, 2, '收藏商品降价提醒', '你收藏的「智能手表 S8 运动版」降价了：￥1999 → ￥1499，快来看看吧', 0, NOW()),
(1, 2, 4, '收藏商品进入秒杀会场', '你收藏的「轻弹缓震跑鞋」已加入秒杀活动「周末大促」，秒杀价 ￥499，先到先得', 0, NOW()),
(1, 2, 2, '收藏商品进入秒杀会场', '你收藏的「智能手表 S8 运动版」已加入秒杀活动「今晚八点场」，秒杀价 ￥999，先到先得', 1, NOW()),
(2, 1, 1, '收藏商品降价提醒', '你收藏的「降噪无线耳机 Pro」降价了：￥1299 → ￥899，快来看看吧', 0, NOW()),
(2, 1, 3, '收藏商品降价提醒', '你收藏的「轻奢通勤双肩包」降价了：￥599 → ￥459，快来看看吧', 1, NOW()),
(3, 1, 5, '收藏商品降价提醒', '你收藏的「便携意式咖啡机」降价了：￥1299 → ￥1099，快来看看吧', 0, NOW()),
(3, 2, 6, '收藏商品进入秒杀会场', '你收藏的「客制化机械键盘」已加入秒杀活动「明日午间场」，秒杀价 ￥399，先到先得', 0, NOW()),
(4, 2, 8, '收藏商品进入秒杀会场', '你收藏的「无线蓝牙音箱 Mini」已加入秒杀活动「今晚八点场」，秒杀价 ￥199，先到先得', 0, NOW());
