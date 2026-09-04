import { defineStore } from 'pinia'

const KEY = 'rh_compare_ids'

export const useCompareStore = defineStore('compare', {
  state: () => ({
    ids: JSON.parse(localStorage.getItem(KEY) || '[]')
  }),
  getters: {
    count: (state) => state.ids.length,
    has: (state) => (id) => state.ids.some((item) => String(item) === String(id))
  },
  actions: {
    toggle (id) {
      const key = String(id)
      const idx = this.ids.findIndex((item) => String(item) === key)
      if (idx >= 0) {
        this.ids.splice(idx, 1)
      } else {
        if (this.ids.length >= 4) {
          return { ok: false, message: '最多同时对比 4 台机器人' }
        }
        this.ids.push(Number(id))
      }
      this.persist()
      return { ok: true }
    },
    remove (id) {
      this.ids = this.ids.filter((item) => String(item) !== String(id))
      this.persist()
    },
    clear () {
      this.ids = []
      this.persist()
    },
    persist () {
      localStorage.setItem(KEY, JSON.stringify(this.ids))
    }
  }
})
