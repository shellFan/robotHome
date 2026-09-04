<template>
  <MainLayout>
    <div class="rh-container login-page">
      <div class="login-card rh-card">
        <h1 class="login-card__title">登录 / 注册</h1>
        <el-tabs v-model="mode">
          <el-tab-pane label="密码登录" name="password">
            <el-form :model="pwdForm" @submit.prevent="submitPassword">
              <el-form-item>
                <el-input v-model="pwdForm.username" placeholder="用户名 / 手机号" size="large" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="pwdForm.password" type="password" placeholder="密码" size="large" show-password />
              </el-form-item>
              <el-button type="primary" size="large" class="login-card__btn" :loading="loading" @click="submitPassword">
                登录
              </el-button>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="短信登录" name="sms">
            <el-form :model="smsForm" @submit.prevent="submitSms">
              <el-form-item>
                <el-input v-model="smsForm.phone" placeholder="手机号" size="large" maxlength="11" />
              </el-form-item>
              <el-form-item class="login-card__code">
                <el-input v-model="smsForm.code" placeholder="验证码" size="large" maxlength="6" />
                <el-button :disabled="countdown > 0" @click="sendCode">{{ countdown > 0 ? countdown + 's' : '获取验证码' }}</el-button>
              </el-form-item>
              <p v-if="devCode" class="login-card__dev">开发模式验证码：{{ devCode }}</p>
              <el-button type="primary" size="large" class="login-card__btn" :loading="loading" @click="submitSms">登录</el-button>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="注册" name="register">
            <el-form :model="regForm" @submit.prevent="submitRegister">
              <el-form-item>
                <el-input v-model="regForm.phone" placeholder="手机号（选填）" size="large" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="regForm.username" placeholder="用户名" size="large" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="regForm.nickname" placeholder="昵称（选填）" size="large" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="regForm.password" type="password" placeholder="密码" size="large" show-password />
              </el-form-item>
              <el-button type="primary" size="large" class="login-card__btn" :loading="loading" @click="submitRegister">注册并登录</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layout/MainLayout.vue'
import { authApi } from '@/api'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const mode = ref('password')
const loading = ref(false)
const countdown = ref(0)
const devCode = ref('')
const pwdForm = reactive({ username: '', password: '' })
const smsForm = reactive({ phone: '', code: '' })
const regForm = reactive({ username: '', phone: '', password: '', nickname: '' })

function goBack () {
  const redirect = route.query.redirect || '/'
  router.replace(String(redirect))
}

async function submitPassword () {
  if (!pwdForm.username || !pwdForm.password) return ElMessage.warning('请输入账号和密码')
  loading.value = true
  try {
    await userStore.loginByPassword(pwdForm.username, pwdForm.password)
    ElMessage.success('登录成功')
    goBack()
  } finally {
    loading.value = false
  }
}

async function sendCode () {
  if (!/^1\d{10}$/.test(smsForm.phone)) return ElMessage.warning('请输入正确手机号')
  const data = await authApi.sendSmsCode(smsForm.phone, 'login')
  devCode.value = data && data.devCode ? data.devCode : ''
  ElMessage.success('验证码已发送')
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}

async function submitSms () {
  if (!smsForm.phone || !smsForm.code) return ElMessage.warning('请输入手机号和验证码')
  loading.value = true
  try {
    await userStore.loginBySms(smsForm.phone, smsForm.code)
    ElMessage.success('登录成功')
    goBack()
  } finally {
    loading.value = false
  }
}

async function submitRegister () {
  if (!regForm.username || !regForm.password) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    await userStore.register(regForm)
    ElMessage.success('注册成功')
    goBack()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { display: flex; justify-content: center; padding: 48px 0 80px; }
.login-card { width: 420px; padding: 32px; }
.login-card__title { margin: 0 0 24px; font-size: 22px; text-align: center; }
.login-card__btn { width: 100%; margin-top: 8px; }
.login-card__code { display: flex; gap: 8px; }
.login-card__dev { margin: 0 0 8px; font-size: 12px; color: var(--rh-warning); }
</style>
