package com.example.fcmordermicroservice.mapper;

import com.example.fcmspi.model.OrderEntity;
import com.example.fcmspi.model.OrderItemEntity;
import com.example.fcmspi.model.search.OrderSearchBean;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    List<OrderEntity> findAll(OrderSearchBean search);

    OrderEntity findById(Long id);

    OrderEntity findByOrderNo(String orderNo);
    OrderEntity findBySeckillNo(String seckillNo);

    int save(OrderEntity order);

    /** 状态机通用更新：仅更新传入的非空字段 */
    int updateStatus(OrderEntity order);
    int markPaid(OrderEntity order);
    int markCancelled(@Param("id") Long id, @Param("updatedTime") String updatedTime);

    /** 批量逻辑删除（is_del = 1） */
    int deleteByIds(List<Long> ids);

    /** 查询超时（30 分钟）未支付订单号（定时关单逐单回补库存用） */
    List<String> findTimeoutOrderNos();

    /* ===== order_item ===== */

    List<OrderItemEntity> findItemsByOrderId(Long orderId);

    /** 批量查询订单明细（列表装配用，一次 IN 查询避免 N+1） */
    List<OrderItemEntity> findItemsByOrderIds(@Param("orderIds") List<Long> orderIds);

    int saveItems(@Param("items") List<OrderItemEntity> items);
}
