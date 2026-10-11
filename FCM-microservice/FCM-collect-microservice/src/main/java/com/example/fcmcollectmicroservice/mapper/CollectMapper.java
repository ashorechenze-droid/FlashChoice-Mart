package com.example.fcmcollectmicroservice.mapper;

import com.example.fcmspi.model.CollectEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CollectMapper {

    List<CollectEntity> findByMemberId(Long memberId);

    /** 会员收藏中是否已有该商品（唯一索引 member_id + good_id） */
    CollectEntity findByMemberAndGood(@Param("memberId") Long memberId, @Param("goodId") Long goodId);

    int save(CollectEntity collect);

    /** 批量删除（where 带 member_id，归属校验） */
    int deleteByIds(@Param("memberId") Long memberId, @Param("ids") List<Long> ids);

    /** 按商品删除（where 带 member_id，归属校验） */
    int deleteByMemberAndGood(@Param("memberId") Long memberId, @Param("goodId") Long goodId);

    /** 收藏了指定商品的会员 ID 列表（通知接收人，唯一索引保证天然去重） */
    List<Long> findMemberIdsByGoodId(Long goodId);
}
