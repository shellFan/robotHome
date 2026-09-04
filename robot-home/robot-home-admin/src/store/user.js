import { defineStore } from 'pinia'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { login as loginApi, logout as logoutApi, getInfo } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    id: null,
    username: '',
    nickname: '',
    avatar: '',
    roles: [],
    permissions: [],
    menus: [],
    loaded: false
  }),
  getters: {
    displayName: (state) => state.nickname || state.username || '管理员'
  },
  actions: {
    async login(userInfo) {
      const data = await loginApi(userInfo)
      const token = (data && data.token) || ''
      if (!token) {
        throw new Error('登录失败：未返回 token')
      }
      this.token = token
      setToken(token)
      if (data.nickname) this.nickname = data.nickname
      if (data.username) this.username = data.username
      return data
    },

    async fetchInfo() {
      const data = await getInfo()
      if (!data) return null
      this.id = data.id ?? null
      this.username = data.username || ''
      this.nickname = data.nickname || ''
      this.avatar = data.avatar || ''
      this.roles = data.roles || []
      this.permissions = data.permissions || []
      this.menus = data.menus || []
      this.loaded = true
      return data
    },

    hasPermission(code) {
      if (!code) return true
      if (!this.permissions || this.permissions.length === 0) return true
      return this.permissions.includes(code)
    },

    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 登出接口失败也要清理本地状态
      }
      this.resetState()
    },

    resetState() {
      this.token = ''
      this.id = null
      this.username = ''
      this.nickname = ''
      this.avatar = ''
      this.roles = []
      this.permissions = []
      this.menus = []
      this.loaded = false
      removeToken()
    }
  }
})
