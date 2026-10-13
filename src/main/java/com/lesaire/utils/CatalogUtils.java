package com.lesaire.utils;
/**
 * 集中管理目录的颜色标识、定制选项编码和制服预览类型转换。
 */
public final class CatalogUtils {
    private CatalogUtils() {}
    /**
     * 常用颜色使用稳定的页面标识，其他颜色由十六进制值生成标识。
     */
    public static String colorId(String hex) {
        return switch(hex.toUpperCase()) { case "#8DAF9D"->"jade";case "#DFD8C7"->"oat";case "#586E80"->"ink";case "#ADA1BD"->"lilac";case "#C6AF96"->"sand";case "#E5E8E4"->"cloud";default->"hex_"+hex.substring(1).toLowerCase(); };
    }
    /**
     * 为当前定制表单生成 LS_ 前缀的选项编码。
     */
    public static String optionCode(String group,String value) { return "LS_"+group+"_"+value; }
}
