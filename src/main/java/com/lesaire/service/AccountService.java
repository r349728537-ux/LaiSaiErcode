package com.lesaire.service;
import com.lesaire.pojo.*;
/**
 * 账号业务接口，统一定义登录校验、账号资料与量体档案操作。
 */
public interface AccountService {
    /**
     * 支持用户名或手机号的账号查询。
     */
    Account find(String login);
    /**
     * 校验账号启用状态和密码，返回认证成功的账号。
     */
    Account authenticate(String login,String password);
    /**
     * 获取必须存在且启用的账号。
     */
    Account require(String login);
    /**
     * 在事务中锁定账号行，用于串行处理该用户的下单请求。
     */
    Account lock(String login);
    /**
     * 转换为不包含密码哈希的账号响应。
     */
    AccountView view(Account account);
    /**
     * 读取当前账号资料和量体信息。
     */
    AccountView current(String login);
    /**
     * 注册手机号对应的普通用户。
     */
    AccountView register(String phone,String password,String name);
    /**
     * 保存当前用户的量体表单。
     */
    AccountView update(String login,Measurements measurements);
    /**
     * 在演示用户名不存在时创建账号。
     */
    void createDemo(String username,String phone,String password,String name,String role,Measurements measurements);
}
