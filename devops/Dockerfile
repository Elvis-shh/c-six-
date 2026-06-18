# Dockerfile — Spring Boot 后端（多阶段构建）
# context: 项目根目录 (c-six-)

ARG BUILDER_IMAGE=maven:3.9-eclipse-temurin-17
ARG RUNTIME_IMAGE=eclipse-temurin:17-jre

FROM ${BUILDER_IMAGE} AS build
WORKDIR /app
ARG MAVEN_MIRROR_URL=https://maven.aliyun.com/repository/public
RUN mkdir -p /root/.m2 && printf '<settings><mirrors><mirror><id>mirror</id><mirrorOf>*</mirrorOf><url>%s</url></mirror></mirrors></settings>' "${MAVEN_MIRROR_URL}" > /root/.m2/settings.xml
COPY backend/pom.xml ./pom.xml
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -DskipTests dependency:go-offline
COPY backend/src ./src
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -DskipTests package

FROM ${RUNTIME_IMAGE}
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
