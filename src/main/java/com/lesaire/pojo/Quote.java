package com.lesaire.pojo;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
/**
 * 报价结果，分别返回基础价、面料加价、选项加价、单价、数量和总价。
 */
public record Quote(BigDecimal basePrice,BigDecimal fabricPrice,BigDecimal detailPrice,BigDecimal unitPrice,int quantity,BigDecimal total,String delivery) {}
