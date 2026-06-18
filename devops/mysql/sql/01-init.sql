-- ============================================================
-- 01-init.sql — SmartReport 数据库初始化 (18 张表)
-- 基于 docs/tech-design.md ER 图设计
-- ============================================================

CREATE DATABASE IF NOT EXISTS smartreport
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

SET NAMES utf8mb4;

USE smartreport;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE COMMENT '邮箱（登录账号）',
    password_hash VARCHAR(255) NOT NULL COMMENT 'bcrypt 加密密码',
    nickname VARCHAR(100) COMMENT '昵称',
    avatar_url VARCHAR(500) COMMENT '头像URL（MinIO）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1正常 2未验证',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 2. 公司表
-- ============================================================
CREATE TABLE IF NOT EXISTS companies (
    code VARCHAR(10) NOT NULL PRIMARY KEY COMMENT '股票代码 如600519',
    name VARCHAR(100) NOT NULL COMMENT '公司名称',
    short_name VARCHAR(50) COMMENT '简称',
    industry VARCHAR(100) COMMENT '证监会行业分类',
    market VARCHAR(5) COMMENT 'SH/SZ/BJ',
    listing_date DATE COMMENT '上市日期',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0退市 1正常',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_companies_name (name),
    INDEX idx_companies_industry (industry),
    INDEX idx_companies_market (market)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司表';

-- ============================================================
-- 3. 公司行业标签
-- ============================================================
CREATE TABLE IF NOT EXISTS company_industry_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_code VARCHAR(10) NOT NULL COMMENT '股票代码',
    tag VARCHAR(50) NOT NULL COMMENT '标签 如白酒、消费、龙头',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tags_company (company_code),
    INDEX idx_tags_tag (tag),
    CONSTRAINT fk_tags_company FOREIGN KEY (company_code) REFERENCES companies(code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司行业标签';

-- ============================================================
-- 4. 财报主表
-- ============================================================
CREATE TABLE IF NOT EXISTS financial_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_code VARCHAR(10) NOT NULL COMMENT '股票代码',
    report_type VARCHAR(20) NOT NULL COMMENT 'annual/quarter1/quarter2/quarter3/semi_annual',
    report_year INT NOT NULL COMMENT '财报年份',
    source VARCHAR(20) NOT NULL DEFAULT 'system' COMMENT 'system/upload/manual',
    source_file_url VARCHAR(500) COMMENT 'MinIO 原始文件路径',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0解析中 1已解析 2解析失败',
    published_at DATE COMMENT '财报发布日期',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_reports_company_year (company_code, report_year),
    INDEX idx_reports_type_year (report_type, report_year),
    CONSTRAINT fk_reports_company FOREIGN KEY (company_code) REFERENCES companies(code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财报主表';

-- ============================================================
-- 5. 财报原始文件
-- ============================================================
CREATE TABLE IF NOT EXISTS report_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL COMMENT '关联财报',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_url VARCHAR(500) NOT NULL COMMENT 'MinIO 存储路径',
    file_type VARCHAR(20) NOT NULL COMMENT 'pdf/docx/xlsx/jpg',
    file_size BIGINT NOT NULL COMMENT '字节',
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_files_report (report_id),
    CONSTRAINT fk_files_report FOREIGN KEY (report_id) REFERENCES financial_reports(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财报原始文件';

-- ============================================================
-- 6. 财务指标定义
-- ============================================================
CREATE TABLE IF NOT EXISTS indicator_definitions (
    `key` VARCHAR(50) NOT NULL PRIMARY KEY COMMENT '如 revenue, profit',
    name VARCHAR(100) NOT NULL COMMENT '中文名：营业收入',
    unit VARCHAR(20) COMMENT '亿/%',
    is_percentage TINYINT NOT NULL DEFAULT 0 COMMENT '0数值 1百分比',
    category VARCHAR(50) COMMENT 'revenue/profit/debt/cashflow/ratio',
    term_explanation TEXT COMMENT '白话解释文案',
    sort_order INT DEFAULT 0 COMMENT '展示排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财务指标定义';

-- ============================================================
-- 7. 财务指标数值
-- ============================================================
CREATE TABLE IF NOT EXISTS financial_indicators (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL COMMENT '关联财报',
    indicator_key VARCHAR(50) NOT NULL COMMENT '对应 indicator_definitions.key',
    value DECIMAL(20,4) COMMENT '指标值',
    yoy_change DECIMAL(10,4) COMMENT '同比变化率',
    qoq_change DECIMAL(10,4) COMMENT '环比变化率',
    industry_avg VARCHAR(50) COMMENT '行业均值',
    industry_rank VARCHAR(50) COMMENT '行业排名 如 top10%',
    rating VARCHAR(20) COMMENT '评价：excellent/good/healthy/warning',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_indicators_report (report_id),
    INDEX idx_indicators_key (indicator_key),
    INDEX idx_indicators_report_key (report_id, indicator_key),
    CONSTRAINT fk_indicators_report FOREIGN KEY (report_id) REFERENCES financial_reports(id) ON DELETE CASCADE,
    CONSTRAINT fk_indicators_def FOREIGN KEY (indicator_key) REFERENCES indicator_definitions(`key`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财务指标数值';

-- ============================================================
-- 8. 行业均值
-- ============================================================
CREATE TABLE IF NOT EXISTS industry_averages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    industry VARCHAR(100) NOT NULL COMMENT '行业分类',
    indicator_key VARCHAR(50) NOT NULL COMMENT '指标key',
    year INT NOT NULL COMMENT '年份',
    avg_value DECIMAL(20,4) COMMENT '行业均值',
    median_value DECIMAL(20,4) COMMENT '行业中位数',
    INDEX idx_avg_industry_year (industry, year),
    INDEX idx_avg_indicator (indicator_key),
    CONSTRAINT fk_avg_indicator FOREIGN KEY (indicator_key) REFERENCES indicator_definitions(`key`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行业均值';

-- ============================================================
-- 9. 亮点规则
-- ============================================================
CREATE TABLE IF NOT EXISTS highlight_rules (
    rule_key VARCHAR(50) NOT NULL PRIMARY KEY COMMENT '如 profit_gold_content',
    indicator_key VARCHAR(50) COMMENT '关联指标',
    condition_expr VARCHAR(500) COMMENT '如 value > 80 AND yoy_change > 0',
    title VARCHAR(100) COMMENT '亮点标题模板',
    desc_template TEXT COMMENT '描述模板 {value} {indicator_name}',
    icon VARCHAR(10) DEFAULT '✨' COMMENT 'emoji图标',
    priority TINYINT DEFAULT 0 COMMENT '优先级 0最高',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='亮点规则';

-- ============================================================
-- 10. 风险规则
-- ============================================================
CREATE TABLE IF NOT EXISTS risk_rules (
    rule_key VARCHAR(50) NOT NULL PRIMARY KEY,
    indicator_key VARCHAR(50) COMMENT '关联指标',
    condition_expr VARCHAR(500),
    title VARCHAR(100),
    desc_template TEXT,
    icon VARCHAR(10) DEFAULT '⚠️' COMMENT 'emoji图标',
    priority TINYINT DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险规则';

-- ============================================================
-- 11. 分析报告
-- ============================================================
CREATE TABLE IF NOT EXISTS analysis_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '可为空（匿名用户）',
    company_code VARCHAR(10) NOT NULL COMMENT '股票代码',
    report_id BIGINT COMMENT '关联的财报',
    session_id VARCHAR(100) COMMENT '匿名会话ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0生成中 1已完成 2失败',
    summary_data JSON COMMENT '缓存的分析结果JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ar_company (company_code),
    INDEX idx_ar_user (user_id),
    INDEX idx_ar_session (session_id),
    CONSTRAINT fk_ar_company FOREIGN KEY (company_code) REFERENCES companies(code) ON DELETE CASCADE,
    CONSTRAINT fk_ar_report FOREIGN KEY (report_id) REFERENCES financial_reports(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析报告';

-- ============================================================
-- 12. 报告亮点
-- ============================================================
CREATE TABLE IF NOT EXISTS report_highlights (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    analysis_report_id BIGINT NOT NULL COMMENT '关联分析报告',
    rule_key VARCHAR(50) NOT NULL COMMENT '关联规则',
    icon VARCHAR(10) COMMENT 'emoji',
    title VARCHAR(200) COMMENT '亮点标题',
    description TEXT COMMENT '亮点描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rh_report (analysis_report_id),
    CONSTRAINT fk_rh_report FOREIGN KEY (analysis_report_id) REFERENCES analysis_reports(id) ON DELETE CASCADE,
    CONSTRAINT fk_rh_rule FOREIGN KEY (rule_key) REFERENCES highlight_rules(rule_key) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告亮点';

-- ============================================================
-- 13. 报告风险
-- ============================================================
CREATE TABLE IF NOT EXISTS report_risks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    analysis_report_id BIGINT NOT NULL,
    rule_key VARCHAR(50) NOT NULL,
    icon VARCHAR(10),
    title VARCHAR(200),
    description TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rr_report (analysis_report_id),
    CONSTRAINT fk_rr_report FOREIGN KEY (analysis_report_id) REFERENCES analysis_reports(id) ON DELETE CASCADE,
    CONSTRAINT fk_rr_rule FOREIGN KEY (rule_key) REFERENCES risk_rules(rule_key) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告风险';

-- ============================================================
-- 14. 聊天消息
-- ============================================================
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    analysis_report_id BIGINT NOT NULL COMMENT '关联分析会话',
    role VARCHAR(20) NOT NULL COMMENT 'user/assistant',
    content TEXT NOT NULL COMMENT '消息内容',
    rag_refs JSON COMMENT 'RAG检索引用 [ {source, snippet, score} ]',
    token_usage INT DEFAULT 0 COMMENT '消耗Token数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cm_report (analysis_report_id),
    CONSTRAINT fk_cm_report FOREIGN KEY (analysis_report_id) REFERENCES analysis_reports(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息';

-- ============================================================
-- 15. 用户搜索历史
-- ============================================================
CREATE TABLE IF NOT EXISTS user_search_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '可为空（匿名用户）',
    session_id VARCHAR(100) COMMENT '匿名会话ID',
    company_code VARCHAR(10) NOT NULL COMMENT '股票代码',
    searched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ush_user (user_id),
    INDEX idx_ush_session (session_id),
    INDEX idx_ush_company (company_code),
    CONSTRAINT fk_ush_company FOREIGN KEY (company_code) REFERENCES companies(code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户搜索历史';

-- ============================================================
-- 16. 用户收藏
-- ============================================================
CREATE TABLE IF NOT EXISTS user_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company_code VARCHAR(10) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uq_fav_user_company (user_id, company_code),
    CONSTRAINT fk_fav_company FOREIGN KEY (company_code) REFERENCES companies(code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏';

-- ============================================================
-- 17. 导出记录
-- ============================================================
CREATE TABLE IF NOT EXISTS export_records (
    task_id VARCHAR(64) PRIMARY KEY,
    company_code VARCHAR(20),
    format VARCHAR(10) NOT NULL COMMENT 'png/pdf/word/excel',
    status VARCHAR(50) DEFAULT 'pending' COMMENT 'pending/rendering/ready/failed',
    file_name VARCHAR(255) COMMENT '下载文件名',
    file_url VARCHAR(500) COMMENT '文件路径',
    file_size BIGINT,
    progress INT DEFAULT 0,
    error_msg VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    INDEX idx_er_status (status),
    INDEX idx_er_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导出记录';

-- ============================================================
-- 18. 消息队列任务记录
-- ============================================================
CREATE TABLE IF NOT EXISTS mq_task_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(100) NOT NULL UNIQUE COMMENT 'RocketMQ messageId',
    task_type VARCHAR(50) NOT NULL COMMENT 'ocr/ner/predict/chat/export',
    request_payload JSON COMMENT '请求参数',
    response_payload JSON COMMENT '响应结果',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/completed/failed',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    progress_msg VARCHAR(500) COMMENT '进度描述',
    progress_percent INT DEFAULT 0 COMMENT '进度百分比',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    INDEX idx_mq_status (status),
    INDEX idx_mq_type (task_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息队列任务记录';

-- ============================================================
-- 19. 财报爬取任务
-- ============================================================
CREATE TABLE IF NOT EXISTS report_crawl_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_code VARCHAR(10) NOT NULL COMMENT '股票代码',
    report_year INT NOT NULL COMMENT '报告年份',
    report_type VARCHAR(30) NOT NULL COMMENT 'annual/quarter1/semi_annual/quarter3',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/completed/failed',
    announcement_title VARCHAR(255),
    source_url VARCHAR(500),
    file_path VARCHAR(500),
    file_name VARCHAR(255),
    file_size BIGINT,
    published_at DATE,
    error_msg VARCHAR(1000),
    retry_count INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at DATETIME,
    UNIQUE KEY uq_crawl_company_year_type (company_code, report_year, report_type),
    INDEX idx_crawl_status (status),
    INDEX idx_crawl_company (company_code),
    CONSTRAINT fk_crawl_company FOREIGN KEY (company_code) REFERENCES companies(code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财报爬取任务';

-- ============================================================
-- 授予 smartreport 用户全部权限
-- ============================================================
GRANT ALL PRIVILEGES ON smartreport.* TO 'smartreport'@'%';
FLUSH PRIVILEGES;
