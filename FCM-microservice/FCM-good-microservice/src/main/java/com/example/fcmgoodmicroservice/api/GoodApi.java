package com.example.fcmgoodmicroservice.api;

import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.response.JsonResp;
import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmspi.model.GoodEntity;
import com.example.fcmspi.model.StockChangeParam;
import com.example.fcmspi.model.search.GoodSearchBean;
import com.example.fcmspi.service.GoodService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/goods")
public class GoodApi {

    private final GoodService goodService;

    public GoodApi(GoodService goodService) {
        this.goodService = goodService;
    }

    /** 商品分页（关键字/分类/品牌/价格区间/排序/热销，公开） */
    @GetMapping
    public JsonResp page(GoodSearchBean search,
                         @RequestParam(defaultValue = "1") int pageNo,
                         @RequestParam(defaultValue = "0") int pageSize) {
        return JsonResp.ok(goodService.page(search, PaginateInfo.from(pageNo, pageSize)));
    }

    /** 商品详情（full=true 时经 Feign 组装品牌/分类，含详情图） */
    @GetMapping("/id/{id}")
    public JsonResp getById(@PathVariable Long id,
                            @RequestParam(defaultValue = "false") boolean full) {
        return JsonResp.ok(full ? goodService.getDetail(id) : goodService.getById(id));
    }

    /** 按 ID 批量查询（服务间 Feign 使用：下单快照/购物车回显等） */
    @GetMapping("/ids")
    public JsonResp listByIds(@RequestParam List<Long> ids) {
        return JsonResp.ok(goodService.listByIds(ids));
    }

    /** 引用计数（服务间 Feign：品牌/分类删除前校验，brandId 与 categoryId 二选一） */
    @GetMapping("/count")
    public JsonResp count(@RequestParam(required = false) Long brandId,
                          @RequestParam(required = false) Long categoryId) {
        if (brandId != null) {
            return JsonResp.ok(goodService.countByBrandId(brandId));
        }
        if (categoryId != null) {
            return JsonResp.ok(goodService.countByCategoryId(categoryId));
        }
        throw new BizException(ResponseCode.BAD_REQUEST, "brandId 与 categoryId 至少传一个");
    }

    /** 批量扣减库存（服务间 Feign：下单链路，条件更新 qty >= count，不足/已下架整体失败） */
    @PutMapping("/stock/deduct")
    public JsonResp deductStock(@RequestBody List<StockChangeParam> changes) {
        return JsonResp.ok(goodService.deductStock(changes));
    }

    /** 批量回补库存（服务间 Feign：取消/超时关单回补） */
    @PutMapping("/stock/restore")
    public JsonResp restoreStock(@RequestBody List<StockChangeParam> changes) {
        return JsonResp.ok(goodService.restoreStock(changes));
    }

    @PostMapping
    public JsonResp insert(@RequestBody GoodEntity good) {
        return JsonResp.ok(goodService.insert(good));
    }

    @PutMapping
    public JsonResp update(@RequestBody GoodEntity good) {
        return JsonResp.ok(goodService.update(good));
    }

    /** 批量上/下架 */
    @PutMapping("/take-down")
    public JsonResp updateTakeDown(@RequestParam List<Long> ids,
                                   @RequestParam boolean isTakeDown) {
        return JsonResp.ok(goodService.updateTakeDown(ids, isTakeDown));
    }

    /** 批量逻辑删除 */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody List<Long> ids) {
        return JsonResp.ok(goodService.deleteByIds(ids));
    }
}
