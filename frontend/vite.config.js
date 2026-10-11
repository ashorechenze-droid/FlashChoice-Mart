import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite 配置：开发代理将 /api 与 /upload 反代到后端网关（设计文档 2.2.2，网关 :8888）
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8888',
        changeOrigin: true,
      },
      '/upload': {
        target: 'http://localhost:8888',
        changeOrigin: true,
      },
    },
  },
})
