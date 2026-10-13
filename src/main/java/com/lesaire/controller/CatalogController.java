package com.lesaire.controller;

import com.lesaire.pojo.CatalogView;
import com.lesaire.pojo.Customization;
import com.lesaire.pojo.ProductView;
import com.lesaire.pojo.Quote;
import com.lesaire.service.CatalogService;
import jakarta.validation.Valid;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 目录模块：款式展示、报价和管理员图片上传。 */
@RestController
public class CatalogController {
    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/api/catalog")
    public CatalogView catalog() {
        return catalog.get();
    }

    @PostMapping("/api/quotes")
    public Quote quote(@Valid @RequestBody Customization request) {
        return catalog.quote(request);
    }

    // /api/admin/ 下的接口统一由登录拦截器检查管理员身份。
    @PostMapping(value = "/api/admin/styles/{styleCode}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductView upload(@PathVariable String styleCode, @RequestParam("file") MultipartFile file) {
        return catalog.uploadImage(styleCode, file);
    }

    @GetMapping("/uploads/styles/{filename}")
    public ResponseEntity<Resource> image(@PathVariable String filename) {
        Resource file = new FileSystemResource(catalog.imageFile(filename));
        MediaType type = filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type)
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
            .body(file);
    }
}
