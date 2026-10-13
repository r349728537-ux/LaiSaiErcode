package com.lesaire.pojo;

import java.math.BigDecimal;

/**
 * 定制选项实体，保存选项分组、展示名称、选项值及单件加价。
 */
public class CustomizationOption extends CatalogEntity {
    /**
     * 稳定业务编码，当前表单选项使用 LS_ 前缀。
     */
    private String code;
    /**
     * 选项分组，例如 FIT、LENGTH、EMBROIDERY、COLOR。
     */
    private String group;
    private String name;
    private String value;
    /**
     * 选择此选项时增加的单件金额。
     */
    private BigDecimal priceDelta;
    /**
     * 数据库保留的选项展示排序值。
     */
    private int sortOrder;
    public CustomizationOption() {}
    public CustomizationOption(String code,String group,String name,String value,int price,int sortOrder) {
        this.code=code;this.group=group;this.name=name;this.value=value;this.priceDelta=BigDecimal.valueOf(price);this.sortOrder=sortOrder;
    }
    public String getCode() { return code; }
    public String getGroup() { return group; }
    public String getName() { return name; }
    public String getValue() { return value; }
    public BigDecimal getPriceDelta() { return priceDelta; }

    public void setCode(String value) { code=value; }
    public void setGroup(String value) { group=value; }
    public void setName(String value) { name=value; }
    public void setValue(String value) { this.value=value; }
    public void setPriceDelta(BigDecimal value) { priceDelta=value; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int value) { sortOrder=value; }
}
