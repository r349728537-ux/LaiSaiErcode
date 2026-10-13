package com.lesaire.pojo;
import java.time.Instant;
import jakarta.validation.constraints.*;
/**
 * 登录结果，包含 Bearer 令牌、过期时间和安全的账号资料。
 */
public record LoginResponse(String token,String tokenType,Instant expiresAt,AccountView account) {}
