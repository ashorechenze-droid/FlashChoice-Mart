package com.example.fcmcartmicroservice.mapper;

import com.example.fcmspi.model.CartEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartMapper {

    List<CartEntity> findByMemberId(Long memberId);

    /** 按 ID 批量查询（服务间 Feign：下单按 cartIds 结算） */
    List<CartEntity> findByIds(List<Long> ids);

    CartEntity findById(Long id);

    /** 会员购物车中是否已有该商品（唯一索引 member_id + good_id） */
    CartEntity findByMemberAndGood(@Param("memberId") Long memberId, @Param("goodId") Long goodId);

    int save(CartEntity cart);

    /** 修改数量（where 带 member_id，归属不符则影响 0 行） */
    int updateQty(@Param("id") Long id, @Param("memberId") Long memberId, @Param("qty") Integer qty);

    /** 批量删除（where 带 member_id，归属校验） */
    int deleteByIds(@Param("memberId") Long memberId, @Param("ids") List<Long> ids);
}
