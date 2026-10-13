package com.lesaire.mapper;
import com.lesaire.pojo.FabricMaterial;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/**
 * 面料的 MyBatis 映射接口，负责目录面料查询和初始化写入。
 */
@Mapper
public interface FabricMapper {
    /**
     * 查询启用面料，供选配和颜色目录使用。
     */
    List<FabricMaterial> findActive();
    /**
     * 按业务编码读取可用面料。
     */
    FabricMaterial findActiveByCode(@Param("code") String code);
    /**
     * 统计面料记录，用于判断是否需要初始化空目录。
     */
    long count();
    /**
     * 保存演示面料并回填主键。
     */
    int insert(FabricMaterial fabric);
}
