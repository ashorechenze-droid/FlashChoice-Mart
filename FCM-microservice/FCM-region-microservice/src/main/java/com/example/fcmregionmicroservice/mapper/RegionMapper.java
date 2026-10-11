package com.example.fcmregionmicroservice.mapper;

import com.example.fcmspi.model.ChinaRegionEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RegionMapper {

    /** 全部区域（内存组装树用） */
    List<ChinaRegionEntity> findAll();

    /** 按父级查询子区域 */
    List<ChinaRegionEntity> findByParentId(Long parentId);
}
