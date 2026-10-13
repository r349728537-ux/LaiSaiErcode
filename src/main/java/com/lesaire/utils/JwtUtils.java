package com.lesaire.utils;

import com.lesaire.pojo.Account;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** 登录签发令牌，拦截器验证令牌，退出记录失效令牌 ID。 */
@Component
public class JwtUtils {
    public static final String ISSUER = "lesaire-catering";
    private final SecretKey key;
    private final JwtParser parser;
    private final long ttlSeconds;
    private final String instanceId = UUID.randomUUID().toString();
    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public JwtUtils(@Value("${lesaire.jwt.secret:}") String secret,
                    @Value("${lesaire.jwt.ttl-seconds:7200}") long ttlSeconds) {
        if (secret == null || secret.isBlank()) throw new IllegalArgumentException("请在 .local/jwt.properties 或 JWT_SECRET 配置签名密钥");
        if (ttlSeconds < 1 || ttlSeconds > 86400) throw new IllegalArgumentException("JWT 有效期需为 1–86400 秒");
        key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret.trim()));
        this.ttlSeconds = ttlSeconds;
        // 重启会产生新的标识，使旧令牌统一失效，无需读写撤销文件。
        parser = Jwts.parser().verifyWith(key).requireIssuer(ISSUER).require("instance", instanceId).build();
    }

    public record IssuedToken(String token, Instant expiresAt) {}
    public record VerifiedToken(Long accountId, String tokenId, Instant expiresAt) {}

    public IssuedToken issue(Account account) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(ttlSeconds);
        String token = Jwts.builder().issuer(ISSUER).subject(account.getId().toString())
            .id(UUID.randomUUID().toString()).claim("instance", instanceId)
            .issuedAt(Date.from(now)).expiration(Date.from(expiry)).signWith(key, Jwts.SIG.HS256).compact();
        return new IssuedToken(token, expiry);
    }

    public VerifiedToken verify(String token) {
        try {
            Jws<Claims> signed = parser.parseSignedClaims(token);
            if (!"HS256".equals(signed.getHeader().getAlgorithm())) throw new JwtException("签名算法不正确");
            Claims claims = signed.getPayload();
            if (claims.getExpiration() == null || claims.getId() == null || claims.getSubject() == null) {
                throw new JwtException("令牌缺少必要字段");
            }
            UUID.fromString(claims.getId());
            Long accountId = Long.valueOf(claims.getSubject());
            if (accountId <= 0 || revokedTokens.containsKey(claims.getId())) throw new JwtException("令牌已失效");
            return new VerifiedToken(accountId, claims.getId(), claims.getExpiration().toInstant());
        } catch (ExpiredJwtException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录令牌无效，请重新登录");
        }
    }

    public void revoke(VerifiedToken token) {
        revokedTokens.entrySet().removeIf(entry -> !entry.getValue().isAfter(Instant.now()));
        revokedTokens.put(token.tokenId(), token.expiresAt());
    }
}
