-- 独立离线演示和测试的表结构，MySQL 环境不执行此脚本。
-- 用户账号：保存用户名、密码哈希、角色和启用状态。
CREATE TABLE IF NOT EXISTS app_user (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,username VARCHAR(50) NOT NULL UNIQUE,password_hash VARCHAR(100) NOT NULL,
 full_name VARCHAR(50) NOT NULL,mobile VARCHAR(20),email VARCHAR(100),role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',enabled BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- 量体档案：每个客户最多一份，尺寸字段采用精确小数。
CREATE TABLE IF NOT EXISTS customer_measurement (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,customer_id BIGINT NOT NULL UNIQUE REFERENCES app_user(id),
 neck DECIMAL(6,2),shoulder DECIMAL(6,2),chest DECIMAL(6,2),waist DECIMAL(6,2),hip DECIMAL(6,2),shirt_length DECIMAL(6,2),
 sleeve_length DECIMAL(6,2),cuff DECIMAL(6,2),height DECIMAL(6,2),weight DECIMAL(6,2),body_type VARCHAR(30),remark VARCHAR(500),
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- 制服款式：保存餐饮场景、基础价、预览特征和照片地址。
CREATE TABLE IF NOT EXISTS shirt_style (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,style_code VARCHAR(30) NOT NULL UNIQUE,style_name VARCHAR(100) NOT NULL,restaurant_scene VARCHAR(50) NOT NULL,
 description VARCHAR(1000),collar_type VARCHAR(50) NOT NULL,sleeve_type VARCHAR(50) NOT NULL,placket_type VARCHAR(50),pocket_type VARCHAR(50),fit_type VARCHAR(50),
 base_price DECIMAL(10,2) NOT NULL,image_url VARCHAR(500),active BOOLEAN NOT NULL DEFAULT TRUE,created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- 面料目录：具体配色、成分、克重、库存米数和单件加价。
CREATE TABLE IF NOT EXISTS fabric (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,fabric_code VARCHAR(30) NOT NULL UNIQUE,fabric_name VARCHAR(100) NOT NULL,composition VARCHAR(200) NOT NULL,
 color_name VARCHAR(50) NOT NULL,color_hex VARCHAR(7),weight_gsm INT,stock_meters DECIMAL(10,2) NOT NULL DEFAULT 0,extra_price DECIMAL(10,2) NOT NULL DEFAULT 0,
 image_url VARCHAR(500),active BOOLEAN NOT NULL DEFAULT TRUE,created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- 定制选项：以唯一业务编码标识选项，并保存选项加价。
CREATE TABLE IF NOT EXISTS customization_option (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,option_code VARCHAR(40) NOT NULL UNIQUE,option_group VARCHAR(50) NOT NULL,option_name VARCHAR(100) NOT NULL,
 option_value VARCHAR(100) NOT NULL,price_delta DECIMAL(10,2) NOT NULL DEFAULT 0,image_url VARCHAR(500),sort_order INT NOT NULL DEFAULT 0,
 active BOOLEAN NOT NULL DEFAULT TRUE,created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- 订单主表：保存客户、幂等订单号、状态、收件信息和成交总额。
CREATE TABLE IF NOT EXISTS custom_order (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,order_no VARCHAR(32) NOT NULL UNIQUE,customer_id BIGINT NOT NULL REFERENCES app_user(id),
 status VARCHAR(30) NOT NULL DEFAULT 'PENDING_CONFIRMATION',contact_name VARCHAR(50) NOT NULL,contact_mobile VARCHAR(20) NOT NULL,
 delivery_address VARCHAR(500) NOT NULL,expected_delivery_date DATE,total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
 customer_note VARCHAR(1000),internal_note VARCHAR(1000),created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- 订单明细：关联款式与面料，保存成交金额和下单量体 JSON 快照。
CREATE TABLE IF NOT EXISTS custom_order_item (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,order_id BIGINT NOT NULL REFERENCES custom_order(id),style_id BIGINT NOT NULL REFERENCES shirt_style(id),fabric_id BIGINT NOT NULL REFERENCES fabric(id),
 product_name VARCHAR(150) NOT NULL,quantity INT NOT NULL CHECK(quantity>0),unit_price DECIMAL(10,2) NOT NULL,item_amount DECIMAL(12,2) NOT NULL,measurement_snapshot JSON,
 production_status VARCHAR(30) NOT NULL DEFAULT 'WAITING',created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- 明细选项快照：保存成交时的选项分组、名称、值与加价。
CREATE TABLE IF NOT EXISTS order_item_option (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,order_item_id BIGINT NOT NULL REFERENCES custom_order_item(id),customization_option_id BIGINT NOT NULL REFERENCES customization_option(id),
 option_group_snapshot VARCHAR(50) NOT NULL,option_name_snapshot VARCHAR(100) NOT NULL,option_value_snapshot VARCHAR(100) NOT NULL,price_delta_snapshot DECIMAL(10,2) NOT NULL DEFAULT 0,
 UNIQUE(order_item_id,customization_option_id)
);
