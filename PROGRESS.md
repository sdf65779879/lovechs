# 项目进度 · 情侣点餐系统（couple-ordering）

> 本文件随每个阶段完成后更新，记录交付内容与验证结果。

## 大厨新订单邮件通知（2026-10-13）

### 补充集成与最终端到端验收

- 新增 `tests/email/` 的隔离 Compose 环境、SMTP 收件 / 故障注入服务、12 项真实 HTTP + MySQL + SMTP 集成测试、Playwright CLI E2E 脚本及统一运行入口。
- 全新数据库上集成测试 **12/12 通过**（39.517 秒），覆盖 SMTP 实际正文与收件人、幂等、通知开关、认证 / 拒收、3 次重试、30/120 秒间隔、重启恢复、最新设置、真实事务回滚和权限安全。
- 最终 E2E **PASS / 退出码 0**，管理员配置和测试发信、独立 390px 食客登录 / 两份菜品 / 微辣 / 单品与整单备注 / 指定时间 / 界面下单、SMTP 实际收件、大厨界面接单→制作→完成、食客自动轮询更新均通过。
- E2E 同时覆盖错误授权码持续脱敏提示、关闭通知后下单无邮件；实际捕获 1 封测试 + 1 封订单邮件，订单最终 `COMPLETED`，没有重复新单提醒，页面无横向溢出，JavaScript 错误 0。
- 统一复跑：`python tests/email/run_tests.py all`（默认构建镜像并在结束后清理隔离环境）。详细命令见 `tests/email/README.md`，结果和截图 / 回放存入 Git 忽略的测试产物目录。
- 本轮修正测试脚本假设，没有修改应用业务逻辑；隔离数据库和容器已清理，本地原部署保持运行。详细记录见 `docs/email-self-test-report.md` 的补充验收章节。

### 邮件实现与首次验收

- 已熟悉用户下单、订单状态流转、系统设置、JWT 管理端权限和 Docker 部署。沿用现有 Service / Mapper / sys_config 配置边界，新增通知服务、SMTP / Mock Provider 与持久化投递 Repository。
- 系统设置新增发件邮箱、大厨收件地址、新订单通知开关、测试发送与最近 20 条记录。支持 TLS / STARTTLS / 内部无加密通道；收件人校验、最多 5 个、忽略大小写去重。
- 授权码以 AES-GCM 加密存储；密码不回显，不经通用配置接口读取或覆盖；解析错误日志不打印原始请求详情。
- 订单和投递任务共用事务，提交后后台发送；稳定订单事件键去重，回滚无通知，关闭渠道跳过未发任务。临时故障最多 3 次，30 / 120 秒延迟重试，永久故障立即失败，结果记录脱敏。
- 增量启动 SQL 仅创建邮件表，不修改已有业务表或数据；`.env.example`、Compose、README 和 API 文档同步。本地 `.env` 已补充随机加密密钥，未输出或提交密钥。
- `mvn -B -ntp -s maven-settings.xml package` 通过，26 项测试全部通过（API 权限、设置安全、队列事务、订单事件路由、失败分类、重试限制、真实回环 SMTP 的 UTF-8 MIME 与信封收件人）。测试使用 H2 与 Mock / 本地回环 SMTP，无真实外部投递。
- 前端 `npm run type-check` 通过，改动文件 ESLint 零警告，Vite 及 Docker 前后端生产构建通过。修复现有 Axios 封装与新类型声明的兼容问题。
- 独立 MySQL 8 + 后端 + 浏览器验收通过：收件错误持续可见、去重保存、测试邮件 MOCK 成功、顾客下单生成一条通知、重复下单不重复排队、关闭通知不排队、普通顾客无权限、SMTP 授权码保存后空白。390px 与 1440px 布局已检查。
- 本地 `http://127.0.0.1:18080` 已更新；后端 / MySQL healthy，健康接口及邮件管理 API 正常。升级前后用户=2、订单=0、菜品=15、原系统配置=2，数据保持。邮件默认为关闭，待管理员填写真实 SMTP 配置和大厨地址后启用。
- 完整验收与局限见 `docs/email-self-test-report.md`。临时 QA 服务和数据库已在验收后清理。

## 已确认的关键决策（2026-10-09）

| # | 决策 |
|---|---|
| 1 | 后端服务端口统一 **8081**（容器内、EXPOSE、nginx 反代一致） |
| 2 | `cart_item`、`order_item` 新增 `taste`（口味快照）、`remark`（单品备注） |
| 3 | 默认账号：`admin/admin123`（大厨，ADMIN）、`love/love123`（宝贝，USER） |
| 4 | 新订单提醒：标题闪烁 + Web Audio 提示音；Notification 仅 HTTPS 可用时启用；HTTPS 写 README 可选 |
| 5 | 15 张暖色 SVG 占位图内置在前端 `public/images/placeholder/` |
| 6 | 图片上传接口 `/api/v1/files/image` 对所有登录用户开放 |
| 7 | 统一响应体：成功 `code=0` |
| 8 | 按**阶段**确认交付；当前先本地开发测试，后续部署 Linux 服务器 |

## 总体进度

| 阶段 | 模块 | 交付物 | 状态 | 完成时间 |
|---|---|---|---|---|
| 一·地基 | M1 | 项目骨架 + 目录结构 | ✅ 已完成 | 2026-10-09 |
| 一·地基 | M2 | 数据库设计 + `docs/init.sql` | ✅ 已完成 | 2026-10-09 |
| 二·后端 | M3 | 通用模块（响应/异常/JWT/Security/MP/限流/日志） | ✅ 已完成 | 2026-10-10 |
| 二·后端 | M4 | 认证与用户模块 | ✅ 已完成 | 2026-10-10 |
| 二·后端 | M5 | 分类 / 菜品模块 | ✅ 已完成 | 2026-10-10 |
| 二·后端 | M6 | 购物车模块 | ✅ 已完成 | 2026-10-10 |
| 二·后端 | M7 | 订单模块 | ✅ 已完成 | 2026-10-10 |
| 二·后端 | M8 | 上传模块 + 管理端接口（看板/配置） | ✅ 已完成 | 2026-10-10 |
| 二·后端 | M9 | 后端全量接口联调 | ✅ 已完成 | 2026-10-10 |
| 三·前端 | M10 | 项目初始化 + 路由 + Axios + Tailwind 主题 | ✅ 已完成 | 2026-10-10 |
| 三·前端 | M11 | 用户端全部页面 | ✅ 已完成 | 2026-10-10 |
| 三·前端 | M12 | 管理端全部页面 | ✅ 已完成 | 2026-10-10 |
| 四·交付 | M13 | Docker 相关文件 | ✅ 已完成 | 2026-10-10 |
| 四·交付 | M14 | README + API 文档 | ✅ 已完成 | 2026-10-10 |
| 四·交付 | M15 | 自测报告 | ✅ 已完成 | 2026-10-10 |

## 阶段一 · 交付说明

### M1 项目骨架
- 建立 monorepo：`backend/`（Spring Boot 分层包结构）、`frontend/`（Vue 分层目录）、`docs/`、`data/uploads/`。
- 根目录：`.gitignore`、`PRD.md`、`PROGRESS.md`。
- 空目录以 `.gitkeep` 占位。

### M2 数据库初始化脚本
- 文件：`docs/init.sql`。
- 内容：8 张表（sys_user / category / dish / cart_item / orders / order_item / favorite / sys_config），含字段注释、索引、逻辑删除字段。
- 初始化数据：2 个默认账号（BCrypt 密文）、6 个分类、15 个示例菜品、2 项系统配置。
- 幂等：含 `DROP TABLE IF EXISTS`，可重复执行（重建会清空数据，仅用于初始化场景）。

### 验证记录（2026-10-09，真实 MySQL 8.0.46 容器校验）
| 验证项 | 方式 | 结果 |
|---|---|---|
| init.sql 可在 MySQL 8.0 干净导入 | 一次性 `mysql:8.0` 容器挂载到 `/docker-entrypoint-initdb.d/`，启动日志 `MySQL init process done`，无错误 | ✅ 通过 |
| 表数量与初始数据 | 实测：sys_user=2、category=6、dish=15、sys_config=2，其余业务表=0；分类菜品分布 3/4/2/2/3/1 | ✅ 通过 |
| 中文存储正确性 | HEX 核验：主食 `E4B8BBE9A39F`、小炒 `E5B08FE78292`、番茄牛腩拌面、可乐鸡翅均为正确 UTF-8（控制台显示 ?? 仅为 Windows 终端编码问题） | ✅ 通过 |
| 引擎与排序规则 | information_schema 核验 8 表均为 InnoDB / utf8mb4_unicode_ci | ✅ 通过 |
| 目录结构与 PRD 第 14 节一致 | 逐项核对 | ✅ 通过 |
| 默认账号密码与密文匹配 | bcryptjs(cost=10) 生成；M9 后端登录联调时再次实测登录 | ✅ 通过（admin/love 均登录成功） |

## 阶段二 · 交付说明（2026-10-10）

### M3 通用模块
- 统一响应 `Result<T>`（code=0 成功）、`PageResult<T>`、`ResultCode` 枚举（400/401/403/404/429/1000~1006/500）。
- 全局异常处理 `GlobalExceptionHandler`：业务异常、校验异常、权限异常、Spring 异常统一转 JSON，不泄露堆栈。
- JWT：`JwtUtils`（jjwt 0.12.5）、`JwtAuthenticationFilter`、`LoginUser`、`UserDetailsServiceImpl`、`SecurityUtils`。
- Security：无状态会话、白名单、`/v1/admin/**` 需 ADMIN、JSON 401/403、CORS、BCrypt。
- MyBatis-Plus：分页插件、逻辑删除、`MetaObjectHandler` 自动填充 create_time/update_time。
- 登录限流 `LoginRateLimitInterceptor`：同 IP 每分钟 10 次；下单幂等 `IdempotencyStore`（TTL 10 分钟）。
- 日志：`logback-spring.xml` 按天切割，保留 7 天。

### M4~M8 业务模块
- 认证：登录/登出/当前用户/改资料/改密码。
- 分类、菜品：用户端列表/分页/推荐/详情/搜索；管理端 CRUD、上下架、推荐、批量操作。
- 购物车：增删改查、清空，taste/remark 随条目持久化。
- 订单：下单（购物车快照冗余名称/图片/爱心值/taste/remark）、状态机校验、取消、拒绝、再下单、吃饭日历。
- 上传：jpg/jpeg/png/webp 双校验（扩展名+Content-Type）、≤5MB、UUID 重命名、按月分目录。
- 管理端：订单筛选与流转、用户管理（新增/禁用/重置密码）、营业开关与公告、数据看板（指标卡/Top5/7 天趋势）。

### 构建卡点与解决（记录备查）
- 现象：本机仅 JDK 23，`mvn compile` 报所有 Lombok getter「找不到符号」。
- 根因：① Lombok 1.18.34 对 JDK 23 支持不完整，升级至 **1.18.36**；② JDK 23 的 javac 不再从普通 classpath 隐式发现注解处理器，需在 `maven-compiler-plugin` 显式配置 `annotationProcessorPaths`。
- 结果：`mvn clean package -DskipTests` BUILD SUCCESS，产出 `backend/target/couple-ordering.jar`。

### 联调记录（2026-10-10，真实 MySQL 8.0 容器 + 运行中的后端）
环境：MySQL 容器 `couple-mysql`（本机 3307→3306，挂载 init.sql）；后端本机 8091（8081 被其他容器占用，仅本地联调端口，部署仍 8081）。脚本：`backend/test-integration.ps1`、`test-errors.ps1`、`test-supplement.ps1`；结果：`integration-result.txt`、`diag-errors.txt`、`integration-supplement.txt`。

| 验证链路 | 结果 |
|---|---|
| admin/love 登录，角色与昵称正确，错误密码返回 1001，无 Token 返回 401 | ✅ |
| 管理端建分类、建菜品（含标签/推荐/爱心值），数据正确落库 | ✅ |
| 图片上传：合法 PNG 返回 `/uploads/202610/xxx.png` 且文件落盘；txt 类型返回 1005 | ✅ |
| 用户端分类/菜品分页/推荐/详情/中文搜索，数量与字段正确 | ✅ |
| 收藏：添加/取消，详情 favorite 标记联动 | ✅ |
| 购物车：加购含 taste/remark、改数量与口味、字段正确回显 | ✅ |
| 下单：生成订单号、合计爱心值、购物车清空；相同 Idempotency-Key 幂等返回同一单 | ✅ |
| 状态流转：PENDING→ACCEPTED→COOKING→COMPLETED，accept/finish 时间写入；非法跳转返回 1003 | ✅ |
| 用户轮询订单详情可见最新状态与状态文案 | ✅ |
| 待接单可取消（CANCELLED），对已完成单取消被拒 | ✅ |
| 拒绝需填理由（REJECTED），无理由被拒；理由正确返回 | ✅ |
| 再下单：菜品加回购物车（addedCount 正确） | ✅ |
| 吃饭日历：有订单日期、订单摘要、合计爱心值正确 | ✅ |
| 看板：今日/待处理/累计、Top5（名称+份数）、7 天趋势（缺日补 0） | ✅ |
| 配置：营业开关与公告更新后用户端即时生效；歇业下单返回 1006 | ✅ |
| 改资料、改密码（含错误旧密码 1000）、登录验证 | ✅ |
| 用户管理：新增、重复账号 1000、禁用登录 1002、重置密码、分页搜索 | ✅ |
| 越权：love 访问 `/v1/admin/**` 返回 403 | ✅ |
| 测试残留数据：guest2 保持禁用、遗留 PENDING 单已拒绝；最终看板 pending=0 | ✅ |

## 阶段三 · 交付说明（2026-10-10，前端）

### M10 工程地基
- Vite 5 + Vue 3.4 + TypeScript + Tailwind（暖色 Token：奶油橘/草莓粉/蜂蜜黄/奶白/暖棕）。
- Axios 统一封装：请求注入 Bearer Token；响应解包 `{code,message,data}`；401 自动跳登录、403/业务错误统一 toast。
- Pinia 用户 store：「记住我」→ localStorage，否则 sessionStorage；UI store（抽屉/弹层）。
- 15 张暖色 SVG 占位图由 `scripts/gen-placeholders.mjs` 数据驱动生成，离线可用。
- `npm install`、`npm run build` 均通过；dev server 5173 代理 `/api`→8091。

### M11 用户端页面
- 登录页（品牌区、记住我、loading）；用户端布局（底部三 Tab + 安全区适配）。
- 首页：问候语 + 中文日期 + 随机情话、公告/歇业横幅、分类横向 Tab、模糊搜索、骨架屏与空状态。
- 菜品卡片 + 详情底部弹层（辣度/甜度按菜品出现、单品备注、数量步进）。
- 悬浮购物车（角标）+ 抽屉式购物车 + 全屏购物车页（备注内联编辑）。
- 确认订单页（清单、合计、尽快/指定时间、订单备注、UUID 幂等键、防重复提交）。
- 订单列表（进行中/已完成、10s 轮询、待接单可取消、加载更多）；订单详情（状态时间轴、明细、信息、取消/再来一单、10s 轮询）。
- 我的：用户卡、收藏、吃饭日历（月视图打点 + 当日订单）、修改密码、退出登录。

### 前端联调中发现并修复的问题
- 后端订单列表（用户端/管理端）未返回明细，导致列表页 `order.items.slice` 渲染报错：`OrderServiceImpl.myPage/adminPage` 改为 `buildOrderVO(order, true, …)` 携带明细；前端同步加 `order.items || []` 防御。
- 已重新打包后端并重启 8091 实例验证。

### 验证记录（2026-10-10，浏览器真实操作 + 接口核对）
| 验证链路 | 结果 |
|---|---|
| 首页问候/日期/情话/公告/分类 Tab/搜索/卡片/骨架屏渲染 | ✅ |
| 详情弹层：辣度/备注/数量，加购 toast，角标联动 | ✅ |
| 购物车抽屉与全屏页：增减、删除、清空、备注编辑、合计 | ✅ |
| 下单全流程：确认页 → 订单号生成 → 详情页 PENDING，购物车清空 | ✅ |
| 大厨接单/制作/完成，用户端时间轴与时间字段正确（15:47→15:53→15:54） | ✅ |
| 订单列表卡片（缩略图/名称/备注/取消）与 10s 轮询 | ✅ |
| 收藏页、吃饭日历（今日 2 单 31 爱心打点）、修改密码页 | ✅ |
| 测试单 id=5 已 COMPLETED；看板 pending=0、today=2、total=5 | ✅ |
| `npm run build` 生产构建通过（gzip 后主包 64KB） | ✅ |

### M12 管理端页面
- 独立登录页 `/admin/login` + 角色路由守卫（未登录跳登录、非 ADMIN 回 `/home`）。
- 管理端布局：PC 固定侧边栏 + 移动端横向菜单，顶栏铃铛（角标）+ 每 10s 轮询 PENDING；首次只建基线，新 id 才触发提醒，pending 归零自动停止。
- 数据看板：4 张指标卡（今日/待处理/今日已完成/累计）、纯 CSS 最近 7 天柱状图、最常点 Top 5 进度条（不引入 ECharts）。
- 菜品管理：分类/状态/名称筛选、分页、勾选与批量上下架、新增/编辑弹窗（图片拖拽上传组件、标签、爱心值、排序、推荐/上架）、行内上下架/推荐/删除。
- 分类管理：列表、启停开关、排序、新增/编辑弹窗。
- 订单管理：状态/日期筛选、PENDING 行高亮、详情弹窗（明细 + 订单备注、接单/开始制作/完成、拒绝理由内联流程）、10s 轮询同步弹窗。
- 用户管理：搜索/分页、新增、编辑、重置密码弹窗、启停开关。
- 系统设置：营业开关、公告文案、修改管理员密码。
- 新订单提醒：标题闪烁（800ms 切换「🔔 有新订单」）+ Web Audio 合成两声「叮咚」（880/660Hz）；浏览器 Notification 仅 HTTPS 可用，README 中写可选方案。

### M12 构建卡点与解决（记录备查）
- 现象：所有管理端弹窗（含用户管理简单弹窗）在截图/合成画面中只有标题栏，body 区域透出底层页面；命中测试与输入功能却正常。
- 排查：SFC 最小复现全部通过 → 逐行截断二分 → node 逐行 JSON 打印先发现 `ImageUpload.vue` 中 `const uploading = ref(false` 缺右括号（补全）；弹窗空洞仍在 → 最终定位为 AdminLayout 顶栏 `backdrop-blur`（backdrop-filter）在 Chromium 下与固定弹窗合成冲突，移除后改用纯白底 `bg-white`，弹窗全部正常。
- 结果：`npm run build` 通过（gzip 后主包 65KB）。

### 验证记录（2026-10-10，浏览器真实操作 + 接口核对）
| 验证链路 | 结果 |
|---|---|
| 看板：今日 3/待处理 0/今日已完成 3/累计 8；7 天趋势今日 3；Top5 爱心煎蛋 3 次、番茄牛腩拌面多次 | ✅ |
| 菜品表格：筛选/勾选/缩略图/分类/爱心值/推荐星/上架开关/分页 | ✅ |
| 菜品编辑弹窗：图片预览、名称/分类/描述/标签/爱心值/排序/推荐/上架，保存生效 | ✅ |
| 分类管理：7 个分类、启停、排序、编辑弹窗 | ✅ |
| 订单 6（16 爱心、含口味快照与订单备注）UI 接单→制作→完成全流程 | ✅ |
| 订单 7 拒绝流程：理由内联填写，用户端展示「订单被拒绝 + 理由」 | ✅ |
| 管理端打开状态下用户新下订单 8：10s 轮询到单、铃铛角标、行高亮、标题闪烁（提示音同分支触发） | ✅ |
| 订单 8 完成后用户端轮询到全绿时间轴（接单 16:32:42、完成 16:32:44） | ✅ |
| 用户管理：表格、新增/编辑/重置密码弹窗、启停开关 | ✅ |
| 系统设置：歇业保存后用户端展示「今日暂不营业」横幅、下单返回 1006；重新开业恢复 | ✅ |
| 越权：admin 身份访问他人订单详情返回 403（归属校验） | ✅ |
| `npm run build` 生产构建通过 | ✅ |

## 阶段四 · 交付说明（2026-10-10，Docker Compose 一键部署）

### M13 Docker 交付文件
- `backend/Dockerfile`：多阶段构建（`maven:3.9.9-eclipse-temurin-17` 构建 → `eclipse-temurin:17-jre-alpine` 运行）；非 root 用户 `app(10001)`；tzdata 时区 Asia/Shanghai；JVM `MaxRAMPercentage=75`；EXPOSE 8081；BuildKit `~/.m2` 缓存挂载。
- `backend/maven-settings.xml`：阿里云 Maven 镜像（国内构建加速，海外删除 mirrors 段即可）；`backend/.dockerignore`。
- `frontend/Dockerfile`：多阶段构建（`node:20.19.0-alpine3.21` `npm ci` 构建 → `nginx:1.27.5-alpine`）；`ARG NPM_REGISTRY`（默认 npmmirror，可切换官方源）；非 root `nginx(101)` 运行并修正 cache/pid 权限；EXPOSE 80；`frontend/.dockerignore`。
- `frontend/nginx.conf`：SPA history 回退、`/api/` 反代 `backend:8081`、`/uploads/` alias 共享挂载、静态资源缓存、gzip、`client_max_body_size 20m`。
- `frontend/nginx.https.conf.example`：443 SSL 自签证书模板（README 含使用步骤）。
- `docker-compose.yml`：固定镜像版本；mysql/backend 双 healthcheck；`depends_on: service_healthy` 启动顺序；`JWT_SECRET` 用 `${JWT_SECRET:?}` 强制提供；`./data/uploads` 三向共享（后端读写、nginx 只读）、`./logs` 日志挂载、`docs/init.sql` 仅首次初始化；自定义网络 couple-net、命名卷 mysql-data。
- `.env.example`：WEB_PORT（默认 80）、MySQL 账号密码、JWT_SECRET（含 openssl 生成提示）、JWT_EXPIRE。

### M14 文档
- `README.md`：功能/技术栈/目录、一键部署、默认账号、部署验证、**HTTPS 三方案**（自签 nginx / Caddy 自动证书 / 云负载均衡）、环境变量表、本地开发、备份恢复、日志、运维命令、端口调整、FAQ、验收清单。
- `docs/api.md`：统一响应/错误码/鉴权白名单/幂等说明 + 全部分组接口（路径、参数、示例）；实测后修正：分类/菜品/配置查询均需登录，白名单仅 login/health/文档路径。

### M15 自测（完整报告见 `docs/self-test-report.md`）
- 镜像实测构建成功：backend **436 MB**、frontend **75.1 MB**。
- 构建卡点：Maven central 与 npmjs 官方源本机不可达 → 分别固化阿里云 maven settings 与 npmmirror ARG（海外可切回）。
- API 自动化自测 `backend/test-docker.ps1`：**69 项断言全部 PASS**（健康/认证鉴权/分类菜品/上传+nginx 取图/购物车口味备注/下单幂等/全状态机/取消/拒绝/再下单/日历/收藏/看板/歇业 1006/用户管理/菜品 CRUD/改密/静态页/Knife4j）。
- 页面验证（Headless Chromium + CDP 注入登录态，截图存 `docs/self-test-screenshots/`）：食客登录/菜单/订单空状态、管理登录/看板/订单管理共 6 张，真实数据渲染正常。
- 非功能：容器非 root（10001 / 101）、时区 CST、健康检查与启动顺序正确；`down`+`up` 保留卷时订单与上传文件不丢；`down -v`+`up` 全新初始化数据精确为 2 用户/6 分类/15 菜品/0 订单/2 配置。
- 已知限制：真机 iPhone/Android 未覆盖（移动视口模拟，建议真机人工验收）；Notification 需 HTTPS（HTTP 已降级标题闪烁+提示音）；测试脚本为 Windows PowerShell 5.1+。
- 测试后本地栈已重置为全新初始化并保持运行（本机自测端口 18080；交付默认 80）。

### 阶段四验证记录（2026-10-10，Docker 29.8.0 / Compose v5.5.1 真实部署）
| 验证项 | 方式 | 结果 |
|---|---|---|
| 两镜像从零构建 | `docker compose build`（含国内镜像源配置） | ✅ backend 436MB / frontend 75.1MB |
| 一键启动与健康状态 | `docker compose up -d`，mysql/backend healthy、frontend running | ✅ |
| 全量接口自测 | `backend/test-docker.ps1` 69 项断言 | ✅ 69 PASS / 0 FAIL |
| 上传图片三向共享 | 登录用户上传 → nginx `/uploads/` 200 取回，文件落 `./data/uploads` | ✅ |
| 订单全链路 | 下单（幂等）→接单→制作→完成、取消、拒绝（无理由 400/有理由 REJECTED）、再来一单 | ✅ |
| 管理端能力 | 看板/订单/用户/分类/菜品 CRUD/歇业公告（中文 UTF-8 字节级核验） | ✅ |
| 页面渲染 | 6 张关键页面截图（移动 390×844 + 桌面 1280） | ✅ |
| 非 root 与时区 | 容器内 `id`、`date` 核验 | ✅ uid=10001/101，CST |
| 持久化 | `down` 后 `up`（保留卷），订单 7 条与头像图片均在 | ✅ |
| 全新初始化 | `down -v` 后 `up`，init.sql 重跑，计数 2/6/15/0/2 | ✅ |
| 安全默认 | JWT_SECRET 缺失拒绝启动；查询类接口未登录 401、越权 403 | ✅ |

**四个阶段（M1–M15）全部完成，系统具备交付部署条件。**
