package com.lesaire.pojo;

import java.time.LocalDateTime;

/**
 * 款式、面料和定制选项共用的目录实体字段。
 */
public abstract class CatalogEntity {
    /**
     * 数据库自增主键，由 MyBatis 插入后回填。
     */
    protected Long id;
    /**
     * 目录记录的启用标记，停用后不再提供给新的选配和报价。
     */
    protected boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }

    public void setId(Long value) { id=value; }
    public boolean getActive() { return active; }
    public void setActive(boolean value) { active=value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt=value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt=value; }
}
