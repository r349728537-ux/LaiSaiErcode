package com.lesaire.interceptor;
import com.lesaire.mapper.AccountMapper;
import com.lesaire.pojo.Account;
import com.lesaire.utils.JwtUtils;

import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 统一处理 API 登录校验：验证 JWT、读取当前账号状态并检查管理员权限。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {
    private final JwtUtils jwt;
    private final AccountMapper accounts;
    public LoginInterceptor(JwtUtils jwt,AccountMapper accounts) { this.jwt=jwt;this.accounts=accounts; }
    /**
     * 在控制器执行前补充安全响应头；受保护接口必须携带有效的 Bearer 令牌。
     */
    @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) {
        response.setHeader("X-Content-Type-Options","nosniff");response.setHeader("X-Frame-Options","DENY");
        response.setHeader("Referrer-Policy","same-origin");
        response.setHeader("Content-Security-Policy","default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; object-src 'none'; frame-ancestors 'none'");
        // 使用 Spring 匹配到的接口路径，编码后的 URL 也必须经过相同权限检查。
        Object matchedPath=request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String path=matchedPath==null ? request.getRequestURI().substring(request.getContextPath().length()) : matchedPath.toString();
        if(!path.startsWith("/api/") || publicEndpoint(request.getMethod(),path)) return true;
        String authorization=request.getHeader("Authorization");
        if(authorization==null || !authorization.startsWith("Bearer ") || authorization.length()>4096) {
            response.setHeader("WWW-Authenticate","Bearer");throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"请先登录");
        }
        JwtUtils.VerifiedToken token=jwt.verify(authorization.substring(7));
        Account account=accounts.findById(token.accountId());
        if(account==null || !account.isEnabled()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"账号不存在或已停用，请重新登录");
        // 权限以数据库中当前账号的角色为准，避免使用客户端声明或旧令牌中的过期角色。
        if(path.startsWith("/api/admin/") && !"ADMIN".equals(account.getRole()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"此操作需要管理员权限");
        // 把验证后的账号和令牌放到请求属性中，供控制器直接使用。
        request.setAttribute("authenticatedAccount",account);request.setAttribute("verifiedToken",token);
        return true;
    }
    /**
     * 目录、报价、登录和注册按请求方法及路径精确放行。
     */
    private boolean publicEndpoint(String method,String path) {
        return ("GET".equals(method) && "/api/catalog".equals(path)) || ("POST".equals(method) &&
            ("/api/quotes".equals(path) || "/api/auth/login".equals(path) || "/api/auth/register".equals(path)));
    }
}
