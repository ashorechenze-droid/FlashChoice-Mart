package com.example.fcmspi.service;

import com.example.fcmspi.model.CategoryEntity;

import java.util.List;

/**
 * 分类业务接口（category-api）：树、CRUD
 */
public interface CategoryService {

    /** 全部数据（供组装下拉/树） */
    List<CategoryEntity> listAll();

    /** 按父级查询子分类 */
    List<CategoryEntity> listByParentId(Long parentId);

    /** 递归组装分类树（根为 parent_id=0） */
    List<CategoryEntity> tree();

    CategoryEntity getById(Long id);

    int insert(CategoryEntity category);

    int update(CategoryEntity category);

    /** 删除（实现层校验子分类与商品引用） */
    int deleteById(Long id);
}
