# SmartReport — 技术规划文档（Epic / Story / 任务分解）

> 技术负责人视角 · 基于 `product-analysis.md`  
> 日期：2026-05-29

---

## 一、优先级排序 & Epic 概览

| 优先级 | Epic | 代号 | 说明 | 预估故事数 |
|--------|------|------|------|-----------|
| **P0** | 财报数据引擎 | `data-engine` | 数据获取、解析、存储、计算——一切的基础 | 4 |
| **P0** | 公司搜索与加载 | `search-load` | 搜索入口 → 主控台的核心用户闭环 | 3 |
| **P1** | 财报总览看板 | `dashboard` | KPI 卡片、数据表格/图表、指标详解 | 4 |
| **P1** | 风险与亮点分析 | `risk-highlight` | 经营亮点、风险识别展示 | 2 |
| **P1** | 趋势预测 | `predict` | 历史趋势 + 预测图表 + 洞察文案 | 2 |
| **P2** | 智能问答 | `chat` | AI 对话面板 + RAG 检索 | 3 |
| **P2** | 文件上传与解析 | `upload` | 多格式文件上传、OCR、解析流水线 | 3 |
| **P2** | 报告导出 | `export` | PNG/PDF/Word/Excel 多格式导出 | 2 |
| **P3** | 用户系统 | `auth` | 登录注册、用户偏好、历史云同步 | 3 |
| **P3** | 系统基建 | `infra` | 部署、监控、CI/CD、API 网关 | 2 |

---

## 二、Epic → User Story → 开发任务 详细分解

---

### Epic 1: 财报数据引擎 `data-engine` [P0]

#### Story 1.1 — 财报数据结构定义与存储
> 作为系统，我需要统一的财报数据模型，以支撑所有分析模块的数据读写。

| 层级 | 任务 |
|------|------|
| **数据库** | 设计 PostgreSQL 表结构：`companies`、`financial_reports`（按年份+指标列存储）、`financial_indicators`（指标维度宽表） |
| **数据库** | 创建索引：`company_code`、`report_year`、复合索引 `(company_id, year)` |
| **数据库** | 设计行业均值表 `industry_averages`（行业 + 年份 + 指标 + 均值） |
| **后端** | 定义数据模型（ORM：Prisma / SQLAlchemy / TypeORM） |
| **后端** | 实现数据访问层 Repository：`CompanyRepo`、`ReportRepo`、`IndicatorRepo` |
| **前端** | TypeScript 类型定义文件 `types/financial.ts`（与后端模型对齐） |

> ⚠️ **外部依赖**：无（自建数据库）

---

#### Story 1.2 — 上市公司基础信息入库
> 作为系统，我需要预置 A 股上市公司基础信息（名称、代码、行业），以支撑搜索功能。

| 层级 | 任务 |
|------|------|
| **后端** | 编写数据初始化脚本：从公开数据源批量导入 A 股公司列表 |
| **后端** | 实现 `GET /api/companies?q={keyword}` 模糊搜索接口 |
| **后端** | 实现 `GET /api/companies/:code` 公司详情接口 |
| **前端** | 对接搜索接口，替换前端硬编码 `companyDB` |

> ⚠️ **外部依赖**：
> - 🔗 **Tushare / AKShare**（A 股公司基础数据——免费开源）
> - 🔗 **东方财富 API**（备选）

---

#### Story 1.3 — 财报指标计算引擎
> 作为系统，我需要根据原始财报数据自动计算衍生指标（同比、环比、毛利率、净利率、ROE 等）。

| 层级 | 任务 |
|------|------|
| **后端** | 实现指标计算服务 `IndicatorService`：接收原始数据 → 输出标准化指标 Map |
| **后端** | 支持的计算类型：`YoY`（同比）、`QoQ`（环比）、`CAGR`（复合增长率）、`ratio`（比率类） |
| **后端** | 单元测试覆盖所有计算逻辑（边界值：除零、负数、null） |
| **前端** | 移除前端 `formatMetricChange()` 硬编码逻辑，改为消费后端计算结果 |

> ⚠️ **外部依赖**：无

---

#### Story 1.4 — 行业均值与排名数据
> 作为系统，我需要提供行业均值对比数据，让用户了解公司在同行业中的位置。

| 层级 | 任务 |
|------|------|
| **数据库** | 填充 `industry_averages` 表（按证监会行业分类） |
| **后端** | 实现 `GET /api/indicators/:code/benchmark` 行业对比接口 |
| **后端** | 实现行业排名计算（如"毛利率行业前 10%"） |
| **前端** | 渲染指标详解表中的"行业均值"和"行业对比"列 |

> ⚠️ **外部依赖**：
> - 🔗 **Tushare / Wind 数据**（行业财务均值）

---

### Epic 2: 公司搜索与加载 `search-load` [P0]

#### Story 2.1 — 统一搜索入口
> 作为用户，我可以在搜索框输入公司名或股票代码，获得实时建议并快速加载分析。

| 层级 | 任务 |
|------|------|
| **前端** | 搜索框 debounce（300ms）+ 调用模糊搜索 API |
| **前端** | 搜索建议下拉 UI（高亮匹配字符、行业标签） |
| **前端** | 键盘导航（↑↓ 选择、Enter 确认、Esc 关闭） |
| **前端** | 空状态/无结果状态处理 |
| **后端** | `GET /api/search?q=&limit=8` 接口（返回 name/code/industry） |

> ⚠️ **外部依赖**：无

---

#### Story 2.2 — 页面间参数传递与加载衔接
> 作为用户，从搜索入口页跳转到主控台时，系统应自动加载对应公司数据。

| 层级 | 任务 |
|------|------|
| **前端** | 统一 URL 参数传递方案（`?code=600519` → 替换 `sessionStorage` 方案） |
| **前端** | 路由：`/search` → `/dashboard/:code` |
| **前端** | 加载骨架屏（Skeleton）替代 blank 闪烁 |
| **后端** | `GET /api/reports/:code/latest` 获取最新财报摘要 |

> ⚠️ **外部依赖**：无（如需 SSR 可用 Next.js）

---

#### Story 2.3 — 分析历史管理
> 作为用户，我可以查看最近分析过的公司记录，点击即可快速切换。

| 层级 | 任务 |
|------|------|
| **数据库** | `user_search_history` 表（user_id, company_code, searched_at） |
| **后端** | `GET /api/history` + `POST /api/history` 接口 |
| **后端** | 自动去重 + 最多保留 20 条 |
| **前端** | 历史下拉列表 UI（相对时间展示："3 分钟前"） |

> ⚠️ **外部依赖**：无（P0 阶段用 localStorage，P3 接入用户系统后迁移到数据库）

---

### Epic 3: 财报总览看板 `dashboard` [P1]

#### Story 3.1 — KPI 指标卡片
> 作为用户，我可以在页面顶部看到 4 个核心 KPI（营收、净利润、资产负债率、现金流）及同比变化。

| 层级 | 任务 |
|------|------|
| **前端** | 4 卡片响应式 Grid 布局（复用现有 CSS） |
| **前端** | 数字滚动动画（count-up effect） |
| **前端** | 同比箭头 ↑↓ 颜色逻辑（绿涨红跌 / 红涨绿跌——金融行业惯例） |
| **后端** | `GET /api/reports/:code/kpi` 接口（返回最新值 + 同比变化率） |

> ⚠️ **外部依赖**：无

---

#### Story 3.2 — 关键数据速览表格
> 作为用户，我可以查看 5 个核心指标近 5 年的表格数据。

| 层级 | 任务 |
|------|------|
| **前端** | 响应式数据表格组件（sticky 表头、斑马纹） |
| **前端** | 表头固定 + 横向滚动（移动端适配） |
| **后端** | `GET /api/reports/:code/timeline?metrics=revenue,profit,grossMargin,debtRatio,cashFlow` |

> ⚠️ **外部依赖**：无

---

#### Story 3.3 — 折线图趋势展示
> 作为用户，我可以切换到折线图模式，查看单个指标的 5 年趋势，并自由切换指标。

| 层级 | 任务 |
|------|------|
| **前端** | Chart.js 折线图组件封装（支持动态切换 dataset） |
| **前端** | 指标切换 Tab 按钮组 |
| **前端** | 图表 Tooltip 格式化（单位、千分位） |
| **后端** | 复用 Story 3.2 接口 |

> ⚠️ **外部依赖**：
> - 🔗 **Chart.js**（CDN / npm）

---

#### Story 3.4 — 核心财务指标详解表
> 作为用户，我可以查看带术语解释的指标详情表，了解每个指标的含义和行业对比。

| 层级 | 任务 |
|------|------|
| **前端** | 带 Tooltip 的术语标签组件（`?` 悬浮解释） |
| **前端** | 评价 Tag 组件（优秀/良好/健康/关注） |
| **后端** | `GET /api/reports/:code/indicators` 返回指标详情列表（含行业对比） |
| **后端** | `GET /api/terms` 返回术语解释字典 |

> ⚠️ **外部依赖**：无

---

### Epic 4: 风险与亮点分析 `risk-highlight` [P1]

#### Story 4.1 — 经营亮点识别与展示
> 作为用户，我希望能看到系统自动识别出的公司经营亮点。

| 层级 | 任务 |
|------|------|
| **前端** | 亮点卡片组件（绿色左边框 + icon + 标题 + 描述） |
| **后端** | `GET /api/reports/:code/highlights` 返回亮点列表 |
| **后端** | 实现亮点规则引擎：毛利率 > 80% → "盈利能力卓越"、现金流 > 净利润 → "利润含金量高"、资产负债率 < 30% → "财务结构安全" |
| **数据库** | `highlight_rules` 配置表（阈值可调） |

> ⚠️ **外部依赖**：无（P2 可升级为 LLM 生成）

---

#### Story 4.2 — 风险识别与展示
> 作为用户，我希望能看到系统自动识别出的潜在风险项。

| 层级 | 任务 |
|------|------|
| **前端** | 风险卡片组件（红色左边框） |
| **后端** | `GET /api/reports/:code/risks` 返回风险列表 |
| **后端** | 实现风险规则引擎：利润增速连续下降 → "增速放缓"、行业政策标签 → "政策风险" |
| **数据库** | `risk_rules` 配置表 |

> ⚠️ **外部依赖**：无（P2 可升级为 LLM 生成）

---

### Epic 5: 趋势预测 `predict` [P1]

#### Story 5.1 — 趋势预测图表
> 作为用户，我可以查看基于历史数据外推的营收和净利润预测图表。

| 层级 | 任务 |
|------|------|
| **前端** | 双折线图（实际值实线 + 预测值虚线）+ 图例标注 |
| **前端** | 预测区间半透明色带（confidence band） |
| **后端** | `GET /api/reports/:code/predict` 返回预测数据点 |
| **后端** | 实现简单线性回归 / 移动平均预测算法 |
| **后端** | 返回预测数据 + 置信区间 |

> ⚠️ **外部依赖**：无（简单统计算法自研；P2 可升级为 ML 模型）

---

#### Story 5.2 — 预测洞察文案
> 作为用户，我可以看到基于预测数据的文字解读和风险提示。

| 层级 | 任务 |
|------|------|
| **前端** | 洞察文案面板渲染 |
| **后端** | `GET /api/reports/:code/predict/insights` 返回模板化洞察 |
| **后端** | 免责声明强制附加逻辑 |

> ⚠️ **外部依赖**：无（P2 可升级为 LLM 生成洞察文案）

---

### Epic 6: 智能问答 `chat` [P2]

#### Story 6.1 — 聊天面板 UI
> 作为用户，我可以在浮动面板中与 AI 助手进行财报相关问答。

| 层级 | 任务 |
|------|------|
| **前端** | 聊天面板组件（消息列表 + 输入框 + 发送按钮） |
| **前端** | 自动滚动到底部 + 打字机效果（streaming 准备） |
| **前端** | 浮动按钮拖拽交互（保留现有实现） |
| **前端** | 欢迎语 + 建议问题快捷按钮 |
| **后端** | `POST /api/chat` 接口（SSE / WebSocket 准备） |

> ⚠️ **外部依赖**：无

---

#### Story 6.2 — RAG 检索增强
> 作为系统，我需要在回答前先检索相关财报段落，提升回答准确性。

| 层级 | 任务 |
|------|------|
| **后端** | 财报文本向量化（Embedding）并存入向量数据库 |
| **后端** | 查询时：用户问题 → Embedding → 向量相似度检索 Top-K → 拼接 Context |
| **后端** | 实现检索结果重排序（Reranker） |
| **数据库** | 接入向量数据库：Milvus / Pinecone / pgvector |

> ⚠️ **外部依赖**：
> - 🔗 **OpenAI Embeddings API** 或 **智谱 Embedding API**（中文优化）
> - 🔗 **Milvus** / **Pinecone** / **pgvector**（向量数据库）
> - 🔗 **Cohere Rerank**（可选，重排序）

---

#### Story 6.3 — LLM 对话生成
> 作为系统，我需要调用 LLM 生成财报分析回答。

| 层级 | 任务 |
|------|------|
| **后端** | LLM 调用层（支持多 Provider：OpenAI / 智谱 / 百度文心 / DeepSeek） |
| **后端** | Prompt 模板管理（System Prompt + 财报 Context 注入） |
| **后端** | 流式输出（SSE）支持 |
| **后端** | Token 用量统计 + 成本控制 |
| **前端** | SSE 流式渲染（逐字输出） |

> ⚠️ **外部依赖**：
> - 🔗 **OpenAI GPT-4o** / **DeepSeek V3** / **智谱 GLM-4**（LLM API）
> - 🔗 **LangChain**（可选，Prompt 编排）

---

### Epic 7: 文件上传与解析 `upload` [P2]

#### Story 7.1 — 多格式文件上传
> 作为用户，我可以上传 PDF/Word/Excel/图片/TXT 格式的财报文件。

| 层级 | 任务 |
|------|------|
| **前端** | 拖拽上传区域 + 点击上传 + 粘贴上传 |
| **前端** | 文件类型校验 + 大小限制（单文件 ≤ 50MB） |
| **前端** | 上传进度条（分片上传 준비） |
| **后端** | `POST /api/upload` 文件接收接口 |
| **后端** | 文件格式校验 + 安全扫描（防病毒） |
| **存储** | 对象存储：阿里云 OSS / AWS S3 / MinIO |

> ⚠️ **外部依赖**：
> - 🔗 **阿里云 OSS** / **AWS S3**（文件存储）

---

#### Story 7.2 — PDF/图片 OCR 解析
> 作为系统，我需要从 PDF 和图片中提取财务文本和表格数据。

| 层级 | 任务 |
|------|------|
| **后端** | PDF 文本提取（PyMuPDF / pdfplumber） |
| **后端** | 图片 OCR（PaddleOCR / Tesseract） |
| **后端** | 表格识别与结构化提取 |
| **后端** | 异步任务队列（Celery / BullMQ）处理大文件 |

> ⚠️ **外部依赖**：
> - 🔗 **PaddleOCR**（开源，中文 OCR 效果好）
> - 🔗 **百度 OCR API** / **腾讯 OCR API**（商业级备选）

---

#### Story 7.3 — 财报文本结构化提取
> 作为系统，我需要从非结构化财报文本中提取关键财务指标。

| 层级 | 任务 |
|------|------|
| **后端** | 正则 + NER 提取：营收、净利润、现金流、资产等 |
| **后端** | LLM 辅助提取（少量 Prompt → 结构化 JSON） |
| **后端** | 人工校验标记（不确定的字段标记为 `confidence < 0.8`） |
| **前端** | 提取结果预览 + 用户手动修正界面 |

> ⚠️ **外部依赖**：
> - 🔗 **LLM API**（GPT-4o / DeepSeek，用于非结构化提取）

---

### Epic 8: 报告导出 `export` [P2]

#### Story 8.1 — 多格式导出
> 作为用户，我可以将当前分析报告导出为 PNG/PDF/Word/Excel。

| 层级 | 任务 |
|------|------|
| **前端** | 导出菜单 UI + 格式选择 |
| **前端** | 前端截图导出（PNG/PDF）—— `html2canvas` + `jsPDF` |
| **后端** | `POST /api/export` 服务端渲染导出（Word/Excel）——更高保真度 |
| **后端** | Word 模板（docx-templater / python-docx） |
| **后端** | Excel 模板（openpyxl / exceljs） |

> ⚠️ **外部依赖**：
> - 🔗 **html2canvas** + **jsPDF**（前端 PNG/PDF，已有）
> - 🔗 **Puppeteer**（服务端 PDF 渲染，可选升级）

---

#### Story 8.2 — 导出水印与合规
> 作为系统，导出的报告需要包含免责声明和水印。

| 层级 | 任务 |
|------|------|
| **前端** | 导出前自动附加免责声明到 DOM |
| **后端** | Word/Excel 模板内嵌免责声明 |
| **后端** | PDF 水印叠加 |

> ⚠️ **外部依赖**：无

---

### Epic 9: 用户系统 `auth` [P3]

#### Story 9.1 — 注册与登录
> 作为用户，我可以注册账户并登录，以保存我的分析历史和偏好。

| 层级 | 任务 |
|------|------|
| **数据库** | `users` 表（id, email, password_hash, created_at） |
| **后端** | `POST /api/auth/register` + `POST /api/auth/login` |
| **后端** | JWT Token 签发与验证中间件 |
| **后端** | 密码加密（bcrypt） |
| **前端** | 登录/注册模态框 |

> ⚠️ **外部依赖**：
> - 🔗 **Auth0** / **Clerk** / **NextAuth**（可选，快速接入）

---

#### Story 9.2 — 历史记录云同步
> 作为用户，我的分析历史可以在多设备间同步。

| 层级 | 任务 |
|------|------|
| **后端** | `GET/POST /api/history` 改为需认证 |
| **后端** | 历史数据与用户 ID 关联 |
| **前端** | localStorage → API 迁移（渐进式：本地优先 + 后台同步） |

> ⚠️ **外部依赖**：无

---

### Epic 10: 系统基建 `infra` [P3]

#### Story 10.1 — CI/CD 与部署
> 作为团队，我们需要自动化构建、测试和部署流程。

| 层级 | 任务 |
|------|------|
| **DevOps** | Docker 容器化（前端 Nginx + 后端 Node/Python） |
| **DevOps** | GitHub Actions / GitLab CI 流水线 |
| **DevOps** | 部署到阿里云 ECS / Vercel（前端）+ Railway（后端） |
| **DevOps** | 环境变量管理 + 密钥轮换 |

> ⚠️ **外部依赖**：
> - 🔗 **阿里云 ECS** / **Vercel** / **Railway**（部署平台）
> - 🔗 **Docker Hub** / **阿里云 ACR**（镜像仓库）

---

#### Story 10.2 — 监控与日志
> 作为团队，我们需要可观测性基础设施。

| 层级 | 任务 |
|------|------|
| **DevOps** | 日志收集：Winston/Pino → ELK / Loki |
| **DevOps** | APM：Sentry（错误追踪）+ OpenTelemetry |
| **DevOps** | API 限流（rate-limit）+ API 网关（Kong / Nginx） |
| **DevOps** | 健康检查端点 `/api/health` |

> ⚠️ **外部依赖**：
> - 🔗 **Sentry**（错误追踪）
> - 🔗 **Grafana Loki** / **ELK**（日志聚合）

---

## 三、模块划分建议

```
src/
├── modules/
│   ├── auth/                  # 用户认证与授权
│   │   ├── auth.controller.ts
│   │   ├── auth.service.ts
│   │   ├── auth.guard.ts
│   │   └── dto/
│   │
│   ├── search/                # 公司搜索
│   │   ├── search.controller.ts
│   │   ├── search.service.ts
│   │   └── dto/
│   │
│   ├── dashboard/             # 财报总览看板
│   │   ├── dashboard.controller.ts
│   │   ├── dashboard.service.ts    # KPI / 数据表 / 图表数据聚合
│   │   └── dto/
│   │
│   ├── indicators/            # 财务指标计算引擎
│   │   ├── indicators.controller.ts
│   │   ├── indicators.service.ts   # 同比/环比/比率计算
│   │   ├── indicators.benchmark.ts # 行业对比
│   │   └── utils/
│   │
│   ├── risk-highlight/        # 风险与亮点
│   │   ├── risk-highlight.controller.ts
│   │   ├── highlight.service.ts    # 亮点规则引擎
│   │   ├── risk.service.ts         # 风险规则引擎
│   │   └── rules/                  # 可配置规则集
│   │
│   ├── predict/               # 趋势预测
│   │   ├── predict.controller.ts
│   │   ├── predict.service.ts      # 回归/移动平均
│   │   └── utils/
│   │
│   ├── chat/                  # 智能问答
│   │   ├── chat.controller.ts
│   │   ├── chat.service.ts         # LLM 调用编排
│   │   ├── rag.service.ts          # RAG 检索
│   │   └── prompts/                # Prompt 模板
│   │
│   ├── upload/                # 文件上传与解析
│   │   ├── upload.controller.ts
│   │   ├── upload.service.ts
│   │   ├── ocr.service.ts          # OCR 识别
│   │   ├── parser.service.ts       # 财报文本结构化
│   │   └── storage/                # 对象存储适配器
│   │
│   ├── export/                # 报告导出
│   │   ├── export.controller.ts
│   │   ├── export.service.ts
│   │   ├── templates/              # Word/Excel 模板
│   │   └── utils/
│   │
│   └── history/               # 分析历史
│       ├── history.controller.ts
│       └── history.service.ts
│
├── common/                    # 公共模块
│   ├── database/              # 数据库连接、ORM 配置
│   ├── cache/                 # Redis 缓存
│   ├── queue/                 # 消息队列（BullMQ/Celery）
│   ├── llm/                   # LLM Provider 适配器（OpenAI/智谱/DeepSeek）
│   └── types/                 # 共享 TypeScript 类型
│
└── infrastructure/            # 基础设施
    ├── docker/
    ├── k8s/
    ├── terraform/
    └── monitoring/
```

---

## 四、外部依赖汇总

| 类别 | 服务 | 用途 | Epic | 优先级 |
|------|------|------|------|--------|
| 🤖 **AI/LLM** | OpenAI GPT-4o / DeepSeek V3 / 智谱 GLM-4 | 智能问答、非结构化提取 | `chat` `upload` | P2 |
| 🤖 **AI/LLM** | OpenAI Embeddings / 智谱 Embedding | RAG 向量化 | `chat` | P2 |
| 📊 **数据源** | Tushare / AKShare | A 股公司列表、财报数据 | `data-engine` | P0 |
| 📊 **数据源** | Wind 数据（商业） | 行业均值（高精度） | `data-engine` | P2 |
| 🔍 **OCR** | PaddleOCR / 百度 OCR | PDF/图片文字识别 | `upload` | P2 |
| 🗄️ **向量库** | Milvus / pgvector / Pinecone | RAG 检索存储 | `chat` | P2 |
| 📦 **存储** | 阿里云 OSS / AWS S3 | 上传文件存储 | `upload` | P2 |
| 📈 **图表** | Chart.js | 前端折线图渲染 | `dashboard` `predict` | P1 |
| 📄 **导出** | html2canvas + jsPDF | 前端 PNG/PDF 导出 | `export` | P2 |
| 🔐 **认证** | Auth0 / Clerk（可选） | 用户登录注册 | `auth` | P3 |
| 🚀 **部署** | Vercel / 阿里云 ECS | 应用部署 | `infra` | P3 |
| 📊 **监控** | Sentry / Grafana Loki | 错误追踪、日志 | `infra` | P3 |

---

## 五、建议迭代路线

```
Phase 1 (MVP, 2-3 周)
├── Epic 1: 财报数据引擎（Story 1.1-1.3）
├── Epic 2: 公司搜索与加载（全部）
└── Epic 3: 财报总览看板（全部）

Phase 2 (核心闭环, 2-3 周)
├── Epic 4: 风险与亮点（全部）
├── Epic 5: 趋势预测（全部）
└── Epic 8: 报告导出（Story 8.1）

Phase 3 (AI 增强, 3-4 周)
├── Epic 6: 智能问答（全部）← 需要 LLM API
├── Epic 7: 文件上传与解析（全部）← 需要 OCR
└── Epic 8: 导出合规（Story 8.2）

Phase 4 (商业化, 2-3 周)
├── Epic 9: 用户系统（全部）
└── Epic 10: 系统基建（全部）
```
