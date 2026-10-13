package com.lesaire.pojo;
import java.util.List;
import jakarta.validation.constraints.*;
/**
 * 订单分页结果，items 为当前页，total 为总条数，page 从 0 开始，pages 为总页数。
 */
public record OrderPage(List<OrderView> items,long total,int page,int pages) {}
