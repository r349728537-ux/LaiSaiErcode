package com.lesaire.pojo;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;
/**
 * 量体表单，单位为厘米；尺寸使用 BigDecimal，最多保留两位小数。
 * 各字段的范围校验与当前定制页面对应。
 */
public record Measurements(
    @NotNull @DecimalMin("130") @DecimalMax("210") @Digits(integer=3,fraction=2) BigDecimal height,
    @NotNull @DecimalMin("60") @DecimalMax("150") @Digits(integer=3,fraction=2) BigDecimal chest,
    @NotNull @DecimalMin("45") @DecimalMax("150") @Digits(integer=3,fraction=2) BigDecimal waist,
    @NotNull @DecimalMin("60") @DecimalMax("160") @Digits(integer=3,fraction=2) BigDecimal hip,
    @NotNull @DecimalMin("25") @DecimalMax("65") @Digits(integer=2,fraction=2) BigDecimal shoulder
) {
    /**
     * 便于演示数据和测试使用整数尺寸，统一转换为精确小数。
     */
    public Measurements(int height,int chest,int waist,int hip,int shoulder) {
        this(BigDecimal.valueOf(height),BigDecimal.valueOf(chest),BigDecimal.valueOf(waist),BigDecimal.valueOf(hip),BigDecimal.valueOf(shoulder));
    }
}
