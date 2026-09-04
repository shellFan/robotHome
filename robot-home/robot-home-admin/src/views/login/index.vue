<template>
  <div class="login">
    <div class="login__panel">
      <div class="login__brand">
        <span class="login__logo">RH</span>
        <div>
          <h1 class="login__title">机器人之家</h1>
          <p class="login__subtitle">管理后台</p>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入管理员账号" :prefix-icon="'User'" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="'Lock'"
            show-password
            clearable
          />
        </el-form-item>
        <el-button type="primary" class="login__submit" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>


    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function handleLogin() {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login({ username: form.username, password: form.password })
      await userStore.fetchInfo()
      ElMessage.success('登录成功')
      const redirect = route.query && route.query.redirect ? String(route.query.redirect) : '/dashboard'
      router.replace(redirect)
    } catch (e) {
      // request.js 已统一提示错误
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: #f4f6f9;
}

.login__panel {
  width: 380px;
  padding: 36px 32px 28px;
  background: #fff;
  border: 1px solid var(--rh-border);
  border-radius: 10px;
}

.login__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.login__logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: var(--rh-primary);
  color: #fff;
  font-weight: 700;
}

.login__title {
  margin: 0;
  font-size: 19px;
  font-weight: 600;
  color: var(--rh-text);
}

.login__subtitle {
  margin: 2px 0 0;
  font-size: 13px;
  color: var(--rh-text-weak);
}

.login__submit {
  width: 100%;
  margin-top: 4px;
  background: var(--rh-primary);
  border-color: var(--rh-primary);
}

.login__tip {
  margin: 18px 0 0;
  font-size: 12px;
  color: var(--rh-text-weak);
  text-align: center;
}
</style>
