package com.lesaire.pojo;

import java.math.BigDecimal;

/**
 * 制服款式目录实体，提供款式场景、基础价格、图片地址及预览特征。
 */
public class ShirtStyle extends CatalogEntity {
    /**
     * 款式业务编码，用于目录选择、报价和图片上传。
     */
    private String code;
    private String name;
    /**
     * 适用餐饮场景，供首页和目录筛选使用。
     */
    private String scene;
    private String description;
    private String collarType;
    private String sleeveType;
    private String fitType;
    /**
     * 单件款式基础价格，面料和定制选项加价另行计算。
     */
    private BigDecimal basePrice;
    /**
     * 真实款式照片地址；为空时前端使用 SVG 制服示意图。
     */
    private String imageUrl;
    public ShirtStyle() {}
    public ShirtStyle(String code,String name,String scene,String description,int price) {
        this.code=code; this.name=name; this.scene=scene; this.description=description;
        collarType="round"; sleeveType="standard"; fitType="regular"; basePrice=BigDecimal.valueOf(price);
    }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getScene() { return scene; }
    public String getDescription() { return description == null ? "" : description; }
    public BigDecimal getBasePrice() { return basePrice; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String value) { imageUrl=value; }
    /**
     * 兼容中英文袖型字段，将预览统一为 short 或 long。
     */
    public String getPreviewSleeve() { return sleeveType != null && (sleeveType.contains("短") || sleeveType.toLowerCase().contains("short")) ? "short" : "long"; }
    /**
     * 兼容中英文领型字段，将预览统一为 stand 或 shirt。
     */
    public String getPreviewCollar() { return collarType != null && (collarType.contains("立") || collarType.toLowerCase().contains("stand") || collarType.toLowerCase().contains("mandarin")) ? "stand" : "shirt"; }

    public void setCode(String value) { code=value; }
    public void setName(String value) { name=value; }
    public void setScene(String value) { scene=value; }
    public void setDescription(String value) { description=value; }
    public String getCollarType() { return collarType; }
    public void setCollarType(String value) { collarType=value; }
    public String getSleeveType() { return sleeveType; }
    public void setSleeveType(String value) { sleeveType=value; }
    public String getFitType() { return fitType; }
    public void setFitType(String value) { fitType=value; }
    public void setBasePrice(BigDecimal value) { basePrice=value; }
}
