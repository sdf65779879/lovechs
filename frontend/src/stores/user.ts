import { defineStore } from 'pinia'

import * as authApi from '@/api/auth'
import type { UserInfo } from '@/api/types'

const TOKEN_KEY = 'couple_token'
const USER_KEY = 'couple_user'

/** 从 localStorage / sessionStorage 恢复登录态 */
function restore(key: string): string | null {
  return localStorage.getItem(key) ?? sessionStorage.getItem(key)
}

interface UserState {
  token: string
  user: UserInfo | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: restore(TOKEN_KEY) || '',
    user: (() => {
      const raw = restore(USER_KEY)
      try {
        return raw ? (JSON.parse(raw) as UserInfo) : null
      } catch {
        return null
      }
    })(),
  }),

  getters: {
    isLogin: (state) => !!state.token,
    isAdmin: (state) => state.user?.role === 'ADMIN',
    nickname: (state) => state.user?.nickname ?? '',
  },

  actions: {
    /**
     * 登录。
     *
     * @param remember true 存 localStorage（自动登录），false 存 sessionStorage（本次会话）
     */
    async login(username: string, password: string, remember = true) {
      const result = await authApi.login({ username, password })
      this.persist(result.token, result.user, remember)
    },

    /** 持久化登录态 */
    persist(token: string, user: UserInfo, remember: boolean) {
      this.token = token
      this.user = user
      const storage = remember ? localStorage : sessionStorage
      storage.setItem(TOKEN_KEY, token)
      storage.setItem(USER_KEY, JSON.stringify(user))
    },

    /** 拉取最新用户信息并同步缓存 */
    async fetchMe() {
      const user = await authApi.getMe()
      this.user = user
      const raw = localStorage.getItem(USER_KEY)
      const storage = raw ? localStorage : sessionStorage
      storage.setItem(USER_KEY, JSON.stringify(user))
      return user
    },

    /** 登出并清理本地态 */
    async logout() {
      try {
        await authApi.logout()
      } finally {
        this.clearAuth()
      }
    },

    /** 仅清理本地登录态 */
    clearAuth() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      sessionStorage.removeItem(TOKEN_KEY)
      sessionStorage.removeItem(USER_KEY)
    },
  },
})
