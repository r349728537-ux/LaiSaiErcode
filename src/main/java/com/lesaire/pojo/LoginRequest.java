package com.lesaire.pojo;
import jakarta.validation.constraints.*;
/**
 * 登录表单；username 可填写用户名或手机号，注解限制必填项和输入长度。
 */
public record LoginRequest(@NotBlank @Size(max=50) String username,@NotBlank @Size(max=64) String password) {}
