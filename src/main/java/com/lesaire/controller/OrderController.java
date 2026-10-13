package com.lesaire.controller;

import com.lesaire.pojo.Account;
import com.lesaire.pojo.OrderPage;
import com.lesaire.pojo.OrderRequest;
import com.lesaire.pojo.OrderView;
import com.lesaire.pojo.StatusChangeRequest;
import com.lesaire.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 用户下单与订单查询，管理员维护制作进度。 */
@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView create(@RequestAttribute("authenticatedAccount") Account user,
                            @Valid @RequestBody OrderRequest request) {
        return orders.create(user.getUsername(), request);
    }

    @GetMapping("/orders")
    public OrderPage mine(@RequestAttribute("authenticatedAccount") Account user,
                          @RequestParam(defaultValue = "0") int page) {
        return orders.list(user.getUsername(), false, page);
    }

    @PatchMapping("/orders/{id}/status")
    public OrderView customerStatus(@RequestAttribute("authenticatedAccount") Account user,
                                    @PathVariable Long id, @Valid @RequestBody StatusChangeRequest change) {
        return orders.customerStatus(user.getUsername(), id, change.status());
    }

    @GetMapping("/admin/orders")
    public OrderPage all(@RequestAttribute("authenticatedAccount") Account user,
                         @RequestParam(defaultValue = "0") int page) {
        return orders.list(user.getUsername(), true, page);
    }

    @PatchMapping("/admin/orders/{id}/status")
    public OrderView advance(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest change) {
        return orders.advance(id, change.status());
    }
}
