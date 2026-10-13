package com.lesaire.mapper;
import com.lesaire.pojo.CustomOrder;
import com.lesaire.pojo.OrderItem;
import com.lesaire.pojo.OrderItemOption;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/**
 * 订单主表、明细和选项快照的 MyBatis 映射接口；多表写入由业务层事务协调。
 */
@Mapper
public interface OrderMapper {
    /**
     * 按用户和订单号查询已处理的请求。
     */
    CustomOrder findByNumber(@Param("accountId") Long accountId,@Param("orderNumber") String orderNumber);
    /**
     * 按 ID 查询并锁定订单主表行。
     */
    CustomOrder findForUpdate(@Param("id") Long id);
    /**
     * 先对订单主表分页，避免一对多联表造成分页截断。
     */
    List<CustomOrder> findPage(@Param("accountId") Long accountId,@Param("offset") int offset,@Param("limit") int limit);
    /**
     * 按用户统计订单；用户 ID 为空时统计全部订单。
     */
    long count(@Param("accountId") Long accountId);
    /**
     * 批量读取指定订单的明细，调用前由业务层排除空 ID 列表。
     */
    List<OrderItem> findItems(@Param("orderIds") List<Long> orderIds);
    /**
     * 批量读取指定明细的定制选项快照。
     */
    List<OrderItemOption> findOptions(@Param("itemIds") List<Long> itemIds);
    /**
     * 插入订单主表，并回填订单 ID。
     */
    int insertOrder(CustomOrder order);
    /**
     * 插入订单明细，并回填明细 ID。
     */
    int insertItem(OrderItem item);
    /**
     * 插入明细对应的定制选项快照。
     */
    int insertOption(OrderItemOption option);
    /**
     * 附带预期原状态进行条件更新，业务层检查受影响行数以识别冲突。
     */
    int updateStatus(@Param("id") Long id,@Param("expected") String expected,@Param("next") String next,@Param("updatedAt") java.time.LocalDateTime updatedAt);
    /**
     * 同步指定订单下各明细的制作状态。
     */
    int updateProduction(@Param("orderId") Long orderId,@Param("status") String status);
}
