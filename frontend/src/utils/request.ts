import axios, {
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'

import router from '@/router'
import { useUserStore } from '@/stores/user'
import { toast } from '@/utils/toast'

/**
 * Axios 统一封装：
 * - 请求拦截自动注入 Bearer Token；
 * - 响应拦截解包 { code, message, data }，code=0 直接返回 data；
 * - 401 清理登录态并跳转登录，403 提示无权限，其余统一 toast。
 */
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    // blob 等非统一包装响应原样返回
    if (response.config.responseType === 'blob') {
      return response
    }
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) {
        return body.data
      }
      toast(body.message || '操作失败')
      return Promise.reject(new Error(body.message || '业务异常'))
    }
    return body
  },
  (error) => {
    const status = error?.response?.status as number | undefined
    const message = (error?.response?.data?.message as string | undefined) || ''
    if (status === 401) {
      const userStore = useUserStore()
      userStore.clearAuth()
      const inAdmin = router.currentRoute.value.path.startsWith('/admin')
      toast(message || '登录已过期，请重新登录')
      router.replace(inAdmin ? '/admin/login' : '/login')
    } else if (status === 403) {
      toast(message || '没有权限访问')
    } else if (message) {
      toast(message)
    } else {
      toast('网络开小差了，请稍后再试')
    }
    return Promise.reject(error)
  },
)

/**
 * 统一请求方法，返回值即后端 data 字段。
 */
export function request<T>(config: AxiosRequestConfig): Promise<T> {
  return service.request<unknown, T>(config)
}

export default service
