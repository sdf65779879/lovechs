import { request } from '@/utils/request'

import type { PublicConfig } from './types'

/** 公开配置（营业状态 + 公告） */
export function getPublicConfig() {
  return request<PublicConfig>({ url: '/v1/configs/public', method: 'get' })
}
