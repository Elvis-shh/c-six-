# SmartReport Story 文档索引

> 每个 Story 包含完整的 PRD（产品需求文档）+ 开发方案  
> 基于 `docs/tech-design.md` + `docs/tech-plan.md`

---

## Phase 1: MVP（搜索 + 财报数据 + 看板）

### Epic 1: 财报数据引擎 `data-engine` [P0]

| Story | 文档 | 预估 | 依赖 |
|-------|------|------|------|
| 1.1 数据库表结构创建 | [1.1-database-schema.md](1.1-database-schema.md) | 3d | — |
| 1.2 上市公司基础信息入库 | [1.2-company-data.md](1.2-company-data.md) | 2d | 1.1 |
| 1.3 财报指标计算引擎 | [1.3-indicator-engine.md](1.3-indicator-engine.md) | 2d | 1.1 |
| 1.4 行业均值与排名数据 | [1.4-industry-benchmark.md](1.4-industry-benchmark.md) | 1.5d | 1.1, 1.3 |

### Epic 2: 公司搜索与加载 `search-load` [P0]

| Story | 文档 | 预估 | 依赖 |
|-------|------|------|------|
| 2.1 统一搜索入口 | [2.1-search-entry.md](2.1-search-entry.md) | 2d | 1.2 |
| 2.2 页面间参数传递与加载衔接 | [2.2-page-navigation.md](2.2-page-navigation.md) | 1.5d | 2.1 |
| 2.3 分析历史管理 | [2.3-history-management.md](2.3-history-management.md) | 1d | 2.2 |

### Epic 3: 财报总览看板 `dashboard` [P1]

| Story | 文档 | 预估 | 依赖 |
|-------|------|------|------|
| 3.1 KPI 指标卡片 | [3.1-kpi-cards.md](3.1-kpi-cards.md) | 2d | 2.2 |
| 3.2 关键数据速览表格 | [3.2-data-table.md](3.2-data-table.md) | 1.5d | 3.1 |
| 3.3 折线图趋势展示 | [3.3-trend-chart.md](3.3-trend-chart.md) | 1.5d | 3.2 |
| 3.4 核心财务指标详解表 | [3.4-indicator-detail.md](3.4-indicator-detail.md) | 1.5d | 3.1, 1.4 |

---

## Phase 2: 核心闭环（风险亮点 + 预测 + 导出）

### Epic 4: 风险与亮点分析 `risk-highlight` [P1]

| Story | 文档 | 预估 | 依赖 |
|-------|------|------|------|
| 4.1 经营亮点识别与展示 | [4.1-highlights.md](4.1-highlights.md) | 1.5d | Phase 1 |
| 4.2 风险识别与展示 | [4.2-risks.md](4.2-risks.md) | 1.5d | Phase 1 |

### Epic 5: 趋势预测 `predict` [P1]

| Story | 文档 | 预估 | 依赖 |
|-------|------|------|------|
| 5.1 趋势预测图表 | [5.1-predict-chart.md](5.1-predict-chart.md) | 2d | Phase 1 |
| 5.2 预测洞察文案 | [5.2-predict-insights.md](5.2-predict-insights.md) | 1d | 5.1 |

### Epic 8: 报告导出 `export` [P2]

| Story | 文档 | 预估 | 依赖 |
|-------|------|------|------|
| 8.1 多格式导出（前端） | [8.1-export-frontend.md](8.1-export-frontend.md) | 2d | Phase 1 |
| 8.2 导出水印与合规（后端） | [8.2-export-backend.md](8.2-export-backend.md) | 1.5d | 8.1 |

---

## Phase 3: AI 增强（智能问答 + 文件上传解析）

### Epic 6: 智能问答 `chat` [P2]

| Story | 文档 | 预估 | 依赖 |
|-------|------|------|------|
| 6.1 聊天面板 UI | [6.1-chat-ui.md](6.1-chat-ui.md) | 2d | Phase 1 |
| 6.2 RAG 检索增强 | [6.2-rag-retrieval.md](6.2-rag-retrieval.md) | 3d | 6.1 |
| 6.3 LLM 对话生成 | [6.3-llm-generation.md](6.3-llm-generation.md) | 3d | 6.2 |

### Epic 7: 文件上传与解析 `upload` [P2]

| Story | 文档 | 预估 | 依赖 |
|-------|------|------|------|
| 7.1 多格式文件上传 | [7.1-file-upload.md](7.1-file-upload.md) | 2d | Phase 1 |
| 7.2 PDF/图片 OCR 解析 | [7.2-ocr-parsing.md](7.2-ocr-parsing.md) | 2.5d | 7.1 |
| 7.3 财报文本结构化提取 | [7.3-ner-extraction.md](7.3-ner-extraction.md) | 2.5d | 7.2 |

---

## Phase 4: 商业化（用户系统 + 基建）

### Epic 9: 用户系统 `auth` [P3]

| Story | 文档 | 预估 | 依赖 |
|-------|------|------|------|
| 9.1 注册与登录 | [9.1-auth.md](9.1-auth.md) | 2.5d | — |
| 9.2 历史记录云同步 | [9.2-history-sync.md](9.2-history-sync.md) | 1.5d | 9.1 |

### Epic 10: 系统基建 `infra` [P3]

| Story | 文档 | 预估 | 依赖 |
|-------|------|------|------|
| 10.1 CI/CD 与部署 | [10.1-cicd.md](10.1-cicd.md) | 2d | — |
| 10.2 监控与日志 | [10.2-monitoring.md](10.2-monitoring.md) | 1.5d | 10.1 |

---

## 预估工时汇总

| 阶段 | Epic | 故事数 | 预估工时 |
|------|------|--------|---------|
| Phase 1 | data-engine | 4 | 8.5d |
| Phase 1 | search-load | 3 | 4.5d |
| Phase 1 | dashboard | 4 | 6.5d |
| **Phase 1 小计** | | **11** | **19.5d** |
| Phase 2 | risk-highlight | 2 | 3d |
| Phase 2 | predict | 2 | 3d |
| Phase 2 | export | 2 | 3.5d |
| **Phase 2 小计** | | **6** | **9.5d** |
| Phase 3 | chat | 3 | 8d |
| Phase 3 | upload | 3 | 7d |
| **Phase 3 小计** | | **6** | **15d** |
| Phase 4 | auth | 2 | 4d |
| Phase 4 | infra | 2 | 3.5d |
| **Phase 4 小计** | | **4** | **7.5d** |
| **总计** | | **25** | **51.5d (~10.5 周)** |
