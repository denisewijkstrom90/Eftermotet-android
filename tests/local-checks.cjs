// Runs the actual app functions without a browser. Does not simulate Android or Play.
const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const html = fs.readFileSync(require('node:path').join(__dirname,'../app/src/main/assets/index.html'),'utf8');
let checks = 0;
for (const match of html.matchAll(/<script>([\s\S]*?)<\/script>/g)) { new Function(match[1]); checks++; }
const nodes = new Map();
const node = id => { if (!nodes.has(id)) nodes.set(id,{hidden:false,textContent:''}); return nodes.get(id); };
const storage = new Map([['eftermotet-v1','ORIGINAL']]);
const context = vm.createContext({ console, Set, Date, JSON, Error,
  document: {getElementById:node,body:{classList:{toggle:()=>{}}}},
  window:{}, accessAllowed:false, enforceAccess:()=>{},
  THEME_NAMES:{green:1,blue:1,purple:1,pink:1,beige:1,dark:1},
  validTheme: theme => ['green','blue','purple','pink','beige','dark'].includes(theme),
  KEY:'eftermotet-v1',localStorage:{getItem:k=>storage.get(k)??null,setItem:(k,v)=>storage.set(k,v),removeItem:k=>storage.delete(k)},
  dbOpen:async()=>{throw Error('SIMULATED_STORAGE_FAILURE');}
});
vm.runInContext(html.slice(html.indexOf('function normalizeBackup('),html.indexOf("const THEME_KEY=")),context);
vm.runInContext(html.slice(html.indexOf('window.updateSubscription='),html.indexOf("document.getElementById('subscribeButton').onclick=")),context);
const uuid=n=>`00000000-0000-0000-0000-${String(n).padStart(12,'0')}`;
const valid={app:'EfterMötet',version:2,theme:'blue',meetings:[{id:uuid(1),title:'Möte',date:'2026-10-04',actions:[]}],documents:[{id:uuid(2),name:'Papper',created:'2026-10-04T00:00:00Z',meetingId:uuid(1),text:'Behåll mig',image:'',questions:['Fråga?']}]};
const clone=()=>JSON.parse(JSON.stringify(valid));
const run=data=>context.normalizeBackup(data);
assert.equal(run(clone()).documents[0].meetingId,uuid(1)); checks++;
for (const corrupt of [
  d=>d.meetings[0].date='2026-02-30',
  d=>d.documents[0].id=uuid(1),
  d=>d.documents[0].meetingId=uuid(99),
  d=>d.documents[0].image='https://evil.example/document.png',
  d=>d.documents[0].image='data:image/svg+xml;base64,PHN2Zz4=',
  d=>d.meetings[0].title='a'.repeat(101),
  d=>{d.meetings[0].reminder='hour';d.meetings[0].time='25:00';},
  d=>d.meetings[0].actions=[{id:uuid(3),text:'task',done:'false'}],
  d=>d.version=999
]) { const data=clone(); corrupt(data); assert.throws(()=>run(data)); checks++; }
context.window.updateSubscription(false,true,true,'29 kr','Väntar');
assert.equal(context.accessAllowed,false);
assert.match(node('subscriptionOffer').textContent,/14 dagar gratis.*29 kr/);
assert.equal(node('subscribeButton').textContent,'Starta 14 dagar gratis'); checks++;
context.window.updateSubscription(false,true,false,'29 kr','');
assert.match(node('subscriptionOffer').textContent,/Ingen gratis provperiod/);
assert.equal(node('subscribeButton').textContent,'Prenumerera via Google Play'); checks++;
context.window.updateSubscription(true,false,false,'29 kr','');
assert.equal(context.accessAllowed,true); assert.equal(node('subscriptionGate').hidden,true); checks++;
context.window.updateSubscription(false,false,false,'','Anslutningen bröts');
assert.equal(context.accessAllowed,false); assert.equal(node('subscribeButton').hidden,true); checks++;
(async()=>{
  await assert.rejects(()=>context.replaceBackup(run(clone())),/SIMULATED_STORAGE_FAILURE/);
  assert.equal(storage.get('eftermotet-v1'),'ORIGINAL'); checks++;
  storage.delete('eftermotet-v1');
  await assert.rejects(()=>context.replaceBackup(run(clone())),/SIMULATED_STORAGE_FAILURE/);
  assert.equal(storage.has('eftermotet-v1'),false); checks++;
  console.log(`PASS: ${checks} local checks (syntax, backup validation/rollback, subscription view states). No Android/Google Play execution.`);
})().catch(e=>{console.error(e);process.exitCode=1;});
