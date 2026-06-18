# Docker 部署指南

本文档用于本地或服务器部署 SmartReport 全套服务：MySQL、Redis、Spring Boot 后端、FastAPI AI 引擎、Vue/Nginx 前端。

## 1. 前置要求

- 安装 Docker Desktop 或 Docker Engine
- 安装 Docker Compose v2，命令应为 `docker compose`
- 确保端口未被占用：`3000`、`8080`、`8000`、`3307`、`6379`

## 2. 准备环境变量

```bash
cd devops
cp env/.env.example .env
```

默认 `LLM_PROVIDER=mock`，不需要真实大模型 Key 也能跑通聊天链路。后续接真实模型时再填写 `DEEPSEEK_API_KEY`、`OPENAI_API_KEY` 或 `ZHIPU_API_KEY`。

## 3. 构建并启动

```bash
cd devops
docker compose --env-file .env up -d --build
```

启动后访问：

- 前端：`http://localhost:3000`
- 后端健康与 API：`http://localhost:8080/api/v1/search/companies/hot`
- AI 引擎健康检查：`http://localhost:8000/health`
- MySQL：`localhost:3307`，账号 `smartreport` / `smartreport123`

## 4. 查看日志

```bash
docker compose --env-file .env logs -f frontend
docker compose --env-file .env logs -f backend
docker compose --env-file .env logs -f ai-engine
```

## 5. 停止与清理

```bash
docker compose --env-file .env down
```

如果要同时删除数据库数据卷：

```bash
docker compose --env-file .env down -v
```

## 6. 常见问题

- 后端连接 AI 失败：确认 `backend` 容器环境变量 `AI_ENGINE_URL=http://ai-engine:8000`，并检查 `ai-engine` 健康状态。
- 前端聊天无响应：查看 `backend` 和 `ai-engine` 日志，SSE 代理已在 Nginx `location /api/v1/chat/` 中关闭 buffering。
- MySQL 首次初始化慢：等待 `smartreport-mysql` healthcheck 变为 healthy 后后端才会启动。
- 构建依赖下载慢：默认 `.env.example` 使用 DaoCloud / npmmirror / aliyun maven 镜像，可按网络环境替换。
