package com.lesaire.service.impl;

import com.lesaire.mapper.OrderMapper;
import com.lesaire.pojo.*;
import com.lesaire.service.AccountService;
import com.lesaire.service.CatalogService;
import com.lesaire.service.OrderService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 订单模块：下单、分页查询和制作进度。 */
@Service
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orders;
    private final AccountService accounts;
    private final CatalogService catalog;

    public OrderServiceImpl(OrderMapper orders, AccountService accounts, CatalogService catalog) {
        this.orders = orders;
        this.accounts = accounts;
        this.catalog = catalog;
    }

    // 重新报价后依次保存主表、明细、选项；任一步失败，整个事务回滚。
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderView create(String login, OrderRequest request) {
        Long owner = accounts.lock(login).getId();
        String number = CustomOrder.requestNumber(owner, request.requestKey());
        CustomOrder existing = orders.findByNumber(owner, number);
        if (existing != null) {
            loadDetails(List.of(existing));
            return existing.view();
        }
        Quote quote = catalog.quote(request.customization());
        CustomOrder order = new CustomOrder(owner, request,
            catalog.style(request.customization().productId()),
            catalog.material(request.customization().fabricId()),
            catalog.selectedOptions(request.customization()), quote);
        orders.insertOrder(order);
        for (OrderItem item : order.getItems()) {
            orders.insertItem(item);
            for (OrderItemOption option : item.getOptions()) orders.insertOption(option);
        }
        return order.view();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderPage list(String login, boolean admin, int page) {
        if (page < 0 || page > 10000) throw bad("页码不正确");
        Long owner = admin ? null : accounts.require(login).getId();
        long count = orders.count(owner);
        List<CustomOrder> rows = orders.findPage(owner, page * 20, 20);
        loadDetails(rows);
        List<OrderView> views = new ArrayList<>();
        for (CustomOrder order : rows) views.add(order.view());
        return new OrderPage(views, count, page, (int) ((count + 19) / 20));
    }

    // 只读取当前页订单的明细和选项，用普通循环把数据放回对应订单。
    private void loadDetails(List<CustomOrder> rows) {
        if (rows.isEmpty()) return;
        List<Long> orderIds = new ArrayList<>();
        Map<Long, CustomOrder> orderMap = new HashMap<>();
        for (CustomOrder order : rows) {
            orderIds.add(order.getId());
            order.setItems(new ArrayList<>());
            orderMap.put(order.getId(), order);
        }
        List<OrderItem> items = orders.findItems(orderIds);
        List<Long> itemIds = new ArrayList<>();
        Map<Long, OrderItem> itemMap = new HashMap<>();
        for (OrderItem item : items) {
            item.setOptions(new ArrayList<>());
            itemIds.add(item.getId());
            itemMap.put(item.getId(), item);
            orderMap.get(item.getOrderId()).getItems().add(item);
        }
        if (!itemIds.isEmpty()) {
            for (OrderItemOption option : orders.findOptions(itemIds)) {
                itemMap.get(option.getOrderItemId()).getOptions().add(option);
            }
        }
    }

    @Override
    @Transactional
    public OrderView customerStatus(String login, Long id, OrderStatus next) {
        CustomOrder order = locked(id);
        if (!order.getAccountId().equals(accounts.require(login).getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        }
        boolean cancel = next == OrderStatus.CANCELLED && order.getStatus() == OrderStatus.PENDING;
        boolean receive = next == OrderStatus.COMPLETED && order.getStatus() == OrderStatus.SHIPPED;
        if (!cancel && !receive) throw bad("当前订单不能执行此操作");
        saveStatus(order, next);
        return order.view();
    }

    @Override
    @Transactional
    public OrderView advance(Long id, OrderStatus next) {
        CustomOrder order = locked(id);
        OrderStatus expected = switch (order.getStatus()) {
            case PENDING -> OrderStatus.CUTTING;
            case CUTTING -> OrderStatus.SEWING;
            case SEWING -> OrderStatus.QUALITY;
            case QUALITY -> OrderStatus.SHIPPED;
            case SHIPPED -> OrderStatus.COMPLETED;
            default -> null;
        };
        boolean cancel = next == OrderStatus.CANCELLED && order.getStatus() == OrderStatus.PENDING;
        if (next != expected && !cancel) throw bad("请按制作流程更新订单进度");
        saveStatus(order, next);
        return order.view();
    }

    private CustomOrder locked(Long id) {
        CustomOrder order = orders.findForUpdate(id);
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        loadDetails(List.of(order));
        return order;
    }

    private void saveStatus(CustomOrder order, OrderStatus next) {
        String previous = order.getStoredStatus();
        order.changeStatus(next);
        if (orders.updateStatus(order.getId(), previous, order.getStoredStatus(), order.getUpdatedAt()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "订单已更新，请刷新后重试");
        }
        orders.updateProduction(order.getId(), next == OrderStatus.PENDING ? "WAITING" : next.name());
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
