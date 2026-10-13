package com.lesaire.mapper;
import com.lesaire.pojo.Account;
import com.lesaire.pojo.CustomerMeasurement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/**
 * 账号与量体档案的 MyBatis 映射接口，SQL 位于同名 Mapper XML。
 */
@Mapper
public interface AccountMapper {
    /**
     * 按唯一用户名读取账号及关联量体档案。
     */
    Account findByUsername(@Param("username") String username);
    /**
     * 按手机号查找账号，兼容已有用户数据。
     */
    Account findFirstByPhone(@Param("phone") String phone);
    /**
     * 按账号 ID 读取当前资料，供 JWT 身份校验使用。
     */
    Account findById(@Param("id") Long id);
    /**
     * 查询并锁定用户行，调用方必须处在事务中。
     */
    Account findForUpdate(@Param("id") Long id);
    /**
     * 检查用户名是否已被使用。
     */
    boolean existsByUsername(@Param("username") String username);
    /**
     * 检查手机号是否已有账号使用。
     */
    boolean existsByPhone(@Param("phone") String phone);
    /**
     * 插入用户，并把数据库生成的主键回填到实体。
     */
    int insert(Account account);
    /**
     * 按客户 ID 新增或更新量体档案。
     */
    int saveMeasurement(CustomerMeasurement measurement);
}
