package com.lesaire.controller;
import com.lesaire.pojo.Account;
import com.lesaire.pojo.AccountView;
import com.lesaire.pojo.LoginRequest;
import com.lesaire.pojo.LoginResponse;
import com.lesaire.pojo.Measurements;
import com.lesaire.pojo.RegistrationRequest;
import com.lesaire.service.AccountService;
import com.lesaire.utils.JwtUtils;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

/**
 * 账号 HTTP 接口：接收已验证的请求参数，调用账号业务并返回前端需要的数据。
 */
@RestController @RequestMapping("/api")
public class AccountController {
    private final AccountService accounts;
    private final JwtUtils jwt;
    public AccountController(AccountService accounts,JwtUtils jwt) { this.accounts=accounts;this.jwt=jwt; }

    /**
     * 核对登录凭据后签发 JWT，同时返回账号资料和令牌过期时间。
     */
    @PostMapping("/auth/login") public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Account account=accounts.authenticate(request.username(),request.password());
        var token=jwt.issue(account);return new LoginResponse(token.token(),"Bearer",token.expiresAt(),accounts.view(account));
    }
    /**
     * 撤销拦截器已验证的当前令牌，使本次登录立即失效。
     */
    @PostMapping("/auth/logout") public Map<String,String> logout(@RequestAttribute("verifiedToken") JwtUtils.VerifiedToken token) {
        jwt.revoke(token);return Map.of("message","已退出登录");
    }
    /**
     * 返回拦截器查询到的当前有效账号资料。
     */
    @GetMapping("/auth/me") public AccountView me(@RequestAttribute("authenticatedAccount") Account account) { return accounts.view(account); }
    /**
     * 按手机号注册普通用户，创建成功返回 HTTP 201。
     */
    @PostMapping("/auth/register") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public AccountView register(@Valid @RequestBody RegistrationRequest request) { return accounts.register(request.phone(),request.password(),request.displayName()); }
    /**
     * 只更新当前登录用户的量体档案，账号身份来自请求属性。
     */
    @PutMapping("/account/measurements") public AccountView measurements(@RequestAttribute("authenticatedAccount") Account account,@Valid @RequestBody Measurements request) {
        return accounts.update(account.getUsername(),request);
    }
}
