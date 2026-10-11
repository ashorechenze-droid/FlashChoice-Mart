package com.example.fcmregionmicroservice.api;

import com.example.fcmcommon.response.JsonResp;
import com.example.fcmspi.service.RegionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/regions")
public class RegionApi {

    private final RegionService regionService;

    public RegionApi(RegionService regionService) {
        this.regionService = regionService;
    }

    /** 行政区划树（省/市/区三级） */
    @GetMapping("/tree")
    public JsonResp tree() {
        return JsonResp.ok(regionService.tree());
    }

    /** 按父级查询子区域（parentId 缺省查省级，用于级联下拉） */
    @GetMapping
    public JsonResp listByParentId(@RequestParam(defaultValue = "0") Long parentId) {
        return JsonResp.ok(regionService.listByParentId(parentId));
    }
}
