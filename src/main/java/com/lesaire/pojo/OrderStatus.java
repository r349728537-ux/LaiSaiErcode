package com.lesaire.pojo;

/**
 * 页面订单状态：待确认、裁剪中、缝制中、质检中、已发货、已完成、已取消。
 * 数据库保存值与此枚举的对应关系由 CustomOrder 转换。
 */
public enum OrderStatus { PENDING, CUTTING, SEWING, QUALITY, SHIPPED, COMPLETED, CANCELLED }
