# 莱赛尔餐饮服装定制平台（精简版）

面向咖啡厅、酒店西餐厅和烘焙工坊。主要流程是 **登录 → 选择制服并报价 → 量体下单 → 查看订单与制作进度**，管理员可以上传款式照片和推进订单状态。

前端：Vue 3 + TypeScript + Vite。后端：Java 17 + Spring Boot + MyBatis Mapper/XML + MySQL + JWT `HandlerInterceptor`。密码使用 BCrypt 工具，未使用 Spring Security 的认证框架。

## 在 IDEA 运行

1. 打开 `D:\lsecode`，导入 Maven，选择 JDK 17 或更高。
2. 本机已有连接配置 `.local/mysql.properties` 和签名密钥 `.local/jwt.properties`，都被 Git 忽略。
3. 运行共享配置“莱赛尔餐饮服装定制平台”，或运行 `com.lesaire.LesaireApplication`。工作目录设为项目根目录。
4. 打开 [本地平台](http://127.0.0.1:8080/)。默认使用 `mysql` profile。

若后台脚本已占用 8080，先运行 `scripts/Stop.ps1`，再用 IDEA 启动。IDEA 启动的进程用 IDEA 停止按钮关闭。

```powershell
# 完整构建：前端类型检查、页面打包、Java 测试和 JAR
powershell -ExecutionPolicy Bypass -File scripts/Build.ps1
# 前台运行
powershell -ExecutionPolicy Bypass -File scripts/Run.ps1
# 后台启动 / 停止
powershell -ExecutionPolicy Bypass -File scripts/Start.ps1
powershell -ExecutionPolicy Bypass -File scripts/Stop.ps1
```

完整 JAR 同时提供页面、接口和照片，只使用 8080。前端开发可在 `frontend` 运行 `node node_modules/vite/bin/vite.js --host 127.0.0.1`，5173 的 `/api` 与 `/uploads` 代理到 8080。

本机 Nginx 入口使用 `scripts/Start-Nginx.ps1` 启动，可通过 [http://localhost/#home](http://localhost/#home) 访问。配置、停止方法及 Apifox 地址调整见 [Nginx 本机代理说明](docs/Nginx本机代理说明.md)。

## 只需要先理解三个业务模块

| 模块 | Controller | Service 实现 | 主要工作 |
| --- | --- | --- | --- |
| 账号 | `AccountController` | `AccountServiceImpl` | 注册、核对密码、量体保存；JWT 由工具签发和拦截器校验 |
| 目录 | `CatalogController` | `CatalogServiceImpl` | 制服和面料查询、选配校验、报价、管理员照片上传 |
| 订单 | `OrderController` | `OrderServiceImpl` | 下单、分页、订单归属、制作顺序和收货 |

`controller → service 接口 → service.impl → mapper 接口 → resources/mapper/*.xml → MySQL`。实体和请求响应数据放在 `pojo`，登录检查放在 `interceptor`，配置放在 `config`，工具放在 `utils`。Mapper 的执行对象由 MyBatis 生成，不需要手写 MapperImpl。

已去掉收藏、搜索排序、独立面料介绍页、帮助弹窗、草稿缓存、管理员统计和独立报价/图片业务层，保留完成实际定制所需的功能。先阅读 [精简版讲解](docs/精简版讲解.md)，完整文件索引见 [汇报说明](docs/汇报说明.md)。

## 数据库和登录配置

默认复用本机已有的 `lyocell_uniform`，八张表、三个制服款式和三种面料保留，MySQL 不自动执行建表或改表 SQL。

| 表 | 数据 |
| --- | --- |
| `app_user`、`customer_measurement` | 用户、角色、启用状态和量体 |
| `shirt_style`、`fabric`、`customization_option` | 制服、面料、配色与选项价格 |
| `custom_order`、`custom_order_item`、`order_item_option` | 订单、明细、尺寸与选项快照 |

`.local/mysql.properties` 保存 `spring.datasource.url`、`spring.datasource.username`、`spring.datasource.password`；也可通过 `DB_URL`、`DB_USER`、`DB_PASSWORD` 配置。新环境的签名密钥需在 `.local/jwt.properties` 配置 `lesaire.jwt.secret`，或通过 `JWT_SECRET` 提供 Base64 编码密钥；当前项目直接使用已有配置，不自动生成新密钥。不要把本地密码或密钥提交到源码。

登录返回两小时有效的 HS256 JWT，前端保存到 `sessionStorage`，请求头使用 `Authorization: Bearer <token>`。`LoginInterceptor` 校验 token 和账号，`/api/admin/` 下的接口还检查当前数据库角色。公开接口为目录、报价、注册和登录。

退出时将当前 token ID 加入内存撤销集合。每次启动生成新的运行实例标识，旧实例签发的 token 会失效，所以 **重启后需要重新登录**；不再使用撤销记录文件。JWT 只用于登录身份，不含密码。

## 保留的业务规则

- 单价 = 款式基础价 + 面料加价 + 定制选项加价，总价 = 单价 × 数量。报价和提交订单都由后端计算。
- 保存量体和金额使用 `BigDecimal`。订单保存当时的尺寸、商品名称、价格和选项快照。
- 主表、明细和选项在同一事务中保存；重复请求返回已有订单，保留请求标识、行锁和唯一约束检查。
- 管理员按待确认、裁剪、缝制、质检、发货、完成推进；用户可取消自己的待确认订单或确认收货。
- 订单每页 20 条，状态筛选作用于当前页。先查询分页主表，再批量读取明细与选项。
- 管理员上传不超过 5 MB 的 JPG/PNG；文件保存在 `.local/uploads/styles`，已有 `shirt_style.image_url` 保存地址。照片固定显示，选配不会自动修改照片。操作见 [图片上传说明](docs/图片上传说明.md)。

当前新订单是一项制服方案与一份尺寸，数量可为多件；已有多明细历史订单仍可读取。支付、短信、库存扣减、真实生产和物流没有接入，制作进度由管理员手动维护。

## 演示账号与验证

| 用途 | 用户名 | 演示密码 |
| --- | --- | --- |
| 用户 | `lesaire_demo` | `Lyocell123!` |
| 管理员 | `lesaire_admin` | `Admin123!` |

已有 MySQL 用户按原用户名、手机号和密码登录。启动时只补充缺少的演示用户名和 `LS_` 表单选项，不替换原有账号与目录数据。

`mvn test` 运行常规测试；设置 `MYSQL_INTEGRATION=true` 可额外验证真实 MySQL。应用启动后，在 `frontend` 设置 `PLAYWRIGHT_BASE_URL=http://127.0.0.1:8080` 并执行 `node node_modules/@playwright/test/cli.js test`。浏览器验收会产生标注“浏览器验收”的演示用户和订单。本次精简版的实际结果见 [验收记录](docs/verification.md)，不沿用旧版测试数量。

Apifox 导入文件在 `docs/apifox`：先导入核心 Postman 集合与本地环境，按编号测试登录、报价、订单和权限。完整步骤见 [导入与测试说明](docs/apifox/导入与测试说明.md)，另附标准 OpenAPI 文档和图片手动测试集合。

数据接入前备份：`.local/backups/lyocell_uniform-20261010-124717.sql`。精简前源码备份：`.local/backups/source-before-simplify-20261013-124713.zip`。原 H2 文件保留，未自动合并到 MySQL；可用 `--spring.profiles.active=h2` 启动独立离线演示。
