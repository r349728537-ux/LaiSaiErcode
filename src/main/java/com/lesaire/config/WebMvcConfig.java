package com.lesaire.config;
import com.lesaire.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
/**
 * 注册统一登录拦截器，具体公开接口的放行规则由拦截器内部决定。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final LoginInterceptor loginInterceptor;
    public WebMvcConfig(LoginInterceptor loginInterceptor) { this.loginInterceptor=loginInterceptor; }
    /**
     * 对所有 MVC 请求应用拦截器，使页面和 API 都能获得统一响应头。
     */
    @Override public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(loginInterceptor).addPathPatterns("/**"); }
}
