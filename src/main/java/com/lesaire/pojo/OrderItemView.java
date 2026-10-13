package com.lesaire.pojo;

import java.math.BigDecimal;
import java.util.List;
/**
 * 前端订单明细响应，包含选配、量体、成交金额以及选项快照。
 */
public record OrderItemView(Long id,String productName,Customization customization,Measurements measurements,BigDecimal unitPrice,BigDecimal total,
    String fabricName,String colorName,String colorHex,String previewType,List<OptionSnapshot> options) {
    /**
     * 订单提交时保存的定制选项内容及单件加价。
     */
    public record OptionSnapshot(String group,String name,String value,BigDecimal priceDelta) {}
    /**
     * 为缺少明细的历史订单提供展示占位，保留其原有总金额。
     */
    static OrderItemView missing(BigDecimal total) {
        return new OrderItemView(null,"待补充款式的定制订单",new Customization("archived","archived","cloud","regular","standard",false,"round",1),
            new Measurements(null,null,null,null,null),total,total,"未记录面料","未记录配色","#E5E8E4","shirt",List.of());
    }
}
