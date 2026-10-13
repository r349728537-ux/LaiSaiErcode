package com.lesaire.utils;
import com.lesaire.interceptor.LoginInterceptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** BCrypt 用于密码哈希与比对；请求登录状态由 LoginInterceptor 校验。 */
@Component
public class PasswordHasher {
    /**
     * 采用强度参数 12，生成带随机盐的 BCrypt 密码哈希。
     */
    private final BCryptPasswordEncoder bcrypt=new BCryptPasswordEncoder(12);
    /**
     * 注册和创建演示账号时生成密码哈希。
     */
    public String encode(String password) { return bcrypt.encode(password); }
    /**
     * 比对输入密码与已有哈希；无效哈希按密码不匹配处理。
     */
    public boolean matches(String password,String hash) {
        try { return hash!=null && bcrypt.matches(password,hash); } catch(IllegalArgumentException ex) { return false; }
    }
}
