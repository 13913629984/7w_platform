const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
  });
  const context = await browser.newContext({
    viewport: { width: 375, height: 812 },
    userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1'
  });
  const page = await context.newPage();

  // Screenshot 1: customers
  await page.goto('http://localhost:5173/#/pages/crm/customers', { waitUntil: 'networkidle' });
  await page.waitForTimeout(5000);
  await page.screenshot({ path: 'd:/7_info/7w_platform/mobile/screenshot_customers.png', fullPage: false });
  console.log('Screenshot customers saved.');

  // Screenshot 2: dashboard
  await page.goto('http://localhost:5173/#/pages/crm/dashboard', { waitUntil: 'networkidle' });
  await page.waitForTimeout(5000);
  await page.screenshot({ path: 'd:/7_info/7w_platform/mobile/screenshot_dashboard.png', fullPage: false });
  console.log('Screenshot dashboard saved.');

  await browser.close();
})();
