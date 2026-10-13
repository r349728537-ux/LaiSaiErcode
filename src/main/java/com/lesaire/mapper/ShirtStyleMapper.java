package com.lesaire.mapper;
import com.lesaire.pojo.ShirtStyle;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/**
 * 制服款式的 MyBatis 映射接口，SQL 与数据库字段映射位于同名 XML。
 */
@Mapper
public interface ShirtStyleMapper {
    /**
     * 查询当前启用的制服款式。
     */
    List<ShirtStyle> findActive();
    /**
     * 按业务编码读取可用款式。
     */
    ShirtStyle findActiveByCode(@Param("code") String code);
    /**
     * 统计款式记录，用于判断目录是否为空。
     */
    long count();
    /**
     * 保存演示款式并回填主键。
     */
    int insert(ShirtStyle style);
    /**
     * 仅更新指定启用款式的图片地址，返回受影响行数。
     */
    int updateImageUrl(@Param("code") String code, @Param("imageUrl") String imageUrl);
}
