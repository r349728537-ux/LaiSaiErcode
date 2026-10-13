package com.lesaire;

import com.lesaire.pojo.Customization;
import com.lesaire.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

/**
 * 使用独立 H2 内存库校验真实目录报价，测试数据修改由事务回滚。
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:quote-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.password=", "lesaire.demo-accounts=false",
    "lesaire.jwt.secret=" + ApiTestSupport.TEST_SECRET
})
@ActiveProfiles("h2") @Transactional
class CatalogQuoteTest {
    @Autowired CatalogService catalog;
    @Autowired JdbcTemplate jdbc;

    @ParameterizedTest
    @CsvSource({
        "LY-TW-001,hex_f7f2e8,standard,false,1,198.00",
        "LY-BL-002,hex_193a5b,long,true,2,572.00",
        "LY-GR-003,hex_98a68e,standard,true,5,1315.00"
    })
    /**
     * 用多组面料、衣长、刺绣和数量验证服务端总价。
     */
    void calculatesQuoteFromDatabaseCatalog(String fabric, String color, String length, boolean embroidery, int quantity, String total) {
        var quote = catalog.quote(new Customization("RS-CF-001", fabric, color, "regular", length, embroidery, "round", quantity));
        assertThat(quote.total()).isEqualByComparingTo(total);
    }

    @Test
    /**
     * 修改目录为小数金额，验证报价读取数据库最新值且保留精度。
     */
    void usesDatabasePricesWithoutLosingDecimalPrecision() {
        jdbc.update("update shirt_style set base_price=198.95 where style_code='RS-CF-001'");
        jdbc.update("update fabric set extra_price=18.15 where fabric_code='LY-BL-002'");
        jdbc.update("update customization_option set price_delta=30.05 where option_code='LS_LENGTH_long'");
        jdbc.update("update customization_option set price_delta=40.10 where option_code='LS_EMBROIDERY_true'");
        assertThat(catalog.quote(ApiTestSupport.customization()).total()).isEqualByComparingTo("574.50");
    }

    @Test
    /**
     * 不存在的款式不能参与报价。
     */
    void rejectsUnknownStyle() {
        assertThatThrownBy(() -> catalog.quote(new Customization("missing-style", "LY-TW-001", "hex_f7f2e8", "regular", "standard", false, "round", 1)))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    /**
     * 配色必须属于所选面料，拒绝不一致组合。
     */
    void rejectsMismatchedFabricColor() {
        assertThatThrownBy(() -> catalog.quote(new Customization("RS-CF-001", "LY-TW-001", "hex_193a5b", "regular", "standard", false, "round", 1)))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    /**
     * 款式或定制选项停用后，新的报价请求必须失败。
     */
    void rejectsDisabledStyleAndOption() {
        jdbc.update("update shirt_style set active=false where style_code='RS-CF-001'");
        assertThatThrownBy(() -> catalog.quote(ApiTestSupport.customization())).isInstanceOf(ResponseStatusException.class);
        jdbc.update("update shirt_style set active=true where style_code='RS-CF-001'");
        jdbc.update("update customization_option set active=false where option_code='LS_LENGTH_long'");
        assertThatThrownBy(() -> catalog.quote(ApiTestSupport.customization())).isInstanceOf(ResponseStatusException.class);
    }
}
