package com.lesaire.pojo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户量体档案实体，对应 customer_measurement 表，尺寸使用精确小数保存。
 */
public class CustomerMeasurement {
    private Long id;
    /**
     * 新建档案时关联用户对象，以便取得数据库回填的用户 ID。
     */
    private Account account;
    /**
     * 从数据库读取档案时使用的客户外键。
     */
    private Long customerId;
    /**
     * 当前表单包含身高、胸围、腰围、臀围和肩宽，单位为厘米。
     */
    private BigDecimal height;
    private BigDecimal chest;
    private BigDecimal waist;
    private BigDecimal hip;
    private BigDecimal shoulder;
    private LocalDateTime updatedAt;
    public CustomerMeasurement() {}
    CustomerMeasurement(Account account) { this.account=account; }

    /**
     * 仅更新当前表单的五项尺寸；数据库中的其他量体字段由 SQL 保留。
     */
    void update(Measurements m) { height=m.height(); chest=m.chest(); waist=m.waist(); hip=m.hip(); shoulder=m.shoulder(); }
    /**
     * 写入时优先取关联账号的 ID，读取后则使用映射到的外键值。
     */
    public Long getCustomerId() { return account == null ? customerId : account.getId(); }
    /**
     * 任一必填尺寸缺失时返回空档案，避免向页面提供不完整表单。
     */
    public Measurements view() { return height == null || chest == null || waist == null || hip == null || shoulder == null ? null : new Measurements(height,chest,waist,hip,shoulder); }

    public void setId(Long value) { id=value; }
    public Account getAccount() { return account; }
    public void setAccount(Account value) { account=value; }
    public void setCustomerId(Long value) { customerId=value; }
    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal value) { height=value; }
    public BigDecimal getChest() { return chest; }
    public void setChest(BigDecimal value) { chest=value; }
    public BigDecimal getWaist() { return waist; }
    public void setWaist(BigDecimal value) { waist=value; }
    public BigDecimal getHip() { return hip; }
    public void setHip(BigDecimal value) { hip=value; }
    public BigDecimal getShoulder() { return shoulder; }
    public void setShoulder(BigDecimal value) { shoulder=value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt=value; }

    public Long getId() { return id; }
}
