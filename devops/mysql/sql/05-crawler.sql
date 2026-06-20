USE smartreport;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS report_crawl_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_code VARCHAR(10) NOT NULL COMMENT '股票代码',
    report_year INT NOT NULL COMMENT '报告年份',
    report_type VARCHAR(30) NOT NULL COMMENT 'annual/quarter1/semi_annual/quarter3',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/completed/failed/skipped',
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
