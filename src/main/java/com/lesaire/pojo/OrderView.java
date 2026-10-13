package com.lesaire.pojo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
/**
 * 订单响应，同时提供首个明细摘要和完整 items 集合，兼容单明细与历史多明细订单。
 */
public record OrderView(Long id,String orderNumber,String productName,Customization customization,Measurements measurements,
    String recipient,String phone,String address,String note,BigDecimal unitPrice,BigDecimal total,OrderStatus status,LocalDateTime createdAt,LocalDateTime updatedAt,
    String fabricName,String colorName,String colorHex,String previewType,List<OrderItemView> items) {}
