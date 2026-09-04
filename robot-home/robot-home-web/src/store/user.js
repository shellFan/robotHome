import { defineStore } from 'pinia'
import { authApi, userApi } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('rh_token') || '',
    refreshToken: localStorage.getItem('rh_refresh_token') || '',
    profile: null,
    stats: null,
    unread: 0
  }),
  getters: {
    isLogin: (state) => !!state.token,
    nickname: (state) => (state.profile && (state.profile.nickname || state.profile.username)) || ''
  },
  actions: {
    setToken (token) {
      this.token = token
      if (token) {
        localStorage.setItem('rh_token', token)
      } else {
        localStorage.removeItem('rh_token')
        localStorage.removeItem('rh_refresh_token')
        this.refreshToken = ''
        this.profile = null
        this.stats = null
      }
    },
    applyAuthResult (data) {
      if (!data || !data.token) {
        throw new Error('登录结果异常')
      }
      this.setToken(data.token)
      if (data.refreshToken) {
        this.refreshToken = data.refreshToken
        localStorage.setItem('rh_refresh_token', data.refreshToken)
      }
      if (data.user) {
        this.profile = data.user
      }
      return data
    },
    async login (payload) {
      const data = await authApi.login(payload)
      return this.applyAuthResult(data)
    },
    async register (payload) {
      const data = await authApi.register(payload)
      return this.applyAuthResult(data)
    },
    async smsLogin (payload) {
      const data = await authApi.smsLogin(payload)
      return this.applyAuthResult(data)
    },
    async loadProfile () {
      if (!this.token) {
        return null
      }
      try {
        this.profile = await userApi.me()
        return this.profile
      } catch (e) {
        this.setToken('')
        return null
      }
    },
    async loadStats () {
      if (!this.token) {
        return
      }
      try {
        this.stats = await userApi.stats()
      } catch (e) {
        this.stats = null
      }
    },
    async logout () {
      try {
        if (this.token) {
          await authApi.logout()
        }
      } catch (e) {
        // 忽略登出接口失败，本地仍清理
      }
      this.setToken('')
    }
  }
})
