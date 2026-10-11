package com.example.fcmcollectmicroservice.service.impl;

import com.example.fcmcollectmicroservice.feign.GoodFeignClient;
import com.example.fcmcollectmicroservice.mapper.CollectMapper;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.response.TypedJsonResp;
import com.example.fcmspi.model.CollectEntity;
import com.example.fcmspi.model.GoodEntity;
import com.example.fcmspi.service.CollectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CollectServiceImpl implements CollectService {

    private final CollectMapper collectMapper;
    private final GoodFeignClient goodFeignClient;

    public CollectServiceImpl(CollectMapper collectMapper, GoodFeignClient goodFeignClient) {
        this.collectMapper = collectMapper;
        this.goodFeignClient = goodFeignClient;
    }

    @Override
    public List<CollectEntity> listByMemberId(Long memberId) {
        List<CollectEntity> collects = collectMapper.findByMemberId(memberId);
        // 经 Feign 批量回显商品信息；商品服务不可用时降级为 good=null，不影响列表主体
        fillGoods(collects);
        return collects;
    }

    /** 按 goodId 批量回显商品信息（Feign 调用失败/返回异常时跳过填充） */
    private void fillGoods(List<CollectEntity> collects) {
        if (collects == null || collects.isEmpty()) {
            return;
        }
        List<Long> goodIds = collects.stream()
                .map(CollectEntity::getGoodId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (goodIds.isEmpty()) {
            return;
        }
        try {
            TypedJsonResp<List<GoodEntity>> resp = goodFeignClient.listByIds(goodIds);
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess()) || resp.getData() == null) {
                return;
            }
            Map<Long, GoodEntity> goodMap = resp.getData().stream()
                    .collect(Collectors.toMap(GoodEntity::getId, Function.identity(), (a, b) -> a));
            for (CollectEntity collect : collects) {
                collect.setGood(goodMap.get(collect.getGoodId()));
            }
        } catch (Exception e) {
            log.warn("商品服务 Feign 调用失败，跳过收藏商品回显: {}", e.getMessage());
        }
    }

    @Override
    public boolean isCollected(Long memberId, Long goodId) {
        if (memberId == null || goodId == null) {
            return false;
        }
        return collectMapper.findByMemberAndGood(memberId, goodId) != null;
    }

    @Override
    public int add(CollectEntity collect) {
        if (collect.getMemberId() == null || collect.getGoodId() == null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "会员与商品不能为空");
        }
        // 已收藏则幂等返回，避免重复插入触发唯一索引冲突
        CollectEntity exists = collectMapper.findByMemberAndGood(collect.getMemberId(), collect.getGoodId());
        if (exists != null) {
            return 1;
        }
        // 经 Feign 校验商品可收藏（存在、未下架；远程异常时降级放行）
        validateGood(collect.getGoodId());
        return collectMapper.save(collect);
    }

    /** 收藏校验：商品存在、未下架、未删除；远程调用异常时降级放行 */
    private void validateGood(Long goodId) {
        TypedJsonResp<GoodEntity> resp;
        try {
            resp = goodFeignClient.getById(goodId);
        } catch (Exception e) {
            log.warn("商品服务 Feign 调用失败，跳过收藏校验 goodId={}: {}", goodId, e.getMessage());
            return;
        }
        if (resp == null || !Boolean.TRUE.equals(resp.getSuccess())) {
            log.warn("商品服务返回异常，跳过收藏校验 goodId={}", goodId);
            return;
        }
        GoodEntity good = resp.getData();
        if (good == null) {
            throw new BizException(ResponseCode.NOT_FOUND, "商品不存在");
        }
        if (Boolean.TRUE.equals(good.getIsTakeDown()) || Boolean.TRUE.equals(good.getIsDel())) {
            throw new BizException(ResponseCode.BAD_REQUEST, "商品已下架");
        }
    }

    @Override
    public int deleteByIds(Long memberId, List<Long> ids) {
        if (memberId == null || ids == null || ids.isEmpty()) {
            throw new BizException(ResponseCode.BAD_REQUEST, "缺少会员或收藏条目");
        }
        return collectMapper.deleteByIds(memberId, ids);
    }

    @Override
    public int deleteByGoodId(Long memberId, Long goodId) {
        if (memberId == null || goodId == null) {
            throw new BizException(ResponseCode.BAD_REQUEST, "缺少会员或商品");
        }
        return collectMapper.deleteByMemberAndGood(memberId, goodId);
    }

    @Override
    public List<Long> listMemberIdsByGoodId(Long goodId) {
        if (goodId == null) {
            return List.of();
        }
        return collectMapper.findMemberIdsByGoodId(goodId);
    }
}
