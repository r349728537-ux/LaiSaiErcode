<!-- 登录与注册共用弹窗；注册成功后继续登录，统一返回账号资料。 -->
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Leaf, LoaderCircle } from 'lucide-vue-next'
import ModalShell from './ModalShell.vue'
import { api, saveToken } from '../api'
import type { Account } from '../types'
const emit = defineEmits<{ close: []; authenticated: [account: Account] }>()
const mode = ref<'login' | 'register'>('login')
const form = reactive({ phone: '', password: '', displayName: '' })
const busy = ref(false)
const error = ref('')
function chooseMode(next: 'login' | 'register') {
  mode.value = next
  error.value = ''
}
// 填入已配置的本地演示账号，仍通过正常登录接口校验。
function demo(admin = false) {
  mode.value = 'login'
  form.phone = admin ? 'lesaire_admin' : 'lesaire_demo'
  form.password = admin ? 'Admin123!' : 'Lyocell123!'
}
// 按当前模式先完成注册或直接登录，保存 JWT 后通知父组件更新账号状态。
async function submit() {
  busy.value = true
  error.value = ''
  try {
    if (mode.value === 'register') await api('/auth/register', { method: 'POST', body: JSON.stringify(form) })
    const result = await api<{ token: string; account: Account }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username: form.phone, password: form.password }),
    })
    saveToken(result.token)
    emit('authenticated', result.account)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '登录失败，请重试'
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <ModalShell title="欢迎来到莱赛尔" @close="emit('close')">
    <div class="auth-intro">
      <span class="brand-mark"><Leaf :size="26" /></span>
      <p>保存你的量体数据，让每一次定制都更合身。</p>
    </div>
    <div class="segment">
      <button
        :class="{ active: mode === 'login' }"
        @click="chooseMode('login')"
      >
        账号登录
      </button>
      <button
        :class="{ active: mode === 'register' }"
        @click="chooseMode('register')"
      >
        创建账号
      </button>
    </div>
    <form class="form-stack" @submit.prevent="submit">
      <label v-if="mode === 'register'">
        你的称呼
        <input
          v-model.trim="form.displayName"
          autocomplete="nickname"
          maxlength="30"
          required
          placeholder="我们该如何称呼你"
        />
      </label>
      <label>
        {{ mode === 'register' ? '手机号' : '用户名或手机号' }}
        <input
          v-model="form.phone"
          :type="mode === 'register' ? 'tel' : 'text'"
          autocomplete="username"
          :pattern="mode === 'register' ? '1[3-9][0-9]{9}' : undefined"
          :maxlength="mode === 'register' ? 11 : 50"
          required
          :placeholder="mode === 'register' ? '请输入手机号' : '请输入用户名或手机号'"
        />
      </label>
      <label>
        密码
        <input
          v-model="form.password"
          type="password"
          :autocomplete="mode === 'register' ? 'new-password' : 'current-password'"
          minlength="8"
          maxlength="64"
          required
          placeholder="至少 8 位字符"
        />
      </label>
      <p v-if="error" class="error-message" role="alert">{{ error }}</p>
      <button class="button primary full" :disabled="busy">
        <LoaderCircle v-if="busy" class="spin" :size="17" />
        {{ busy ? '正在登录…' : mode === 'login' ? '登录' : '注册并登录' }}
      </button>
    </form>
    <div class="demo-account">
      <span>本地演示账号</span>
      <div>
        <button class="text-button" @click="demo()">填入用户账号</button>
        <button class="text-button" @click="demo(true)">填入管理员账号</button>
      </div>
    </div>
  </ModalShell>
</template>
