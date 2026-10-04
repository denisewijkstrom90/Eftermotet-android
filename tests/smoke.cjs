const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const root = path.join(__dirname, '../app/src/main/assets');
const server = http.createServer((req, res) => {
  const file = path.join(root, req.url === '/' ? 'index.html' : req.url.slice(1));
  if (!file.startsWith(root) || !fs.existsSync(file)) { res.writeHead(404).end(); return; }
  res.setHeader('Content-Type', 'text/html; charset=utf-8'); res.end(fs.readFileSync(file));
});
(async () => {
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 390, height: 844 }});
  await context.addInitScript(() => { window.AndroidApp = {
    isDemoBuild: () => false, syncReminders: () => {}, refreshSubscription: () => {},
    startSubscription: () => { window.buyCalls = (window.buyCalls || 0) + 1; },
    manageSubscription: () => { window.manageCalls = (window.manageCalls || 0) + 1; },
    saveBackup: json => { window.exportedBackup = JSON.parse(json); }
  }; });
  const page = await context.newPage();
  const errors = []; page.on('pageerror', e => errors.push(e.message));
  page.on('dialog', d => d.accept());
  await page.goto(`http://127.0.0.1:${server.address().port}/`);
  await page.waitForFunction(() => typeof window.updateSubscription === 'function');
  assert.equal(await page.locator('#newButton').isVisible(), false);
  await page.evaluate(() => window.updateSubscription(false, true, true, '29,00 kr', ''));
  assert.match(await page.locator('#subscriptionOffer').textContent(), /14 dagar gratis.*29,00 kr/);
  await page.locator('#subscribeButton').click();
  assert.equal(await page.evaluate(() => window.buyCalls), 1);
  assert.equal(await page.locator('#newButton').isVisible(), false, 'Tapping Buy must not grant access');
  await page.evaluate(() => window.updateSubscription(false, true, false, '29,00 kr', ''));
  assert.match(await page.locator('#subscriptionOffer').textContent(), /Ingen gratis provperiod/);
  assert.match(await page.locator('#subscribeButton').textContent(), /Prenumerera/);
  await page.evaluate(() => window.updateSubscription(true, false, false, '29,00 kr', ''));
  await page.locator('#newButton').click();
  await page.locator('#title').fill('Testmöte');
  await page.locator('#decisions').fill('Ta med dokument.');
  await page.locator('#meetingForm button[type=submit]').click();
  await page.waitForFunction(() => meetings.length === 1);
  await page.locator('#newDocument').click();
  await page.locator('#docName').fill('Underlag');
  await page.locator('#docText').fill('Den här texten ska gå att läsa efter avslut.');
  await page.locator('#docQuestions').fill('Vem ansvarar?\nNär blir det klart?');
  const meetingId = await page.evaluate(() => meetings[0].id);
  await page.locator('#docMeeting').selectOption(meetingId);
  await page.locator('#documentForm button[type=submit]').click();
  await page.waitForFunction(() => documents.length === 1);
  assert.match(await page.locator('#meetings').textContent(), /Underlag/);
  await page.locator('#themeButton').click();
  for (const theme of ['green', 'blue', 'purple', 'pink', 'beige', 'dark']) {
    await page.locator(`input[name=theme][value=${theme}]`).check();
    assert.equal(await page.evaluate(() => currentTheme), theme);
  }
  await page.locator('[data-close=themeDialog]').last().click();
  await page.reload();
  await page.waitForFunction(() => documents.length === 1);
  assert.equal(await page.evaluate(() => currentTheme), 'dark');
  assert.equal(await page.locator('#newButton').isVisible(), false);
  assert.match(await page.locator('#meetings').textContent(), /Testmöte/);
  assert.match(await page.locator('#documents').textContent(), /Vem ansvarar/);
  await page.locator('#meetings button').filter({ hasText: 'Visa' }).first().click();
  assert.equal(await page.locator('#title').getAttribute('readonly'), '');
  await page.locator('[data-close=meetingDialog]').first().click();
  await page.locator('#aboutButton').click();
  await page.locator('#exportButton').click();
  const backup = await page.evaluate(() => window.exportedBackup);
  assert.equal(backup.meetings.length, 1); assert.equal(backup.documents.length, 1);
  assert.equal(backup.documents[0].questions.length, 2);
  await page.locator('#manageSubscriptionInApp').click();
  assert.equal(await page.evaluate(() => window.manageCalls), 1);
  await page.locator('[data-close=aboutDialog]').first().click();
  await page.evaluate(() => window.updateSubscription(true, false, false, '29 kr', ''));
  await page.evaluate(async () => { const data = normalizeBackup(window.exportedBackup); await replaceBackup(data); });
  assert.equal(await page.evaluate(() => documents.length), 1);
  assert.equal(await page.evaluate(() => { try { normalizeBackup({app:'EfterMötet',version:2,meetings:[{id:'invalid',date:'2026-99-99'}]}); return false; } catch { return true; } }), true);
  for (const viewport of [{width:390,height:844},{width:1024,height:768}]) {
    await page.setViewportSize(viewport);
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
  }
  assert.deepEqual(errors, []);
  await browser.close();
  console.log('PASS: purchase UI, no premature access, paid/trial text, meetings, documents, six themes, persistence, read-only/export, restore/import validation, phone/tablet layout.');
})().catch(e => { console.error(e); process.exitCode=1; }).finally(() => server.close());
