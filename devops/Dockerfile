# Dockerfile — Spring Boot 后端（本地构建 JAR，仅打包镜像）
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
