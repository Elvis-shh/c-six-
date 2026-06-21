CREATE TABLE IF NOT EXISTS report_quote_chunks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    company_code VARCHAR(10) NOT NULL,
    report_year INT NOT NULL,
    page_no INT NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rqc_report (report_id),
    INDEX idx_rqc_company_year (company_code, report_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS report_crawl_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    company_code VARCHAR(10),
    payload TEXT,
    result TEXT,
    error_msg VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
