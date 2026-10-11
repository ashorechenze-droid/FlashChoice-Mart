package com.example.fcmgoodmicroservice.feign;

import com.example.fcmcommon.response.TypedJsonResp;
import com.example.fcmspi.model.CategoryEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 分类服务 Feign 客户端（good-api 组装商品分类、递归子分类查询用）
 * 对应 FCM-category-microservice 的 GET /api/categories/*
 */
@FeignClient(name = "FCM-category-microservice", path = "api/categories")
public interface CategoryFeignClient {

    /** 分类详情（data 为 null 表示不存在） */
    @GetMapping("/id/{id}")
    TypedJsonResp<CategoryEntity> getById(@PathVariable Long id);

    /** 分类树（根分类嵌套 children，用于递归收集子分类 ID） */
    @GetMapping("/tree")
    TypedJsonResp<List<CategoryEntity>> tree();
}
