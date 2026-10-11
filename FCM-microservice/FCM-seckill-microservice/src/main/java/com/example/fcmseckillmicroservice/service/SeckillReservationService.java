package com.example.fcmseckillmicroservice.service;

import com.example.fcmseckillmicroservice.mapper.SeckillMapper;
import com.example.fcmspi.model.SeckillGoodEntity;
import com.example.fcmspi.model.SeckillGrabEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Database guard and idempotent state transitions for the asynchronous consumer. */
@Service
public class SeckillReservationService {
    public enum ReserveResult { READY, REJECTED, FINISHED }

    private final SeckillMapper mapper;
    public SeckillReservationService(SeckillMapper mapper) { this.mapper = mapper; }

    @Transactional(rollbackFor = Exception.class)
    public ReserveResult reserve(String seckillNo) {
        SeckillGrabEntity grab = mapper.findGrab(seckillNo);
        if (grab == null || "FAILED".equals(grab.getStatus()) || "CANCELLED".equals(grab.getStatus())
                || "SUCCESS".equals(grab.getStatus())) return ReserveResult.FINISHED;
        // All reservations for this product serialize on the product row.
        SeckillGoodEntity good = mapper.lockGood(grab.getSeckillGoodId());
        if (good == null) {
            mapper.markFailed(seckillNo);
            return ReserveResult.REJECTED;
        }
        grab = mapper.lockGrab(seckillNo);
        if ("PROCESSING".equals(grab.getStatus())) return ReserveResult.READY;
        if (!"PENDING".equals(grab.getStatus())) return ReserveResult.FINISHED;
        int limit = good.getLimitPerUser() == null ? 1 : good.getLimitPerUser();
        if (mapper.countMemberGrabs(grab.getMemberId(), grab.getSeckillGoodId(), seckillNo) >= limit
                || mapper.reserveStock(good.getId()) == 0) {
            mapper.markFailed(seckillNo);
            return ReserveResult.REJECTED;
        }
        mapper.markProcessing(seckillNo);
        return ReserveResult.READY;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean failProcessing(String seckillNo) {
        SeckillGrabEntity grab = mapper.lockGrab(seckillNo);
        if (grab == null || !"PROCESSING".equals(grab.getStatus())) return false;
        if (mapper.markFailed(seckillNo) == 0) return false;
        mapper.restoreStock(grab.getSeckillGoodId());
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean release(String seckillNo) {
        SeckillGrabEntity grab = mapper.lockGrab(seckillNo);
        if (grab == null || !"SUCCESS".equals(grab.getStatus())) return false;
        if (mapper.markCancelled(seckillNo) == 0) return false;
        mapper.restoreStock(grab.getSeckillGoodId());
        return true;
    }
}
