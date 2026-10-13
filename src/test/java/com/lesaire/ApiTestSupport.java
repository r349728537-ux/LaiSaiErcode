package com.lesaire;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lesaire.pojo.Customization;
import com.lesaire.pojo.Measurements;
import com.lesaire.pojo.OrderRequest;
import com.lesaire.service.AccountService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 共用请求测试数据；认证始终通过真实登录接口完成。 */
abstract class ApiTestSupport {
    /**
     * 仅供测试使用的固定签名密钥，与运行环境的本地密钥分开。
     */
    static final String TEST_SECRET = "bGVzYWlyZS10ZXN0LWtleS1vbmx5LW5ldmVyLXVzZS1pbi1wcm9kdWN0aW9uLTAxMjM0NTY3ODktYWJjZGVmZ2hpag==";
    static final String OWNER = "13811111111", OTHER = "13822222222", PASSWORD = "Test123!";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AccountService accounts;
    @Autowired JdbcTemplate jdbc;

    /**
     * 通过真实登录接口获取响应，使请求测试覆盖密码校验与令牌签发。
     */
    MvcResult loginResponse(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", password))))
            .andExpect(status().isOk()).andReturn();
    }
    /**
     * 从登录响应中提取令牌，供受保护接口的请求复用。
     */
    String login(String username) throws Exception { return read(loginResponse(username, PASSWORD)).get("token").asText(); }
    /**
     * 把 MockMvc 响应正文转换为可断言的 JSON 树。
     */
    JsonNode read(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()); }
    /**
     * 构造带面料、加长和刺绣的固定选配，供报价与订单测试共用。
     */
    static Customization customization() {
        return new Customization("RS-CF-001", "LY-BL-002", "hex_193a5b", "regular", "long", true, "round", 2);
    }
    /**
     * 使用含两位小数的尺寸，验证量体值和 JSON 快照的精度。
     */
    static Measurements measurements() {
        return new Measurements(new BigDecimal("165"), new BigDecimal("86.25"), new BigDecimal("68.50"), new BigDecimal("92"), new BigDecimal("38"));
    }
    /**
     * 构造合法的收件信息及指定幂等请求标识。
     */
    static OrderRequest orderRequest(String phone, String key) {
        return new OrderRequest(customization(), measurements(), "测试顾客", phone,
            "上海市徐汇区测试路 100 号", "测试餐饮制服订单", key);
    }
    String orderBody() throws Exception { return json.writeValueAsString(orderRequest(OWNER, UUID.randomUUID().toString())); }
    /**
     * 通过真实下单接口创建订单，并返回数据库生成的订单 ID。
     */
    long createOrder(String token) throws Exception {
        return read(mvc.perform(post("/api/orders").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(orderBody()))
            .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }
}
