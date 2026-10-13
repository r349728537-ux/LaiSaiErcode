package com.lesaire.pojo;
import jakarta.validation.constraints.*;
/**
 * 订单状态变更请求，非空枚举值由业务层进一步检查是否允许流转。
 */
public record StatusChangeRequest(@NotNull OrderStatus status) {}
