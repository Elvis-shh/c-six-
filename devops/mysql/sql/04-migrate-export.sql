-- Migration: recreate export_records table for Epic 8
DROP TABLE IF EXISTS export_records;

CREATE TABLE export_records (
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
