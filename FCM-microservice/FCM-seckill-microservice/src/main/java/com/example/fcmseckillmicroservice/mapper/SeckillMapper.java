package com.example.fcmseckillmicroservice.mapper;

import com.example.fcmspi.model.SeckillEntity;
import com.example.fcmspi.model.SeckillGoodEntity;
import com.example.fcmspi.model.SeckillGrabEntity;
import com.example.fcmspi.model.search.SeckillSearchBean;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SeckillMapper {

    /* ===== 秒杀活动 ===== */

    List<SeckillEntity> findAll(SeckillSearchBean search);

    /** 前台活动列表：enabled=1 且未结束（进行中/即将开始） */
    List<SeckillEntity> findActive();

    SeckillEntity findById(Long id);

    int save(SeckillEntity seckill);

    int update(SeckillEntity seckill);

    int updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);

    int deleteById(Long id);

    /* ===== 秒杀活动商品 ===== */

    List<SeckillGoodEntity> findGoodsBySeckillId(Long seckillId);

    SeckillGoodEntity findGoodById(Long id);
    SeckillGoodEntity lockGood(Long id);

    /** 按活动+商品查重（seckill_id + good_id 唯一） */
    SeckillGoodEntity findGoodBySeckillAndGood(@Param("seckillId") Long seckillId, @Param("goodId") Long goodId);

    int saveGood(SeckillGoodEntity seckillGood);

    int updateGood(SeckillGoodEntity seckillGood);

    int deleteGoodById(Long id);

    /** 删除活动时级联删除其活动商品 */
    int deleteGoodsBySeckillId(Long seckillId);

    int reserveStock(Long id);
    int restoreStock(Long id);
    int insertGrab(SeckillGrabEntity grab);
    SeckillGrabEntity findGrab(String seckillNo);
    SeckillGrabEntity lockGrab(String seckillNo);
    int countMemberGrabs(@Param("memberId") Long memberId, @Param("seckillGoodId") Long seckillGoodId,
                         @Param("seckillNo") String seckillNo);
    int markProcessing(String seckillNo);
    int markSuccess(@Param("seckillNo") String seckillNo, @Param("orderNo") String orderNo);
    int markFailed(String seckillNo);
    int markCancelled(String seckillNo);
    SeckillGrabEntity findByOrderNo(String orderNo);
    List<String> findCancelledSuccessGrabs();
    List<String> findRetryableGrabs();
    int touchRetry(String seckillNo);
}
