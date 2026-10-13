package com.lesaire.pojo;

import java.math.BigDecimal;

/**
 * 明细定制选项快照，保存下单时的分组、名称、值和加价。
 */
public class OrderItemOption {
    private Long id;
    /**
     * 新建时关联明细对象，使用插入后回填的明细 ID。
     */
    private OrderItem item;
    /**
     * 从数据库加载快照时使用的明细外键。
     */
    private Long orderItemId;
    /**
     * 新建快照时关联原始选项，以取得选项主键。
     */
    private CustomizationOption option;
    private Long customizationOptionId;
    private String group;
    private String name;
    private String value;
    /**
     * 成交时的选项加价快照，后续目录改价不会改变此值。
     */
    private BigDecimal priceDelta;
    public OrderItemOption() {}
    /**
     * 把目录选项的展示字段和价格复制到订单快照中。
     */
    OrderItemOption(OrderItem item,CustomizationOption option) {
        this.item=item;this.option=option;group=option.getGroup();name=option.getName();value=option.getValue();priceDelta=option.getPriceDelta();
    }
    public String getGroup() { return group; }
    public String getValue() { return value; }
    public Long getOrderItemId() { return item == null ? orderItemId : item.getId(); }
    public Long getCustomizationOptionId() { return option == null ? customizationOptionId : option.getId(); }
    /**
     * 返回快照展示数据，避免重新按目录选项价格计算历史订单。
     */
    OrderItemView.OptionSnapshot view() { return new OrderItemView.OptionSnapshot(group,name,value,priceDelta); }

    public void setId(Long value) { id=value; }
    public OrderItem getItem() { return item; }
    public void setItem(OrderItem value) { item=value; }
    public void setOrderItemId(Long value) { orderItemId=value; }
    public CustomizationOption getOption() { return option; }
    public void setOption(CustomizationOption value) { option=value; }
    public void setCustomizationOptionId(Long value) { customizationOptionId=value; }
    public void setGroup(String value) { group=value; }
    public void setName(String value) { name=value; }
    public void setValue(String value) { this.value=value; }
    public void setPriceDelta(BigDecimal value) { priceDelta=value; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPriceDelta() { return priceDelta; }
}
