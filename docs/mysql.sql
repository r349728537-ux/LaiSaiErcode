-- 仅用于首次安装 MySQL 环境，已有本机数据库无需执行此脚本。
CREATE DATABASE IF NOT EXISTS lyocell_uniform CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- 选择该数据库后，执行一次 docs/existing-mysql-schema.sql 创建八张业务表。
-- 应用启动时校验已有结构，不自动创建、修改或删除 MySQL 表。
