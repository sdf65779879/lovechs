# 情侣点餐系统（couple-ordering）

一款给两个人用的点餐小程序：**食客（宝贝）** 浏览、加购、下单，攒「爱心值」；**大厨（对象）** 在管理端接单、制作、完成。从下单到端上桌，全程有爱。

- 用户端（食客）：`http://服务器IP/`
- 管理端（大厨）：`http://服务器IP/admin`
- 接口文档（Knife4j）：`http://服务器IP/api/doc.html`

## 功能一览

| 端 | 功能 |
|---|---|
| 食客端 | 登录/记住我、首页（问候语+中文日期+随机情话）、公告/歇业横幅、分类浏览、关键词搜索、菜品详情（辣度/甜度/忌口）、收藏、购物车（抽屉+全屏、备注）、下单（尽快/指定时间、幂等防重复）、订单列表/详情（10s 轮询、可取消）、吃饭日历、修改资料/密码 |
| 大厨管理端 | 独立登录、数据看板（今日/待处理/累计/Top5/7 天趋势）、订单管理（高亮新单、标题闪烁+提示音提醒、接单→制作→完成、拒绝填理由）、菜品管理（新增/编辑/上下架/推荐/批量/图片上传）、分类管理、用户管理（新增/禁用/重置密码）、系统设置（营业开关/公告/改密） |
| 亮点玩法 | 每道菜有「爱心值」，下单累计爱心；吃饭日历打点记录每顿爱心；「再来一单」一键把上次的菜加回购物车 |

## 技术栈

| 层 | 选型 |
|---|---|
| 前端 | Vue 3.4 + Vite 5 + TypeScript + Tailwind CSS（暖色主题）+ Pinia + Axios + ECharts |
| 后端 | Spring Boot 3.2.5 + Java 17 + Spring Security(JWT) + MyBatis-Plus + MySQL 8.0 + Knife4j |
| 部署 | Docker Compose（Nginx 托管静态资源 + 反向代理 + 图片托管） |

## 目录结构

```
couple-ordering/
├── docker-compose.yml          # 一键部署编排
├── .env.example                # 环境变量示例（复制为 .env）
├── README.md                   # 本文档
├── PRD.md                      # 需求文档
├── PROGRESS.md                 # 开发进度与验证记录
├── docs/
│   ├── api.md                  # 完整 API 接口文档
│   ├── init.sql                # 数据库初始化脚本（8 张表 + 默认数据）
│   └── self-test-report.md     # 阶段四自测报告
├── data/uploads/               # 上传图片（与后端/nginx 共享，git 忽略）
├── logs/                       # 后端日志（按天切割，git 忽略）
├── backend/
│   ├── Dockerfile              # 后端镜像（多阶段，非 root）
│   ├── .dockerignore
│   ├── pom.xml
│   └── src/main/               # Spring Boot 源码
└── frontend/
    ├── Dockerfile              # 前端镜像（多阶段，非 root）
    ├── nginx.conf              # Nginx 站点配置
    ├── nginx.https.conf.example# HTTPS 配置示例（自签证书方案）
    ├── .dockerignore
    ├── package.json
    └── src/                    # Vue 源码
```

## 快速开始（Docker 一键部署）

### 前置条件

- 已安装 Docker Engine 20.10+ 与 Docker Compose v2（`docker compose version` 可查）
- 服务器可访问 Docker Hub（构建需拉取基础镜像；离线环境请先配置镜像加速，见 FAQ）

### 部署步骤

```bash
# 1. 进入项目目录
cd couple-ordering

# 2. 复制环境变量并修改（务必修改 JWT_SECRET）
cp .env.example .env        # Windows: copy .env.example .env

# 3. 修改 .env 中的 JWT_SECRET 为随机值（长度 >= 32 字节）
#    openssl rand -hex 32

# 4. 构建并启动（首次构建约需 5~15 分钟，取决于网络）
docker compose up -d --build

# 5. 查看状态，等待三个容器 healthy
docker compose ps
```

启动成功后：

| 入口 | 地址 |
|---|---|
| 食客端 | `http://服务器IP/`（默认端口 80，可在 .env 改 WEB_PORT） |
| 管理端 | `http://服务器IP/admin` |
| 接口文档 | `http://服务器IP/api/doc.html` |

### 默认账号

| 角色 | 账号 | 密码 | 昵称 | 说明 |
|---|---|---|---|---|
| 大厨（ADMIN） | `admin` | `admin123` | 大厨 | 管理端使用，首次登录后建议改密 |
| 宝贝（USER） | `love` | `love123` | 宝贝 | 食客端使用 |

> 提示：数据库初始化脚本只在 MySQL 数据卷**首次创建**时自动执行，之后重启容器不会重复导入。

### 验证部署

```bash
docker compose ps                      # 三个服务均应为 healthy / running
docker compose logs -f backend         # 查看后端日志（启动无 ERROR 即正常）
curl http://服务器IP/api/v1/health     # 应返回 {"code":0,...}
```

## 大厨新订单邮件通知

管理端 **系统设置 → 新订单邮件提醒** 可配置系统发件邮箱和大厨收件邮箱。顾客下单成功后，后台自动发送订单号、顾客昵称、菜品数量、口味、单品备注、整单备注和期望用餐时间。浏览器关闭时也能发送；默认关闭，需配置后启用。

1. 在部署环境的 `.env` 设置 `EMAIL_PROVIDER=SMTP` 和 `MAIL_ENCRYPTION_KEY`。密钥为 32 字节随机数据的 Base64，可运行 `openssl rand -base64 32` 生成；Windows 可用 `[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))`。
2. 在管理端填写 SMTP 主机、端口、加密方式、发件邮箱、用户名和授权码。QQ / 163 等邮箱通常需要先开启 SMTP 服务，并使用邮箱授权码。TLS 常用 465，STARTTLS 常用 587，实际以邮箱服务商为准。
3. 填写大厨收件邮箱，最多 5 个，用逗号或分号分隔；系统校验并自动去重。通知事件固定为“顾客新下单”。
4. 保存设置后点击 **发送测试邮件**，查看 **最近投递记录** 和收件箱。测试使用已保存配置，可在通知开关关闭时执行；修改未保存时不能测试。
5. 测试成功后开启新订单邮件通知并保存。修改邮箱或收件地址立即影响后续投递和重试，无需重启。

授权码以 AES-GCM 加密存储在现有 `sys_config` 中，API 不返回密码或密文；留空保留原授权码，勾选“清除已保存的授权码”可删除。`MAIL_ENCRYPTION_KEY` 需要和数据库一起备份并保持稳定；更换密钥后需重新填写授权码。密钥只通过环境变量注入，不能填写在通用系统配置中。

订单和待发邮件使用同一数据库事务：回滚订单不会发送通知，重复提交同一订单不会重复建立任务。后台约每 3 秒检查队列，连接、读写均设置 5 秒超时；临时网络或 SMTP 4xx 故障在约 30 秒、120 秒后重试，最多尝试 3 次。认证、证书、配置错误及 5xx 拒收直接标记失败，订单仍可正常接单。关闭通知后未发出的订单邮件会标记为“已跳过”。

投递记录保留在 `email_delivery` 表，管理页显示最近 20 条及脱敏错误。`SENT` 表示邮件服务接受发送，不保证收件箱投递；请检查垃圾邮件。SMTP 在网络中断时可能已接收邮件，因此重试存在重复送达的可能；发送中服务异常中断超过 5 分钟的任务会标记“结果未知”，请核对收件箱和订单列表。

**已有部署升级**：设置环境变量后执行 `docker compose up -d --build backend frontend`。后端启动自动执行 `CREATE TABLE IF NOT EXISTS email_delivery`，兼容已有数据卷，无需重新执行 `docs/init.sql` 或删除数据卷。业务数据库账号需要创建此表的权限。

**开发与验收**：设置 `EMAIL_PROVIDER=MOCK` 可模拟发送，记录显示“模拟发送成功”；Mock 不读取/解密真实密钥、不连接 SMTP、无需填写发件配置或授权码。真实投递仅通过明确点击测试发送或启用正式通知后发生。

```bash
# 后端：隔离的 H2 测试数据库与 Mock，不投递真实邮件
cd backend
mvn test

# 前端
cd ../frontend
npm run type-check
npm run build
```

集成及端到端复跑（项目根目录，需要 Docker、Python 3.10+、Node.js/npm）：

```powershell
python tests/email/run_tests.py all
```

该入口先运行 12 项真实 MySQL / SMTP 集成测试，再完成双浏览器下单、SMTP 收件、大厨接单与食客状态同步的端到端测试，默认结束后清理隔离环境。详细命令和产物见 [邮件测试说明](tests/email/README.md)。

## HTTPS 可选方案

> 背景：浏览器 **Notification 通知** 仅在 HTTPS（或 localhost）环境可用。局域网 HTTP 下系统已自动降级为「标题闪烁 + Web Audio 提示音」，功能不受影响；如需浏览器通知或公网安全访问，可按以下任一方案启用 HTTPS。

### 方案 A：自签证书 + Nginx（适合局域网/内网）

项目已附可直接替换的配置 `frontend/nginx.https.conf.example`：

```bash
# 1. 生成自签证书（有效期 365 天；CN 填实际访问 IP/域名）
mkdir -p certs
openssl req -x509 -newkey rsa:2048 -nodes \
  -keyout certs/server.key -out certs/server.crt \
  -days 365 -subj "/CN=192.168.1.100"      # 换成你的 IP 或域名

# 2. 用 HTTPS 配置替换默认配置
cp frontend/nginx.https.conf.example frontend/nginx.conf

# 3. 修改 docker-compose.yml 的 frontend 服务：
#    ports:
#      - "${WEB_PORT:-443}:443"      # 映射 443
#    volumes: 增加证书挂载
#      - ./certs:/etc/nginx/certs:ro

# 4. 重建前端容器
docker compose up -d --build frontend
```

自签证书浏览器会提示「不受信任」，选择信任即可；仅适合内网/测试环境。

### 方案 B：Caddy 自动 HTTPS（适合有公网域名的服务器）

用 Caddy 做反代网关，自动申请并续期 Let's Encrypt 证书，无需手动管理证书：

```bash
# Caddyfile
couple.example.com {                  # 换成你的域名，需已解析到服务器
    reverse_proxy localhost:WEB_PORT  # WEB_PORT 为 compose 对外端口
}
```

```bash
docker run -d --name caddy \
  -p 80:80 -p 443:443 \
  -v $PWD/Caddyfile:/etc/caddy/Caddyfile \
  -v caddy_data:/data -v caddy_config:/config \
  caddy:2
```

Caddy 自动完成证书申请与 HTTPS 跳转，浏览器地址栏显示安全锁，Notification 可用。

### 方案 C：云负载均衡 / 反向代理（生产推荐）

- 阿里云/腾讯云等：购买 SSL 证书 → 配置到 SLB/CLB 或 Web 防火墙 → 回源到服务器 `WEB_PORT`。
- 已有 Nginx/宝塔等网关：把 `443` 流量反代到本系统的 `WEB_PORT`，证书由网关托管。

任选一种即可。启用 HTTPS 后，食客端与管理端保持相对路径请求（`/api` 同源），无需改代码。

## 环境变量参考

| 变量 | 默认值 | 说明 |
|---|---|---|
| `WEB_PORT` | `80` | 前端对外访问端口（宿主机） |
| `MYSQL_DATABASE` | `couple_ordering` | 数据库名 |
| `MYSQL_USER` | `couple` | 业务账号 |
| `MYSQL_PASSWORD` | `couple123` | 业务账号密码 |
| `MYSQL_ROOT_PASSWORD` | `couple_root` | root 密码（仅初始化/运维用） |
| `JWT_SECRET` | **必填，无默认** | JWT 签名密钥，长度 ≥ 32 字节，必须修改 |
| `JWT_EXPIRE` | `604800` | Token 有效期（秒），默认 7 天 |
| `EMAIL_PROVIDER` | `SMTP` | 邮件发送通道；`MOCK` 仅模拟并记录结果 |
| `MAIL_ENCRYPTION_KEY` | 空 | 32 字节随机密钥的 Base64，首次保存 SMTP 授权码前设置 |

## 本地开发（不使用 Docker）

```bash
# 1. 准备 MySQL（本地 3306 或任意端口）
docker run -d --name couple-dev-mysql \
  -p 3307:3306 \
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=couple_ordering \
  -v $PWD/docs/init.sql:/docker-entrypoint-initdb.d/init.sql:ro \
  mysql:8.0

# 2. 启动后端（默认读 application-dev.yml，连 localhost:3306）
cd backend
mvn spring-boot:run        # 或 java -jar target/couple-ordering.jar

# 3. 启动前端（Vite dev server，代理 /api 与 /uploads 到后端）
cd frontend
npm install
npm run dev                # 访问 http://localhost:5173
```

> 本地联调端口为 8091（8081 留给 Docker 部署），对应 `frontend/vite.config.ts` 的代理目标；如本地后端端口不同，修改该代理即可。

## 数据备份与恢复

```bash
# 备份数据库（导出为 SQL 文件）
docker compose exec mysql sh -c \
  'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" couple_ordering' > backup-$(date +%F).sql

# 恢复数据库（导入）
docker compose exec -T mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" couple_ordering' < backup-2026-10-10.sql

# 上传图片目录
# 直接备份 data/uploads/ 整个目录即可（与数据库无强关联，但菜品图被 dish.image 引用）
```

## 日志

```bash
docker compose logs -f backend     # 实时查看后端容器日志
docker compose logs -f frontend    # 实时查看 nginx 日志

# 后端文件日志（挂载到宿主机 ./logs/，按天切割、保留 7 天）
# ./logs/couple-ordering.log
```

## 常用运维命令

```bash
docker compose ps                   # 查看服务状态
docker compose up -d                # 启动（不重新构建）
docker compose up -d --build        # 代码变更后重建并启动
docker compose restart backend      # 重启单个服务
docker compose down                 # 停止并移除容器（数据卷保留）
docker compose down -v              # 停止并删除数据卷（⚠ 会清空数据库，重新初始化）
```

**重新初始化数据库**（清空所有业务数据，恢复默认账号/分类/菜品）：

```bash
docker compose down -v
docker compose up -d --build
```

## 端口调整指南

- **对外访问端口**：改 `.env` 的 `WEB_PORT`，`docker compose up -d` 生效。
- **后端内部端口（8081）**：一般无需改动。若确实要改，需同步修改 4 处：`backend/src/main/resources/application.yml`（SERVER_PORT 默认值）、`backend/Dockerfile`（EXPOSE）、`frontend/nginx.conf`（proxy_pass）、`docker-compose.yml`（SERVER_PORT 环境变量），并重建后端与前端镜像。

## 常见问题（FAQ）

**Q：`docker compose up` 报「required variable JWT_SECRET is missing」？**
A：未设置 JWT_SECRET。复制 `.env.example` 为 `.env` 并填入随机密钥后重试。

**Q：构建很慢或拉取依赖失败？**
A：首次构建需下载 Maven/NPM 依赖与基础镜像。可配置国内镜像加速（如阿里云加速器、Maven 镜像 `settings.xml` 挂载进构建阶段）；确认服务器可访问 Docker Hub。

**Q：80 端口被占用？**
A：修改 `.env` 的 `WEB_PORT` 为 8080/8081 等空闲端口，访问地址相应变为 `http://IP:端口/`。

**Q：浏览器通知（Notification）不弹？**
A：局域网 HTTP 环境受浏览器安全策略限制，系统已自动降级为「标题闪烁 + 提示音」，不漏单。如需系统通知，按上文启用 HTTPS。

**Q：数据被我误删/想恢复初始状态？**
A：`docker compose down -v && docker compose up -d` 会重建数据库并重新执行 `docs/init.sql`（清空业务数据，恢复默认账号）。删除前建议先备份。

**Q：MySQL 初始化脚本没生效？**
A：`docs/init.sql` 仅在数据卷**首次创建**时执行。若卷已存在，删除卷（`docker compose down -v`）后重新启动即可。

**Q：中文在日志/控制台显示为问号？**
A：Windows 终端显示问题（编码），数据本身为正确 UTF-8（init.sql 已验证）。Linux 服务器无此问题。

**Q：上传图片后访问 404？**
A：确认 `data/uploads` 目录存在且三个服务均已 healthy；图片经 `nginx /uploads/` 直接托管，文件应落在 `data/uploads/<年月>/` 下。

## 验收清单（阶段四自测摘要）

完整自测过程与结果见 `docs/self-test-report.md`：

- [x] `docker compose up -d` 后 mysql/backend/frontend 全部健康
- [x] 食客端 `/` 与管理端 `/admin` 可访问，静态资源与占位图正常
- [x] admin/love 登录、菜品管理、图片上传（nginx 可访问图片）
- [x] 下单→接单→制作→完成 全链路、10s 轮询、取消/拒绝/再下单
- [x] 收藏、吃饭日历、看板、营业开关
- [x] 重启容器后数据不丢失（mysql-data、uploads 卷持久化）
- [x] 接口文档 `http://IP/api/doc.html` 可访问

## 文档索引

- 需求与验收标准：[PRD.md](PRD.md)
- 开发进度与决策记录：[PROGRESS.md](PROGRESS.md)
- API 接口文档：[docs/api.md](docs/api.md)
- 阶段四自测报告：[docs/self-test-report.md](docs/self-test-report.md)
- 新订单邮件验收：[docs/email-self-test-report.md](docs/email-self-test-report.md)
