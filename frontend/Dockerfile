# ====================================================================
# 情侣点餐系统 · 前端镜像（多阶段构建）
#
# 构建阶段：node:20.19.0-alpine3.21（npm ci 锁定依赖）
# 运行阶段：nginx:1.27.5-alpine（非 root 运行，托管静态资源 + 反向代理）
#
# 端口：容器内 80（宿主机端口由 compose 的 WEB_PORT 映射）
# ====================================================================

# ---------- 阶段 1：构建 ----------
FROM node:20.19.0-alpine3.21 AS build
WORKDIR /build

# npm 镜像（默认 npmmirror，加速国内构建；海外网络可覆盖为官方源：
#   docker compose build --build-arg NPM_REGISTRY=https://registry.npmjs.org frontend
# 或修改此处默认值）
ARG NPM_REGISTRY=https://registry.npmmirror.com

# 先复制 package.json + lockfile 并 npm ci，最大化层缓存
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund --registry=${NPM_REGISTRY}

# 复制源码（.dockerignore 已排除 node_modules/dist/日志）
COPY . .

# 生产构建（VITE_API_BASE_URL 默认 /api，见 request.ts 兜底）
RUN npm run build

# ---------- 阶段 2：运行（nginx） ----------
FROM nginx:1.27.5-alpine AS runtime

# 时区
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone
ENV TZ=Asia/Shanghai

# 站点配置（反向代理 /api、托管 /uploads 与静态资源）
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 构建产物（含内置占位图 /images/placeholder/*.svg）
COPY --from=build /build/dist /usr/share/nginx/html

# 非 root 运行：nginx 官方镜像默认 worker 用户为 nginx；
# 将需要写入的目录授权给 nginx 用户（/var/cache/nginx、pid 文件）
RUN chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx \
    && touch /var/run/nginx.pid && chown nginx:nginx /var/run/nginx.pid

USER nginx

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
