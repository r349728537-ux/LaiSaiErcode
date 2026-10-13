import { defineConfig } from '@playwright/test'
// 浏览器测试配置，使用本机 Chrome，失败时保留截图和追踪信息。
export default defineConfig({
// 串行执行业务验收，避免共享演示数据的并发测试互相影响。
  testDir: './e2e', fullyParallel: false, workers: 1, timeout: 45000,
// 通过 PLAYWRIGHT_BASE_URL 指定目标服务，默认连接前端开发端口。
  use: { baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173', channel: 'chrome', headless: true, viewport: { width: 1440, height: 1000 }, trace: 'retain-on-failure', screenshot: 'only-on-failure' },
  reporter: 'list',
})
