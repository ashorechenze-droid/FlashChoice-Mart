package com.example.fcmregionmicroservice.service.impl;

import com.example.fcmregionmicroservice.mapper.RegionMapper;
import com.example.fcmspi.model.ChinaRegionEntity;
import com.example.fcmspi.service.RegionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class RegionServiceImpl implements RegionService {

    private final RegionMapper regionMapper;

    public RegionServiceImpl(RegionMapper regionMapper) {
        this.regionMapper = regionMapper;
    }

    @Override
    public List<ChinaRegionEntity> listByParentId(Long parentId) {
        return regionMapper.findByParentId(parentId);
    }

    @Override
    public List<ChinaRegionEntity> tree() {
        // 区划数据量不大，一次全量加载后内存组装（根为 parent_id=0）
        return buildTree(regionMapper.findAll(), 0L);
    }

    private List<ChinaRegionEntity> buildTree(List<ChinaRegionEntity> all, Long parentId) {
        List<ChinaRegionEntity> nodes = new ArrayList<>();
        for (ChinaRegionEntity region : all) {
            if (Objects.equals(parentId, region.getParentId())) {
                region.setChildren(buildTree(all, region.getId()));
                nodes.add(region);
            }
        }
        return nodes;
    }
}
