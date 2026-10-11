package com.example.fcmspi.service;

import com.example.fcmspi.model.ChinaRegionEntity;

import java.util.List;

/**
 * 行政区划业务接口（region-api）：树形查询（level 1/2/3）
 */
public interface RegionService {

    /** 按父级查询子区域 */
    List<ChinaRegionEntity> listByParentId(Long parentId);

    /** 递归组装地区树（根为 parent_id=0） */
    List<ChinaRegionEntity> tree();
}
