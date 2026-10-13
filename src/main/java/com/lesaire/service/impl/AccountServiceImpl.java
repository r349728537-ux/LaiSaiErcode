package com.lesaire.service.impl;
import com.lesaire.mapper.AccountMapper;
import com.lesaire.pojo.Account;
import com.lesaire.pojo.AccountView;
import com.lesaire.pojo.Measurements;
import com.lesaire.service.AccountService;
import com.lesaire.utils.PasswordHasher;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 账号业务实现：负责账号查询、密码校验、注册以及量体档案维护。
 */
@Service
public class AccountServiceImpl implements AccountService {
    private final AccountMapper accounts;
    private final PasswordHasher passwords;
    public AccountServiceImpl(AccountMapper accounts,PasswordHasher passwords) { this.accounts=accounts;this.passwords=passwords; }
    /**
     * 先按用户名查找，未找到时再按手机号查找，兼容两种登录方式。
     */
    public Account find(String login) {
        Account account=accounts.findByUsername(login);
        return account == null ? accounts.findFirstByPhone(login) : account;
    }
    /**
     * 校验账号启用状态与 BCrypt 密码哈希；失败时统一返回登录错误。
     */
    @Transactional(readOnly=true) public Account authenticate(String login,String password) {
        Account account=find(login);
        if(account==null || !account.isEnabled() || !passwords.matches(password,account.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"用户名、手机号或密码不正确，或账号已停用");
        return account;
    }
    /**
     * 获取当前可用账号，供需要登录身份的业务操作复用。
     */
    public Account require(String login) {
        Account account=find(login);
        if(account==null || !account.isEnabled()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"账号不存在或已停用，请重新登录");
        return account;
    }
    /**
     * 在调用方事务中锁定用户行，串行处理同一用户的并发下单请求。
     */
    public Account lock(String login) {
        Account account=accounts.findForUpdate(require(login).getId());
        if(account==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"账号不存在");
        return account;
    }
    
    /**
     * 组装可返回给前端的账号信息，不包含密码哈希。
     */
    public AccountView view(Account account) { return new AccountView(account.getId(),account.getPhone(),account.getDisplayName(),account.getRole(),account.getMeasurements()); }
    /**
     * 查询当前登录用户的资料和量体档案。
     */
    @Transactional(readOnly=true) public AccountView current(String login) { return view(require(login)); }
    /**
     * 注册普通用户；限制 BCrypt 输入长度，并同时处理预检查和数据库唯一约束冲突。
     */
    @Transactional public AccountView register(String phone,String password,String name) {
        if(password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"密码的 UTF-8 长度不能超过 72 字节");
        if(accounts.existsByUsername(phone) || accounts.existsByPhone(phone)) throw new ResponseStatusException(HttpStatus.CONFLICT,"该手机号已注册，请直接登录");
        Account account=new Account(phone,passwords.encode(password),name.strip(),"CUSTOMER");
        try { accounts.insert(account);return view(account); }
        catch(DataIntegrityViolationException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT,"该手机号已注册，请直接登录"); }
    }
    /**
     * 更新当前表单中的量体数据，再保存到独立的量体档案表。
     */
    @Transactional public AccountView update(String login,Measurements measurements) {
        Account account=require(login);
        account.updateMeasurements(measurements);
        accounts.saveMeasurement(account.getMeasurement());
        return view(account);
    }
    /**
     * 仅在用户名不存在时创建演示账号，避免覆盖已有用户资料。
     */
    @Transactional public void createDemo(String username,String phone,String password,String name,String role,Measurements measurements) {
        if(accounts.existsByUsername(username)) return;
        Account account=new Account(username,phone,passwords.encode(password),name,role);
        accounts.insert(account);
        if(measurements!=null) { account.updateMeasurements(measurements);accounts.saveMeasurement(account.getMeasurement()); }
    }
}
