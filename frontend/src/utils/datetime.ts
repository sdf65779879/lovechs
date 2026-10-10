/**
 * 日期与问候相关工具。
 */

/** 订单状态 → 中文文案（与后端 statusLabel 保持一致，兜底用） */
export const ORDER_STATUS_LABEL: Record<string, string> = {
  PENDING: '待接单',
  ACCEPTED: '已接单',
  COOKING: '制作中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REJECTED: '已拒绝',
}

/** 进行中的状态集合 */
export const ACTIVE_STATUSES = ['PENDING', 'ACCEPTED', 'COOKING']

/** 按当前小时返回问候语 */
export function greeting(date = new Date()): string {
  const hour = date.getHours()
  if (hour < 5) return '夜深了'
  if (hour < 10) return '早上好呀'
  if (hour < 12) return '中午好'
  if (hour < 14) return '午饭时间到'
  if (hour < 18) return '下午好'
  if (hour < 22) return '晚上好呀'
  return '夜宵时间'
}

/** 格式化为 yyyy-MM-dd */
export function formatDate(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** 中文长日期，如「10月10日 星期六」 */
export function chineseDate(date = new Date()): string {
  const weeks = ['日', '一', '二', '三', '四', '五', '六']
  return `${date.getMonth() + 1}月${date.getDate()}日 星期${weeks[date.getDay()]}`
}

/** 把后端时间字符串裁剪为 HH:mm */
export function shortTime(time?: string): string {
  if (!time) return ''
  // 后端格式 yyyy-MM-dd HH:mm:ss 或 ISO
  const match = time.match(/(\d{2}:\d{2})/)
  return match ? match[1] : ''
}

/** 生成 yyyy-MM 月份字符串 */
export function monthString(date = new Date()): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

/** 简单防抖 */
export function debounce<A extends unknown[]>(
  fn: (...args: A) => void,
  delay = 300,
): (...args: A) => void {
  let timer: number | undefined
  return (...args: A) => {
    window.clearTimeout(timer)
    timer = window.setTimeout(() => fn(...args), delay)
  }
}
