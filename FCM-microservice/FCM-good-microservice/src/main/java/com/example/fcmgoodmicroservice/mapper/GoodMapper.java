package com.example.fcmgoodmicroservice.mapper;

import com.example.fcmspi.model.GoodDetailPicEntity;
import com.example.fcmspi.model.GoodEntity;
import com.example.fcmspi.model.search.GoodSearchBean;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GoodMapper {

    List<GoodEntity> findAll(GoodSearchBean search);

    GoodEntity findById(Long id);

    List<GoodEntity> findByIds(List<Long> ids);

    int save(GoodEntity good);

    int update(GoodEntity good);

    int updateTakeDown(@Param("ids") List<Long> ids, @Param("isTakeDown") Boolean isTakeDown);

    /** 批量逻辑删除（is_del = 1） */
    int deleteByIds(List<Long> ids);

    /** 条件扣减库存（在售且 qty >= count 才生效，影响 0 行表示不足/不可售） */
    int deductStock(@Param("goodId") Long goodId, @Param("count") Integer count);

    /** 回补库存（取消/超时关单；商品不存在时影响 0 行） */
    int restoreStock(@Param("goodId") Long goodId, @Param("count") Integer count);

    /** 统计引用指定品牌的商品数（is_del=0，删除品牌前校验） */
    int countByBrandId(Long brandId);

    /** 统计引用指定分类的商品数（is_del=0，删除分类前校验） */
    int countByCategoryId(Long categoryId);

    /* ===== good_detail_pics（详情图） ===== */

    List<GoodDetailPicEntity> findDetailPics(Long goodId);

    int deleteDetailPicsByGoodId(Long goodId);

    int saveDetailPics(@Param("pics") List<GoodDetailPicEntity> pics);
}
