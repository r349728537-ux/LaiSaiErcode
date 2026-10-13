package com.lesaire.service;
import com.lesaire.pojo.*;
/**
 * 订单业务接口，控制重复提交、订单可见范围和制作进度。
 */
public interface OrderService {
    /**
     * 依据请求标识幂等创建订单，并保存服务端价格快照。
     */
    OrderView create(String login,OrderRequest request);
    /**
     * 分页读取用户订单或管理员可见的全部订单。
     */
    OrderPage list(String login,boolean admin,int page);
    /**
     * 处理用户取消订单或确认收货。
     */
    OrderView customerStatus(String login,Long id,OrderStatus next);
    /**
     * 处理管理员制作流程更新。
     */
    OrderView advance(Long id,OrderStatus next);
}
