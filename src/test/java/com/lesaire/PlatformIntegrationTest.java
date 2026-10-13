package com.lesaire;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 使用 MockMvc 与独立 H2 库验证登录、权限、量体及订单完整流程。
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:platform-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.password=", "lesaire.demo-accounts=false",
    "lesaire.jwt.secret=" + ApiTestSupport.TEST_SECRET
})
@AutoConfigureMockMvc @ActiveProfiles("h2") @Transactional
class PlatformIntegrationTest extends ApiTestSupport {
    private String ownerToken, otherToken, adminToken;
    @BeforeEach
    /**
     * 在回滚事务内准备不同用户和管理员，使用真实登录获取各自令牌。
     */
    void registerAndLogin() throws Exception {
        accounts.register(OWNER, PASSWORD, "测试顾客");
        accounts.register(OTHER, PASSWORD, "其他顾客");
        accounts.createDemo("test-admin", "13911111111", PASSWORD, "测试管理员", "ADMIN", null);
        ownerToken = login(OWNER); otherToken = login(OTHER); adminToken = login("test-admin");
    }

    @Test
    /**
     * 核对登录响应携带 JWT，并验证请求不会创建服务端会话。
     */
    void loginReturnsJwtWithoutCreatingSession() throws Exception {
        var response = loginResponse(OWNER, PASSWORD); var body = read(response);
        assertThat(body.get("token").asText().split("\\.")).hasSize(3);
        assertThat(body.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(Instant.parse(body.get("expiresAt").asText())).isAfter(Instant.now());
        assertThat(body.get("account").get("phone").asText()).isEqualTo(OWNER);
        assertThat(body.get("account").has("passwordHash")).isFalse();
        assertThat(response.getRequest().getSession(false)).isNull();
        assertThat(response.getResponse().getHeader("Set-Cookie")).isNull();
        assertThat(jdbc.queryForObject("select password_hash from app_user where username=?", String.class, OWNER))
            .startsWith("$2").isNotEqualTo(PASSWORD);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", OWNER, "password", "wrong-password"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    /**
     * 缺少令牌或令牌被篡改时，受保护接口返回认证失败。
     */
    void interceptorRejectsMissingAndTamperedTokens() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
        int offset = ownerToken.lastIndexOf('.') + 1;
        char replacement = ownerToken.charAt(offset) == 'A' ? 'B' : 'A';
        String tampered = ownerToken.substring(0, offset) + replacement + ownerToken.substring(offset + 1);
        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + tampered)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders").header("Authorization", "Basic " + ownerToken)).andExpect(status().isUnauthorized());
    }

    @Test
    /**
     * 正确签名的过期令牌也应被拦截器拒绝。
     */
    void interceptorRejectsExpiredSignedToken() throws Exception {
        var key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(ownerToken).getPayload();
        String expired = Jwts.builder().claims(new HashMap<>(claims))
            .issuedAt(Date.from(Instant.now().minusSeconds(120))).expiration(Date.from(Instant.now().minusSeconds(60)))
            .signWith(key, Jwts.SIG.HS256).compact();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + expired)).andExpect(status().isUnauthorized());
    }

    @Test
    /**
     * 退出后旧令牌立即失效，重新登录得到的新令牌仍可访问。
     */
    void logoutRevokesTokenAndAllowsFreshLogin() throws Exception {
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + ownerToken)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + ownerToken)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + login(OWNER)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value(OWNER));
    }

    @Test
    /**
     * 账号停用后，之前签发的令牌不能继续使用。
     */
    void interceptorChecksCurrentAccountEnabledFlag() throws Exception {
        jdbc.update("update app_user set enabled=false where username=?", OWNER);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + ownerToken)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", OWNER, "password", PASSWORD))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    /**
     * 管理员权限跟随数据库当前角色，旧令牌中的角色不能维持已撤销权限。
     */
    void adminPathGuardUsesCurrentDatabaseRole() throws Exception {
        mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + ownerToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        jdbc.update("update app_user set role='CUSTOMER' where username='test-admin'");
        mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + adminToken)).andExpect(status().isForbidden());
    }

    @Test
    void encodedControllerPathsStillRequireLoginAndAdminRole() throws Exception {
        // 使用 URI 保留百分号编码，经过真实 MVC 路由验证，避免测试字符串被再次编码。
        URI adminOrders = URI.create("/api/%61dmin/orders");
        mvc.perform(get(adminOrders).header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isForbidden());
        mvc.perform(get(adminOrders).header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
        mvc.perform(get(URI.create("/%61pi/orders"))).andExpect(status().isUnauthorized());
    }

    @Test
    /**
     * 不同用户的订单查询和状态操作保持归属隔离。
     */
    void isolatesOrdersBetweenAccounts() throws Exception {
        long id = createOrder(ownerToken);
        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(patch("/api/orders/" + id + "/status").header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + ownerToken))
            .andExpect(jsonPath("$.items[0].status").value("PENDING"));
    }

    @Test
    /**
     * 校验订单主表、明细与快照写入，并验证客户端金额不影响服务端成交价。
     */
    void normalizesOrderWritesAndIgnoresClientPrice() throws Exception {
        var request = json.valueToTree(orderRequest(OWNER, UUID.randomUUID().toString()));
        ((com.fasterxml.jackson.databind.node.ObjectNode) request).put("total", 1);
        var body = read(mvc.perform(post("/api/orders").header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(572)).andReturn());
        long id = body.get("id").asLong(); assertThat(id).isPositive();
        Long itemId = jdbc.queryForObject("select id from custom_order_item where order_id=?", Long.class, id);
        assertThat(itemId).isPositive();
        assertThat(jdbc.queryForObject("select customer_id from custom_order where id=?", Long.class, id)).isEqualTo(accounts.require(OWNER).getId());
        assertThat(jdbc.queryForObject("select total_amount from custom_order where id=?", BigDecimal.class, id)).isEqualByComparingTo("572.00");
        assertThat(jdbc.queryForObject("select status from custom_order where id=?", String.class, id)).isEqualTo("PENDING_CONFIRMATION");
        assertThat(jdbc.queryForObject("select count(*) from order_item_option where order_item_id=?", Long.class, itemId)).isEqualTo(4L);
        assertThat(body.get("items").get(0).get("options").size()).isEqualTo(4);
        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].measurements.chest").value(86.25));
    }

    @Test
    /**
     * 同一请求标识重试返回原订单，不重复新增明细或选项。
     */
    void retriesReturnTheSameOrderWithoutDuplicateItems() throws Exception {
        String payload = orderBody(); Long id = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            long returnedId = read(mvc.perform(post("/api/orders").header("Authorization", "Bearer " + ownerToken)
                    .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isCreated()).andReturn()).get("id").asLong();
            if (id == null) id = returnedId; else assertThat(returnedId).isEqualTo(id);
        }
        assertThat(jdbc.queryForObject("select count(*) from custom_order where customer_id=?", Long.class, accounts.require(OWNER).getId())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select count(*) from custom_order_item where order_id=?", Long.class, id)).isEqualTo(1L);
    }

    @Test
    /**
     * 非法数量和量体输入在请求校验阶段被拒绝。
     */
    void rejectsInvalidQuantityAndMeasurements() throws Exception {
        var payload = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(orderBody());
        ((com.fasterxml.jackson.databind.node.ObjectNode) payload.get("customization")).put("quantity", 0);
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload))).andExpect(status().isBadRequest());
        payload = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(orderBody());
        ((com.fasterxml.jackson.databind.node.ObjectNode) payload.get("measurements")).put("waist", 5);
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload))).andExpect(status().isBadRequest());
    }

    @Test
    /**
     * 制作进度必须按流程推进，只有已发货订单可由用户确认收货。
     */
    void requiresProductionSequenceBeforeCustomerReceipt() throws Exception {
        long id = createOrder(ownerToken);
        mvc.perform(patch("/api/admin/orders/" + id + "/status").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHIPPED\"}")).andExpect(status().isBadRequest());
        for (String next : new String[] {"CUTTING", "SEWING", "QUALITY", "SHIPPED"}) {
            mvc.perform(patch("/api/admin/orders/" + id + "/status").header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("status", next)))).andExpect(status().isOk());
        }
        mvc.perform(patch("/api/orders/" + id + "/status").header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/orders/" + id + "/status").header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        assertThat(jdbc.queryForObject("select status from custom_order where id=?", String.class, id)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("select production_status from custom_order_item where order_id=?", String.class, id)).isEqualTo("COMPLETED");
    }

    @Test
    /**
     * 更新当前五项尺寸时，数据库中其他已有量体字段得到保留。
     */
    void updatesMeasurementProfileWithoutReplacingUneditedFields() throws Exception {
        long customerId = accounts.require(OWNER).getId();
        jdbc.update("insert into customer_measurement(customer_id,neck,weight,body_type,remark) values(?,?,?,?,?)",
            customerId, new BigDecimal("37.50"), new BigDecimal("60.25"), "regular", "保留原量体备注");
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(put("/api/account/measurements").header("Authorization", "Bearer " + ownerToken)
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(measurements())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.measurements.chest").value(86.25));
        }
        assertThat(jdbc.queryForObject("select count(*) from customer_measurement where customer_id=?", Long.class, customerId)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select neck from customer_measurement where customer_id=?", BigDecimal.class, customerId)).isEqualByComparingTo("37.50");
        assertThat(jdbc.queryForObject("select weight from customer_measurement where customer_id=?", BigDecimal.class, customerId)).isEqualByComparingTo("60.25");
        assertThat(jdbc.queryForObject("select remark from customer_measurement where customer_id=?", String.class, customerId)).isEqualTo("保留原量体备注");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.measurements.waist").value(68.5));
    }

    @Test
    /**
     * 覆盖手机号重复注册和密码 UTF-8 字节数超出 BCrypt 限制的输入。
     */
    void rejectsDuplicatePhoneRegistrationAndOverlongBcryptInput() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("phone", OWNER, "password", PASSWORD, "displayName", "重复用户")))).andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("phone", "13833333333", "password", "密".repeat(25), "displayName", "新顾客")))).andExpect(status().isBadRequest());
    }
}
