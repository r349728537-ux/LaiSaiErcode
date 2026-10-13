package com.lesaire.pojo;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
/**
 * 面料展示信息，surcharge 为单件加价，colorId 对应当前面料的配色。
 */
public record FabricView(String id,String name,String composition,String description,BigDecimal surcharge,String weight,String colorId) {}
