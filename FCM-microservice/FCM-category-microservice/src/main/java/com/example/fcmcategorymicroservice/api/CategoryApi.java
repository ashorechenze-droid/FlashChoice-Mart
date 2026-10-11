package com.example.fcmcategorymicroservice.api;

import com.example.fcmcommon.response.JsonResp;
import com.example.fcmspi.model.CategoryEntity;
import com.example.fcmspi.service.CategoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/categories")
public class CategoryApi {

    private final CategoryService categoryService;

    public CategoryApi(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** 全部分类（平铺，供后台管理表格） */
    @GetMapping
    public JsonResp listAll() {
        return JsonResp.ok(categoryService.listAll());
    }

    /** 分类树（前台导航/后台树选择） */
    @GetMapping("/tree")
    public JsonResp tree() {
        return JsonResp.ok(categoryService.tree());
    }

    /** 按父级查询子分类 */
    @GetMapping("/parent/{parentId}")
    public JsonResp listByParentId(@PathVariable Long parentId) {
        return JsonResp.ok(categoryService.listByParentId(parentId));
    }

    @GetMapping("/id/{id}")
    public JsonResp getById(@PathVariable Long id) {
        return JsonResp.ok(categoryService.getById(id));
    }

    @PostMapping
    public JsonResp insert(@RequestBody CategoryEntity category) {
        return JsonResp.ok(categoryService.insert(category));
    }

    @PutMapping
    public JsonResp update(@RequestBody CategoryEntity category) {
        return JsonResp.ok(categoryService.update(category));
    }

    @DeleteMapping("/{id}")
    public JsonResp deleteById(@PathVariable Long id) {
        return JsonResp.ok(categoryService.deleteById(id));
    }
}
