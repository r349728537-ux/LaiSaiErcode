package com.lesaire.pojo;
import java.util.List;
import jakarta.validation.constraints.*;
/**
 * 目录响应对象，汇总款式、面料、配色及部分选项展示加价。
 */
public record CatalogView(List<ProductView> products,List<FabricView> fabrics,List<ColorView> colors,OptionPrices optionPrices) {}
