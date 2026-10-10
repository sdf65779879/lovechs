import { request } from '@/utils/request'

/** 上传图片，返回可访问路径 */
export function uploadImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request<string>({
    url: '/v1/files/image',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000,
  })
}
