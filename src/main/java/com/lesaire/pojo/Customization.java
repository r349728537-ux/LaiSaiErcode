package com.lesaire.pojo;

import jakarta.validation.constraints.*;
/**
 * 定制选配表单；注解校验输入格式，业务层再校验目录存在性和选配组合。
 */
public record Customization(
    // 款式、面料和颜色使用目录提供的业务标识。
    @NotBlank String productId, @NotBlank String fabricId, @NotBlank String colorId,
    // 版型只允许自然合身或宽松自在。
    @NotBlank @Pattern(regexp="regular|relaxed") String fit,
    @NotBlank @Pattern(regexp="standard|long") String length,
    @NotNull Boolean embroidery, @NotBlank @Pattern(regexp="round|v") String neckline,
    // 当前一次定制数量限定为 1 至 5 件。
    @NotNull @Min(1) @Max(5) Integer quantity
) {}
