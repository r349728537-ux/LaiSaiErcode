# Nginx 本机访问入口

已使用 `D:\nginx\nginx-1.22.0-web\nginx.exe` 为莱赛尔平台建立独立本机入口：

```text
浏览器 http://localhost/ → Nginx 80 → Spring Boot 127.0.0.1:8080 → MySQL
```

直接打开 [平台首页](http://localhost/#home)。Nginx 接收请求后把原路径转发给 Spring Boot，后端同时提供打包后的 Vue 页面、接口和图片。浏览器根据 `#home` 切换首页，其他页面操作保持原有方式。

- 页面：`http://localhost/#home`
- 目录接口：`http://localhost/api/catalog`
- 用户订单：`GET http://localhost/api/orders`，需要 JWT。
- Apifox 使用代理入口时，将环境的前置 URL 与 `baseUrl` 都改为 `http://localhost`，重新登录后按编号测试。

从 8080 改为 80 后，浏览器的 sessionStorage 属于不同来源，第一次在新地址需要重新登录。Nginx 传递 `Authorization` 请求头，JWT 仍由后端拦截器检查。

## 配置与日常启动

项目配置为 `D:\lsecode\deploy\nginx\lesaire.conf`，只监听本机 `127.0.0.1:80` 和 `[::1]:80`。已有 `D:\nginx\nginx-1.22.0-web\conf\nginx.conf` 及其 90 端口入口保留，本项目通过独立 `-p`、`-c`、PID 与日志运行。

```powershell
cd D:\lsecode
# 已有 IDEA 的 8080 服务时复用它；服务未启动则启动项目 JAR。
.\scripts\Start-Nginx.ps1
# 只停止本项目的代理入口。
.\scripts\Stop-Nginx.ps1
```

IDEA 可以继续运行 Spring Boot。停止代理后仍可用 8080；停止后端而保留代理时请求会返回 502，重新启动后端即可。

配置核心是 `proxy_pass http://127.0.0.1:8080;`，目标不附加 URI，因此保留 `/api` 前缀和查询参数。现有 90 端口配置会删除 `/api`，所以本项目使用独立配置。所有路径统一代理，页面资源和图片使用相同入口；`client_max_body_size 6m` 与后端上传请求上限匹配。

日志及临时文件在 `D:\lsecode\.local\nginx`，启动失败查看 `error.log` 与 `startup-error.log`。修改配置后使用本项目停止、启动脚本重新运行。只运行 Nginx 安装目录里的默认命令会操作原 90 端口配置。

转发路径和 Windows 启动参数依据 [Nginx proxy_pass 文档](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_pass)及 [Windows 文档](https://nginx.org/en/docs/windows.html)。

## 本次验证

2026-10-13 配置语法检查通过；通过 16 项检查、22 次真实 HTTP 请求。IPv4/IPv6 首页、JS/CSS 与目录和 8080 一致；JWT 登录、401/403/管理员 200 保持；2 MiB 无效 multipart 请求转发到后端返回 400，已有图片地址未变。实测未创建用户或订单，也未上传有效照片。详情见 `docs/nginx-verification.json`。
