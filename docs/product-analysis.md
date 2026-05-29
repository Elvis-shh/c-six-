# SmartReport — 智能财报研读与分析助手 · 产品分析文档

> 分析日期：2026-05-29  
> 分析范围：`proto/search.html`（搜索入口）+ `proto/homepage.html`（分析主控台）

---

## 一、页面清单

| 序号 | 页面 | 文件 | 职责 |
|------|------|------|------|
| P1 | 搜索入口页 | `search.html` | 公司搜索、示例引导、文件上传、手动输入 |
| P2 | 分析主控台 | `homepage.html` | 财报总览、趋势预测、智能问答、导出、历史管理 |

> 两页面通过 `sessionStorage` 传递搜索参数实现跳转衔接（`search.html` → `homepage.html`）。

---

## 二、功能模块详细分析

---

### P1：搜索入口页 (`search.html`)

#### M1.1 品牌展示区
- **核心输入**：无
- **核心操作**：静态展示
- **核心输出**：品牌名 "SmartReport" + 副标题说明
- **状态依赖**：无

#### M1.2 公司搜索框
- **核心输入**：用户键入的公司名称/股票代码（文本）
- **核心操作**：
  - 输入时实时过滤 `companyDB`（模糊匹配 name 或 code）
  - 匹配成功：渲染搜索建议下拉列表（含公司名、代码、行业、摘要）
  - 匹配失败：显示"未找到匹配公司"提示
  - 点击建议项或回车 → 写入 `sessionStorage` 并跳转 `homepage.html`
- **核心输出**：
  - 搜索建议列表（UI）
  - `sessionStorage.reportSearchPayload`（JSON）
  - 页面跳转至 `homepage.html`
- **状态依赖**：依赖 `companyDB`（本地硬编码 8 家公司）

#### M1.3 上传财报文件
- **核心输入**：用户点击 📁 按钮 → 展开 Action Panel → 点击上传卡片 → 选择本地文件
- **核心操作**：
  - 打开文件选择器（`accept=".pdf,.doc,.docx,.txt,.xls,.xlsx"`）
  - 选择文件后 → 写入 `sessionStorage` → 跳转 `homepage.html`
- **核心输出**：`sessionStorage.reportSearchPayload = { type: 'upload', fileName }`
- **状态依赖**：`actionPanel` 显示/隐藏状态

#### M1.4 手动输入财报文本
- **核心输入**：用户点击 ✏️ 按钮 → 在 textarea 中粘贴/输入财报文本
- **核心操作**：
  - 展开 Action Panel，聚焦 textarea
  - 点击"开始分析" → 校验非空 → 写入 `sessionStorage` → 跳转 `homepage.html`
- **核心输出**：`sessionStorage.reportSearchPayload = { type: 'manualText', text }`
- **状态依赖**：`actionPanel` 显示/隐藏状态

#### M1.5 示例公司 Chip 列表
- **核心输入**：用户点击示例 Chip（6 家热门公司）
- **核心操作**：写入 `sessionStorage` → 跳转 `homepage.html`
- **核心输出**：`sessionStorage.reportSearchPayload = { type: 'company', name, code }`
- **状态依赖**：无

---

### P2：分析主控台 (`homepage.html`)

#### M2.1 顶部导航栏 (Navbar)
- **子功能**：
  - **品牌 Logo**：静态展示 "📊 SmartReport"
  - **内嵌搜索框**：与 P1 搜索逻辑相同（`companyDB` 过滤 + 下拉建议）
  - **上传/手动输入按钮**：与 P1 的 Action Panel 逻辑相同
  - **分析历史下拉**：展示最近分析记录，点击可重新加载
  - **导出按钮**：展开导出菜单（PNG/PDF/Word/Excel）
- **核心输入**：搜索文本 / 文件 / 手动文本 / 历史选择 / 导出格式选择
- **核心操作**：
  - 搜索 → 匹配公司 → `loadCompany()` 或 `loadCustomTextReport()`
  - 历史下拉 → `toggleHistoryDropdown()` → 点击历史项 → `loadCompany()`
  - 导出 → `exportReport(format)` → 生成对应格式文件并触发下载
- **核心输出**：
  - 公司数据加载（更新全局状态 `currentCompany`）
  - 页面跳转不涉及（已在主控台内）
  - 导出文件下载（PNG/PDF/Word/CSV）
- **状态依赖**：
  - `currentCompany` 全局状态（`{ name, code }`）
  - `analysisHistory` 数组（localStorage 持久化）
  - 滚动方向（控制 Navbar 显隐）
  - `chat-open` CSS 类（控制 Navbar 宽度）

#### M2.2 粘性报告头 (Sticky Report Header)
- **子功能**：
  - **公司横幅 (Company Banner)**：显示"当前公司：XXX（代码）"
  - **侧边导航 (Sidebar)**：模块切换标签（财报总览 / 趋势预测）
- **核心输入**：`currentCompany` 状态变更
- **核心操作**：
  - 切换模块 → `switchModule(moduleName)` → 显示/隐藏对应 `.module-panel`
  - 粘性定位跟随滚动
- **核心输出**：
  - 公司名称横幅更新
  - 当前激活模块面板渲染
  - 活跃 Nav 标签高亮
- **状态依赖**：
  - `currentCompany` 全局状态
  - `currentModule` 全局状态（`'overview'` / `'predict'`）
  - 滚动位置（sticky offset 动态调整）
  - Navbar 显隐状态（`body.topbar-hidden`）

#### M2.3 欢迎引导状态 (Welcome State)
- **核心输入**：无公司数据时自动展示
- **核心操作**：点击"上传财报文件"或"搜索A股公司"按钮
- **核心输出**：引导文案 + 两个 CTA 按钮
- **状态依赖**：当 `welcomeState.style.display !== 'none'` 时可见；加载公司后隐藏

#### M2.4 财报总览模块 (Module: overview)
这是核心分析模块，包含 5 个子模块：

##### M2.4.1 KPI 指标卡片
- **核心输入**：`getFinancialData(currentCompany.name)` 返回的财务数据
- **核心操作**：
  - 读取最近两年 revenue/profit/debtRatio/cashFlow
  - 计算同比变化率
  - 判定涨跌方向（up/down/neutral）
- **核心输出**：4 张 KPI 卡片（营业总收入、归母净利润、资产负债率、经营现金流），每张含数值 + 同比变化
- **状态依赖**：`currentCompany.name`

##### M2.4.2 关键数据速览（表格/折线图切换）
- **核心输入**：`getOverviewMetrics(d)` 返回的 5 个指标（营收、净利润、毛利率、资产负债率、现金流）
- **核心操作**：
  - 表格视图：渲染 5 行 × 5 年数据表
  - 折线图视图：Canvas Chart.js 渲染单个指标折线图，支持切换指标
  - `switchOverviewDataView('table'|'line')` 切换视图
  - `switchOverviewMetric(key)` 切换折线图指标
- **核心输出**：
  - `<table>` HTML 表格（5 年数据）
  - Chart.js 折线图实例（`chartOverviewMetricLine`）
  - 指标切换按钮组
- **状态依赖**：
  - `currentOverviewDataView`（`'table'` / `'line'`）
  - `currentOverviewMetric`（`'revenue'` / `'profit'` / `'grossMargin'` / `'debtRatio'` / `'cashFlow'`）
  - `chartInstances['overviewMetricLine']`（Chart.js 实例，切换前需 destroy）

##### M2.4.3 核心财务指标详解表
- **核心输入**：财务数据 + `termTips` 术语解释映射
- **核心操作**：渲染 5 个指标（毛利率、净利率、资产负债率、经营现金流、ROE）的详细行，含数值、同比变化、行业均值、行业对比、评价标签
- **核心输出**：带术语 tooltip（`?` 悬浮提示）的指标表格
- **状态依赖**：`currentCompany.name`

##### M2.4.4 经营亮点 / 风险识别
- **核心输入**：`getFinancialData()` 中的 debtRatio、cashFlow、grossMargin 等
- **核心操作**：
  - 渲染 4 个风险项（利润增速放缓、政策监管、宏观经济、现金流充裕）
  - 渲染 4 个亮点项（品牌护城河、盈利能力、财务安全、现金流质量）
  - 动态插入当前公司实际数值
- **核心输出**：`.risk-grid` 风险卡片 + `.risk-grid` 亮点卡片（带颜色边框区分 positive/negative）
- **状态依赖**：`currentCompany.name`

##### M2.4.5 免责声明
- **核心输入**：无
- **核心操作**：静态展示
- **核心输出**：黄色虚线边框提示文案
- **状态依赖**：无

#### M2.5 趋势预测模块 (Module: predict)
- **核心输入**：`getFinancialData()` 的 revenue、profit、years
- **核心操作**：
  - 基于近 5 年 CAGR 估算 2025E/2026E（营收 12%/10%，利润 10%/9%）
  - 渲染双折线图（实际值实线 + 预测值虚线）
  - 生成文字洞察（营收趋势、盈利能力预测、关键假设、风险提示）
- **核心输出**：
  - Chart.js 折线图实例（`chartPredict`）
  - 预测洞察文字面板
  - 免责声明
- **状态依赖**：
  - `currentCompany.name`
  - `chartInstances['chartPredict']`（切换前需 destroy）

#### M2.6 智能问答面板 (Chat Panel)
- **核心输入**：
  - 用户文本问题（`chatInput`）
  - `currentCompany` 财务数据
- **核心操作**：
  - 发送消息 → 渲染用户气泡 → 模拟延迟 600-1200ms → 关键词匹配回复
  - 关键词：盈利/赚钱、风险/问题、现金流/现金、增长/营收、其他（默认）
  - 每条回复附带 `refs` 参考来源标注
  - 支持导出聊天记录为 .txt 文件
- **核心输出**：
  - 聊天气泡 HTML（用户 + 助手）
  - RAG 参考来源标注
  - 导出 .txt 文件（`exportChat()`）
- **状态依赖**：
  - `chatPanel.classList.contains('show')` → 面板显隐
  - `body.chat-open` → Navbar/主区域宽度联动
  - `chatWidget` 浮动按钮显隐（面板打开时隐藏）
  - `chatIntroBubble` 引导气泡显隐（`dismissChatIntro()` 一次性）
  - `chatToggle` 拖拽位置（`pointerdown/move/up` 事件，`dataset.dragged` 标记）

#### M2.7 文件上传处理 (Upload Processing)
- **核心输入**：本地文件（PDF/Word/Excel/TXT/图片）
- **核心操作**：
  - 文件选择 → 显示遮罩 Overlay → 4 阶段模拟进度 → 随机加载一家公司数据
  - 支持拖拽上传（`drop` 事件）
- **核心输出**：
  - 处理遮罩动画（spinner + 阶段文字）
  - Toast 通知
- **状态依赖**：`processingOverlay` 显隐

#### M2.8 导出功能 (Export)
- **核心输入**：用户选择导出格式（PNG / PDF / Word / Excel）
- **核心操作**：
  - PNG：`html2canvas` 截图 → Blob 下载
  - PDF：`html2canvas` → `jsPDF` 分页处理
  - Word：构建 HTML blob → 下载 .doc
  - Excel：提取表格 → CSV 生成 → 下载 .csv
- **核心输出**：浏览器触发文件下载
- **状态依赖**：
  - `currentCompany.name`（用于文件命名前缀）
  - `exportMenu` 显隐

#### M2.9 分析历史管理 (Analysis History)
- **核心输入**：每次 `loadCompany()` 调用
- **核心操作**：
  - 写入 `analysisHistory` 数组头部（去重）
  - 最多保留 10 条
  - 持久化到 `localStorage('analysisHistory')`
  - 渲染历史下拉列表
- **核心输出**：
  - `localStorage` 更新
  - 下拉列表 UI 更新
  - 最近分析时间戳 Navbar 标签更新
- **状态依赖**：`analysisHistory` 数组

#### M2.10 Toast 通知
- **核心输入**：各模块操作结果
- **核心操作**：显示 2.8 秒后自动消失
- **核心输出**：底部右侧浮动消息
- **状态依赖**：`toastTimer`（防抖）

#### M2.11 键盘快捷键
- `Ctrl+K`：聚焦搜索框
- `Ctrl+O`：触发上传
- `Escape`：关闭 Action Panel

---

## 三、状态依赖全图

### 3.1 页面级状态传递

```
search.html                         homepage.html
    │                                     │
    │  sessionStorage                     │
    │  .reportSearchPayload               │
    ├─────────────────────────────────────►
    │  { type, name, code }               │  processStartupPayload()
    │  { type, manualText, text }         │  → loadCompany() / loadCustomTextReport()
    │  { type, upload, fileName }         │
```

### 3.2 全局运行时状态（homepage.html）

| 状态变量 | 类型 | 初始值 | 作用域 | 持久化 |
|----------|------|--------|--------|--------|
| `currentCompany` | `{name, code}` | `{贵州茅台, 600519}` | 全局 JS | 否 |
| `currentModule` | `string` | `'overview'` | 全局 JS | 否 |
| `currentOverviewMetric` | `string` | `'revenue'` | 全局 JS | 否 |
| `currentOverviewDataView` | `string` | `'table'` | 全局 JS | 否 |
| `analysisHistory` | `Array<{name,code,time}>` | 从 localStorage 读取 | 全局 JS | ✅ localStorage |
| `chartInstances` | `Object<string, Chart>` | `{}` | 全局 JS | 否 |
| `chatPanel.show` | CSS class | 关闭 | DOM | 否 |
| `body.chat-open` | CSS class | 关闭 | DOM | 否 |
| `chatIntroBubble.hidden` | CSS class | 可见 | DOM | 否（一次性关闭） |
| `chatToggle.dataset.dragged` | boolean | false | DOM | 否 |
| `navbar.hide-on-scroll` | CSS class | 可见 | DOM | 否 |
| `body.topbar-hidden` | CSS class | 关闭 | DOM | 否 |
| `actionPanel.show` | CSS class | 隐藏 | DOM | 否 |
| `exportMenu.show` | CSS class | 隐藏 | DOM | 否 |
| `historyDropdown.show` | CSS class | 隐藏 | DOM | 否 |
| `processingOverlay.active` | CSS class | 隐藏 | DOM | 否 |
| `toast.show` + `toastTimer` | CSS class + timer | 隐藏 | DOM + JS | 否 |

### 3.3 模块间状态依赖关系

```
currentCompany ─────┬──► updateCompanyBanner()
                    ├──► renderOverview()     ──► KPI卡片 + 数据表 + 折线图
                    ├──► renderIndicators()   ──► 指标详解表
                    ├──► renderRisk()         ──► 亮点/风险卡片
                    ├──► renderPredict()      ──► 预测图表 + 洞察
                    ├──► updateAnalysisTimestamp()
                    └──► addToHistory()       ──► localStorage + 下拉列表

currentModule ────────► switchModule()        ──► 面板显隐 + Nav高亮

chatPanel.show ───────► body.chat-open        ──► Navbar宽度 + 主区域宽度
                    └──► chatWidget.hidden     ──► 浮动按钮显隐

navbar.hide-on-scroll ► body.topbar-hidden    ──► sticky offset 归零
```

---

## 四、数据实体提炼

### 4.1 公司 (Company)

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | string | 公司名称（如"贵州茅台"） |
| `code` | string | 股票代码（如"600519"） |
| `industry` | string | 所属行业（如"白酒"） |
| `summary` | string | 简要摘要（仅 search.html 的 companyDB 使用） |

### 4.2 财务数据集 (FinancialData)

| 字段 | 类型 | 说明 |
|------|------|------|
| `revenue` | `number[]` | 营业收入数组（5 年，单位：亿） |
| `profit` | `number[]` | 归母净利润数组（5 年，单位：亿） |
| `years` | `string[]` | 年份标签（如 `['2020','2021','2022','2023','2024']`） |
| `grossMargin` | `number[]` | 毛利率数组（%，5 年） |
| `netMargin` | `number[]` | 净利率数组（%，5 年） |
| `debtRatio` | `number[]` | 资产负债率数组（%，5 年） |
| `cashFlow` | `number[]` | 经营现金流数组（5 年，单位：亿） |
| `revenueStruct` | `{labels:string[], data:number[]}` | 营收结构（分业务） |

### 4.3 分析历史记录 (AnalysisHistoryItem)

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | string | 公司名称 |
| `code` | string | 股票代码 |
| `time` | string | 分析时间（locale 字符串） |

### 4.4 搜索传递载荷 (SearchPayload)

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | `'company'` / `'manualText'` / `'upload'` | 来源类型 |
| `name` | string? | 公司名称（type=company 时） |
| `code` | string? | 股票代码（type=company 时） |
| `text` | string? | 手动输入文本（type=manualText 时） |
| `fileName` | string? | 上传文件名（type=upload 时） |

### 4.5 财务指标 (OverviewMetric)

| 字段 | 类型 | 说明 |
|------|------|------|
| `key` | string | 指标键名（revenue/profit/grossMargin/debtRatio/cashFlow） |
| `name` | string | 中文名称 |
| `unit` | string | 单位（亿 / %） |
| `data` | `number[]` | 5 年数值数组 |
| `changeType` | `'percent'` / `'pp'` / `'ppInverse'` | 变化计算方式 |
| `industryAvg` | string | 行业均值描述 |
| `color` | string | 图表颜色 |
| `highlight` | boolean? | 是否高亮行 |

### 4.6 指标详解 (IndicatorDetail)

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | string | 指标名称 |
| `key` | string? | 关联 OverviewMetric 的 key |
| `val` | string | 当前值（格式化后） |
| `industryAvg` | string | 行业均值 |
| `vs` | string | 行业对比描述 |
| `rating` | string | 评价（优秀/健康/良好） |
| `changeText` | string? | 变化文本 |
| `changeGood` | boolean? | 变化是否为正向 |

### 4.7 风险/亮点项 (RiskHighlightItem)

| 字段 | 类型 | 说明 |
|------|------|------|
| `icon` | string | 图标 emoji |
| `title` | string | 标题 |
| `desc` | string | 描述文本 |
| `type` | `'positive'` / `'negative'` | 正面/负面 |

### 4.8 聊天消息 (ChatMessage)

| 字段 | 类型 | 说明 |
|------|------|------|
| `role` | `'user'` / `'assistant'` | 发送者角色 |
| `content` | string (HTML) | 消息内容 |
| `refs` | string? | 参考来源（仅助手消息） |

### 4.9 导出格式 (ExportFormat)

| 值 | 格式 | 生成方式 |
|----|------|----------|
| `'image'` | PNG | html2canvas → canvas.toBlob |
| `'pdf'` | PDF | html2canvas → jsPDF |
| `'word'` | .doc | HTML Blob |
| `'excel'` | .csv | 表格 → CSV 文本 Blob |

### 4.10 术语解释 (TermTips)

```typescript
Map<string, string>  // 指标名称 → 白话解释文案
```

---

## 五、架构特征总结

| 维度 | 特征 |
|------|------|
| **页面数量** | 2 个（搜索入口 + 分析主控台） |
| **功能模块** | 11 个（含子模块共 20+） |
| **状态依赖** | 16 个全局/持久化状态变量 |
| **数据实体** | 10 个核心实体 |
| **数据持久化** | `localStorage`（分析历史）+ `sessionStorage`（页面传参） |
| **图表库** | Chart.js 4.x（折线图） |
| **导出库** | html2canvas + jsPDF |
| **数据来源** | 前端硬编码模拟数据（`getFinancialData()`），未来可替换为 API |
| **AI 模拟** | 关键词匹配规则（聊天回复），标注为 RAG 检索增强形态 |
