# 多阶段构建 Dockerfile
# 阶段 1: 构建阶段
FROM gradle:8.10.2-jdk21-alpine AS builder

# 设置工作目录
WORKDIR /app

# 复制 Gradle 构建文件并预热依赖（利用 Docker 缓存）
COPY build.gradle settings.gradle gradle.properties ./
RUN gradle dependencies --no-daemon

# 复制源代码
COPY src ./src

# 构建应用（跳过测试以加快构建速度）
RUN gradle clean bootJar -x test --no-daemon

# 阶段 2: 运行阶段
FROM eclipse-temurin:21-jre-alpine

# 设置工作目录
WORKDIR /app

# 创建非 root 用户运行应用
RUN addgroup -g 1001 appuser && \
    adduser -D -u 1001 -G appuser appuser

# 从构建阶段复制 jar 文件
COPY --from=builder /app/build/libs/*.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# 切换到非 root 用户
USER appuser

# 暴露应用端口
EXPOSE 8090

# 设置 JVM 参数和时区
ENV JAVA_OPTS="-Xms512m -Xmx2048m -Djava.security.egd=file:/dev/./urandom" \
    TZ=Asia/Shanghai

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8090/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
