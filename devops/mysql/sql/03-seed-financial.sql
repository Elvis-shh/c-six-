-- ============================================================
-- 03-seed-financial.sql — 财报示例数据 + 指标定义 + 行业均值 + 规则
-- ============================================================
USE smartreport;
SET NAMES utf8mb4;

-- ============================================================
-- 指标定义
-- ============================================================
INSERT IGNORE INTO indicator_definitions (`key`, name, unit, is_percentage, category, term_explanation, sort_order) VALUES
('revenue', '营业总收入', '亿', 0, 'revenue', '公司在一个会计年度内通过销售商品或提供服务获得的总收入，是衡量公司经营规模的最基础指标。', 1),
('profit', '归母净利润', '亿', 0, 'profit', '归属于母公司股东的净利润，是"真正"落到股东口袋里的钱。这是投资者最关心的盈利指标。', 2),
('grossMargin', '毛利率', '%', 1, 'ratio', '毛利率 =（营业收入 - 营业成本）/ 营业收入 × 100%，反映公司产品或服务的基本盈利能力。毛利率越高，说明竞争力越强。', 3),
('netMargin', '净利率', '%', 1, 'ratio', '净利率 = 净利润 / 营业收入 × 100%，体现了公司最终的赚钱效率。净利率越高，整体经营效率越好。', 4),
('debtRatio', '资产负债率', '%', 1, 'debt', '资产负债率 = 总负债 / 总资产 × 100%，衡量财务杠杆水平。过低浪费扩张机会，过高则财务风险大。40%-60% 为合理区间。', 5),
('cashFlow', '经营现金流', '亿', 0, 'cashflow', '经营活动现金流净额，反映公司主业实际收到和付出的现金。现金流持续大于净利润说明利润含金量高。', 6),
('roe', '净资产收益率(ROE)', '%', 1, 'ratio', 'ROE = 净利润 / 净资产 × 100%，衡量股东投入资本的回报效率。巴菲特最看重的指标，>15% 即为优秀。', 7),
('totalAssets', '总资产', '亿', 0, 'debt', '公司控制的所有经济资源的总和，包括流动资产（现金、存货）和非流动资产（厂房、设备）。', 8),
('totalLiabilities', '总负债', '亿', 0, 'debt', '公司需偿还的全部债务，包括流动负债（1年内到期）和长期负债。', 9),
('equity', '股东权益', '亿', 0, 'debt', '总资产减去总负债后的净资产，代表真正属于股东的部分。', 10);

-- ============================================================
-- 行业均值数据
-- ============================================================
INSERT IGNORE INTO industry_averages (industry, indicator_key, year, avg_value, median_value) VALUES
-- 白酒 2024
('白酒', 'grossMargin', 2024, 68.5, 65.2),
('白酒', 'netMargin', 2024, 25.1, 22.0),
('白酒', 'debtRatio', 2024, 28.3, 30.1),
('白酒', 'roe', 2024, 18.5, 16.3),
-- 银行 2024
('银行', 'grossMargin', 2024, 45.0, 43.0),
('银行', 'netMargin', 2024, 30.5, 28.0),
('银行', 'debtRatio', 2024, 92.0, 92.5),
('银行', 'roe', 2024, 11.0, 10.5),
-- 新能源 2024
('新能源', 'grossMargin', 2024, 25.0, 22.0),
('新能源', 'netMargin', 2024, 10.5, 8.5),
('新能源', 'debtRatio', 2024, 55.0, 58.0),
('新能源', 'roe', 2024, 14.0, 12.0),
-- 医药 2024
('医药', 'grossMargin', 2024, 58.0, 55.0),
('医药', 'netMargin', 2024, 15.0, 13.0),
('医药', 'debtRatio', 2024, 35.0, 38.0),
('医药', 'roe', 2024, 12.5, 11.0),
-- 家电 2024
('家电', 'grossMargin', 2024, 28.0, 26.0),
('家电', 'netMargin', 2024, 8.5, 7.5),
('家电', 'debtRatio', 2024, 60.0, 62.0),
('家电', 'roe', 2024, 18.0, 16.0);

-- ============================================================
-- 亮点规则
-- ============================================================
INSERT IGNORE INTO highlight_rules (rule_key, indicator_key, condition_expr, title, desc_template, icon, priority, enabled) VALUES
('brand_moat', 'grossMargin', 'grossMargin > 80', '品牌护城河深厚',
 '作为{industry}龙头企业，{company}品牌价值无可替代，毛利率持续保持在 {grossMargin}% 以上，远超行业均值 {industryAvgGrossMargin}%。',
 '🏆', 1, 1),
('profitability', 'netMargin', 'netMargin > 40', '盈利能力卓越',
 '净利率高达 {netMargin}%，远高于行业均值 {industryAvgNetMargin}%。每收入 100 元可赚取近 {netMargin} 元净利润，赚钱效率行业领先。',
 '📊', 2, 1),
('profit_gold', 'cashFlow', 'cashFlow > profit * 0.8', '利润含金量高',
 '经营现金流 {cashFlow} 亿达到净利润 {profit} 亿的 {ratio}%，公司赚的是"真金白银"，利润质量极高。',
 '💰', 3, 1),
('low_debt', 'debtRatio', 'debtRatio < 30', '财务结构安全',
 '资产负债率仅 {debtRatio}%，远低于行业均值 {industryAvgDebtRatio}%，财务杠杆极低，几乎没有偿债压力。',
 '🛡️', 4, 1),
('high_roe', 'roe', 'roe > 20', '股东回报优异',
 'ROE 达到 {roe}%，远高于行业均值 {industryAvgRoe}%，为股东创造了优异的价值回报。',
 '📈', 5, 1),
('revenue_growth', 'revenue', 'revenue_yoy > 20', '营收高速增长',
 '营收同比增速高达 {revenue_yoy}%，远超行业平均水平，显示出强劲的增长动能。',
 '🚀', 6, 1);

-- ============================================================
-- 风险规则
-- ============================================================
INSERT IGNORE INTO risk_rules (rule_key, indicator_key, condition_expr, title, desc_template, icon, priority, enabled) VALUES
('profit_slowdown', 'profit', 'profit_yoy < 20 AND profit_yoy_prev < 25', '利润增速放缓',
 '近两年归母净利润增速从 {profit_yoy_prev}% 回落至 {profit_yoy}% 左右，需关注未来增长的可持续性。',
 '📉', 1, 1),
('policy_risk', NULL, 'industry IN (白酒,房地产,教培)', '政策监管风险',
 '{industry}行业面临政策调整的不确定性，可能影响公司经营环境和利润率。',
 '🏛️', 2, 1),
('high_debt', 'debtRatio', 'debtRatio > 70', '高负债风险',
 '资产负债率高达 {debtRatio}%，远超行业均值 {industryAvgDebtRatio}%，财务杠杆过高，抗风险能力弱。',
 '🔴', 3, 1),
('cashflow_weak', 'cashFlow', 'cashFlow < profit * 0.5', '现金流薄弱',
 '经营现金流 {cashFlow} 亿仅为净利润 {profit} 亿的 {ratio}%，利润回收质量偏低，需关注应收账款风险。',
 '💧', 4, 1),
('revenue_decline', 'revenue', 'revenue_yoy < 0', '营收下滑',
 '营收同比下降 {abs_revenue_yoy}%，公司主业可能面临增长瓶颈或市场萎缩。',
 '⬇️', 5, 1);

-- ============================================================
-- 财报数据（贵州茅台 600519 2020-2024）
-- ============================================================
INSERT IGNORE INTO financial_reports (id, company_code, report_type, report_year, source, status, published_at) VALUES
(1, '600519', 'annual', 2020, 'system', 1, '2021-03-31'),
(2, '600519', 'annual', 2021, 'system', 1, '2022-03-31'),
(3, '600519', 'annual', 2022, 'system', 1, '2023-03-31'),
(4, '600519', 'annual', 2023, 'system', 1, '2024-04-02'),
(5, '600519', 'annual', 2024, 'system', 1, '2025-04-02');

-- 贵州茅台指标数值
INSERT IGNORE INTO financial_indicators (report_id, indicator_key, value, yoy_change, rating) VALUES
-- 2020
(1, 'revenue', 979.93, 10.3, 'excellent'),
(1, 'profit', 466.97, 13.3, 'excellent'),
(1, 'grossMargin', 91.3, NULL, 'excellent'),
(1, 'netMargin', 47.6, NULL, 'excellent'),
(1, 'debtRatio', 21.4, NULL, 'healthy'),
(1, 'cashFlow', 516.69, 14.2, 'excellent'),
(1, 'roe', 28.1, NULL, 'excellent'),
(1, 'totalAssets', 2134.5, NULL, NULL),
(1, 'totalLiabilities', 456.8, NULL, NULL),
(1, 'equity', 1677.7, NULL, NULL),
-- 2021
(2, 'revenue', 1094.64, 11.7, 'excellent'),
(2, 'profit', 524.60, 12.3, 'excellent'),
(2, 'grossMargin', 91.8, NULL, 'excellent'),
(2, 'netMargin', 47.9, NULL, 'excellent'),
(2, 'debtRatio', 19.8, NULL, 'healthy'),
(2, 'cashFlow', 609.34, 17.9, 'excellent'),
(2, 'roe', 27.9, NULL, 'excellent'),
(2, 'totalAssets', 2560.3, NULL, NULL),
(2, 'totalLiabilities', 506.9, NULL, NULL),
(2, 'equity', 2053.4, NULL, NULL),
-- 2022
(3, 'revenue', 1275.54, 16.5, 'excellent'),
(3, 'profit', 627.16, 19.6, 'excellent'),
(3, 'grossMargin', 92.1, NULL, 'excellent'),
(3, 'netMargin', 49.2, NULL, 'excellent'),
(3, 'debtRatio', 18.1, NULL, 'healthy'),
(3, 'cashFlow', 678.90, 11.4, 'excellent'),
(3, 'roe', 28.5, NULL, 'excellent'),
(3, 'totalAssets', 2885.6, NULL, NULL),
(3, 'totalLiabilities', 522.3, NULL, NULL),
(3, 'equity', 2363.3, NULL, NULL),
-- 2023
(4, 'revenue', 1505.60, 18.0, 'excellent'),
(4, 'profit', 747.34, 19.2, 'excellent'),
(4, 'grossMargin', 92.5, NULL, 'excellent'),
(4, 'netMargin', 49.6, NULL, 'excellent'),
(4, 'debtRatio', 17.2, NULL, 'healthy'),
(4, 'cashFlow', 721.80, 6.3, 'excellent'),
(4, 'roe', 29.2, NULL, 'excellent'),
(4, 'totalAssets', 3200.8, NULL, NULL),
(4, 'totalLiabilities', 550.5, NULL, NULL),
(4, 'equity', 2650.3, NULL, NULL),
-- 2024
(5, 'revenue', 1748.00, 16.1, 'excellent'),
(5, 'profit', 862.00, 15.4, 'excellent'),
(5, 'grossMargin', 92.8, NULL, 'excellent'),
(5, 'netMargin', 49.3, NULL, 'excellent'),
(5, 'debtRatio', 16.1, NULL, 'healthy'),
(5, 'cashFlow', 810.00, 12.2, 'excellent'),
(5, 'roe', 30.1, NULL, 'excellent'),
(5, 'totalAssets', 3560.0, NULL, NULL),
(5, 'totalLiabilities', 573.2, NULL, NULL),
(5, 'equity', 2986.8, NULL, NULL);

-- ============================================================
-- 五粮液 000858 2024 简化数据
-- ============================================================
INSERT IGNORE INTO financial_reports (id, company_code, report_type, report_year, source, status, published_at) VALUES
(6, '000858', 'annual', 2024, 'system', 1, '2025-04-28');

INSERT IGNORE INTO financial_indicators (report_id, indicator_key, value, yoy_change, rating) VALUES
(6, 'revenue', 832.72, 12.0, 'excellent'),
(6, 'profit', 316.90, 10.5, 'excellent'),
(6, 'grossMargin', 75.2, NULL, 'excellent'),
(6, 'netMargin', 38.1, NULL, 'excellent'),
(6, 'debtRatio', 22.5, NULL, 'healthy'),
(6, 'cashFlow', 290.00, 8.3, 'excellent'),
(6, 'roe', 22.8, NULL, 'excellent');

-- ============================================================
-- 宁德时代 300750 2024 简化数据
-- ============================================================
INSERT IGNORE INTO financial_reports (id, company_code, report_type, report_year, source, status, published_at) VALUES
(7, '300750', 'annual', 2024, 'system', 1, '2025-03-15');

INSERT IGNORE INTO financial_indicators (report_id, indicator_key, value, yoy_change, rating) VALUES
(7, 'revenue', 4009.00, -4.5, 'good'),
(7, 'profit', 441.21, 15.0, 'excellent'),
(7, 'grossMargin', 24.5, NULL, 'good'),
(7, 'netMargin', 11.0, NULL, 'good'),
(7, 'debtRatio', 68.5, NULL, 'warning'),
(7, 'cashFlow', 928.00, 22.3, 'excellent'),
(7, 'roe', 22.0, NULL, 'excellent');

-- ============================================================
-- 工商银行 601398 2024 简化数据
-- ============================================================
INSERT IGNORE INTO financial_reports (id, company_code, report_type, report_year, source, status, published_at) VALUES
(8, '601398', 'annual', 2024, 'system', 1, '2025-03-28');

INSERT IGNORE INTO financial_indicators (report_id, indicator_key, value, yoy_change, rating) VALUES
(8, 'revenue', 8200.00, -2.8, 'good'),
(8, 'profit', 3650.00, 1.2, 'healthy'),
(8, 'grossMargin', 42.0, NULL, 'good'),
(8, 'netMargin', 44.5, NULL, 'excellent'),
(8, 'debtRatio', 91.5, NULL, 'warning'),
(8, 'cashFlow', 5500.00, 5.8, 'good'),
(8, 'roe', 10.8, NULL, 'healthy');

-- ============================================================
-- 海康威视 002415 2020-2024 简化数据
-- ============================================================
INSERT IGNORE INTO financial_reports (id, company_code, report_type, report_year, source, status, published_at) VALUES
(9, '002415', 'annual', 2020, 'system', 1, '2021-04-17'),
(10, '002415', 'annual', 2021, 'system', 1, '2022-04-16'),
(11, '002415', 'annual', 2022, 'system', 1, '2023-04-15'),
(12, '002415', 'annual', 2023, 'system', 1, '2024-04-20'),
(13, '002415', 'annual', 2024, 'system', 1, '2025-04-19');

INSERT IGNORE INTO financial_indicators (report_id, indicator_key, value, yoy_change, rating) VALUES
-- 2020
(9, 'revenue', 635.03, 10.1, 'good'),
(9, 'profit', 133.86, 7.8, 'good'),
(9, 'grossMargin', 46.5, NULL, 'good'),
(9, 'netMargin', 21.1, NULL, 'good'),
(9, 'debtRatio', 38.2, NULL, 'healthy'),
(9, 'cashFlow', 160.88, 12.4, 'excellent'),
(9, 'roe', 27.3, NULL, 'excellent'),
-- 2021
(10, 'revenue', 814.20, 28.2, 'excellent'),
(10, 'profit', 168.00, 25.5, 'excellent'),
(10, 'grossMargin', 44.3, NULL, 'good'),
(10, 'netMargin', 20.6, NULL, 'good'),
(10, 'debtRatio', 39.5, NULL, 'healthy'),
(10, 'cashFlow', 184.22, 14.5, 'excellent'),
(10, 'roe', 28.8, NULL, 'excellent'),
-- 2022
(11, 'revenue', 831.66, 2.1, 'good'),
(11, 'profit', 128.37, -23.6, 'warning'),
(11, 'grossMargin', 42.3, NULL, 'good'),
(11, 'netMargin', 15.4, NULL, 'healthy'),
(11, 'debtRatio', 41.2, NULL, 'healthy'),
(11, 'cashFlow', 118.00, -35.9, 'warning'),
(11, 'roe', 19.2, NULL, 'good'),
-- 2023
(12, 'revenue', 893.40, 7.4, 'good'),
(12, 'profit', 141.08, 9.9, 'good'),
(12, 'grossMargin', 44.0, NULL, 'good'),
(12, 'netMargin', 15.8, NULL, 'healthy'),
(12, 'debtRatio', 40.5, NULL, 'healthy'),
(12, 'cashFlow', 155.20, 31.5, 'excellent'),
(12, 'roe', 20.1, NULL, 'excellent'),
-- 2024
(13, 'revenue', 924.50, 3.5, 'good'),
(13, 'profit', 151.20, 7.2, 'good'),
(13, 'grossMargin', 44.8, NULL, 'good'),
(13, 'netMargin', 16.4, NULL, 'healthy'),
(13, 'debtRatio', 39.8, NULL, 'healthy'),
(13, 'cashFlow', 168.00, 8.2, 'excellent'),
(13, 'roe', 20.8, NULL, 'excellent');
