package com.lesaire.mapper;
import com.lesaire.pojo.CustomizationOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/**
 * 定制选项的 MyBatis 映射接口，按业务编码读取选项与加价。
 */
@Mapper
public interface CustomizationOptionMapper {
    /**
     * 只读取启用的定制选项。
     */
    CustomizationOption findActiveByCode(@Param("code") String code);
    /**
     * 初始化前检查选项编码是否已存在。
     */
    boolean existsByCode(@Param("code") String code);
    /**
     * 插入缺失的定制选项并回填主键。
     */
    int insert(CustomizationOption option);
}
