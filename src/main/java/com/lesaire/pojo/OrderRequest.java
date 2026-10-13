package com.lesaire.pojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
/**
 * 下单请求，包含选配、独立量体快照、收件信息及幂等请求标识。
 */
public record OrderRequest(
    // 递归校验选配字段，避免只检查对象本身是否为空。
    @Valid @NotNull Customization customization,
    // 此次下单采用的量体数据，保存后不随用户档案修改。
    @Valid @NotNull Measurements measurements,
    @NotBlank @Size(max=30) String recipient,
    @NotBlank @Pattern(regexp="1[3-9]\\d{9}") String phone,
    @NotBlank @Size(min=5,max=200) String address,
    @NotNull @Size(max=300) String note,
    // 同一次提交及其重试共用请求标识，服务端据此返回已有订单。
    @NotBlank @Pattern(regexp="[a-fA-F0-9-]{36}") String requestKey
) {}
