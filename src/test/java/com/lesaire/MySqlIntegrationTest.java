package com.lesaire;

import com.lesaire.pojo.Customization;
import com.lesaire.service.CatalogService;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 仅在 MYSQL_INTEGRATION=true 时显式启用，测试写入的账号、量体和订单由事务回滚。 */
@EnabledIfEnvironmentVariable(named = "MYSQL_INTEGRATION", matches = "true")
@SpringBootTest(properties = {
    "lesaire.demo-accounts=false", "lesaire.jwt.secret=" + ApiTestSupport.TEST_SECRET
})
@AutoConfigureMockMvc @ActiveProfiles("mysql") @Transactional
class MySqlIntegrationTest extends ApiTestSupport {
    @Autowired CatalogService catalog;
    private String phone, customerToken, adminToken;

    @BeforeEach
    /**
     * 在测试回滚事务内创建随机手机号用户和管理员。
     */
    void createCustomersInsideRollbackTransaction() throws Exception {
        phone = randomPhone();
        accounts.register(phone, PASSWORD, "MySQL 验收顾客");
        String adminName = "mysql-test-" + UUID.randomUUID();
        accounts.createDemo(adminName, randomPhone(), PASSWORD, "MySQL 验收管理员", "ADMIN", null);
        customerToken = login(phone); adminToken = login(adminName);
    }

    @Test
    /**
     * 在真实 MySQL 结构上验证小数尺寸、订单明细、量体 JSON 和选项快照。
     */
    void persistsNormalizedOrderAndDecimalMeasurementsInExistingMysqlSchema() throws Exception {
        assertThat(jdbc.queryForObject("select database()", String.class)).isEqualTo("lyocell_uniform");
        var data = catalog.get(); var product = data.products().get(0); var fabric = data.fabrics().get(0);
        var customization = new Customization(product.id(), fabric.id(), fabric.colorId(), "regular", "long", true, "round", 2);
        BigDecimal expected = catalog.quote(customization).total();
        mvc.perform(put("/api/account/measurements").header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(measurements())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.measurements.chest").value(86.25));
        long customerId = accounts.require(phone).getId();
        assertThat(jdbc.queryForObject("select chest from customer_measurement where customer_id=?", BigDecimal.class, customerId))
            .isEqualByComparingTo("86.25");
        var request = new LinkedHashMap<String, Object>();
        request.put("customization", customization); request.put("measurements", measurements());
        request.put("recipient", "MySQL 验收顾客"); request.put("phone", phone);
        request.put("address", "上海市徐汇区 MySQL 验收地址 88 号"); request.put("note", "事务回滚验收");
        request.put("requestKey", UUID.randomUUID().toString()); request.put("total", 0);
        String body = json.writeValueAsString(request);
        var response = read(mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn());
        long orderId = response.get("id").asLong(); assertThat(orderId).isPositive();
        Long itemId = jdbc.queryForObject("select id from custom_order_item where order_id=?", Long.class, orderId);
        assertThat(itemId).isPositive();
        assertThat(jdbc.queryForObject("select total_amount from custom_order where id=?", BigDecimal.class, orderId)).isEqualByComparingTo(expected);
        assertThat(jdbc.queryForObject("select status from custom_order where id=?", String.class, orderId)).isEqualTo("PENDING_CONFIRMATION");
        long optionCount = List.of("dress", "top").contains(product.previewType()) ? 5 : 4;
        assertThat(jdbc.queryForObject("select count(*) from order_item_option where order_item_id=?", Long.class, itemId)).isEqualTo(optionCount);
        String snapshot = jdbc.queryForObject("select measurement_snapshot from custom_order_item where order_id=?", String.class, orderId);
        assertThat(json.readTree(snapshot).get("chest").decimalValue()).isEqualByComparingTo("86.25");
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(jsonPath("$.id").value(orderId));
        assertThat(jdbc.queryForObject("select count(*) from custom_order where customer_id=?", Long.class, customerId)).isEqualTo(1L);
        for (String next : List.of("CUTTING", "SEWING", "QUALITY", "SHIPPED")) {
            mvc.perform(patch("/api/admin/orders/" + orderId + "/status").header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("status", next)))).andExpect(status().isOk());
        }
        mvc.perform(patch("/api/orders/" + orderId + "/status").header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}")).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select status from custom_order where id=?", String.class, orderId)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("select production_status from custom_order_item where order_id=?", String.class, orderId)).isEqualTo("COMPLETED");
    }

    @Test
    /**
     * 验证真实数据库账号停用后，原有令牌立即失效。
     */
    void immediatelyRejectsDisabledMysqlAccountAndItsExistingToken() throws Exception {
        jdbc.update("update app_user set enabled=0 where username=?", phone);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + customerToken)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", phone, "password", PASSWORD)))).andExpect(status().isUnauthorized());
    }

    @Test
    /**
     * 校验已有订单及选项快照可通过当前映射读取。
     */
    void readsExistingMysqlOrdersAndOptionSnapshots() throws Exception {
        var response = read(mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk()).andReturn());
        assertThat(response.get("total").asLong()).isEqualTo(jdbc.queryForObject("select count(*) from custom_order", Long.class));
        for (var order : response.get("items")) for (var item : order.get("items")) {
            assertThat(item.get("productName").asText()).isNotBlank();
            assertThat(item.get("fabricName").asText()).isNotBlank();
            assertThat(item.get("options").isArray()).isTrue();
        }
    }

    /**
     * 为测试生成手机号，降低与已有用户资料重复的概率。
     */
    private static String randomPhone() {
        return "18" + String.format("%09d", new SecureRandom().nextInt(1_000_000_000));
    }
}
