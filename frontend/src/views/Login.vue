<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-side">
        <span class="side-mark">
          <svg viewBox="0 0 24 24" width="30" height="30" fill="currentColor" aria-hidden="true">
            <path d="M13 2 4.5 13.5H11L9.5 22 19 9.5h-6.5L13 2z" />
          </svg>
        </span>
        <h2>极速优选</h2>
        <p>质感生活，即刻拥有</p>
      </div>

      <div class="login-form">
        <el-tabs v-model="mode" stretch>
          <!-- 登录 -->
          <el-tab-pane label="登录" name="login">
            <el-form :model="loginForm" size="large" @submit.prevent>
              <el-form-item>
                <el-input v-model="loginForm.account" placeholder="账号" :prefix-icon="User" clearable />
              </el-form-item>
              <el-form-item>
                <el-input v-model="loginForm.password" type="password" placeholder="密码" :prefix-icon="Lock"
                          show-password @keyup.enter="onLogin" />
              </el-form-item>
              <el-form-item class="captcha-row">
                <el-input v-model="loginForm.captcha" placeholder="验证码" :prefix-icon="Key"
                          maxlength="5" clearable @keyup.enter="onLogin" />
                <img v-if="captchaImg" :src="captchaImg" class="captcha-img"
                     alt="验证码" title="点击刷新" @click="refreshCaptcha" />
                <div v-else class="captcha-img captcha-placeholder" @click="refreshCaptcha">加载中</div>
              </el-form-item>
              <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="onLogin">
                登 录
              </el-button>
            </el-form>
            <p class="demo-tip">演示账号：zhangsan / 123456（已接入真实后端网关，会员密码均为 123456）</p>
          </el-tab-pane>

          <!-- 注册 -->
          <el-tab-pane label="注册" name="register">
            <el-form :model="regForm" size="large" @submit.prevent>
              <el-form-item>
                <el-input v-model="regForm.account" placeholder="账号" :prefix-icon="User" clearable />
              </el-form-item>
              <el-form-item>
                <el-input v-model="regForm.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
              </el-form-item>
              <el-form-item>
                <el-input v-model="regForm.confirm" type="password" placeholder="确认密码" :prefix-icon="Lock"
                          show-password @keyup.enter="onRegister" />
              </el-form-item>
              <el-form-item class="captcha-row">
                <el-input v-model="regForm.captcha" placeholder="验证码" :prefix-icon="Key"
                          maxlength="5" clearable @keyup.enter="onRegister" />
                <img v-if="regCaptchaImg" :src="regCaptchaImg" class="captcha-img"
                     alt="验证码" title="点击刷新" @click="refreshRegCaptcha" />
                <div v-else class="captcha-img captcha-placeholder" @click="refreshRegCaptcha">加载中</div>
              </el-form-item>
              <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="onRegister">
                注 册
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { getCaptcha } from '../api/member'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const mode = ref('login')
const loading = ref(false)
const loginForm = ref({ account: '', password: '', captcha: '', captchaId: '' })
const regForm = ref({ account: '', password: '', confirm: '', captcha: '', captchaId: '' })
const captchaImg = ref('')
const regCaptchaImg = ref('')

// 加载登录验证码
async function refreshCaptcha() {
  try {
    const data = await getCaptcha()
    loginForm.value.captchaId = data.captchaId
    captchaImg.value = data.imageBase64
    loginForm.value.captcha = ''
  } catch (e) { /* 验证码加载失败不阻断 */ }
}

// 加载注册验证码
async function refreshRegCaptcha() {
  try {
    const data = await getCaptcha()
    regForm.value.captchaId = data.captchaId
    regCaptchaImg.value = data.imageBase64
    regForm.value.captcha = ''
  } catch (e) { /* 验证码加载失败不阻断 */ }
}

function back() {
  router.push(route.query.redirect || '/')
}

async function onLogin() {
  if (!loginForm.value.account || !loginForm.value.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  if (!loginForm.value.captcha) {
    ElMessage.warning('请输入验证码')
    return
  }
  loading.value = true
  try {
    await user.login({ ...loginForm.value })
    ElMessage.success('登录成功，欢迎回来')
    back()
  } catch (e) {
    // 登录失败后刷新验证码（验证码一次性）
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

async function onRegister() {
  const { account, password, confirm, captcha, captchaId } = regForm.value
  if (!account || !password) { ElMessage.warning('请填写账号和密码'); return }
  if (password !== confirm) { ElMessage.warning('两次输入的密码不一致'); return }
  if (!captcha) { ElMessage.warning('请输入验证码'); return }
  loading.value = true
  try {
    await user.register({ account, password, captcha, captchaId })
    ElMessage.success('注册成功，已自动登录')
    back()
  } catch (e) {
    // 注册失败后刷新验证码
    refreshRegCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
  refreshRegCaptcha()
})
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - var(--nav-h));
  display: grid; place-items: center;
  padding: 48px 0;
  background:
    radial-gradient(600px 300px at 15% 10%, rgba(47, 107, 255, .1), transparent),
    radial-gradient(500px 260px at 85% 85%, rgba(255, 107, 53, .08), transparent),
    var(--bg);
}
.login-card {
  display: grid;
  grid-template-columns: 1fr 1.1fr;
  width: min(860px, calc(100% - 32px));
  background: var(--surface);
  border-radius: 24px;
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}
.login-side {
  background: linear-gradient(150deg, var(--brand-deep), var(--brand-2));
  color: #fff;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 14px;
  padding: 48px 32px;
}
.side-mark {
  width: 64px; height: 64px; border-radius: 18px;
  display: grid; place-items: center;
  background: rgba(255, 255, 255, .18);
  -webkit-backdrop-filter: blur(6px);
  backdrop-filter: blur(6px);
}
.login-side h2 { font-size: 28px; font-weight: 900; letter-spacing: 2px; }
.login-side p { font-size: 14px; opacity: .85; }

.login-form { padding: 44px 40px; }
.submit-btn { width: 100%; font-weight: 700; letter-spacing: 6px; }
.demo-tip { margin-top: 16px; font-size: 12.5px; color: var(--ink-2); text-align: center; }

/* 验证码行：输入框 + 图片并排（flex 作用于 el-form-item__content 内部） */
.captcha-row :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
  gap: 10px;
}
.captcha-row :deep(.el-input) {
  flex: 1;
}
.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 8px;
  border: 1px solid var(--line);
  cursor: pointer;
  object-fit: cover;
  flex-shrink: 0;
  transition: opacity .2s;
}
.captcha-img:hover { opacity: .8; }
.captcha-placeholder {
  display: grid;
  place-items: center;
  background: var(--bg);
  color: var(--ink-2);
  font-size: 12px;
}

@media (max-width: 768px) {
  .login-card { grid-template-columns: 1fr; }
  .login-side { padding: 30px 24px; }
  .login-form { padding: 30px 24px; }
}
</style>
