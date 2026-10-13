package com.lesaire.pojo;
import com.lesaire.utils.CatalogUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 订单明细实体，保存款式与面料关联、成交金额、量体 JSON 和定制选项快照。
 */
public class OrderItem {
    private Long id;
    /**
     * 新建时关联主表对象，插入主表后即可取得回填的订单 ID。
     */
    private CustomOrder order;
    /**
     * 数据库读取明细时映射到的订单外键。
     */
    private Long orderId;
    private ShirtStyle style;
    private FabricMaterial fabric;
    /**
     * 下单时保存的款式名称。
     */
    private String productName;
    private int quantity;
    /**
     * 下单时的服务端单价快照。
     */
    private BigDecimal unitPrice;
    /**
     * 该明细的成交金额，等于单价乘以数量。
     */
    private BigDecimal amount;
    /**
     * 下单时的五项量体值，由 TypeHandler 转换为数据库 JSON。
     */
    private Map<String,Object> measurementSnapshot;
    /**
     * 明细制作状态，与订单主表状态更新同步维护。
     */
    private String productionStatus;
    private LocalDateTime createdAt;
    private List<OrderItemOption> options=new ArrayList<>();
    public OrderItem() {}
    /**
     * 依据请求和服务端报价创建明细，复制量体值及选项快照。
     */
    OrderItem(CustomOrder order,OrderRequest request,ShirtStyle style,FabricMaterial fabric,List<CustomizationOption> selections,Quote quote) {
        this.order=order;this.style=style;this.fabric=fabric;productName=style.getName();quantity=request.customization().quantity();
        unitPrice=quote.unitPrice();amount=quote.total();productionStatus="WAITING";createdAt=LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        Measurements m=request.measurements();measurementSnapshot=new LinkedHashMap<>();
        measurementSnapshot.put("height",m.height());measurementSnapshot.put("chest",m.chest());measurementSnapshot.put("waist",m.waist());measurementSnapshot.put("hip",m.hip());measurementSnapshot.put("shoulder",m.shoulder());
        for(CustomizationOption option:selections) options.add(new OrderItemOption(this,option));
    }
    void changeProductionStatus(String status) { productionStatus=status; }
    /**
     * 按分组读取下单时保存的选项值，历史订单缺项时使用兼容默认值。
     */
    private String optionValue(String group,String fallback) {
        return options.stream().filter(o->group.equalsIgnoreCase(o.getGroup())).map(OrderItemOption::getValue).findFirst().orElse(fallback);
    }
    /**
     * 从历史 JSON 中还原精确尺寸，缺失或非法数值按未记录处理。
     */
    private BigDecimal measurement(String name) {
        if(measurementSnapshot==null || measurementSnapshot.get(name)==null) return null;
        try { return new BigDecimal(measurementSnapshot.get(name).toString()); } catch(NumberFormatException ex) { return null; }
    }
    /**
     * 兼容新建时的对象关联和读取时的外键映射。
     */
    public Long getOrderId() { return order == null ? orderId : order.getId(); }
    /**
     * 把实体和快照转换为前端选配、尺寸及展示信息。
     */
    OrderItemView view() {
        String color=optionValue("COLOR",CatalogUtils.colorId(fabric.getColorHex()));
        Customization c=new Customization(style.getCode(),fabric.getCode(),color,optionValue("FIT","regular"),optionValue("LENGTH","standard"),
            Boolean.parseBoolean(optionValue("EMBROIDERY","false")),optionValue("NECKLINE","round"),quantity);
        Measurements m=new Measurements(measurement("height"),measurement("chest"),measurement("waist"),measurement("hip"),measurement("shoulder"));
        return new OrderItemView(id,productName,c,m,unitPrice,amount,fabric.getName(),fabric.getColorName(),fabric.getColorHex(),"shirt",options.stream().map(OrderItemOption::view).toList());
    }

    public void setId(Long value) { id=value; }
    public CustomOrder getOrder() { return order; }
    public void setOrder(CustomOrder value) { order=value; }
    public void setOrderId(Long value) { orderId=value; }
    public ShirtStyle getStyle() { return style; }
    public void setStyle(ShirtStyle value) { style=value; }
    public FabricMaterial getFabric() { return fabric; }
    public void setFabric(FabricMaterial value) { fabric=value; }
    public String getProductName() { return productName; }
    public void setProductName(String value) { productName=value; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int value) { quantity=value; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal value) { unitPrice=value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount=value; }
    public Map<String,Object> getMeasurementSnapshot() { return measurementSnapshot; }
    public void setMeasurementSnapshot(Map<String,Object> value) { measurementSnapshot=value; }
    public String getProductionStatus() { return productionStatus; }
    public void setProductionStatus(String value) { productionStatus=value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt=value; }
    public List<OrderItemOption> getOptions() { return options; }
    public void setOptions(List<OrderItemOption> value) { options=value; }

    public Long getId() { return id; }
}
