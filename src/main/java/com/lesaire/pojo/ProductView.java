package com.lesaire.pojo;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
/**
 * 款式展示信息，id 为业务编码；预览字段用于 SVG，imageUrl 用于真实照片。
 */
public record ProductView(String id,String name,String category,String description,BigDecimal basePrice,String color,String tag,String previewType,String sleeveType,String collarType,String imageUrl) {}
