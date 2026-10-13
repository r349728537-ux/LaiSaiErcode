package com.lesaire;

import com.lesaire.pojo.Account;
import com.lesaire.utils.JwtUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

/**
 * 验证签发、验签、有效期、退出撤销以及重启后重新登录。
 */
class JwtUtilsTest {
    private JwtUtils jwt;
    private Account account;

    @BeforeEach
    void createIssuer() {
        jwt = new JwtUtils(ApiTestSupport.TEST_SECRET, 7200);
        account = new Account("13811111111", "unused-password-hash", "测试用户", "CUSTOMER");
        account.setId(42L);
    }

    @Test
    /**
     * 签发后验签，核对账号 ID、唯一令牌标识和有效期。
     */
    void issuesAndVerifiesSignedToken() {
        var issued = jwt.issue(account); var verified = jwt.verify(issued.token());
        assertThat(verified.accountId()).isEqualTo(42L);
        assertThat(verified.tokenId()).isNotBlank();
        assertThat(verified.expiresAt()).isAfter(Instant.now());
        assertThat(issued.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void logoutRevokesTokenAndAllowsFreshLogin() {
        String token = jwt.issue(account).token();
        jwt.revoke(jwt.verify(token));
        assertThatThrownBy(() -> jwt.verify(token)).isInstanceOf(ResponseStatusException.class);
        assertThat(jwt.verify(jwt.issue(account).token()).accountId()).isEqualTo(42L);
    }

    @Test
    void restartRequiresFreshLoginEvenForUnrevokedToken() {
        String token = jwt.issue(account).token();
        var restarted = new JwtUtils(ApiTestSupport.TEST_SECRET, 7200);
        assertThatThrownBy(() -> restarted.verify(token)).isInstanceOf(ResponseStatusException.class);
        assertThat(restarted.verify(restarted.issue(account).token()).accountId()).isEqualTo(42L);
    }

    @Test
    void refusesTamperedSignature() {
        String token = jwt.issue(account).token();
        int position = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(position) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, position) + replacement + token.substring(position + 1);
        assertThatThrownBy(() -> jwt.verify(tampered)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    /**
     * 即使签名正确，已过期令牌也不能使用。
     */
    void refusesExpiredSignedToken() {
        String expired = signedToken(Instant.now().minusSeconds(60), Jwts.SIG.HS256);
        assertThatThrownBy(() -> jwt.verify(expired)).isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getReason()).contains("过期"));
    }

    @Test
    /**
     * 使用已知密钥但采用其他算法的令牌仍必须拒绝。
     */
    void refusesUnexpectedAlgorithmEvenWithKnownKey() {
        assertThat(jwt.verify(signedToken(Instant.now().plusSeconds(600), Jwts.SIG.HS256)).accountId()).isEqualTo(42L);
        String wrongAlgorithm = signedToken(Instant.now().plusSeconds(600), Jwts.SIG.HS384);
        assertThatThrownBy(() -> jwt.verify(wrongAlgorithm)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    /**
     * 无签名和格式损坏的令牌都应转换为认证失败。
     */
    void refusesUnsignedOrMalformedTokens() {
        assertThatThrownBy(() -> jwt.verify("eyJhbGciOiJub25lIn0.eyJzdWIiOiI0MiJ9."))
            .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> jwt.verify("not-a-jwt")).isInstanceOf(ResponseStatusException.class);
    }

    /**
     * 保留真实签发的启动标识等声明，只改变本测试要验证的时间或算法。
     */
    private String signedToken(Instant expiry, io.jsonwebtoken.security.MacAlgorithm algorithm) {
        var key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(ApiTestSupport.TEST_SECRET));
        var claims = Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(jwt.issue(account).token()).getPayload();
        return Jwts.builder().claims(new HashMap<>(claims))
            .issuedAt(Date.from(Instant.now().minusSeconds(120))).expiration(Date.from(expiry))
            .signWith(key, algorithm).compact();
    }
}
