package com.lesaire.pojo;
import jakarta.validation.constraints.*;
/**
 * 账号响应对象，仅包含页面可见资料和量体档案，不返回密码哈希。
 */
public record AccountView(Long id,String phone,String displayName,String role,Measurements measurements) {}
