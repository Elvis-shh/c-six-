# AIServer.Dockerfile — Python FastAPI AI 引擎镜像
# context: 项目根目录 (c-six-)

ARG PYTHON_IMAGE=python:3.11-slim

FROM ${PYTHON_IMAGE}
WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PYTHONPATH=/app

COPY ai-engine/requirements.txt ./requirements.txt
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install -r requirements.txt

COPY ai-engine/app ./app

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
