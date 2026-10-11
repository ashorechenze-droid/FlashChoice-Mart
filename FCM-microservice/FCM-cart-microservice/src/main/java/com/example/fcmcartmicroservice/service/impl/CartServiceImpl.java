package com.example.fcmcartmicroservice.service.impl;

import com.example.fcmcartmicroservice.feign.GoodFeignClient;
import com.example.fcmcartmicroservice.mapper.CartMapper;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.response.TypedJsonResp;
import com.example.fcmspi.model.CartEntity;
import com.example.fcmspi.model.GoodEntity;
import com.example.fcmspi.service.CartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final GoodFeignClient goodFeignClient;

    public CartServiceImpl(CartMapper cartMapper, GoodFeignClient goodFeignClient) {
        this.cartMapper = cartMapper;
        this.goodFeignClient = goodFeignClient;
    }

    @Override
    public List<CartEntity> listByMemberId(Long memberId) {
        List<CartEntity> carts = cartMapper.findByMemberId(memberId);
        // 经 Feign 批量回显商品信息；商品服务不可用时降级为 good=null，不影响列表主体
        fillGoods(carts);
        return carts;
    }

    /** 按 goodId 批量回显商品信息（Feign 调用失败/返回异常时跳过填充） */
    private void fillGoods(List<CartEntity> carts) {
        if (carts == null || carts.isEmpty()) {
            return;
        }
        List<Long> goodIds = carts.stream()
                .map(CartEntity::getGoodId)
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
            for (CartEntity cart : carts) {
                cart.setGood(goodMap.get(cart.getGoodId()));
            }
        } catch (Exception e) {
            log.warn("商品服务 Feign 调用失败，跳过购物车商品回显: {}", e.getMessage());
        }
    }

    @Override
    public CartEntity getById(Long id) {
        return cartMapper.findById(id);
    }

    /** 按 ID 批量查询（不做商品回显；下单结算只取 goodId/qty/memberId） */
    @Override
    public List<CartEntity> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return cartMapper.findByIds(ids);
    }

    @Override
    public int add(CartEntity cart) {
        if (cart.getMemberId() == null || cart.getGoodId() == null || cart.getQty() == null || cart.getQty() < 1) {
            throw new BizException(ResponseCode.BAD_REQUEST, "会员、商品与数量不能为空且数量需大于 0");
        }
        CartEntity exists = cartMapper.findByMemberAndGood(cart.getMemberId(), cart.getGoodId());
        // 经 Feign 校验商品可售（存在/已上架/库存上限，按加入后的总数量校验）
        validateGood(cart.getGoodId(), (exists != null ? exists.getQty() : 0) + cart.getQty());
        if (exists != null) {
            // 已存在则累加数量
            return cartMapper.updateQty(exists.getId(), cart.getMemberId(), exists.getQty() + cart.getQty());
        }
        return cartMapper.save(cart);
    }

    /** 加购校验：商品存在、已上架、库存充足；远程调用异常时降级放行（由下单链路兜底校验） */
    private void validateGood(Long goodId, int totalQty) {
        TypedJsonResp<GoodEntity> resp;
        try {
            resp = goodFeignClient.getById(goodId);
        } catch (Exception e) {
            log.warn("商品服务 Feign 调用失败，跳过加购校验 goodId={}: {}", goodId, e.getMessage());
            return;
        }
        if (resp == null || !Boolean.TRUE.equals(resp.getSuccess())) {
            log.warn("商品服务返回异常，跳过加购校验 goodId={}", goodId);
            return;
        }
        GoodEntity good = resp.getData();
        if (good == null) {
            throw new BizException(ResponseCode.NOT_FOUND, "商品不存在");
        }
        if (Boolean.TRUE.equals(good.getIsTakeDown()) || Boolean.TRUE.equals(good.getIsDel())) {
            throw new BizException(ResponseCode.BAD_REQUEST, "商品已下架");
        }
        if (good.getQty() != null && totalQty > good.getQty()) {
            throw new BizException(ResponseCode.STOCK_NOT_ENOUGH, "库存不足，最多可购 " + good.getQty() + " 件");
        }
    }

    @Override
    public int updateQty(CartEntity cart) {
        if (cart.getId() == null || cart.getQty() == null || cart.getQty() < 1) {
            throw new BizException(ResponseCode.BAD_REQUEST, "缺少 id 或数量需大于 0");
        }
        if (cart.getMemberId() == null) {
            // 归属校验依赖 memberId，缺失时先从库中取
            CartEntity exists = cartMapper.findById(cart.getId());
            if (exists == null) {
                throw new BizException(ResponseCode.NOT_FOUND, "购物车条目不存在");
            }
            cart.setMemberId(exists.getMemberId());
        }
        int rows = cartMapper.updateQty(cart.getId(), cart.getMemberId(), cart.getQty());
        if (rows == 0) {
            throw new BizException(ResponseCode.FORBIDDEN, "无权操作他人购物车");
        }
        return rows;
    }

    @Override
    public int deleteByIds(Long memberId, List<Long> ids) {
        if (memberId == null || ids == null || ids.isEmpty()) {
            throw new BizException(ResponseCode.BAD_REQUEST, "缺少会员或购物车条目");
        }
        return cartMapper.deleteByIds(memberId, ids);
    }
}
