import { chromium } from '@playwright/test'
import { mkdir } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
// 把桌面首页、定制页和手机首页截图保存到项目 artifacts 目录。
const out = new URL('../../artifacts/', import.meta.url)
await mkdir(out, { recursive: true })
const browser = await chromium.launch({ channel: 'chrome', headless: true })
const page = await browser.newPage({ viewport: { width: 1440, height: 1080 } })
// 等待首页和报价真正显示后截图，减少加载状态带来的不稳定结果。
await page.goto(process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173/')
await page.getByRole('heading', { name: /为餐饮团队，/ }).waitFor()
await page.screenshot({ path: fileURLToPath(new URL('home-desktop.png', out)), fullPage: true })
await page.getByRole('button', { name: '开始制服定制' }).click()
await page.locator('.quote-bar strong').filter({ hasText: '¥' }).waitFor()
await page.screenshot({ path: fileURLToPath(new URL('studio-desktop.png', out)), fullPage: true })
// 切换到手机视口，记录响应式布局。
await page.setViewportSize({ width: 390, height: 844 })
await page.getByRole('button', { name: '平台首页', exact: true }).click()
await page.screenshot({ path: fileURLToPath(new URL('home-mobile.png', out)), fullPage: true })
await browser.close()
console.log('Screenshots saved in D:/lsecode/artifacts')
