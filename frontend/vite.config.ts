import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
// 前端开发配置，Vue 单文件组件由插件处理。
export default defineConfig({
  plugins: [vue()],
// 开发服务使用 5173，API 与上传图片代理到本机后端 8080。
  server: { port: 5173, strictPort: true, proxy: { '/api': 'http://127.0.0.1:8080', '/uploads': 'http://127.0.0.1:8080' } },
})
