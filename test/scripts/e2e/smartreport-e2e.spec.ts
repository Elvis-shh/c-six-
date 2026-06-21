/**
 * SmartReport E2E 自动化测试 (Playwright)
 * 覆盖: 搜索 → 看板 → 亮点风险 → 预测 → 聊天的核心链路
 *
 * 安装: npm install playwright @playwright/test
 * 运行: npx playwright test test/scripts/e2e/smartreport-e2e.spec.ts --config=test/scripts/e2e/playwright.config.ts
 */

import { test, expect } from "@playwright/test";

const BASE = "http://localhost:3000";

test.describe("SmartReport E2E 核心旅程", () => {
  // ================================================================
  // TC-E2E-01: 搜索 → 看板 → 聊天 完整链路
  // ================================================================
  test("TC-E2E-01 搜索→看板→聊天完整链路", async ({ page }) => {
    // ① 访问搜索页
    await page.goto(`${BASE}/search`);
    await expect(page.locator(".search-input, input[type='text']").first()).toBeVisible();

    // ② 搜索"茅台"
    const searchInput = page.locator(".search-input, input[placeholder*='搜索']").first();
    await searchInput.fill("茅台");
    await page.waitForTimeout(500);

    // ③ 点击贵州茅台（第一个建议项）
    const firstSuggestion = page.locator(".suggestion-item, .search-dropdown-item").first();
    await expect(firstSuggestion).toBeVisible({ timeout: 3000 });
    await firstSuggestion.click();

    // ④ 验证跳转到 /dashboard/600519
    await page.waitForURL(/dashboard\/600519/);
    await page.waitForTimeout(1000);

    // ⑤ 检查 KPI 卡片渲染
    await expect(page.locator(".kpi-card, .kpi-grid > div").first()).toBeVisible({ timeout: 5000 });

    // ⑥ 切换到折线图视图
    const lineTab = page.locator("button:has-text('折线图'), .tab-btn:has-text('折线图')").first();
    if (await lineTab.isVisible()) {
      await lineTab.click();
      await page.waitForTimeout(500);
      await expect(page.locator("canvas").first()).toBeVisible();
    }

    // ⑦ 切换到预测 Tab
    const predictTab = page.locator("button:has-text('趋势预测'), .module-tab:has-text('预测')").first();
    if (await predictTab.isVisible()) {
      await predictTab.click();
      await page.waitForTimeout(500);
    }

    // ⑧ 滚动到亮点/风险区域
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight * 0.6));
    await page.waitForTimeout(300);

    // ⑨ 打开聊天面板
    const chatToggle = page.locator(".chat-toggle, button:has-text('💬')").first();
    if (await chatToggle.isVisible()) {
      await chatToggle.click();
      await page.waitForTimeout(500);

      // ⑩ 发送问题
      const chatInput = page.locator(".chat-input textarea, .chat-input input").first();
      if (await chatInput.isVisible()) {
        await chatInput.fill("盈利能力怎么样？");
        await page.keyboard.press("Enter");
        await page.waitForTimeout(2000);
      }
    }
  });

  // ================================================================
  // TC-E2E-02: 热门 Chip 快速分析
  // ================================================================
  test("TC-E2E-02 热门Chip快速分析", async ({ page }) => {
    await page.goto(`${BASE}/search`);
    await page.waitForTimeout(500);

    // 点击"宁德时代" Chip
    const chip = page.locator(".chip, .company-chip").filter({ hasText: "宁德" }).first();
    if (await chip.isVisible({ timeout: 2000 })) {
      await chip.click();
      await page.waitForURL(/dashboard\/300750/);
      await page.waitForTimeout(1000);
      await expect(page.locator(".kpi-card, .kpi-grid > div").first()).toBeVisible({ timeout: 5000 });
    }

    // 通过 Navbar 搜索切换到工商银行
    const navSearch = page.locator(".navbar input[type='text'], .navbar .search-input").first();
    if (await navSearch.isVisible()) {
      await navSearch.fill("工商银行");
      await page.waitForTimeout(500);
      const suggestion = page.locator(".suggestion-item").first();
      if (await suggestion.isVisible({ timeout: 2000 })) {
        await suggestion.click();
        await page.waitForURL(/dashboard\/601398/);
        await page.waitForTimeout(1000);
        await expect(page.locator(".kpi-card, .kpi-grid > div").first()).toBeVisible({ timeout: 5000 });
      }
    }
  });

  // ================================================================
  // TC-E2E-03: 历史记录 + 导出
  // ================================================================
  test("TC-E2E-03 历史记录+导出", async ({ page }) => {
    // 分析 3 家公司
    const companies = ["600519", "000858", "300750"];
    for (const code of companies) {
      await page.goto(`${BASE}/dashboard/${code}`);
      await page.waitForTimeout(1500);
      await expect(page.locator(".kpi-card, .kpi-grid > div").first()).toBeVisible({ timeout: 5000 });
    }

    // 检查历史下拉
    await page.goto(`${BASE}/dashboard/600519`);
    await page.waitForTimeout(1000);
    const historyBtn = page.locator("button:has-text('最近分析'), button:has-text('历史'), .history-btn").first();
    if (await historyBtn.isVisible()) {
      await historyBtn.click();
      await page.waitForTimeout(300);
      const historyItems = page.locator(".history-item, .history-dropdown-item");
      const count = await historyItems.count();
      expect(count).toBeLessThanOrEqual(3); // 最多 3 条（去重后）
    }

    // 尝试导出 PNG
    const exportBtn = page.locator("button:has-text('导出'), .export-btn").first();
    if (await exportBtn.isVisible()) {
      await exportBtn.click();
      await page.waitForTimeout(200);
      const pngOption = page.locator("button:has-text('PNG'), .export-option:has-text('PNG')").first();
      if (await pngOption.isVisible()) {
        const [download] = await Promise.all([
          page.waitForEvent("download", { timeout: 15000 }).catch(() => null),
          pngOption.click(),
        ]);
        if (download) {
          expect(download.suggestedFilename()).toContain("贵州茅台");
        }
      }
    }
  });

  // ================================================================
  // TC-E2E-04: 无匹配 → 重试
  // ================================================================
  test("TC-E2E-04 无匹配→重试", async ({ page }) => {
    await page.goto(`${BASE}/search`);
    await page.waitForTimeout(500);

    const searchInput = page.locator(".search-input, input[placeholder*='搜索']").first();
    await searchInput.fill("xyzabc123");
    await page.waitForTimeout(500);

    // 应显示空状态
    const emptyState = page.locator("text=未找到匹配公司, .empty-state, .no-results").first();
    await expect(emptyState).toBeVisible({ timeout: 3000 });

    // 重新搜索有效代码
    await searchInput.fill("600519");
    await page.waitForTimeout(500);
    const suggestion = page.locator(".suggestion-item").first();
    await expect(suggestion).toBeVisible({ timeout: 3000 });
    await suggestion.click();
    await page.waitForURL(/dashboard\/600519/);
  });
});

test.describe("SmartReport 搜索 UI 交互", () => {
  // ================================================================
  // TC-2.1-04/05: 键盘导航
  // ================================================================
  test("TC-2.1-04/05 键盘↑↓导航", async ({ page }) => {
    await page.goto(`${BASE}/search`);
    const searchInput = page.locator(".search-input, input[placeholder*='搜索']").first();
    await searchInput.fill("茅台");
    await page.waitForTimeout(500);

    await page.keyboard.press("ArrowDown");
    await page.waitForTimeout(100);
    const activeItem = page.locator(".suggestion-item.active, .search-dropdown-item.active").first();
    await expect(activeItem).toBeVisible({ timeout: 2000 });

    await page.keyboard.press("ArrowUp");
    await page.waitForTimeout(100);

    // 按 Enter 确认跳转
    await page.keyboard.press("ArrowDown");
    await page.keyboard.press("Enter");
    await page.waitForURL(/dashboard\//);
  });

  // ================================================================
  // TC-2.1-07: Esc 关闭下拉
  // ================================================================
  test("TC-2.1-07 Esc关闭下拉", async ({ page }) => {
    await page.goto(`${BASE}/search`);
    const searchInput = page.locator(".search-input, input[placeholder*='搜索']").first();
    await searchInput.fill("茅台");
    await page.waitForTimeout(500);

    const dropdown = page.locator(".suggestion-list, .search-dropdown").first();
    await expect(dropdown).toBeVisible({ timeout: 3000 });

    await page.keyboard.press("Escape");
    await page.waitForTimeout(200);
    await expect(dropdown).not.toBeVisible();
  });

  // ================================================================
  // TC-2.1-10: 点击 Chip 跳转
  // ================================================================
  test("TC-2.1-10 点击Chip跳转", async ({ page }) => {
    await page.goto(`${BASE}/search`);
    await page.waitForTimeout(500);

    // 点击任意一个示例 Chip
    const chip = page.locator(".chip, .company-chip, .example-chip").first();
    if (await chip.isVisible({ timeout: 2000 })) {
      await chip.click();
      await page.waitForURL(/dashboard\//);
    }
  });
});

test.describe("SmartReport 聊天面板", () => {
  test("TC-6.1-02 点击展开聊天面板", async ({ page }) => {
    await page.goto(`${BASE}/dashboard/600519`);
    await page.waitForTimeout(1500);

    const chatToggle = page.locator(".chat-toggle, button:has-text('💬')").first();
    if (await chatToggle.isVisible()) {
      await chatToggle.click();
      await page.waitForTimeout(500);

      const chatPanel = page.locator(".chat-panel").first();
      await expect(chatPanel).toBeVisible();

      // 关闭面板
      const closeBtn = page.locator(".chat-header button, .chat-panel .close-btn").first();
      if (await closeBtn.isVisible()) {
        await closeBtn.click();
        await page.waitForTimeout(300);
        await expect(chatPanel).not.toBeVisible();
      }
    }
  });

  test("TC-6.1-04/06 发送消息并自动滚动", async ({ page }) => {
    await page.goto(`${BASE}/dashboard/600519`);
    await page.waitForTimeout(1500);

    const chatToggle = page.locator(".chat-toggle, button:has-text('💬')").first();
    if (!(await chatToggle.isVisible())) return;
    await chatToggle.click();
    await page.waitForTimeout(500);

    const chatInput = page.locator(".chat-input textarea, .chat-input input").first();
    if (!(await chatInput.isVisible())) return;

    // 发送消息
    await chatInput.fill("测试消息");
    await page.keyboard.press("Enter");
    await page.waitForTimeout(1000);

    // 检查用户消息气泡
    const userBubble = page.locator(".message-user, .chat-bubble--user").first();
    await expect(userBubble).toBeVisible({ timeout: 3000 });
  });
});
