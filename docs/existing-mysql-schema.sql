-- 已有 MySQL 表结构参考；新安装时可用于建表，已有数据库仅核对结构。
-- 用户账号：保存用户名、密码哈希、角色和启用状态。
CREATE TABLE `app_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(100) NOT NULL,
  `full_name` varchar(50) NOT NULL,
  `mobile` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'CUSTOMER',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  CONSTRAINT `ck_app_user_role` CHECK ((`role` in (_gbk'CUSTOMER',_gbk'ADMIN',_gbk'DESIGNER',_gbk'TAILOR')))
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 制服款式：保存餐饮场景、基础价、预览特征和照片地址。
CREATE TABLE `shirt_style` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `style_code` varchar(30) NOT NULL,
  `style_name` varchar(100) NOT NULL,
  `restaurant_scene` varchar(50) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `collar_type` varchar(50) NOT NULL,
  `sleeve_type` varchar(50) NOT NULL,
  `placket_type` varchar(50) DEFAULT NULL,
  `pocket_type` varchar(50) DEFAULT NULL,
  `fit_type` varchar(50) DEFAULT NULL,
  `base_price` decimal(10,2) NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `style_code` (`style_code`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 面料目录：具体配色、成分、克重、库存米数和单件加价。
CREATE TABLE `fabric` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fabric_code` varchar(30) NOT NULL,
  `fabric_name` varchar(100) NOT NULL,
  `composition` varchar(200) NOT NULL,
  `color_name` varchar(50) NOT NULL,
  `color_hex` varchar(7) DEFAULT NULL,
  `weight_gsm` int DEFAULT NULL,
  `stock_meters` decimal(10,2) NOT NULL DEFAULT '0.00',
  `extra_price` decimal(10,2) NOT NULL DEFAULT '0.00',
  `image_url` varchar(500) DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `fabric_code` (`fabric_code`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 定制选项：以唯一业务编码标识选项，并保存选项加价。
CREATE TABLE `customization_option` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `option_code` varchar(40) NOT NULL,
  `option_group` varchar(50) NOT NULL,
  `option_name` varchar(100) NOT NULL,
  `option_value` varchar(100) NOT NULL,
  `price_delta` decimal(10,2) NOT NULL DEFAULT '0.00',
  `image_url` varchar(500) DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `option_code` (`option_code`)
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 量体档案：每个客户最多一份，尺寸字段采用精确小数。
CREATE TABLE `customer_measurement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customer_id` bigint NOT NULL,
  `neck` decimal(6,2) DEFAULT NULL,
  `shoulder` decimal(6,2) DEFAULT NULL,
  `chest` decimal(6,2) DEFAULT NULL,
  `waist` decimal(6,2) DEFAULT NULL,
  `hip` decimal(6,2) DEFAULT NULL,
  `shirt_length` decimal(6,2) DEFAULT NULL,
  `sleeve_length` decimal(6,2) DEFAULT NULL,
  `cuff` decimal(6,2) DEFAULT NULL,
  `height` decimal(6,2) DEFAULT NULL,
  `weight` decimal(6,2) DEFAULT NULL,
  `body_type` varchar(30) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `customer_id` (`customer_id`),
  CONSTRAINT `fk_measurement_customer` FOREIGN KEY (`customer_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 订单主表：保存客户、幂等订单号、状态、收件信息和成交总额。
CREATE TABLE `custom_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL,
  `customer_id` bigint NOT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'PENDING_CONFIRMATION',
  `contact_name` varchar(50) NOT NULL,
  `contact_mobile` varchar(20) NOT NULL,
  `delivery_address` varchar(500) NOT NULL,
  `expected_delivery_date` date DEFAULT NULL,
  `total_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `customer_note` varchar(1000) DEFAULT NULL,
  `internal_note` varchar(1000) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_no` (`order_no`),
  KEY `idx_custom_order_customer_created` (`customer_id`,`created_at`),
  KEY `idx_custom_order_status` (`status`),
  CONSTRAINT `fk_order_customer` FOREIGN KEY (`customer_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `ck_order_status` CHECK ((`status` in (_gbk'DRAFT',_gbk'PENDING_CONFIRMATION',_gbk'CONFIRMED',_gbk'IN_PRODUCTION',_gbk'QUALITY_CHECK',_gbk'SHIPPED',_gbk'COMPLETED',_gbk'CANCELLED')))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 订单明细：关联款式与面料，保存成交金额和下单量体 JSON 快照。
CREATE TABLE `custom_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `style_id` bigint NOT NULL,
  `fabric_id` bigint NOT NULL,
  `product_name` varchar(150) NOT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `item_amount` decimal(12,2) NOT NULL,
  `measurement_snapshot` json DEFAULT NULL,
  `production_status` varchar(30) NOT NULL DEFAULT 'WAITING',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_order_item_order` (`order_id`),
  KEY `fk_order_item_style` (`style_id`),
  KEY `fk_order_item_fabric` (`fabric_id`),
  CONSTRAINT `fk_order_item_fabric` FOREIGN KEY (`fabric_id`) REFERENCES `fabric` (`id`),
  CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `custom_order` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_order_item_style` FOREIGN KEY (`style_id`) REFERENCES `shirt_style` (`id`),
  CONSTRAINT `ck_item_quantity` CHECK ((`quantity` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 明细选项快照：保存成交时的选项分组、名称、值与加价。
CREATE TABLE `order_item_option` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_item_id` bigint NOT NULL,
  `customization_option_id` bigint NOT NULL,
  `option_group_snapshot` varchar(50) NOT NULL,
  `option_name_snapshot` varchar(100) NOT NULL,
  `option_value_snapshot` varchar(100) NOT NULL,
  `price_delta_snapshot` decimal(10,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_option` (`order_item_id`,`customization_option_id`),
  KEY `fk_item_option_option` (`customization_option_id`),
  CONSTRAINT `fk_item_option_item` FOREIGN KEY (`order_item_id`) REFERENCES `custom_order_item` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_item_option_option` FOREIGN KEY (`customization_option_id`) REFERENCES `customization_option` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

