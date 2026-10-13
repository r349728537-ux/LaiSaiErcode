package com.lesaire.pojo;

import java.time.LocalDateTime;

/**
 * 用户表实体，包含登录资料、当前权限和关联量体档案，供 MyBatis 映射使用。
 */
public class Account {
    private Long id;
    /**
     * 登录用户名；手机号注册时默认与手机号相同。
     */
    private String username;
    /**
     * 数据库保存的 BCrypt 哈希，仅用于密码校验。
     */
    private String passwordHash;
    private String displayName;
    private String phone;
    /**
     * 数据库中的当前角色，管理员接口权限以此字段为准。
     */
    private String role;
    /**
     * 账号是否启用；停用账号即使持有有效 JWT 也不能访问受保护接口。
     */
    private boolean enabled = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /**
     * 独立量体档案表的关联对象，查询时可能为空。
     */
    private CustomerMeasurement measurement;
    public Account() {}
    public Account(String phone, String passwordHash, String displayName, String role) {
        username=phone; this.phone=phone; this.passwordHash=passwordHash; this.displayName=displayName; this.role=role;
    }
    public Account(String username,String phone,String passwordHash,String displayName,String role) {
        this(phone,passwordHash,displayName,role);this.username=username;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPhone() { return phone == null ? "" : phone; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    /**
     * 转换量体档案为当前页面的表单字段。
     */
    public Measurements getMeasurements() { return measurement == null ? null : measurement.view(); }
    /**
     * 首次填写时创建关联量体对象，已有档案则更新当前表单字段。
     */
    public void updateMeasurements(Measurements m) {
        if (measurement == null) measurement=new CustomerMeasurement(this);
        measurement.update(m);
    }

    public void setId(Long value) { id=value; }
    public void setUsername(String value) { username=value; }
    public void setPasswordHash(String value) { passwordHash=value; }
    public void setDisplayName(String value) { displayName=value; }
    public void setPhone(String value) { phone=value; }
    public void setRole(String value) { role=value; }
    public void setEnabled(boolean value) { enabled=value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt=value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt=value; }
    public CustomerMeasurement getMeasurement() { return measurement; }
    public void setMeasurement(CustomerMeasurement value) { measurement=value; }
}
