package com.lesaire.pojo;

import java.math.BigDecimal;

/**
 * 面料目录实体，一条面料记录包含具体成分、配色、克重和单件加价。
 */
public class FabricMaterial extends CatalogEntity {
    /**
     * 面料业务编码，前端选配使用此编码而非数据库主键。
     */
    private String code;
    private String name;
    private String composition;
    private String colorName;
    /**
     * 用于预览和配色目录的六位十六进制颜色。
     */
    private String colorHex;
    /**
     * 面料克重，单位为 g/m²，旧记录可能未填写。
     */
    private Integer weight;
    /**
     * 面料库存米数，与选配时的单件加价分别保存。
     */
    private BigDecimal stockMeters=BigDecimal.ZERO;
    /**
     * 选用此面料时增加的单件金额，对应数据库 extra_price。
     */
    private BigDecimal surcharge;
    public FabricMaterial() {}
    public FabricMaterial(String code,String name,String composition,String colorName,String hex,int weight,int surcharge) {
        this.code=code;this.name=name;this.composition=composition;this.colorName=colorName;this.colorHex=hex;
        this.weight=weight;this.surcharge=BigDecimal.valueOf(surcharge);
    }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getComposition() { return composition; }
    public String getColorName() { return colorName; }
    /**
     * 规范化有效颜色；缺失或格式错误时使用默认暖白色。
     */
    public String getColorHex() { return colorHex != null && colorHex.matches("#[a-fA-F0-9]{6}") ? colorHex.toUpperCase() : "#E5E8E4"; }
    public Integer getWeight() { return weight; }
    public BigDecimal getSurcharge() { return surcharge; }

    public void setCode(String value) { code=value; }
    public void setName(String value) { name=value; }
    public void setComposition(String value) { composition=value; }
    public void setColorName(String value) { colorName=value; }
    public void setColorHex(String value) { colorHex=value; }
    public void setWeight(Integer value) { weight=value; }
    public BigDecimal getStockMeters() { return stockMeters; }
    public void setStockMeters(BigDecimal value) { stockMeters=value; }
    public void setSurcharge(BigDecimal value) { surcharge=value; }
}
