# Dockerfile — Vue 3 前端（多阶段构建 → Nginx）
# ARGS: NODE_IMAGE, NGINX_IMAGE, NPM_REGISTRY
# context: 项目根目录 (c-six-)

ARG NODE_IMAGE=node:18-alpine
ARG NGINX_IMAGE=nginx:alpine

FROM ${NODE_IMAGE} AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json* ./

# npm 镜像加速
ARG NPM_REGISTRY=https://registry.npmmirror.com
RUN if [ -n "${NPM_REGISTRY}" ]; then \
      npm config set registry ${NPM_REGISTRY}; \
    fi

RUN npm ci
COPY frontend/ .
RUN npm run build

FROM ${NGINX_IMAGE}
COPY --from=build /app/dist /usr/share/nginx/html
COPY devops/nginx/default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
