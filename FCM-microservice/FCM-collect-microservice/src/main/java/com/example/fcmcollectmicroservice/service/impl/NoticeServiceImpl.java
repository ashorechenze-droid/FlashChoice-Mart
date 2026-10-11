package com.example.fcmcollectmicroservice.service.impl;

import com.example.fcmcollectmicroservice.mapper.NoticeMapper;
import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmspi.model.NoticeEntity;
import com.example.fcmspi.model.PriceDropNoticeParam;
import com.example.fcmspi.model.SeckillJoinNoticeParam;
import com.example.fcmspi.service.CollectService;
import com.example.fcmspi.service.NoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;
    private final CollectService collectService;

    public NoticeServiceImpl(NoticeMapper noticeMapper, CollectService collectService) {
        this.noticeMapper = noticeMapper;
        this.collectService = collectService;
    }

    @Override
    public List<NoticeEntity> listByMemberId(Long memberId, boolean unreadOnly) {
        if (memberId == null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "缺少会员信息");
        }
        return noticeMapper.findByMemberId(memberId, unreadOnly);
    }

    @Override
    public long countUnread(Long memberId) {
        if (memberId == null) {
            return 0;
        }
        return noticeMapper.countUnread(memberId);
    }

    @Override
    public int markRead(Long memberId, Long id) {
        if (memberId == null || id == null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "缺少会员或通知条目");
        }
        return noticeMapper.markRead(memberId, id);
    }

    @Override
    public int markAllRead(Long memberId) {
        if (memberId == null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "缺少会员信息");
        }
        return noticeMapper.markAllRead(memberId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int notifyPriceDrop(PriceDropNoticeParam param) {
        if (param == null || param.getGoodId() == null) {
            log.warn("降价通知参数不合法，忽略: {}", param);
            return 0;
        }
        String title = "收藏商品降价提醒";
        String content = "你收藏的" + quotedName(param.getGoodName()) + "降价了："
                + priceText(param.getOldPrice()) + " → " + priceText(param.getNewPrice()) + "，快来看看吧";
        return push(param.getGoodId(), NoticeEntity.TYPE_PRICE_DROP, title, content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int notifySeckillJoin(SeckillJoinNoticeParam param) {
        if (param == null || param.getGoodId() == null) {
            log.warn("进入秒杀通知参数不合法，忽略: {}", param);
            return 0;
        }
        String title = "收藏商品进入秒杀会场";
        String content = "你收藏的" + quotedName(param.getGoodName()) + "已加入秒杀活动「"
                + blankToDefault(param.getSeckillName(), "限时秒杀") + "」，秒杀价 "
                + priceText(param.getSeckillPrice()) + "，先到先得";
        return push(param.getGoodId(), NoticeEntity.TYPE_SECKILL_JOIN, title, content);
    }

    /** 查询收藏者并批量写入通知（仅向收藏会员下发；无收藏者时静默返回 0） */
    private int push(Long goodId, int type, String title, String content) {
        List<Long> memberIds = collectService.listMemberIdsByGoodId(goodId);
        if (memberIds == null || memberIds.isEmpty()) {
            return 0;
        }
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern(CommonConstants.DATE_TIME_PATTERN));
        List<NoticeEntity> notices = memberIds.stream().map(memberId -> {
            NoticeEntity notice = new NoticeEntity();
            notice.setMemberId(memberId);
            notice.setType(type);
            notice.setGoodId(goodId);
            notice.setTitle(title);
            notice.setContent(content);
            notice.setIsRead(false);
            notice.setCreatedTime(now);
            return notice;
        }).toList();
        return noticeMapper.saveBatch(notices);
    }

    /** 商品名带书名号展示；名称缺失时回退为「商品」二字 */
    private String quotedName(String name) {
        return name == null || name.isBlank() ? "商品" : "「" + name + "」";
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /** 价格展示：去掉无意义的小数尾零；缺失时回退文案 */
    private String priceText(BigDecimal price) {
        return price == null ? "优惠价" : "￥" + price.stripTrailingZeros().toPlainString();
    }
}
