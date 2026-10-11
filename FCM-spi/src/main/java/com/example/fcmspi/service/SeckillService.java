package com.example.fcmspi.service;

import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmspi.model.GrabResult;
import com.example.fcmspi.model.SeckillEntity;
import com.example.fcmspi.model.SeckillGoodEntity;
import com.example.fcmspi.model.search.SeckillSearchBean;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 秒杀业务接口（seckill-api）：活动/秒杀商品管理、Redis 预库存、Lua 扣减、异步下单
 */
public interface SeckillService {

    /* ===== 秒杀活动管理 ===== */

    PageInfo<SeckillEntity> page(SeckillSearchBean search, PaginateInfo page);

    SeckillEntity getById(Long id);

    int insert(SeckillEntity seckill);

    int update(SeckillEntity seckill);

    /** 启用/禁用（禁用后前台不可见） */
    int updateEnabled(Long id, boolean enabled);

    /** 删除活动（级联删除活动商品） */
    int deleteById(Long id);

    /** 前台活动列表：进行中/即将开始（enabled=1 且未结束，可加 Redis 缓存） */
    List<SeckillEntity> listActive();

    /* ===== 秒杀活动商品管理 ===== */

    /** 活动商品列表（含商品信息与剩余库存，Redis 库存优先） */
    List<SeckillGoodEntity> listGoods(Long seckillId);

    SeckillGoodEntity getGoodById(Long seckillGoodId);

    int insertGood(SeckillGoodEntity seckillGood);

    int updateGood(SeckillGoodEntity seckillGood);

    int deleteGood(Long seckillGoodId);

    /* ===== 库存预热与抢购 ===== */

    /** 活动开始前：将活动内秒杀商品库存预热到 Redis（seckill:stock:{seckillGoodId}） */
    void warmUp(Long seckillId);

    /**
     * 抢购：校验时间窗/启用/限购→Redis+Lua 原子扣减（防超卖）→记录已抢→发送 RabbitMQ
     * 异步下单消息→返回秒杀流水号 orderNo（前端轮询）；失败抛 BizException（7001-7004）
     */
    String grab(Long seckillGoodId, Long memberId, String memberAccount, Long addrId);

    /** 抢购结果查询（前端轮询：处理中/成功/失败） */
    GrabResult getGrabResult(String orderNo, Long memberId);
}
