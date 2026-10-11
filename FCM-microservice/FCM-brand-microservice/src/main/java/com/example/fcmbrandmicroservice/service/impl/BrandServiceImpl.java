package com.example.fcmbrandmicroservice.service.impl;

import com.example.fcmbrandmicroservice.feign.GoodFeignClient;
import com.example.fcmbrandmicroservice.mapper.BrandMapper;
import com.example.fcmcommon.constant.CommonConstants;
import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmcommon.response.TypedJsonResp;
import com.example.fcmspi.model.BrandEntity;
import com.example.fcmspi.model.search.BrandSearchBean;
import com.example.fcmspi.service.BrandService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class BrandServiceImpl implements BrandService {

    private final BrandMapper brandMapper;
    private final GoodFeignClient goodFeignClient;

    public BrandServiceImpl(BrandMapper brandMapper, GoodFeignClient goodFeignClient) {
        this.brandMapper = brandMapper;
        this.goodFeignClient = goodFeignClient;
    }

    @Override
    public PageInfo<BrandEntity> page(BrandSearchBean search, PaginateInfo page) {
        PageHelper.startPage(page.getPageNo(), page.getPageSize());
        return new PageInfo<>(brandMapper.findAll(search));
    }

    @Override
    public List<BrandEntity> list(BrandSearchBean search) {
        return brandMapper.findAll(search);
    }

    @Override
    public BrandEntity getById(Long id) {
        return brandMapper.findById(id);
    }

    @Override
    public BrandEntity getByName(String name) {
        BrandSearchBean search = new BrandSearchBean();
        search.setName(name);
        List<BrandEntity> list = brandMapper.findAll(search);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public int insert(BrandEntity brand) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern(CommonConstants.DATE_TIME_PATTERN));
        brand.setCreatedTime(now);
        brand.setUpdatedTime(now);
        // createdBy/updatedBy 后续由登录上下文（X-User-Name）填充
        return brandMapper.save(brand);
    }

    @Override
    public int update(BrandEntity brand) {
        brand.setUpdatedTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern(CommonConstants.DATE_TIME_PATTERN)));
        return brandMapper.update(brand);
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        // 删除前校验商品引用：任一品牌下存在未删除商品即拒绝
        for (Long id : ids) {
            if (countGoods(id) > 0) {
                throw new BizException(ResponseCode.BAD_REQUEST, "品牌（ID: " + id + "）下存在商品，无法删除");
            }
        }
        return brandMapper.deleteByIds(ids);
    }

    /** 商品引用计数；远程不可用时阻断删除，避免误删仍被引用的品牌 */
    private int countGoods(Long brandId) {
        try {
            TypedJsonResp<Integer> resp = goodFeignClient.countByBrandId(brandId);
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess()) || resp.getData() == null) {
                throw new BizException(ResponseCode.BAD_REQUEST, "商品服务不可用，暂时无法删除品牌");
            }
            return resp.getData();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("商品服务 Feign 调用失败 brandId={}: {}", brandId, e.getMessage());
            throw new BizException(ResponseCode.BAD_REQUEST, "商品服务不可用，暂时无法删除品牌");
        }
    }
}
