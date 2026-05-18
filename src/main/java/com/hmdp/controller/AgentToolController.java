package com.hmdp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Shop;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IShopService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.SystemConstants;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/agent/tools")
public class AgentToolController {

    @Resource
    private IShopService shopService;

    @Resource
    private IVoucherService voucherService;

    @Resource
    private IBlogService blogService;

    @GetMapping("/shops/search")
    public Result searchShops(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "typeId", required = false) Long typeId,
            @RequestParam(value = "minScore", required = false) Integer minScore,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        return shopService.search(keyword, typeId, minScore, current);
    }

    @GetMapping("/shops/detail")
    public Result shopDetail(@RequestParam("id") Long id) {
        return shopService.queryById(id);
    }

    @GetMapping("/vouchers")
    public Result vouchers(@RequestParam("shopId") Long shopId) {
        return voucherService.queryVoucherOfShop(shopId);
    }

    @GetMapping("/blogs/hot")
    public Result hotBlogs(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        Page<Blog> page = blogService.query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        return Result.ok(page.getRecords());
    }
}
