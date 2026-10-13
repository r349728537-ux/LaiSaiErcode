package com.lesaire.config;
import com.lesaire.pojo.Measurements;
import com.lesaire.service.AccountService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 仅在启用 lesaire.demo-accounts 时创建缺失的用户和管理员演示账号。
 */
@Component
@ConditionalOnProperty(name="lesaire.demo-accounts", havingValue="true")
public class DemoAccounts implements CommandLineRunner {
    private final AccountService accounts;
    public DemoAccounts(AccountService accounts) { this.accounts=accounts; }
    /**
     * 交由账号服务判断是否需要创建，避免重置已有账号的密码或角色。
     */
    @Override public void run(String... args) {
        accounts.createDemo("lesaire_demo","13800138000","Lyocell123!","林小姐","CUSTOMER",new Measurements(165,86,68,92,38));
        accounts.createDemo("lesaire_admin","13900139000","Admin123!","莱赛尔餐饮制服工作室","ADMIN",null);
    }
}
