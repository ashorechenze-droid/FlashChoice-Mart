<template>
  <footer class="footer">
    <div class="container footer-inner">
      <div class="footer-brand">
        <span class="logo-mark">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true">
            <path d="M13 2 4.5 13.5H11L9.5 22 19 9.5h-6.5L13 2z" />
          </svg>
        </span>
        <div>
          <strong>极速优选</strong>
          <p>基于 Spring Cloud 的 B2C 微服务秒杀商城 · PC 商城前端</p>
        </div>
      </div>
      <div class="footer-links">
        <span @click="ElMessage.info('关于我们（演示）')">关于我们</span>
        <span @click="ElMessage.info('配送服务（演示）')">配送服务</span>
        <span @click="ElMessage.info('售后服务（演示）')">售后服务</span>
        <span @click="ElMessage.info('联系客服（演示）')">联系客服</span>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { ElMessage } from 'element-plus'
</script>

<style scoped>
.footer {
  margin-top: auto;
  border-top: 1px solid var(--line);
  background: var(--surface);
  padding: 40px 0 calc(120px + var(--safe-bottom)); /* 底部留出悬浮按钮空间 + 安全区 */
}
.footer-inner { display: flex; justify-content: space-between; align-items: center; gap: 20px; flex-wrap: wrap; }
.footer-brand { display: flex; align-items: center; gap: 10px; }
.logo-mark {
  width: 36px; height: 36px; border-radius: 10px;
  display: grid; place-items: center;
  background: linear-gradient(135deg, var(--brand-deep), var(--brand-2));
  color: #fff;
}
.footer-brand strong { font-size: 18px; font-weight: 800; }
.footer p { font-size: 13px; color: var(--ink-2); }
.footer-links { display: flex; gap: 18px; font-size: 13px; color: var(--ink-2); }
.footer-links span { cursor: pointer; transition: color .3s ease; }
.footer-links span:hover { color: var(--brand); }
</style>
