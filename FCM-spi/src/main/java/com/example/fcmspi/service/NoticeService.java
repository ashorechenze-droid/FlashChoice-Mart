package com.example.fcmspi.service;

import com.example.fcmspi.model.NoticeEntity;
import com.example.fcmspi.model.PriceDropNoticeParam;
import com.example.fcmspi.model.SeckillJoinNoticeParam;

import java.util.List;

/**
 * 会员通知业务接口（notice-api）：站内信列表/未读数/已读标记，
 * 以及收藏商品事件（降价/进入秒杀）触发批量下发
 */
public interface NoticeService {

    /** 会员通知列表（按 id 倒序；unreadOnly=true 仅返回未读） */
    List<NoticeEntity> listByMemberId(Long memberId, boolean unreadOnly);

    /** 未读数量（消息中心红点） */
    long countUnread(Long memberId);

    /** 单条标记已读（校验归属） */
    int markRead(Long memberId, Long id);

    /** 全部标记已读 */
    int markAllRead(Long memberId);

    /** 降价通知：向收藏该商品的会员批量下发站内信，返回下发条数（内部接口调用） */
    int notifyPriceDrop(PriceDropNoticeParam param);

    /** 进入秒杀通知：向收藏该商品的会员批量下发站内信，返回下发条数（内部接口调用） */
    int notifySeckillJoin(SeckillJoinNoticeParam param);
}
