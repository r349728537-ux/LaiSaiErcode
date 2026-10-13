package com.lesaire.pojo;
import jakarta.validation.constraints.*;
/**
 * 手机号注册表单，验证手机号格式、密码长度及显示名称。
 */
public record RegistrationRequest(@Pattern(regexp="1[3-9]\\d{9}",message="请输入正确的手机号") @NotBlank String phone,
        @NotBlank @Size(min=8,max=64,message="密码需为 8–64 位") String password,@NotBlank @Size(max=30) String displayName) {}
