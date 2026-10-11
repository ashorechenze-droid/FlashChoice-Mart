package com.example.fcmbrandmicroservice.api;

import com.example.fcmcommon.response.JsonResp;
import com.example.fcmcommon.response.PaginateInfo;
import com.example.fcmspi.model.BrandEntity;
import com.example.fcmspi.model.search.BrandSearchBean;
import com.example.fcmspi.service.BrandService;
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
@RequestMapping("api/brands")
public class BrandApi {

    private final BrandService brandService;

    public BrandApi(BrandService brandService) {
        this.brandService = brandService;
    }

    /** 品牌分页（名称/公司模糊） */
    @GetMapping
    public JsonResp page(BrandSearchBean search,
                         @RequestParam(defaultValue = "1") int pageNo,
                         @RequestParam(defaultValue = "0") int pageSize) {
        return JsonResp.ok(brandService.page(search, PaginateInfo.from(pageNo, pageSize)));
    }

    @GetMapping("/id/{id}")
    public JsonResp getById(@PathVariable Long id) {
        return JsonResp.ok(brandService.getById(id));
    }

    @PostMapping
    public JsonResp insert(@RequestBody BrandEntity brand) {
        return JsonResp.ok(brandService.insert(brand));
    }

    @PutMapping
    public JsonResp update(@RequestBody BrandEntity brand) {
        return JsonResp.ok(brandService.update(brand));
    }

    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody List<Long> ids) {
        return JsonResp.ok(brandService.deleteByIds(ids));
    }
}
