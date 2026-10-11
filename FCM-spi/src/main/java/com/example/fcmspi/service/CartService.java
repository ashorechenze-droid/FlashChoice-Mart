package com.example.fcmspi.service;

import com.example.fcmspi.model.CartEntity;

import java.util.List;

/**
 * 购物车业务接口（cart-api）：增删改查、数量累加、归属校验
 */
public interface CartService {

    /** 当前会员购物车列表（含商品信息回显） */
    List<CartEntity> listByMemberId(Long memberId);

    /** 按 ID 批量查询（服务间 Feign：下单按 cartIds 取条目；归属由调用方校验） */
    List<CartEntity> listByIds(List<Long> ids);

    CartEntity getById(Long id);

    /** 加入购物车：member_id + good_id 已存在则累加数量，否则新增（校验上架与库存上限） */
    int add(CartEntity cart);

    /** 修改数量（校验归属） */
    int updateQty(CartEntity cart);

    /** 批量删除（校验归属） */
    int deleteByIds(Long memberId, List<Long> ids);
}
