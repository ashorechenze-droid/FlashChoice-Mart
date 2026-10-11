package com.example.fcmspi.service;

import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmspi.model.BrandEntity;
import com.example.fcmspi.model.search.BrandSearchBean;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 品牌业务接口（brand-api）：CRUD、名称/公司模糊分页
 */
public interface BrandService {

    /** 分页查询（名称/公司模糊，SearchBean 附加条件） */
    PageInfo<BrandEntity> page(BrandSearchBean search, PaginateInfo page);

    /** 按条件查询全部 */
    List<BrandEntity> list(BrandSearchBean search);

    BrandEntity getById(Long id);

    BrandEntity getByName(String name);

    /** 新增（实现层填充审计字段） */
    int insert(BrandEntity brand);

    int update(BrandEntity brand);

    /** 批量删除（实现层校验商品引用） */
    int deleteByIds(List<Long> ids);
}
