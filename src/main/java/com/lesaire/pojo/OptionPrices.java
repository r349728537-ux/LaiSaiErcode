package com.lesaire.pojo;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
/**
 * 页面展示的加长款和刺绣加价，实际下单仍由服务端重新核算。
 */
public record OptionPrices(BigDecimal longPrice,BigDecimal embroideryPrice) {}
