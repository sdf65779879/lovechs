import { request } from '@/utils/request'

import type { LoginResult, UserInfo } from './types'

/** 账号密码登录 */
export function login(data: { username: string; password: string }) {
  return request<LoginResult>({ url: '/v1/auth/login', method: 'post', data })
}

/** 登出 */
export function logout() {
  return request<void>({ url: '/v1/auth/logout', method: 'post' })
}

/** 当前登录用户信息 */
export function getMe() {
  return request<UserInfo>({ url: '/v1/auth/me', method: 'get' })
}

/** 修改昵称/头像 */
export function updateProfile(data: { nickname: string; avatar?: string }) {
  return request<void>({ url: '/v1/auth/profile', method: 'put', data })
}

/** 修改密码 */
export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request<void>({ url: '/v1/auth/password', method: 'put', data })
}
