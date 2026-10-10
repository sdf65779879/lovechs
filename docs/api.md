# 情侣点餐系统 · API 接口文档

## 1. 通用说明

### 1.1 Base URL

| 环境 | Base URL |
|---|---|
| Docker 部署 | `http://服务器IP[:WEB_PORT]/api`（如 `http://192.168.1.100/api`） |
| 本地开发 | `http://localhost:8091/api`（前端 dev 走 Vite 代理 `/api`） |

> 后端 `server.servlet.context-path=/api`，下文所有路径均以 `/api` 为前缀。

### 1.2 统一响应结构

所有接口返回 `application/json`，结构如下：

```json
{
  "code": 0,          // 0 成功；非 0 失败（见错误码表）
  "message": "成功",  // 提示信息
  "data": { ... }     // 业务数据，失败时为 null
}
```

分页接口的 `data` 结构：

```json
{
  "total": 42,
  "list": [ { ... } ]
}
```

### 1.3 认证方式

除以下**白名单**接口外，所有接口均需在请求头携带登录令牌：

```
Authorization: Bearer <token>
```

白名单（无需登录）：`POST /api/v1/auth/login`、`GET /api/v1/health`、Knife4j 文档路径（`/api/doc.html`、`/api/v3/api-docs/**` 等）。

- 令牌由登录接口返回（`data.token`），有效期默认 7 天（`JWT_EXPIRE`）。
- 未携带/过期 → HTTP 401，`code=401`。
- 已登录但访问管理端接口（`/v1/admin/**`）且角色非 ADMIN → HTTP 403，`code=403`。

### 1.4 幂等键（下单防重）

`POST /api/v1/orders` 支持请求头 `Idempotency-Key`（建议前端用 UUID）。相同键 10 分钟内重复提交返回同一订单，不重复扣减。

### 1.5 错误码表

| code | HTTP | 含义 |
|---|---|---|
| 0 | 200 | 成功 |
| 400 | 400 | 参数错误（校验不通过） |
| 401 | 401 | 未登录或登录已过期 |
| 403 | 403 | 没有权限访问 |
| 404 | 404 | 资源不存在 |
| 429 | 429 | 请求过于频繁（登录接口同 IP 每分钟 10 次） |
| 1000 | 200 | 操作失败（通用业务错误） |
| 1001 | 200 | 账号或密码错误 |
| 1002 | 200 | 账号已被禁用 |
| 1003 | 200 | 当前订单状态不允许该操作 |
| 1004 | 200 | 请勿重复提交 |
| 1005 | 200 | 文件上传失败（格式/类型不符） |
| 1006 | 200 | 今日暂不营业 |
| 500 | 500 | 系统繁忙（未知异常） |

### 1.6 在线接口文档

Docker 部署后访问 Knife4j 文档：`http://服务器IP[:WEB_PORT]/api/doc.html`（无需登录）。

---

## 2. 接口总览

| # | 分组 | 前缀 | 鉴权 |
|---|---|---|---|
| 0 | 健康检查 | `/v1/health` | 白名单 |
| 1 | 认证 | `/v1/auth` | 登录白名单，其余需登录 |
| 2 | 分类（用户端） | `/v1/categories` | 需登录 |
| 3 | 菜品（用户端） | `/v1/dishes` | 需登录 |
| 4 | 购物车 | `/v1/cart` | 需登录 |
| 5 | 收藏 | `/v1/favorites` | 需登录 |
| 6 | 订单（用户端） | `/v1/orders` | 需登录 |
| 7 | 文件 | `/v1/files` | 需登录 |
| 8 | 配置（用户端） | `/v1/configs` | 需登录 |
| 9 | 分类管理 | `/v1/admin/categories` | ADMIN |
| 10 | 菜品管理 | `/v1/admin/dishes` | ADMIN |
| 11 | 订单管理 | `/v1/admin/orders` | ADMIN |
| 12 | 用户管理 | `/v1/admin/users` | ADMIN |
| 13 | 系统设置 | `/v1/admin/configs` | ADMIN |
| 14 | 数据看板 | `/v1/admin/dashboard` | ADMIN |

---

## 3. 健康检查

### GET `/api/v1/health`

公开接口，容器健康检查使用。

```json
{ "code": 0, "message": "成功", "data": "ok" }
```

---

## 4. 认证（/v1/auth）

### 4.1 POST `/api/v1/auth/login` 账号密码登录

同 IP 每分钟限 10 次（超出返回 429）。

请求体：

```json
{ "username": "love", "password": "love123", "remember": true }
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 账号 |
| password | string | 是 | 密码 |
| remember | boolean | 否 | 记住我（前端据此决定 Token 存储位置） |

响应 `data`：

```json
{
  "token": "eyJhbGciOi...",
  "expireIn": 604800,
  "user": { "id": 2, "username": "love", "nickname": "宝贝", "avatar": null, "role": "USER", "status": 1 }
}
```

错误：1001 账号或密码错误；1002 账号已被禁用；429 过于频繁。

### 4.2 POST `/api/v1/auth/logout` 登出

需登录。无请求体。返回 `{ "code": 0, ... }`。

### 4.3 GET `/api/v1/auth/me` 当前用户信息

需登录。`data` 为用户信息（同登录响应中的 `user`）。

### 4.4 PUT `/api/v1/auth/profile` 修改昵称/头像

需登录。

```json
{ "nickname": "宝贝", "avatar": "/uploads/202610/xxx.png" }
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| nickname | string | 是 | 昵称（≤32） |
| avatar | string | 否 | 头像地址（可传空清除） |

### 4.5 PUT `/api/v1/auth/password` 修改密码

需登录。

```json
{ "oldPassword": "love123", "newPassword": "newpass6" }
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| oldPassword | string | 是 | 旧密码 |
| newPassword | string | 是 | 新密码（6~32 位） |

错误：1000 旧密码不正确。

---

## 5. 分类（用户端 /v1/categories）

### GET `/api/v1/categories` 启用中的分类列表

需登录。`data` 为分类数组（按 sort 升序）：

```json
[
  { "id": 1, "name": "主食", "sort": 1, "status": 1 }
]
```

---

## 6. 菜品（用户端 /v1/dishes）

### 6.1 GET `/api/v1/dishes` 菜品分页

需登录。仅返回上架菜品，支持分类与关键词过滤。

Query 参数：

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| categoryId | long | 否 | — | 分类 id |
| keyword | string | 否 | — | 菜品名模糊搜索 |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

响应 `data`（分页）：

```json
{
  "total": 15,
  "list": [
    {
      "id": 1, "categoryId": 1, "name": "番茄牛腩拌面",
      "description": "...", "image": "/images/placeholder/1.svg",
      "tags": "经典,暖心", "tagList": ["经典", "暖心"],
      "loveValue": 8, "sort": 1, "status": 1, "isRecommend": 1,
      "favorite": false
    }
  ]
}
```

### 6.2 GET `/api/v1/dishes/recommend` 推荐菜品

需登录。`limit` 默认 8。`data` 为菜品数组。

### 6.3 GET `/api/v1/dishes/{id}` 菜品详情

需登录。`data` 为菜品对象；`favorite` 字段表示当前用户是否已收藏。

---

## 7. 购物车（/v1/cart，需登录）

### 7.1 GET `/api/v1/cart` 我的购物车

`data` 为条目数组：

```json
[
  {
    "id": 3, "dishId": 1, "name": "番茄牛腩拌面", "image": "/images/placeholder/1.svg",
    "loveValue": 8, "quantity": 2, "taste": "微辣", "remark": "不要香菜",
    "available": true, "lineLove": 16
  }
]
```

### 7.2 POST `/api/v1/cart` 加入购物车

```json
{ "dishId": 1, "quantity": 2, "taste": "微辣", "remark": "不要香菜" }
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| dishId | long | 是 | 菜品 id |
| quantity | int | 否 | 数量，默认 1，≥1 |
| taste | string | 否 | 口味（如「微辣/正常糖」） |
| remark | string | 否 | 单品忌口/留言 |

### 7.3 PUT `/api/v1/cart/{id}` 修改购物车条目

```json
{ "quantity": 3, "taste": "正常辣", "remark": "备注修改" }
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| quantity | int | 是 | 目标数量，≥1 |
| taste | string | 否 | 口味（可选修改） |
| remark | string | 否 | 单品备注（可选修改） |

### 7.4 DELETE `/api/v1/cart/{id}` 删除条目

### 7.5 DELETE `/api/v1/cart` 清空购物车

---

## 8. 收藏（/v1/favorites，需登录）

### 8.1 GET `/api/v1/favorites` 我的收藏列表

`data` 为菜品数组（含 `favorite: true`）。

### 8.2 POST `/api/v1/favorites/{dishId}` 收藏菜品

### 8.3 DELETE `/api/v1/favorites/{dishId}` 取消收藏

---

## 9. 订单（用户端 /v1/orders，需登录）

### 9.1 POST `/api/v1/orders` 提交订单

订单菜品来源于当前用户购物车（下单成功自动清空）。建议携带请求头 `Idempotency-Key` 防重复提交。

```json
{ "remark": "少放盐，爱你", "expectTime": "2026-10-10 18:30" }
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| remark | string | 否 | 整单备注（≤500） |
| expectTime | string | 否 | 期望用餐时间 `yyyy-MM-dd HH:mm`；留空=尽快 |

错误：1006 今日暂不营业；1004 重复提交。

响应 `data` 为订单详情（见 9.4 结构）。

### 9.2 GET `/api/v1/orders` 我的订单分页

Query 参数：

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| tab | string | 否 | active | `active` 进行中 / `finished` 已完成 |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

### 9.3 GET `/api/v1/orders/calendar` 吃饭日历（按月）

Query 参数：`month`（`yyyy-MM`，缺省当月）。`data`：

```json
{
  "month": "2026-10",
  "days": [
    {
      "date": "2026-10-10", "orderCount": 2, "totalLove": 31,
      "orders": [
        { "id": 5, "orderNo": "CO20261010...", "status": "COMPLETED", "statusLabel": "已完成", "totalLove": 16 }
      ]
    }
  ]
}
```

### 9.4 GET `/api/v1/orders/{id}` 订单详情

`data`：

```json
{
  "id": 5, "orderNo": "CO20261010xxxx", "userId": 2,
  "nickname": "宝贝", "status": "ACCEPTED", "statusLabel": "已接单",
  "totalLove": 16, "remark": "少放盐", "expectTime": null,
  "rejectReason": null,
  "createTime": "2026-10-10 15:47:12",
  "acceptTime": "2026-10-10 15:53:02", "finishTime": null,
  "items": [
    { "dishId": 1, "dishName": "番茄牛腩拌面", "dishImage": "/images/placeholder/1.svg",
      "loveValue": 8, "quantity": 2, "taste": "微辣", "remark": "不要香菜", "lineLove": 16 }
  ]
}
```

订单状态机：`PENDING(待接单) → ACCEPTED(已接单) → COOKING(制作中) → COMPLETED(已完成)`；PENDING 可 `REJECTED(已拒绝)`、可取消。

### 9.5 PUT `/api/v1/orders/{id}/cancel` 取消订单

仅待接单（PENDING）可取消。错误：1003 状态不允许。

### 9.6 POST `/api/v1/orders/{id}/rebuy` 再次下单

将订单明细加回购物车（仅上架菜品；下架菜品跳过）。`data`：

```json
{ "addedCount": 2, "skippedNames": ["已下架菜"] }
```

---

## 10. 文件（/v1/files，需登录）

### POST `/api/v1/files/image` 上传图片

所有登录用户可用（食客头像 + 管理端菜品图共用）。

- 请求：`multipart/form-data`，字段名 `file`。
- 限制：jpg / jpeg / png / webp（扩展名 + Content-Type 双校验），≤ 5MB。
- 成功 `data` 为相对 URL：`/uploads/202610/xxxx.png`（UUID 重命名，按年月分目录）。
- 错误：1005 格式或类型不符。

> 上传目录 `data/uploads/` 与后端/nginx 共享；访问路径经 `/uploads/**` 直接托管。

---

## 11. 配置（用户端 /v1/configs）

### GET `/api/v1/configs/public` 公开配置

需登录。

```json
{ "code": 0, "message": "成功", "data": { "businessOpen": 1, "notice": "今日供应新菜！" } }
```

| 字段 | 说明 |
|---|---|
| businessOpen | 1 营业 / 0 歇业 |
| notice | 首页公告文案 |

---

## 12. 分类管理（/v1/admin/categories，ADMIN）

### 12.1 GET `/api/v1/admin/categories` 分类分页

Query：`pageNum`（默认 1）、`pageSize`（默认 10）。

### 12.2 GET `/api/v1/admin/categories/all` 全部分类（下拉选择）

`data` 为全部分类数组（含禁用）。

### 12.3 POST `/api/v1/admin/categories` 新增分类

```json
{ "name": "汤羹", "sort": 7, "status": 1 }
```

### 12.4 PUT `/api/v1/admin/categories/{id}` 修改分类

请求体同新增。

### 12.5 DELETE `/api/v1/admin/categories/{id}` 删除分类

分类下有菜品时拒绝（返回 1000）。

### 12.6 PUT `/api/v1/admin/categories/{id}/status` 启用/禁用

```json
{ "status": 1 }
```

---

## 13. 菜品管理（/v1/admin/dishes，ADMIN）

### 13.1 GET `/api/v1/admin/dishes` 菜品分页

Query：`categoryId`、`status`（1 上架 / 0 下架）、`keyword`、`pageNum`、`pageSize`。

### 13.2 POST `/api/v1/admin/dishes` 新增菜品

```json
{
  "categoryId": 1, "name": "可乐鸡翅", "description": "甜甜的",
  "image": "/uploads/202610/xxx.png", "tags": "甜口,下饭",
  "loveValue": 6, "sort": 1, "status": 1, "isRecommend": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| categoryId | long | 是 | 分类 id |
| name | string | 是 | 名称（≤64） |
| description | string | 否 | 描述 |
| image | string | 否 | 图片地址 |
| tags | string | 否 | 标签，英文逗号分隔 |
| loveValue | int | 是 | 爱心值（≥0） |
| sort | int | 否 | 排序值 |
| status | int | 否 | 1 上架 / 0 下架 |
| isRecommend | int | 否 | 1 推荐 / 0 不推荐 |

### 13.3 PUT `/api/v1/admin/dishes/{id}` 修改菜品

请求体同新增。

### 13.4 DELETE `/api/v1/admin/dishes/{id}` 删除菜品（逻辑删除）

### 13.5 PUT `/api/v1/admin/dishes/{id}/status` 上架/下架

```json
{ "status": 0 }
```

### 13.6 PUT `/api/v1/admin/dishes/{id}/recommend` 设置/取消推荐

```json
{ "isRecommend": 1 }
```

### 13.7 PUT `/api/v1/admin/dishes/batch/status` 批量上架/下架

```json
{ "ids": [1, 2, 3], "status": 0 }
```

---

## 14. 订单管理（/v1/admin/orders，ADMIN）

### 14.1 GET `/api/v1/admin/orders` 订单分页

Query：`status`（如 `PENDING` / `ACCEPTED` / `COMPLETED`）、`date`（`yyyy-MM-dd`）、`pageNum`、`pageSize`。列表携带明细。

### 14.2 GET `/api/v1/admin/orders/{id}` 订单详情

`data` 为订单详情（含 `nickname` 下单用户昵称）。

### 14.3 PUT `/api/v1/admin/orders/{id}/status` 更新订单状态

```json
{ "status": "ACCEPTED" }
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| status | string | 是 | `ACCEPTED` 接单 / `COOKING` 制作 / `COMPLETED` 完成 / `REJECTED` 拒绝 |
| rejectReason | string | 否 | 拒绝理由（REJECTED 时必填） |

非法跳转返回 1003。响应 `data` 为更新后的订单详情。

---

## 15. 用户管理（/v1/admin/users，ADMIN）

### 15.1 GET `/api/v1/admin/users` 用户分页

Query：`keyword`（账号/昵称模糊）、`pageNum`、`pageSize`。

### 15.2 POST `/api/v1/admin/users` 新增用户

```json
{ "username": "dear", "password": "dear123", "nickname": "亲爱的", "avatar": null, "role": "USER" }
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 账号（3~32 位，唯一） |
| password | string | 是 | 初始密码（6~32 位） |
| nickname | string | 是 | 昵称（≤32） |
| avatar | string | 否 | 头像 |
| role | string | 否 | USER / ADMIN，默认 USER |

错误：1000 账号已存在。

### 15.3 PUT `/api/v1/admin/users/{id}` 编辑用户

```json
{ "nickname": "新昵称", "avatar": "/uploads/202610/x.png" }
```

### 15.4 PUT `/api/v1/admin/users/{id}/status` 启用/禁用

```json
{ "status": 0 }
```

禁用后该账号登录返回 1002。

### 15.5 PUT `/api/v1/admin/users/{id}/password/reset` 重置密码

```json
{ "password": "newpass6" }
```

---

## 16. 系统设置（/v1/admin/configs，ADMIN）

### 16.1 GET `/api/v1/admin/configs` 配置列表

`data` 为配置数组：

```json
[
  { "id": 1, "configKey": "business_open", "configValue": "1" },
  { "id": 2, "configKey": "notice", "configValue": "" }
]
```

### 16.2 PUT `/api/v1/admin/configs` 批量更新

```json
{ "configs": { "business_open": "0", "notice": "今日歇业，明天见~" } }
```

更新后用户端 `GET /v1/configs/public` 即时生效；歇业期间下单返回 1006。

---

## 17. 数据看板（/v1/admin/dashboard，ADMIN）

### GET `/api/v1/admin/dashboard`

`data`：

```json
{
  "todayOrderCount": 3,
  "pendingCount": 0,
  "totalCount": 8,
  "todayCompletedCount": 3,
  "topDishes": [ { "dishId": 1, "dishName": "爱心煎蛋", "count": 3 } ],
  "trend": [ { "date": "2026-10-04", "count": 0 }, { "date": "2026-10-10", "count": 3 } ]
}
```

- `trend`：最近 7 天（含今天），缺日补 0。
- `topDishes`：最常点 Top 5（按份数）。
