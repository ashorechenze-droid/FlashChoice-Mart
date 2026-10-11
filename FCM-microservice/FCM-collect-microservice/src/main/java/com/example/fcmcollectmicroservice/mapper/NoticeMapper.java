package com.example.fcmcollectmicroservice.mapper;

import com.example.fcmspi.model.NoticeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeMapper {

    /** 会员通知列表（id 倒序；unreadOnly=true 仅未读） */
    List<NoticeEntity> findByMemberId(@Param("memberId") Long memberId, @Param("unreadOnly") boolean unreadOnly);

    /** 未读数量（消息中心红点） */
    long countUnread(Long memberId);

    /** 单条标记已读（where 带 member_id，归属校验） */
    int markRead(@Param("memberId") Long memberId, @Param("id") Long id);

    /** 全部标记已读 */
    int markAllRead(Long memberId);

    /** 批量写入通知（收藏者列表一次下发） */
    int saveBatch(@Param("list") List<NoticeEntity> notices);
}
