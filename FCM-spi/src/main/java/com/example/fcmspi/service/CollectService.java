package com.example.fcmspi.service;

import com.example.fcmspi.model.CollectEntity;

import java.util.List;

/**
 * 商品收藏业务接口（collect-api）：增删查、幂等收藏、归属校验
 */
public interface CollectService {

    /** 当前会员收藏列表（含商品信息回显，按收藏时间倒序） */
    List<CollectEntity> listByMemberId(Long memberId);

    /** 会员是否已收藏指定商品 */
    boolean isCollected(Long memberId, Long goodId);

    /** 收藏商品：member_id + good_id 已存在则幂等返回（校验商品存在与上架） */
    int add(CollectEntity collect);

    /** 按 ID 批量取消收藏（校验归属） */
    int deleteByIds(Long memberId, List<Long> ids);

    /** 按商品取消收藏（校验归属；详情页/商品卡片「取消收藏」用） */
    int deleteByGoodId(Long memberId, Long goodId);

    /** 收藏了指定商品的会员 ID 列表（降价/进入秒杀通知的接收人，去重） */
    List<Long> listMemberIdsByGoodId(Long goodId);
}
