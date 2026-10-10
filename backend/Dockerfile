# syntax=docker/dockerfile:1
# ====================================================================
# 情侣点餐系统 · 后端镜像（多阶段构建）
#
# 构建阶段：maven:3.9.9-eclipse-temurin-17（JDK 17 + Maven）
# 运行阶段：eclipse-temurin:17-jre-alpine（最小 JRE，非 root 用户）
#
# 产物：couple-ordering.jar（Spring Boot 3.2.5 / Java 17）
# 端口：容器内 8081（与 application.yml、nginx 反代保持一致）
# ====================================================================

# ---------- 阶段 1：构建 ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build

# Maven 镜像配置（默认阿里云，加速国内构建；海外网络可替换或删除 <mirrors>）
ARG MAVEN_SETTINGS_FILE=maven-settings.xml
# 网络较差时调大超时（毫秒）
ARG MAVEN_CONNECT_TIMEOUT=60000
ARG MAVEN_READ_TIMEOUT=120000

# 先只复制 pom.xml 并拉取依赖，最大化利用 Docker 层缓存；
# --mount=type=cache 将 ~/.m2 保存在 BuildKit 缓存中，断网/超时重试不重复下载
COPY pom.xml ${MAVEN_SETTINGS_FILE} .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -s ${MAVEN_SETTINGS_FILE} \
        -Daether.connector.connectTimeout=${MAVEN_CONNECT_TIMEOUT} \
        -Daether.connector.requestTimeout=${MAVEN_READ_TIMEOUT} \
        dependency:go-offline -DskipTests

# 再复制源码并打包（跳过测试，测试在 CI/联调阶段执行）
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -s ${MAVEN_SETTINGS_FILE} \
        -Daether.connector.connectTimeout=${MAVEN_CONNECT_TIMEOUT} \
        -Daether.connector.requestTimeout=${MAVEN_READ_TIMEOUT} \
        clean package -DskipTests

# ---------- 阶段 2：运行 ----------
FROM eclipse-temurin:17-jre-alpine AS runtime

# 时区（Asia/Shanghai）+ tzdata
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone
ENV TZ=Asia/Shanghai

# 创建非 root 运行用户（uid/gid 固定为 10001，方便宿主机权限管理）
RUN addgroup -S -g 10001 app \
    && adduser -S -D -u 10001 -G app app

WORKDIR /app

# 复制可执行 jar 并赋予 app 用户读写权限
COPY --from=build /build/target/couple-ordering.jar /app/couple-ordering.jar
RUN chown -R app:app /app

# 上传图片目录（由 docker-compose 挂载 ./data/uploads，见 compose 文件）
RUN mkdir -p /app/uploads /app/logs && chown -R app:app /app/uploads /app/logs

# 运行用户：非 root
USER app

# 容器内服务端口（与 application.yml 默认 SERVER_PORT=8081 一致）
EXPOSE 8081

# JAVA_TOOL_OPTIONS 可在 compose 中注入额外 JVM 参数（如 -Xmx）
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/couple-ordering.jar"]
