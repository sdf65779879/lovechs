# 阶段四（交付）自测报告 · 情侣点餐系统

> 对应里程碑：**M13（Docker 交付文件）/ M14（README + API 文档）/ M15（自测与交付）**
> 测试时间：2026-10-10　测试方式：Docker Compose 全栈真实部署 + 自动化接口脚本 + Headless 浏览器页面验证
> 测试结论：**通过，可交付部署**（69/69 接口断言通过，6 个关键页面渲染正常，持久化与全新初始化均验证通过）

---

## 1. 测试范围

| 类别 | 范围 | 方式 |
|---|---|---|
| 镜像构建 | 后端、前端两个镜像从零构建 | `docker compose build`（实测） |
| 编排启动 | mysql / backend / frontend 三服务依赖顺序、健康检查、一键启动 | `docker compose up -d`（实测） |
| 接口自测 | 认证鉴权、分类菜品、上传、购物车、订单全状态机、收藏、管理端全部能力、用户资料、静态资源与文档，共 **69 项断言** | `backend/test-docker.ps1`（PowerShell，真实 HTTP 请求） |
| 页面验证 | 食客端（移动视口）、管理端（桌面视口）共 6 个关键页面 | Headless Chromium + CDP 注入登录态截图 |
| 非功能 | 非 root 运行、时区、数据持久化、全新初始化 | 容器命令 + 卷重建实测 |

## 2. 测试环境

| 项 | 实测值 |
|---|---|
| 宿主机 | Windows 10/11（Docker Desktop，WSL2 后端） |
| Docker / Compose | Docker **29.8.0** / Compose **v5.5.1** |
| 测试访问端口 | `18080`（本机 80 端口避让，写入本地 `.env`，该文件不纳入交付；默认交付端口仍为 **80**） |
| mysql 镜像 | `mysql:8.0.41`（固定版本） |
| 后端镜像 | `couple-ordering-backend:latest`，**436 MB**（多阶段：maven 构建 → JRE 运行） |
| 前端镜像 | `couple-ordering-frontend:latest`，**75.1 MB**（node 构建 → nginx 运行） |

部署拓扑（与 `docker-compose.yml` 一致）：

```
浏览器 → frontend(nginx, 容器内80) ──/api/──→ backend(容器内8081) ──→ mysql(3306)
                     └─/uploads/─→ /app/uploads ←─ bind 挂载 ─→ ./data/uploads ←─ backend 读写
couple-net 自定义桥接网络；mysql-data 命名卷；./logs 绑定挂载后端日志
```

## 3. 镜像构建结果与构建卡点

两个镜像均在干净环境下实际构建成功：

| 镜像 | 构建结果 | 关键设计 |
|---|---|---|
| backend | ✅ 构建成功，436 MB | 多阶段构建（`maven:3.9.9-eclipse-temurin-17` → `eclipse-temurin:17-jre-alpine`）；非 root 用户 `app(uid/gid=10001)`；安装 tzdata 并设 `Asia/Shanghai`；JVM `-XX:MaxRAMPercentage=75`；EXPOSE 8081 |
| frontend | ✅ 构建成功，75.1 MB | 多阶段构建（`node:20.19.0-alpine3.21` 执行 `npm ci && npm run build` → `nginx:1.27.5-alpine`）；以非 root `nginx(uid=101)` 运行并修正 cache/pid 目录权限；EXPOSE 80；vite 构建产物 gzip 后主包约 65 KB |

**构建卡点与解决（已固化进交付文件，供后续在国内网络构建参考）：**

1. **Maven 中央仓库超时**：本机访问 `repo.maven.apache.org` 出现 Read timed out。
   → 新增 `backend/maven-settings.xml`（阿里云 public 仓库，`mirrorOf=central`），Dockerfile 中以 `mvn -s` 引用；同时启用 BuildKit `--mount=type=cache,target=/root/.m2` 依赖缓存与 aether 超时参数。海外网络构建时删除 `<mirrors>` 段即可恢复官方源。
2. **npm 官方源不可达**：本机访问 `registry.npmjs.org` 失败（npmmirror/腾讯/华为镜像可达）。
   → 前端 Dockerfile 增加构建参数 `ARG NPM_REGISTRY=https://registry.npmmirror.com`，`npm ci` 时显式指定；海外构建传 `--build-arg NPM_REGISTRY=https://registry.npmjs.org` 即可。

## 4. 编排与启动验证

| 验证项 | 结果 | 证据 |
|---|---|---|
| `docker compose up -d` 一键启动三服务 | ✅ | mysql → backend → frontend 按 `depends_on: service_healthy` 顺序启动 |
| mysql 健康检查 | ✅ | `mysqladmin ping`，状态 `healthy` 后后端才启动 |
| backend 健康检查 | ✅ | 容器内 `wget --spider http://127.0.0.1:8081/api/v1/health`，状态 `healthy` |
| 经 nginx 的健康检查 | ✅ | `http://localhost:18080/api/v1/health` 返回 `{"code":0,...,"data":"ok"}` |
| 后端非 root 运行 | ✅ | 容器内 `id` = `uid=10001(app) gid=10001(app)` |
| 前端非 root 运行 | ✅ | 容器内 `id` = `uid=101(nginx) gid=101(nginx)` |
| 容器时区 | ✅ | 后端容器 `date` 为 `CST`（Asia/Shanghai，UTC+8），MySQL `--default-time-zone=+08:00` |
| JWT_SECRET 强制校验 | ✅ | compose 使用 `${JWT_SECRET:?}`，缺少 `.env` 时直接报错拒绝启动 |
| nginx 路由 | ✅ | `/` SPA history 回退、`/api/` 反代后端、`/uploads/` 图片托管、`/assets/` 缓存、gzip、上传体积上限 20m |

## 5. API 接口自测（69 项，全部通过）

脚本：`backend/test-docker.ps1`（对 `http://localhost:18080` 发真实 HTTP 请求；含中文断言采用 UTF-8 字节级比较，规避 Windows 控制台编码干扰）。
最终结果：**==== 自测汇总：69 PASS / 0 FAIL / 共 69 项 ====**

| 分组 | 断言数 | 覆盖内容 | 结果 |
|---|---|---|---|
| 0 健康检查 | 1 | `/v1/health` 经 nginx → 后端返回 code=0 | ✅ |
| 1 认证与鉴权 | 6 | admin/love 登录、错误密码 1001、无 Token 401、普通用户访问管理端 403、`/me` | ✅ |
| 2 分类与菜品 | 6 | 6 分类、菜品分页 total=15、推荐菜、详情、中文搜索「番茄」total=2、登录后配置 | ✅ |
| 3 图片上传 | 2 | 登录用户上传 PNG 返回 `/uploads/yyyyMM/uuid.png`；该图片经 nginx `/uploads/` 200 可取回（Content-Type 正确） | ✅ |
| 4 购物车 | 4 | 加购（口味 taste/单品备注 remark/爱心值快照 lineLove=16）、列表、修改条目、字段回显 | ✅ |
| 5 订单 | 14 | 下单 PENDING、相同 `Idempotency-Key` 幂等返回同一单、下单后购物车清空、接单→制作→完成全状态机、用户端详情含明细、非法跳转 1003、待接单取消、拒绝未填理由返回 **400**、拒绝填理由 REJECTED（理由正确回显）、再来一单加回购物车、吃饭日历、已完成分页 | ✅ |
| 6 收藏 | 3 | 收藏、收藏列表、取消收藏 | ✅ |
| 7 管理端 | 22 | 看板（累计/待处理）、订单分页含明细、订单详情昵称（UTF-8 字节校验=宝贝）、歇业开关、公告中文正确（字节校验）、歇业下单 1006、恢复营业、用户新增/重复账号 1000/禁用/禁用登录 1002/重置密码/分页搜索、分类新增与空分类删除、菜品新增（引用上传图）/下架/用户端不可见下架菜（详情仍可读）/重新上架/修改/逻辑删除 | ✅ |
| 8 用户资料 | 7 | 改头像并生效、错误旧密码 1000、改密码、新密码登录、恢复原密码、登出 | ✅ |
| 9 静态资源与文档 | 4 | 食客端 `/` 200 text/html、管理端 `/admin` 路由回退 200、内置 SVG 占位图 200 image/svg+xml、Knife4j `/api/doc.html` 200 | ✅ |

关键业务规则与服务端实际行为核对（已同步写入 `docs/api.md`）：

- 统一响应 `code=0` 成功；错误码 400/401/403/404/429/1000~1006/500 与设计一致。
- 安全白名单实测仅 `login`、`health`、接口文档路径公开；**分类/菜品/配置等查询接口均需登录**（未登录返回 401）。
- 订单状态机：`PENDING→ACCEPTED→COOKING→COMPLETED`；PENDING 可 `CANCELLED`/`REJECTED`；管理员不可取消；**拒绝订单不填理由返回 400（参数错误，"拒绝订单请填写理由"）**，以服务端 `OrderServiceImpl` 实现为准。
- 中文全链路（登录昵称、口味、备注、公告、拒绝理由、搜索词）经 UTF-8 字节级断言与数据库 HEX 双重核验，存储与传输均正确。

**复现方式：**

```powershell
# Windows PowerShell（在项目根目录准备好 .env 后）
docker compose up -d --build
cd backend
.\test-docker.ps1          # 结果同时写入 backend/test-docker-result.txt
```

## 6. 页面验证（Headless Chromium 真实渲染）

通过 Chrome DevTools Protocol 注入 `couple_token/couple_user` 登录态，对生产构建（nginx 托管）真实截图，截图存于 `docs/self-test-screenshots/`：

| 截图文件 | 视口 | 验证点 | 结果 |
|---|---|---|---|
| `user-login.png` | 390×844（移动） | 食客端登录页：品牌图标、「欢迎回家」、账号/密码、记住我、「开饭」按钮，Tailwind 样式/图标/中文字体正常 | ✅ |
| `user-home.png` | 390×844（移动，2x DPR） | 登录后菜单页：问候语「晚上好呀，宝贝」、中文日期、随机情话、搜索框、分类 Tab、菜品卡片（SVG 占位图、标签、爱心值、加购按钮）、悬浮购物车、底部三 Tab | ✅ |
| `user-orders.png` | 390×844（移动） | 订单页：进行中/已完成 Tab、无进行中订单时的空状态插画与「去点菜」引导 | ✅ |
| `admin-login.png` | 1280×800（桌面） | 管理端登录页：「大厨工作台」、管理员账号/密码卡片、「进入工作台」、回用户端链接 | ✅ |
| `admin-dashboard.png` | 1280×900（桌面） | 看板：侧边菜单、4 张指标卡（今日/待处理/今日已完成/累计）、最近 7 天趋势柱状图、宝贝最常点 Top 5、铃铛与用户端入口 | ✅ |
| `admin-orders.png` | 1280×900（桌面） | 订单管理：筛选栏、订单表（订单号、宝贝、菜品、合计爱心、状态徽章 已取消/已拒绝/已完成、下单时间、查看）、分页 | ✅ |

页面与接口数据一致（如看板累计 7 单、Top1 番茄牛腩拌面、订单表 7 条记录），证明 nginx → 后端 → MySQL 全链路与前端生产构建均正常。

## 7. 持久化与全新初始化验证

| 场景 | 操作 | 结果 |
|---|---|---|
| 容器重建（保留数据） | `docker compose down` 后 `docker compose up -d`（**不删卷**） | ✅ 三容器重新创建并全部 healthy；登录正常；历史订单（7 单）仍在；用户头像路径保留且图片经 nginx 200 可取回；宿主 `data/uploads` 文件（5 个）保留 |
| 全新初始化（删卷重来） | `docker compose down -v` 后 `docker compose up -d` | ✅ 命名卷重新创建，`docs/init.sql` 自动重新执行；实测 sys_user=**2**、category=**6**、dish=**15**、orders=**0**、sys_config=**2**，与初始化脚本完全一致；admin/love 双账号登录 code=0；健康检查 code=0 |
| 上传目录 | 全新初始化后清理测试图片 | ✅ `data/uploads/` 仅保留 `.gitkeep`，后续上传会自动按月建目录 |

## 8. 已知限制（如实说明）

1. **真机移动端未覆盖**：本次页面验证使用 Headless Chromium 的 390×844 移动视口（含 2x DPR）模拟手机，未在真实 iPhone / Android 设备上操作。PRD §15 中的真机显示、安全区（刘海屏）、触摸手势等项目，建议在手机浏览器访问服务器地址做最终人工验收；前端已按 `viewport-fit=cover` 与安全区 CSS 变量做了适配。
2. **浏览器 Notification 通知**：仅 HTTPS（或 localhost）可用，属浏览器安全策略；HTTP 局域网下系统自动降级为「标题闪烁 + Web Audio 叮咚提示音」，不漏单。HTTPS 三种启用方案见 README「HTTPS 可选方案」（自签证书 / Caddy 自动证书 / 云负载均衡）。
3. **自测脚本平台**：`test-docker.ps1` 为 Windows PowerShell 5.1+ 编写（中文需 UTF-8 BOM、请求体按 UTF-8 字节发送）；Linux 服务器可用 curl/jq 按 `docs/api.md` 等价验证，或安装 PowerShell（`pwsh`）运行同一脚本。
4. **本机测试端口**：自测使用 `WEB_PORT=18080`（本机 `.env`，已被 git 忽略，不纳入交付）；交付默认端口为 80，部署时以 `.env.example` 复制的 `.env` 为准。
5. **资源规模**：本次为两人小规模使用场景验证，未做高并发压测；登录限流（同 IP 10 次/分）与下单幂等（TTL 10 分钟）为功能性验证。

## 9. 测试后环境状态

- 测试结束后已执行 `docker compose down -v` + `up -d`，**本地栈已重置为全新初始化状态并保持运行**（mysql/backend healthy、frontend running，访问 `http://localhost:18080/`），数据库中仅有默认 2 账号 / 6 分类 / 15 菜品，无测试残留订单与用户。
- 含真实随机密钥的本地 `.env`、构建日志 `*.log`、测试结果文本均不纳入交付（git 忽略）。
- 部署到 Linux 服务器时，按 README 复制 `.env.example` 为 `.env`、重新生成 `JWT_SECRET`、`docker compose up -d --build` 即可得到与本次验证一致的全新环境。

## 10. 交付物清单（阶段四）

| 文件 | 说明 |
|---|---|
| `backend/Dockerfile` | 后端多阶段镜像（非 root、时区、国内 Maven 镜像可切换） |
| `backend/.dockerignore` | 后端构建上下文排除规则 |
| `backend/maven-settings.xml` | 阿里云 Maven 镜像配置（国内构建加速，海外可删） |
| `frontend/Dockerfile` | 前端多阶段镜像（npm 镜像源 ARG 可切换、nginx 非 root） |
| `frontend/.dockerignore` | 前端构建上下文排除规则 |
| `frontend/nginx.conf` | SPA 回退 + `/api` 反代 + `/uploads` 托管 + 缓存/gzip |
| `frontend/nginx.https.conf.example` | HTTPS 自签证书配置模板 |
| `docker-compose.yml` | 三服务编排（固定版本、健康检查、持久化、自定义网络） |
| `.env.example` | 环境变量模板（含 JWT_SECRET 生成与修改提示） |
| `README.md` | 一键部署、默认账号、HTTPS 方案、环境变量、备份恢复、运维、FAQ、验收清单 |
| `docs/api.md` | 完整接口文档（统一响应/错误码/鉴权/幂等 + 全部分组接口） |
| `docs/self-test-report.md` | 本报告 |
| `docs/self-test-screenshots/*.png` | 6 张页面验证截图 |
| `backend/test-docker.ps1` | 69 项接口自动化自测脚本（可重复执行） |

**总结论：阶段四出口标准全部满足——一键部署可用、镜像可重复构建、接口与页面功能正确、数据持久化可靠、文档齐备，具备交付到 Linux 服务器的条件。**
