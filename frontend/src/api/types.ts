/** 后端统一出参类型（仅作文档参考，拦截器已解包） */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface PageData<T> {
  total: number
  list: T[]
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar?: string
  role: string
  status: number
}

export interface LoginResult {
  token: string
  expireIn: number
  user: UserInfo
}

export interface Category {
  id: number
  name: string
  sort: number
  status: number
}

export interface Dish {
  id: number
  categoryId: number
  name: string
  description: string
  image: string
  tags: string
  tagList: string[]
  loveValue: number
  sort: number
  status: number
  isRecommend: number
  favorite: boolean
}

export interface CartLine {
  id: number
  dishId: number
  name: string
  image: string
  loveValue: number
  quantity: number
  taste: string
  remark: string
  available: boolean
  lineLove: number
}

export interface OrderItemLine {
  id?: number
  orderId?: number
  dishId: number
  dishName: string
  dishImage: string
  loveValue: number
  quantity: number
  taste: string
  remark: string
  lineLove?: number
}

export interface Order {
  id: number
  orderNo: string
  userId: number
  nickname?: string
  status: string
  statusLabel: string
  totalLove: number
  remark: string
  expectTime?: string
  rejectReason?: string
  createTime: string
  acceptTime?: string
  finishTime?: string
  items: OrderItemLine[]
}

export interface CalendarOrder {
  id: number
  orderNo: string
  status: string
  statusLabel: string
  totalLove: number
}

export interface CalendarDay {
  date: string
  orderCount: number
  totalLove: number
  orders: CalendarOrder[]
}

export interface Calendar {
  month: string
  days: CalendarDay[]
}

export interface TopDish {
  dishId: number
  dishName: string
  count: number
}

export interface Trend {
  date: string
  count: number
}

export interface Dashboard {
  todayOrderCount: number
  pendingCount: number
  totalCount: number
  todayCompletedCount: number
  topDishes: TopDish[]
  trend: Trend[]
}

export interface PublicConfig {
  businessOpen: number
  notice: string
}

export interface RebuyResult {
  addedCount: number
  skippedNames: string[]
}

/** 系统配置实体 */
export interface SysConfigLine {
  id: number
  configKey: string
  configValue: string
  remark?: string
  updateTime?: string
}
