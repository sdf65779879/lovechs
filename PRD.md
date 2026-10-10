# 情侣点餐系统 · 产品需求文档（PRD）

| 项目 | 内容 |
|---|---|
| 文档版本 | v1.0（待确认） |
| 编写日期 | 2026-10-09 |
| 项目代号 | couple-ordering |
| 交付形态 | Docker Compose 一键部署的前后端分离 Web 应用 |
| 使用规模 | ≤ 5 人，极低并发，私人双人使用 |
| 文档状态 | v1.0 已于 2026-10-09 确认；按**阶段**交付；后端端口统一 **8081** |

---

## 1. 项目背景与目标

### 1.1 背景

需求方（以下简称「厨师 / 管理员」）负责做饭，其对象（以下简称「食客 / 普通用户」）通过手机浏览器像点外卖一样点餐。系统替代微信口头报菜，提供菜单浏览、购物车、下单备注、订单状态跟踪的完整闭环，并承担情侣间的情感表达功能（情话、撒娇留言、爱心值、吃饭日历）。

### 1.2 设计目标

1. **食客端**：移动端优先，浏览菜单 → 加购物车 → 下单（含口味/忌口/留言）→ 实时看订单状态。
2. **厨师端**：PC 优先，维护菜品与分类、接收新订单提醒、流转订单状态、维护用户与营业设置。
3. **高颜值**：温馨、可爱、有仪式感的暖色系设计，拒绝丑陋 CRUD 风格。
4. **易部署**：一台 Linux 服务器，`cp .env.example .env && docker compose up -d` 一键上线，数据持久化。
5. **克制设计**：不引入微服务、MQ、Redis、ES、分库分表、复杂 RBAC；仅 ADMIN / USER 两种角色。

### 1.3 衡量指标（验收）

- 一键部署后用户端 `http://服务器IP/`、管理端 `http://服务器IP/admin` 可访问。
- 默认账号可完成「建菜 → 浏览 → 加购 → 下单 → 接单 → 制作 → 完成」全链路。
- 容器重启数据不丢失；iPhone Safari / Chrome 显示正常；无控制台报错、无接口 500。

---

## 2. 用户角色与核心场景

### 2.1 角色

| 角色 | 入口 | 设备 | 权限 |
|---|---|---|---|
| 食客 USER（对象） | `/` | 手机浏览器为主 | 浏览、收藏、购物车、下单、查看自己的订单、改自己资料 |
| 管理员 ADMIN（我） | `/admin` | PC 为主 | 全部业务数据管理、订单流转、用户管理、系统设置 |

> 不开放自助注册。USER 账号由 ADMIN 创建，系统初始化内置 1 个 ADMIN + 1 个 USER。

### 2.2 核心用户故事

| 编号 | 角色 | 用户故事 | 优先级 |
|---|---|---|---|
| US-01 | 食客 | 作为食客，我希望打开手机就能看到今天的菜单和一句情话，有被惦记的感觉 | P0 |
| US-02 | 食客 | 作为食客，我希望按分类浏览、搜索菜品，看图片/描述/标签/爱心值 | P0 |
| US-03 | 食客 | 作为食客，我希望选菜时指定辣度/甜度并写忌口或撒娇留言 | P0 |
| US-04 | 食客 | 作为食客，我希望把菜加入购物车、调整数量、写整单备注后提交 | P0 |
| US-05 | 食客 | 作为食客，我希望下单后看到「待接单→已接单→制作中→已完成」时间轴并自动刷新 | P0 |
| US-06 | 食客 | 作为食客，我希望在没人接单前可以取消订单 | P0 |
| US-07 | 食客 | 作为食客，我希望收藏爱吃的菜、把历史订单一键再加购 | P1 |
| US-08 | 食客 | 作为食客，我希望在日历上看到我们一起吃过的饭 | P1 |
| US-09 | 管理员 | 作为管理员，我希望上传图片、维护菜品/分类、上下架、设推荐 | P0 |
| US-10 | 管理员 | 作为管理员，我希望新订单有声音/标题/通知提醒，不漏单 | P0 |
| US-11 | 管理员 | 作为管理员，我希望按流程接单、制作、完成，或拒绝并填写理由 | P0 |
| US-12 | 管理员 | 作为管理员，我希望看板显示今日订单、待处理数、热门菜、7 天趋势 | P1 |
| US-13 | 管理员 | 作为管理员，我希望开关营业状态、发布首页公告、管理食客账号 | P1 |

---

## 3. 功能需求

### 3.1 食客端（移动端优先）

#### 3.1.1 认证
- 登录页：账号 + 密码；「记住我」自动登录；Token 存 localStorage。
- 无注册入口；路由守卫：未登录跳登录页；USER 禁止进入 `/admin`。
- Token 过期/401：清除登录态并跳登录页，保留来源路径。

#### 3.1.2 首页 / 菜单
- 顶部区域：按时段的问候语（早安/午安/晚安）+ 今日日期（含星期）+ 随机情话（本地文案池，每次进入随机一条）。
- 公告条：管理员配置的公告文案（无公告则不显示）。
- 营业状态：歇业时首页展示「今日暂不营业」温馨空状态，禁止加购/下单。
- 分类 Tab：横向滚动，展示全部 + 各分类；菜品网格按分类锚点切换。
- 菜品卡片：图片、名称、描述（单行省略）、标签（辣/清淡/拿手菜等）、爱心值（❤ 整数）、加购按钮（飞入购物车动效）。
- 菜品详情弹窗：大图、名称、完整描述、标签、爱心值、口味选择（辣度：不辣/微辣/中辣/变态辣；甜度：无糖/微甜/正常/加糖）、单品忌口备注、数量步进器、收藏按钮、加入购物车。
- 搜索：按菜品名模糊搜索，结果列表 + 空状态。
- 加载态：骨架屏；失败态：错误提示 + 重试。

#### 3.1.3 购物车
- 悬浮购物车按钮（底部，适配 iPhone 安全区）+ 数量角标。
- 抽屉面板：条目（图、名、口味/备注、爱心值、数量步进器）、删除、清空、合计爱心值、去结算。
- 购物车数据存后端（`cart_item`），换设备登录可恢复；未登录不展示。
- 同一菜品 + 同口味备注合并为一条，数量累加。

#### 3.1.4 下单
- 确认订单页：菜品清单、合计爱心值、整单备注（口味/忌口/撒娇留言）、期望用餐时间（「尽快」/ 指定时间，时间粒度 10 分钟）。
- 提交：按钮 loading 防连点；请求头携带前端生成的 `Idempotency-Key`（UUID），后端 10 分钟内去重。
- 成功：生成订单号，清空已下单购物车项，跳订单详情页。

#### 3.1.5 订单
- 订单列表：Tab「进行中 / 已完成」，进行中 = PENDING/ACCEPTED/COOKING，已完成 = COMPLETED/CANCELLED/REJECTED；卡片展示订单号、状态、菜品摘要、爱心值、时间。
- 订单详情：状态时间轴（含各节点时间，拒绝时展示理由）、菜品明细（含口味/单品备注）、整单备注、期望时间、合计爱心值。
- 自动刷新：进行中的订单每 10s 轮询；页面不可见时暂停（`visibilitychange`）。
- 取消：仅 PENDING 可取消，二次确认。
- 再次下单：将历史订单有效菜品（仍上架）按原数量与口味加回购物车，已下架菜品提示。

#### 3.1.6 我的
- 个人信息：头像、昵称（可修改并上传头像）、用户名（只读）。
- 我的收藏：收藏菜品网格，可取消收藏、快捷加购。
- 吃饭日历：月度视图，有订单（非取消/拒绝）的日期显示 ❤ 打点，点击日期展示当日订单。
- 修改密码、退出登录。

### 3.2 管理端（PC 优先，兼顾平板）

#### 3.2.1 登录与布局
- 独立入口 `/admin/login`；登录后校验 ROLE_ADMIN，USER 访问返回 403 页面。
- 经典后台布局：左侧导航 + 顶部栏（营业状态、管理员信息），但视觉延续暖色系，不用冰冷默认蓝。

#### 3.2.2 数据看板
- 指标卡：今日订单数、待处理订单数（PENDING）、累计订单数、今日完成数。
- 最常点菜品 Top 5（按历史订单明细聚合数量）。
- 最近 7 天订单趋势柱状图（ECharts，暖色柱）。
- 看板每 30s 刷新。

#### 3.2.3 菜品管理
- 列表：分页、分类/上架状态筛选、名称搜索；展示图片、名称、分类、爱心值、排序、推荐/上架开关。
- 新增/编辑（抽屉或弹窗表单）：图片上传（点击/拖拽 + 预览 + 替换/删除）、名称、分类、描述、标签（多选预设 + 可自定义）、爱心值、排序值、是否上架、是否推荐。
- 行内快捷：上架/下架、设为/取消推荐、删除（软删，二次确认）。
- 批量：勾选后批量上架/下架。

#### 3.2.4 分类管理
- 列表：名称、排序、启用状态、菜品数量；新增/编辑/删除（有关联菜品时禁止删除并提示）、启用禁用、排序。

#### 3.2.5 订单管理
- 列表：状态/日期筛选、分页；PENDING 新订单行高亮 + 「新」标记；列表每 10s 轮询。
- 详情：菜品明细（含口味备注）、用户整单备注、期望时间、用户信息。
- 状态流转按钮：接单（PENDING→ACCEPTED）、开始制作（ACCEPTED→COOKING）、完成（COOKING→COMPLETED）、拒绝（PENDING→REJECTED，必填拒绝理由）。非法流转按钮禁用。
- 新订单提醒（监测到 PENDING 增量时）：
  - 文档标题闪烁（`🆕 新订单 - 情侣点餐` 交替），聚焦页面后恢复；
  - Web Audio 合成简短提示音（无需音频文件）；
  - 浏览器 Notification（仅 HTTPS / localhost 可用，HTTP 局域网自动降级，见 §13）。

#### 3.2.6 用户管理
- 用户列表：用户名、昵称、角色、状态、创建时间。
- 新增用户、编辑昵称/头像/角色、重置密码、启用/禁用；禁止禁用/删除自己。

#### 3.2.7 系统设置
- 营业开关（关闭后食客端展示歇业页、禁止下单）。
- 公告文案（多行文本，展示在食客端首页）。
- 修改管理员密码（复用修改密码接口）。

### 3.3 非功能需求

| 类别 | 要求 |
|---|---|
| 响应格式 | 统一 `{ code: number, message: string, data: T }`；成功 code=0（或 200，见 §13 决策），业务错误码分段 |
| 异常处理 | `@RestControllerAdvice` 全局捕获，堆栈绝不返回前端；未知异常返回友好提示并记录日志 |
| 参数校验 | Jakarta Validation（`@NotBlank` 等），校验错误返回 400 与字段级错误信息 |
| 鉴权 | Spring Security 无状态 + JWT；BCrypt 密码；接口级角色校验；越权拦截（食客只能操作自己的购物车/订单/收藏） |
| 限流 | 登录接口同 IP 每分钟 10 次，内存计数实现，超限返回 429 |
| 幂等 | 下单接口 `Idempotency-Key` 内存去重（TTL 10 分钟） |
| 日志 | Logback 按天切割，保留 7 天，控制台 + 文件双输出 |
| 配置 | 数据库密码、JWT 密钥等全部走环境变量，不硬编码 |
| 上传 | 仅 jpg/jpeg/png/webp，≤ 5MB；UUID 重命名；按年月分目录；用户与管理员均可上传（头像/菜品图） |
| 前端 | 移动端优先响应式；`env(safe-area-inset-bottom)`；所有请求有 loading/错误提示；history 路由 |
| 安全 | MyBatis-Plus 参数绑定防 SQL 注入；前端默认转义防 XSS；不使用富文本/v-html 渲染用户输入 |
| 时区 | 全链路 Asia/Shanghai，时间格式 `yyyy-MM-dd HH:mm:ss` |

---

## 4. 信息架构与页面清单

### 4.1 食客端（路由）

| 路径 | 页面 | 说明 |
|---|---|---|
| `/login` | 登录页 | 温馨插画 + 登录卡片 |
| `/` | 首页/菜单 | 问候、情话、公告、分类 Tab、菜品网格、搜索入口 |
| `/search` | 搜索页 | 关键词搜索结果 |
| `/cart` | 购物车页 | 全屏购物车 + 整单备注入口 |
| `/order/confirm` | 确认订单页 | 清单、备注、期望时间、提交 |
| `/orders` | 订单列表 | 进行中/已完成 Tab |
| `/orders/:id` | 订单详情 | 时间轴 + 明细 |
| `/me` | 我的 | 个人信息入口聚合 |
| `/me/profile` | 个人资料 | 改昵称/头像 |
| `/me/favorites` | 我的收藏 | 收藏网格 |
| `/me/calendar` | 吃饭日历 | 月视图 + 当日订单 |
| `/me/password` | 修改密码 | — |

### 4.2 管理端（路由）

| 路径 | 页面 |
|---|---|
| `/admin/login` | 管理员登录 |
| `/admin` | 数据看板（重定向 `/admin/dashboard`） |
| `/admin/dashboard` | 数据看板 |
| `/admin/dishes` | 菜品管理 |
| `/admin/categories` | 分类管理 |
| `/admin/orders` | 订单管理 |
| `/admin/orders/:id` | 订单详情 |
| `/admin/users` | 用户管理 |
| `/admin/settings` | 系统设置 |

---

## 5. 订单状态机

```
            接单                开始制作              完成
 PENDING ──────────► ACCEPTED ──────────► COOKING ──────────► COMPLETED
    │                                                                 ▲
    │ 食客取消（仅 PENDING）                                           │
    ├──────────► CANCELLED                                            │
    │  管理员拒绝（必填理由）                                          │
    └──────────► REJECTED                                             │
```

- 终态：COMPLETED / CANCELLED / REJECTED。
- 每次流转记录节点时间：`accept_time`（ACCEPTED 时写入）、`finish_time`（COMPLETED 时写入）。
- 非法流转返回 400「当前订单状态不允许该操作」。
- 订单快照：下单时将菜名、图片、爱心值、口味、备注冗余进 `order_item`，菜品后续修改不影响历史订单。

---

## 6. 数据模型设计

### 6.1 ER 关系概览

```
sys_user 1──N cart_item N──1 dish
sys_user 1──N orders 1──N order_item N──1 dish（快照冗余，弱关联）
sys_user 1──N favorite N──1 dish
category 1──N dish
sys_config（KV 配置）
```

### 6.2 表结构（MySQL 8.0，InnoDB，utf8mb4）

> 通用约定：主键 `id BIGINT UNSIGNED AUTO_INCREMENT`；逻辑删除字段 `deleted TINYINT DEFAULT 0`；时间 `DATETIME`；所有表/字段加 COMMENT。

**sys_user 用户表**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint unsigned PK | 主键 |
| username | varchar(32) | 登录账号，唯一索引 |
| password | varchar(100) | BCrypt 密文 |
| nickname | varchar(32) | 昵称 |
| avatar | varchar(255) | 头像 URL |
| role | varchar(16) | ADMIN / USER |
| status | tinyint | 1 启用 0 禁用，默认 1 |
| create_time / update_time | datetime | — |
| deleted | tinyint | 逻辑删除 |

**category 分类表**：id, name(varchar32), sort(int 默认0), status(tinyint 默认1), create_time, update_time, deleted；索引 `(sort)`。

**dish 菜品表**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint unsigned PK | — |
| category_id | bigint | 分类 id，索引 |
| name | varchar(64) | 菜名 |
| description | varchar(500) | 描述 |
| image | varchar(255) | 图片 URL |
| tags | varchar(255) | 标签，逗号分隔（如 `辣,拿手菜`） |
| love_value | int | 爱心值，默认 1 |
| sort | int | 排序，默认 0 |
| status | tinyint | 1 上架 0 下架 |
| is_recommend | tinyint | 1 推荐 0 否 |
| create_time / update_time | datetime | — |
| deleted | tinyint | — |

索引：`(category_id, status, sort)`、`(name)` 前缀索引辅助模糊查询。

**cart_item 购物车表**：id, user_id（索引）, dish_id, quantity（int 默认1）, **taste varchar(100)（口味快照，如「微辣/正常糖」，待确认新增）**, **remark varchar(500)（单品忌口备注，待确认新增）**, create_time, update_time；唯一键 `(user_id, dish_id, taste, remark)`。

**orders 订单表**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint unsigned PK | — |
| order_no | varchar(32) | 订单号，唯一索引；规则 `yyyyMMddHHmmss + 4位随机数` |
| user_id | bigint | 下单用户，索引 |
| status | varchar(16) | 状态枚举 |
| total_love | int | 合计爱心值 |
| remark | varchar(500) | 整单备注 |
| expect_time | datetime null | 期望用餐时间，NULL = 尽快 |
| reject_reason | varchar(255) | 拒绝理由 |
| create_time | datetime | 下单时间 |
| accept_time | datetime null | 接单时间 |
| finish_time | datetime null | 完成时间 |
| update_time | datetime | — |
| deleted | tinyint | — |

索引：`(user_id, status, create_time)`、`(status, create_time)`。

**order_item 订单明细表**：id, order_id（索引）, dish_id, dish_name(varchar64), dish_image(varchar255), love_value(int), quantity(int), **taste varchar(100) / remark varchar(500)（待确认新增）**。物理删除（随订单存在，无单独删除场景）。

**favorite 收藏表**：id, user_id, dish_id, create_time；唯一键 `(user_id, dish_id)`；物理删除。

**sys_config 配置表**：id, config_key(varchar64 唯一), config_value(varchar2000), remark(varchar255), update_time。
初始键：`business_open`（1/0）、`notice`（公告文案）。

### 6.3 初始化数据（init.sql）

- 管理员：`admin / admin123`（BCrypt 密文内置），昵称「大厨」，ROLE_ADMIN。
- 食客：`love / love123`，昵称「宝贝」，ROLE_USER。
- 6 个分类（sort 顺序）：主食、小炒、汤羹、甜品、饮品、夜宵。
- 15 个示例菜品（分布于 6 分类，含标签、爱心值、推荐标记），图片使用前端内置 SVG 占位图 `/images/placeholder/1.svg ~ 15.svg`（离线可用，见 §13）。

---

## 7. API 接口清单

- 统一前缀 `/api/v1`；认证头 `Authorization: Bearer <token>`。
- 分页入参 `pageNum/pageSize`（默认 1 / 10），分页出参 `{ total, list }`。
- 公开接口无需登录；其余默认需要登录；ADMIN 接口要求 ROLE_ADMIN。

### 7.1 认证与个人

| 方法 | 路径 | 说明 | 角色 |
|---|---|---|---|
| POST | /auth/login | 登录（IP 限流 10/min），出参 token + 用户信息 | 公开 |
| POST | /auth/logout | 登出（前端清 Token，后端无状态直接成功） | 登录 |
| GET | /auth/me | 当前用户信息 | 登录 |
| PUT | /auth/profile | 修改昵称/头像 | 登录 |
| PUT | /auth/password | 修改密码（旧密码 + 新密码） | 登录 |

### 7.2 分类

| 方法 | 路径 | 说明 | 角色 |
|---|---|---|---|
| GET | /categories | 食客端：仅启用分类，按 sort | 登录 |
| GET | /admin/categories | 管理端：全部分类 | ADMIN |
| POST | /admin/categories | 新增 | ADMIN |
| PUT | /admin/categories/{id} | 修改 | ADMIN |
| DELETE | /admin/categories/{id} | 删除（有菜品引用则拒绝） | ADMIN |
| PUT | /admin/categories/{id}/status | 启用/禁用 | ADMIN |

### 7.3 菜品

| 方法 | 路径 | 说明 | 角色 |
|---|---|---|---|
| GET | /dishes | 分页：categoryId、keyword；仅上架 | 登录 |
| GET | /dishes/recommend | 推荐菜品列表 | 登录 |
| GET | /dishes/{id} | 菜品详情 | 登录 |
| GET | /admin/dishes | 分页：categoryId/status/keyword | ADMIN |
| POST | /admin/dishes | 新增 | ADMIN |
| PUT | /admin/dishes/{id} | 修改 | ADMIN |
| DELETE | /admin/dishes/{id} | 软删除 | ADMIN |
| PUT | /admin/dishes/{id}/status | 上架/下架 | ADMIN |
| PUT | /admin/dishes/{id}/recommend | 设为/取消推荐 | ADMIN |
| PUT | /admin/dishes/batch/status | 批量上下架（ids + status） | ADMIN |

### 7.4 购物车

| 方法 | 路径 | 说明 | 角色 |
|---|---|---|---|
| GET | /cart | 我的购物车（含菜品快照信息） | 登录 |
| POST | /cart | 加入：dishId、quantity、taste、remark | 登录 |
| PUT | /cart/{id} | 改数量/口味备注 | 登录（仅本人） |
| DELETE | /cart/{id} | 删除单项 | 登录（仅本人） |
| DELETE | /cart | 清空购物车 | 登录 |

### 7.5 收藏

| 方法 | 路径 | 说明 | 角色 |
|---|---|---|---|
| GET | /favorites | 我的收藏列表 | 登录 |
| POST | /favorites/{dishId} | 收藏 | 登录 |
| DELETE | /favorites/{dishId} | 取消收藏 | 登录 |

### 7.6 订单（食客）

| 方法 | 路径 | 说明 | 角色 |
|---|---|---|---|
| POST | /orders | 提交订单（remark、expectTime；`Idempotency-Key` 头） | 登录 |
| GET | /orders | 我的订单分页（status 分组） | 登录 |
| GET | /orders/{id} | 订单详情（仅本人） | 登录 |
| PUT | /orders/{id}/cancel | 取消（仅 PENDING） | 登录 |
| POST | /orders/{id}/rebuy | 再次下单 → 加回购物车 | 登录 |
| GET | /orders/calendar?month=yyyy-MM | 当月有订单日期 + 当日订单 | 登录 |

### 7.7 订单与看板（管理员）

| 方法 | 路径 | 说明 | 角色 |
|---|---|---|---|
| GET | /admin/orders | 订单分页：status、日期范围 | ADMIN |
| GET | /admin/orders/{id} | 任意订单详情 | ADMIN |
| PUT | /admin/orders/{id}/status | 流转：ACCEPTED/COOKING/COMPLETED/REJECTED + rejectReason | ADMIN |
| GET | /admin/dashboard | 看板聚合数据 | ADMIN |

### 7.8 用户管理（ADMIN）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /admin/users | 用户列表 |
| POST | /admin/users | 新增用户 |
| PUT | /admin/users/{id} | 编辑（昵称/头像/角色） |
| PUT | /admin/users/{id}/status | 启用/禁用 |
| PUT | /admin/users/{id}/password/reset | 重置密码 |

### 7.9 配置与文件

| 方法 | 路径 | 说明 | 角色 |
|---|---|---|---|
| GET | /configs/public | 营业状态 + 公告 | 登录 |
| GET | /admin/configs | 全部配置 | ADMIN |
| PUT | /admin/configs | 更新配置（KV 集合） | ADMIN |
| POST | /files/image | 图片上传（multipart，≤5MB，jpg/png/webp），返回 URL | 登录 |
| GET | /health | 健康检查（容器 healthcheck 用） | 公开 |

---

## 8. 视觉与交互设计规范

### 8.1 设计关键词
温馨、奶油感、圆润、可爱、有仪式感；大量留白、柔和投影、圆角卡片、微动效（加购飞入、状态切换淡入、按钮按压反馈）。

### 8.2 色彩 Token（Tailwind 自定义主题，最终值可微调）

| Token | 色值 | 用途 |
|---|---|---|
| cream-50 | #FFF9F4 | 页面底色（奶白） |
| cream-100 | #FFF1E6 | 卡片浅底 |
| warm-400 | #FFB088 | 次强调、渐变浅端 |
| warm-500（主色） | #FF8A65 | 主按钮、选中态（奶油橘） |
| warm-600 | #F26A4A | 按钮按压 |
| rose-400 | #FF6B81 | 爱心、情话、收藏（草莓粉） |
| cocoa-700 | #6D4C41 | 主文字（暖棕） |
| cocoa-400 | #A1887F | 次要文字 |
| line | #F5E4D7 | 分割线 |
| success / warn / danger | #7CB986 / #F5B544 / #EF6A6A | 状态色（低饱和暖调） |

渐变：`linear-gradient(135deg, #FFB088, #FF8A65)` 用于主按钮与首页头图；深色文字一律用暖棕而非纯黑。

### 8.3 字体与字号
- 中文：`-apple-system, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif`；不依赖外网字体（保证离线可用）。
- 数字/爱心值可用 `font-variant-numeric: tabular-nums`。
- 食客端正文 14px、标题 18~22px；管理端正文 14px。

### 8.4 组件风格
- 圆角：卡片 16px、按钮 12px、弹窗 20px；
- 阴影：`0 8px 24px rgba(242, 106, 74, 0.12)` 柔和暖阴影；
- 图标：@iconify/vue（爱心、餐具、表情类线性/圆润图标）；
- 情感化元素：首页头部小插画/大 emoji 氛围、空状态配可爱文案（如「还没有点菜，肚子在等你~」）、情话卡片；
- 管理端保持同一色系与圆角语言，弱化「后台感」。

### 8.5 移动端适配
- 底部悬浮元素 `padding-bottom: calc(8px + env(safe-area-inset-bottom))`；
- 触控目标 ≥ 44px；分类 Tab 横向滚动无滚动条；
- 断点：移动端默认，≥768px 食客端双列/居中限宽（max-width 480px 卡片化），管理端 ≥1024px 完整布局。

---

## 9. 技术架构

### 9.1 技术栈（按需求主推方案）

| 层 | 选型 |
|---|---|
| 前端 | Vue 3 + Vite + TypeScript、Vue Router 4、Pinia、Tailwind CSS、Axios、@iconify/vue、ECharts（看板） |
| 后端 | Spring Boot 3.2.x、Java 17、MyBatis-Plus 3.5.x、Spring Security + JWT（jjwt）、Hutool、SpringDoc/Knife4j、Jakarta Validation |
| 数据库 | MySQL 8.0 |
| 部署 | Docker / Docker Compose、Nginx（静态资源 + 反代 + 图片托管） |

### 9.2 后端分层

```
controller（参数接收/权限注解）→ service（业务逻辑+事务）→ mapper（MyBatis-Plus）
entity（表映射） / dto（入参） / vo（出参）
common：Result 统一响应、ResultCode 枚举、BusinessException、GlobalExceptionHandler、常量、JWT 工具、安全工具
config：SecurityConfig、MybatisPlusConfig（分页插件+自动填充+逻辑删除）、CorsConfig、Knife4jConfig、WebMvcConfig
```

### 9.3 前端分层

```
api/（按模块封装接口）→ stores/（Pinia：auth、cart、config）→ views/
utils/request.ts（Axios 实例：请求注入 Token、401 跳登录、统一错误 toast、Idempotency-Key 注入）
router/（守卫 + 角色判断）、layouts/（UserLayout 移动端壳 / AdminLayout 后台壳）
components/（DishCard、CartButton、Tag、Timeline、Skeleton、Empty 等）
```

---

## 10. 部署架构

```
浏览器（手机/PC）
   │  HTTP :WEB_PORT(默认80)
   ▼
frontend 容器（nginx:alpine，固定版本）
   ├── /            → 前端打包静态资源（history 回退 index.html）
   ├── /api/        → proxy_pass http://backend:8081/api/
   ├── /uploads/    → 挂载卷 /app/uploads（菜品图/头像）
   └── /images/     → 内置占位图（随前端打包）
        │
        ▼
backend 容器（eclipse-temurin:17-jre-alpine，非 root 用户运行，固定版本）
   └── JDBC 3306
        ▼
mysql 容器（mysql:8.0 固定版本，数据卷 mysql-data 持久化，健康检查）

自定义 bridge 网络：couple-net；三个服务均 restart: unless-stopped。
上传目录卷：./data/uploads ↔ backend /app/uploads ↔ frontend nginx 只读挂载同一目录。
```

---

## 11. 初始化数据与默认账号

| 角色 | 账号 | 密码 | 昵称 | 说明 |
|---|---|---|---|---|
| ADMIN | admin | admin123 | 大厨 | 首次登录建议改密（README 提示，不强制） |
| USER | love | love123 | 宝贝 | 对象使用 |

- 配置默认值：`business_open=1`、`notice=`（空）。
- 6 分类、15 菜品的具体明细在 `docs/init.sql` 中交付（模块 2）。

---

## 12. 关键技术决策与风险

| # | 事项 | 决策 | 理由/影响 |
|---|---|---|---|
| D1 | 端口矛盾：需求 7.2 写「暴露 8081」，而 .env/nginx 写 8080 | ✅ 已确认：**统一为 8081**；application 配置、Dockerfile EXPOSE、nginx 反代 `http://backend:8081` 全部使用 8081 | 需求方最终选择 8081 |
| D2 | 购物车/明细无口味字段，但详情页要求选辣度甜度忌口 | ✅ 已确认：cart_item、order_item 各新增 taste、remark（NOT NULL DEFAULT ''） | 核心需求落地 |
| D3 | 浏览器 Notification 在 HTTP 局域网不可用（Secure Context 限制） | 三级提醒：标题闪烁 + Web Audio 提示音始终可用；Notification 仅 HTTPS/localhost 启用并静默降级；README 附可选 HTTPS（自签证书/Caddy）说明 | 保证不漏单 |
| D4 | 占位图外链 placehold.co 依赖公网 | 前端 `public/images/placeholder/*.svg` 内置 15 张暖色 SVG 占位图，init.sql 引用相对路径 | 内网/离线部署可用 |
| D5 | 限流/幂等不引入 Redis | 登录限流：拦截器 + `ConcurrentHashMap` 按 IP 滑动/固定窗口计数；幂等：`ConcurrentHashMap + 定时清理`，TTL 10 分钟 | ≤5 人规模足够，重启丢失可接受 |
| D6 | 上传接口角色 | `POST /api/v1/files/image` 对所有登录用户开放（头像与菜品图共用），服务端统一校验类型/大小/重命名 | 食客需要换头像 |
| D7 | 统一响应 code 值 | ✅ 已确认：成功 `code=0`；401=401、403=403、429=429、参数错误 400、业务异常 1000+ 段、未知异常 500 | 与 HTTP 语义对齐 |
| D8 | 看板图表 | ECharts 5（npm 打包，离线可用） | 比纯 CSS 柱状图更美观且成本低 |
| D9 | 图片上传大小 | 后端限制 5MB，nginx `client_max_body_size 20m`（按需求保留冗余） | 实际以后端 5MB 为准 |
| D10 | MySQL 初始化 | init.sql 挂载到 `/docker-entrypoint-initdb.d/`，仅在数据卷首次初始化时执行；README 说明重新初始化需删卷 | 防止重启重复导入 |

**主要风险**
1. 服务器若为纯内网 HTTP，iPhone 上 Notification 不可用 → 已有降级方案（D3）。
2. Maven 构建依赖公网拉包 → 构建阶段需要网络；README 说明可配置国内镜像（构建时挂载 settings.xml 或构建参数）。
3. 食客上传头像与管理端菜品图共用磁盘目录，nginx 只读挂载，无执行权限，安全可控。

---

## 13. 实施计划（模块拆解与开发顺序）

按需求 15 个交付物分 5 个阶段；**每个模块完成后更新 `PROGRESS.md` 并暂停，等待确认再进入下一模块**。

### 阶段一：地基
| 模块 | 交付物（对应清单） | 出口标准 |
|---|---|---|
| M1 | ① 项目骨架 + 目录结构 | monorepo 目录、.gitignore、PROGRESS.md、各模块占位配置文件可被识别 |
| M2 | ② 数据库设计 + `docs/init.sql` | 8 张表建表 SQL（含新增 taste/remark）、索引、注释、默认账号 + 6 分类 + 15 菜品；本地 MySQL 可干净导入 |

### 阶段二：后端（Spring Boot）
| 模块 | 交付物 | 出口标准 |
|---|---|---|
| M3 | ③ 通用模块 | Result/ResultCode、全局异常、JWT 工具、SecurityConfig、MyBatis-Plus 配置（分页/自动填充/逻辑删除）、CORS、Knife4j、限流拦截器、Logback 切割 |
| M4 | ④ 认证与用户 | 登录/登出/me/改资料/改密码；Admin 用户管理 5 个接口；越权与禁用账号拦截 |
| M5 | ⑤ 分类/菜品 | 分类全套（用户端+管理端）、菜品全套（含推荐、搜索、筛选、批量、软删） |
| M6 | ⑥ 购物车 | 加购/合并、改数量/口味、删除、清空、列表（仅本人） |
| M7 | ⑦ 订单 | 下单（幂等、快照、清购物车、订单号）、列表/详情/取消/再下单/日历、状态机校验 |
| M8 | ⑧ 上传 + ⑨ 管理端剩余 | 图片上传校验/重命名/分目录；订单管理、看板聚合、配置接口、health |
| M9 | 后端联调 | Knife4j 全接口自测通过，SQL 与接口对齐 |

### 阶段三：前端（Vue 3）
| 模块 | 交付物 | 出口标准 |
|---|---|---|
| M10 | ⑩ 前端基座 | Vite+TS、Tailwind 暖色主题、Router 守卫、Pinia、Axios 封装、两套 Layout、占位 SVG |
| M11 | ⑪ 食客端全部页面 | 登录、首页/搜索、详情弹窗、购物车（抽屉+全屏）、确认下单、订单列表/详情（轮询）、我的（资料/收藏/日历/改密） |
| M12 | ⑫ 管理端全部页面 | 登录、看板（ECharts）、菜品（含上传/批量）、分类、订单（提醒三件套）、用户、设置 |

### 阶段四：交付
| 模块 | 交付物 | 出口标准 |
|---|---|---|
| M13 | ⑬ Docker | 多阶段 Dockerfile ×2（非 root、缓存层、时区）、docker-compose（固定版本、健康检查、自定义网络、卷）、nginx.conf、.env.example、.dockerignore |
| M14 | ⑭ README + docs/api.md | 部署/开发/备份/日志/改端口/FAQ；完整 API 文档 |
| M15 | ⑮ 自测报告 | 接口清单 × 验证结果、页面清单 × 验证结果、全链路走查、已知限制 |

> 建议确认粒度：可按上述 M1~M15 逐模块确认；若希望加快，也可按阶段确认（如「后端一次性做完 M3~M9」），由你指定。

---

## 14. 项目目录结构预览

```
couple-ordering/
├── PRD.md                      # 本文档
├── PROGRESS.md                 # 进度文件（M1 时初始化，每模块更新）
├── docker-compose.yml
├── .env.example
├── .gitignore
├── README.md
├── docs/
│   ├── api.md                  # 接口文档
│   └── init.sql                # 建表 + 初始化数据
├── data/                       # 运行时持久化（git 忽略）
│   └── uploads/                # 上传图片（按年月分目录）
├── backend/
│   ├── .dockerignore
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/couple/ordering/
│       │   ├── OrderingApplication.java
│       │   ├── common/
│       │   │   ├── result/        # Result、ResultCode、PageResult
│       │   │   ├── exception/     # BusinessException、GlobalExceptionHandler
│       │   │   ├── constant/      # Constants、SecurityConstants
│       │   │   ├── security/      # JwtUtils、JwtAuthenticationFilter、UserDetailsServiceImpl
│       │   │   └── utils/         # 通用工具
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── MybatisPlusConfig.java
│       │   │   ├── WebMvcConfig.java
│       │   │   ├── CorsConfig.java
│       │   │   └── Knife4jConfig.java
│       │   ├── interceptor/       # 登录限流拦截器
│       │   ├── controller/
│       │   │   ├── AuthController.java
│       │   │   ├── CategoryController.java
│       │   │   ├── DishController.java
│       │   │   ├── CartController.java
│       │   │   ├── FavoriteController.java
│       │   │   ├── OrderController.java
│       │   │   ├── FileController.java
│       │   │   └── admin/         # AdminXxxController
│       │   ├── service/
│       │   │   └── impl/
│       │   ├── mapper/
│       │   ├── entity/            # SysUser、Category、Dish、CartItem、Orders、OrderItem、Favorite、SysConfig
│       │   ├── dto/               # LoginDTO、OrderSubmitDTO、DishDTO、CartDTO ...
│       │   ├── vo/                # LoginVO、DishVO、CartVO、OrderVO、DashboardVO ...
│       │   └── enums/             # OrderStatus、UserRole
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           ├── application-prod.yml
│           └── logback-spring.xml
└── frontend/
    ├── .dockerignore
    ├── Dockerfile
    ├── nginx.conf
    ├── package.json
    ├── tsconfig.json
    ├── vite.config.ts
    ├── tailwind.config.js
    ├── postcss.config.js
    ├── .eslintrc / .prettierrc
    ├── index.html
    ├── public/
    │   └── images/placeholder/    # 1.svg ~ 15.svg 暖色占位图
    └── src/
        ├── main.ts
        ├── App.vue
        ├── api/                   # auth.ts、dish.ts、cart.ts、order.ts、admin.ts ...
        ├── assets/
        ├── components/            # DishCard、DishDetailModal、CartFab、CartDrawer、
        │                         #   OrderTimeline、TagPill、Skeleton、AppEmpty、Toast ...
        ├── layouts/               # UserLayout.vue、AdminLayout.vue
        ├── router/                # index.ts（路由表 + 守卫）
        ├── stores/                # auth.ts、cart.ts、config.ts
        ├── styles/                # main.css（Tailwind 指令 + 全局样式 + 安全区）
        ├── utils/                 # request.ts、auth.ts、loveWords.ts、notify.ts
        └── views/
            ├── user/              # Login、Home、Search、Cart、OrderConfirm、
            │                     #   OrderList、OrderDetail、me/* ...
            └── admin/             # Login、Dashboard、DishList、CategoryList、
                                  #   OrderList、OrderDetail、UserList、Settings
```

---

## 15. 验收标准（清单）

- [ ] `cp .env.example .env && docker compose up -d` 全部容器健康（mysql/backend healthcheck 通过）
- [ ] 访问 `http://服务器IP/` 打开食客端，`/admin` 打开管理端
- [ ] admin/admin123 可登录管理端，新增菜品并上传图片成功（nginx 可访问图片）
- [ ] love/love123 可登录食客端，浏览/搜索/加购（含口味备注）/下单成功
- [ ] 管理端看到新订单（高亮 + 提醒），完成 接单→制作→完成 流转
- [ ] 食客端 10s 轮询看到状态变化；PENDING 可取消；拒绝单展示理由
- [ ] 收藏、再次下单、吃饭日历打点可用
- [ ] 营业关闭后食客端展示歇业、无法下单；公告正常展示
- [ ] 重启容器 / 重启宿主机后数据不丢失（mysql-data、uploads 卷持久化）
- [ ] iPhone Safari 与 Android Chrome 显示正常（安全区、触控尺寸）
- [ ] 浏览器控制台无报错，Network 无 500；Knife4j 文档可访问（`/api/doc.html`）

---

## 附录 A：需求确认记录（2026-10-09 已确认）

1. **后端端口**：✅ 统一 **8081**（application 配置 / Dockerfile EXPOSE / nginx 反代全部一致）。
2. **口味字段**：✅ `cart_item`、`order_item` 各新增 `taste`（口味快照）、`remark`（单品备注）。
3. **默认账号**：✅ `admin/admin123`（昵称「大厨」）、`love/love123`（昵称「宝贝」）。
4. **浏览器通知**：✅ 局域网 HTTP 下采用「标题闪烁 + Web Audio 提示音」降级；HTTPS 配置写入 README 可选项。
5. **占位图**：✅ 前端内置 15 张暖色 SVG 占位图，离线可用。
6. **头像上传**：✅ `/files/image` 对所有登录用户开放，统一校验类型与大小。
7. **成功响应码**：✅ 统一响应体成功 `code=0`。
8. **确认粒度**：✅ 按**阶段**确认（地基 / 后端 / 前端 / 交付）。
9. **部署环境**：✅ 当前阶段本地开发测试，后续部署到 Linux 服务器。
