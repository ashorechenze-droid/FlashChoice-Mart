package com.example.fcmbrandmicroservice.mapper;

import com.example.fcmspi.model.BrandEntity;
import com.example.fcmspi.model.search.BrandSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BrandMapper {

    List<BrandEntity> findAll(BrandSearchBean search);

    BrandEntity findById(Long id);

    int save(BrandEntity brand);

    int update(BrandEntity brand);

    int deleteByIds(List<Long> ids);
}
