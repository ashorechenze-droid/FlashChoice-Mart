package com.example.fcmcategorymicroservice.mapper;

import com.example.fcmspi.model.CategoryEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryMapper {

    /** 全部数据（parentId 为空查全部，否则查该父级下的子分类） */
    List<CategoryEntity> findAll(Long parentId);

    CategoryEntity findById(Long id);

    /** 统计子分类数量（删除前校验） */
    int countByParentId(Long parentId);

    int save(CategoryEntity category);

    int update(CategoryEntity category);

    int deleteById(Long id);
}
