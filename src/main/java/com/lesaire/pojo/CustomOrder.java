package com.lesaire.pojo;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 订单主表实体，保存归属、收件资料、成交总额和状态，并组装明细响应。
 */
public class CustomOrder {
    private Long id;
    /**
     * 下单用户的数据库 ID，用于查询范围限制和归属检查。
     */
    private Long accountId;
    /**
     * 根据用户 ID 与请求标识生成的稳定订单号，供重复提交查询。
     */
    private String orderNumber;
    /**
     * 数据库状态值，需与页面使用的 OrderStatus 进行转换。
     */
    private String storedStatus;
    private String recipient;
    private String phone;
    private String address;
    private LocalDate expectedDeliveryDate;
    /**
     * 下单时的服务端报价总额，目录改价后不会重新计算此金额。
     */
    private BigDecimal totalAmount;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /**
     * 订单明细集合，分页读取主表后再批量填充。
     */
    private List<OrderItem> items=new ArrayList<>();
    public CustomOrder() {}
    /**
     * 根据已校验请求和报价创建订单，记录上海时间及预计交付日期。
     */
    public CustomOrder(Long owner,OrderRequest request,ShirtStyle style,FabricMaterial fabric,List<CustomizationOption> options,Quote quote) {
        accountId=owner;orderNumber=requestNumber(owner,request.requestKey());storedStatus="PENDING_CONFIRMATION";
        recipient=request.recipient().strip();phone=request.phone();address=request.address().strip();note=request.note().strip();
        createdAt=LocalDateTime.now(ZoneId.of("Asia/Shanghai"));updatedAt=createdAt;expectedDeliveryDate=createdAt.toLocalDate().plusDays(16);
        totalAmount=quote.total();items.add(new OrderItem(this,request,style,fabric,options,quote));
    }
    /**
     * 对用户 ID 和请求标识的组合做 SHA-256 摘要，同一用户重复请求得到同一订单号。
     */
    public static String requestNumber(Long owner,String requestKey) {
        try { byte[] hash=MessageDigest.getInstance("SHA-256").digest((owner+":"+requestKey).getBytes(StandardCharsets.UTF_8)); return "LS"+HexFormat.of().formatHex(hash).substring(0,30).toUpperCase(); }
        catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    /**
     * 将已有数据库状态映射到页面流程；历史 DRAFT 也归入待确认。
     */
    public OrderStatus getStatus() {
        return switch(storedStatus) {
            case "DRAFT","PENDING_CONFIRMATION"->OrderStatus.PENDING;case "CONFIRMED"->OrderStatus.CUTTING;
            case "IN_PRODUCTION"->OrderStatus.SEWING;case "QUALITY_CHECK"->OrderStatus.QUALITY;
            case "SHIPPED"->OrderStatus.SHIPPED;case "COMPLETED"->OrderStatus.COMPLETED;case "CANCELLED"->OrderStatus.CANCELLED;
            default->throw new IllegalStateException("无法识别的订单状态："+storedStatus);
        };
    }
    /**
     * 更新数据库状态、更新时间，并同步内存中各明细的制作状态。
     */
    public void changeStatus(OrderStatus next) {
        storedStatus=switch(next) {case PENDING->"PENDING_CONFIRMATION";case CUTTING->"CONFIRMED";case SEWING->"IN_PRODUCTION";case QUALITY->"QUALITY_CHECK";default->next.name();};
        updatedAt=LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        for(OrderItem item:items) item.changeProductionStatus(next==OrderStatus.PENDING ? "WAITING" : next.name());
    }
    /**
     * 返回完整明细集合，同时保留首个明细的摘要字段以兼容当前订单卡片。
     */
    public OrderView view() {
        List<OrderItemView> details=items.stream().map(OrderItem::view).toList();
        OrderItemView first=details.isEmpty() ? OrderItemView.missing(totalAmount) : details.get(0);
        return new OrderView(id,orderNumber,first.productName(),first.customization(),first.measurements(),recipient,phone,address,note==null?"":note,
            first.unitPrice(),totalAmount,getStatus(),createdAt,updatedAt,first.fabricName(),first.colorName(),first.colorHex(),first.previewType(),details);
    }

    public void setId(Long value) { id=value; }
    public void setAccountId(Long value) { accountId=value; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String value) { orderNumber=value; }
    public String getStoredStatus() { return storedStatus; }
    public void setStoredStatus(String value) { storedStatus=value; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String value) { recipient=value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone=value; }
    public String getAddress() { return address; }
    public void setAddress(String value) { address=value; }
    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public void setExpectedDeliveryDate(LocalDate value) { expectedDeliveryDate=value; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal value) { totalAmount=value; }
    public String getNote() { return note; }
    public void setNote(String value) { note=value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt=value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt=value; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> value) { items=value; }
}
