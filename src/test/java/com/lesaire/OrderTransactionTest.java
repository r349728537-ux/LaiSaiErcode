package com.lesaire;

import com.lesaire.mapper.OrderMapper;
import com.lesaire.pojo.OrderItem;
import com.lesaire.pojo.OrderItemOption;
import com.lesaire.service.OrderService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

/** 测试外层不包事务，直接验证订单服务自身的事务确实发生回滚。 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:transaction-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.password=", "lesaire.demo-accounts=false",
    "lesaire.jwt.secret=" + ApiTestSupport.TEST_SECRET
})
@AutoConfigureMockMvc @ActiveProfiles("h2")
class OrderTransactionTest extends ApiTestSupport {
    @Autowired OrderService orders;
    @MockitoSpyBean OrderMapper mapper;
    private Long accountId;

    @BeforeEach
    /**
     * 为当前测试创建独立用户，由服务事务正常提交。
     */
    void createCustomer() {
        accountId = accounts.register(OWNER, PASSWORD, "事务测试顾客").id();
    }

    @AfterEach
    /**
     * 按本测试用户 ID 清理内存库数据，同时恢复 Mapper 替身。
     */
    void removeOnlyThisTestCustomer() {
        reset(mapper);
        jdbc.update("delete from order_item_option where order_item_id in (select i.id from custom_order_item i join custom_order o on o.id=i.order_id where o.customer_id=?)", accountId);
        jdbc.update("delete from custom_order_item where order_id in (select id from custom_order where customer_id=?)", accountId);
        jdbc.update("delete from custom_order where customer_id=?", accountId);
        jdbc.update("delete from customer_measurement where customer_id=?", accountId);
        jdbc.update("delete from app_user where id=?", accountId);
    }

    @ParameterizedTest @ValueSource(strings = {"item", "option"})
    /**
     * 分别模拟明细或选项写入失败，验证服务事务撤销主表和全部子表写入。
     */
    void rollsBackAllOrderRowsWhenChildInsertFails(String failureStage) {
        var failure = new DataIntegrityViolationException("模拟订单子表保存失败");
        if (failureStage.equals("item")) doThrow(failure).when(mapper).insertItem(any(OrderItem.class));
        else doThrow(failure).when(mapper).insertOption(any(OrderItemOption.class));
        assertThatThrownBy(() -> orders.create(OWNER, orderRequest(OWNER, UUID.randomUUID().toString())))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("select count(*) from custom_order where customer_id=?", Long.class, accountId)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from custom_order_item", Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from order_item_option", Long.class)).isZero();
    }

    @Test
    /**
     * 验证空或缺失 ID 列表返回空结果，不会查询全部订单明细。
     */
    void emptyIdCollectionsNeverProduceUnboundedMapperQueries() {
        orders.create(OWNER, orderRequest(OWNER, UUID.randomUUID().toString()));
        assertThat(mapper.findItems(List.of())).isEmpty();
        assertThat(mapper.findItems(null)).isEmpty();
        assertThat(mapper.findOptions(List.of())).isEmpty();
        assertThat(mapper.findOptions(null)).isEmpty();
    }
}
