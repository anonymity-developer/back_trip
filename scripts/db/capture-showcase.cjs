// Capture actual pages from the running root-.env app; never alter screenshot pixels.
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const root = path.resolve(__dirname, '../..');
let puppeteer;
try { puppeteer = require('puppeteer'); }
catch { puppeteer = require(path.join(root, 'target/mermaid-cli/node_modules/puppeteer')); }
const context = JSON.parse(fs.readFileSync(path.join(root, 'target/db/capture-context.json'), 'utf8'));
const config = Object.fromEntries(fs.readFileSync(path.join(root, '.env'), 'utf8').replace(/^\uFEFF/, '').split(/\r?\n/)
  .filter(line => line.trim() && !line.trim().startsWith('#')).map(line => {
    const index = line.indexOf('=');
    let value = line.slice(index + 1).trim();
    if (/^(["']).*\1$/.test(value)) value = value.slice(1, -1);
    return [line.slice(0, index).trim(), value];
  }));
const out = path.join(root, 'deliverables/db/screenshots');
const work = path.join(root, 'target/db');
const privateFile = path.join(work, 'showcase-account.json');
const account = fs.existsSync(privateFile) ? JSON.parse(fs.readFileSync(privateFile, 'utf8')) : {
  login: 'showcase_' + crypto.randomBytes(5).toString('hex'), password: crypto.randomBytes(20).toString('hex'),
};
const manifest = {...context, screenshots: [], apiResponses: {}, demo: {}};
const desktop = {width: 1440, height: 1000};

async function main() {
  fs.mkdirSync(out, {recursive: true});
  const browser = await puppeteer.launch({
    executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe',
    headless: true, userDataDir: path.join(work, 'showcase-browser'), args: ['--disable-gpu', '--no-first-run'],
  });
  try {
    const page = await browser.newPage(); await page.setViewport(desktop);
    page.on('response', response => {
      const url = response.url();
      if (url.startsWith(context.baseUrl + '/api/attractions/') && !url.includes('/search')) {
        manifest.apiResponses[new URL(url).pathname.split('/').pop()] = response.status();
      }
    });
    async function visit(route) {
      const response = await page.goto(context.baseUrl + route, {waitUntil: 'networkidle2', timeout: 60000});
      if (response.status() !== 200) throw Error('Page failed: ' + route + ' ' + response.status());
      await page.evaluate(() => document.fonts.ready);
    }
    async function capture(file, route, description, fullPage = true) {
      if (route) await visit(route);
      await page.screenshot({path: path.join(out, file), fullPage});
      manifest.screenshots.push({file, path: new URL(page.url()).pathname + new URL(page.url()).search,
        description, viewport: page.viewport(), fullPage, timeKst: new Date().toLocaleString('sv-SE', {timeZone: 'Asia/Seoul'})});
      console.log('Captured ' + file);
    }
    async function fill(name, value) {
      await page.$eval('[name="' + name + '"]', (element, text) => {element.value = text; element.dispatchEvent(new Event('input', {bubbles: true}));}, value);
    }
    async function submit() {
      await Promise.all([page.waitForNavigation({waitUntil: 'networkidle2', timeout: 60000}), page.click('form button.primary')]);
    }
    await capture('01-home.png', '/', '관광지 탐색·계획·맛집으로 연결되는 홈');
    await capture('02-attractions-seoul.png', '/attractions?region=1&type=12', 'DB 기준 서울 관광지 목록');
    await visit('/attractions/' + context.placeIds[0]);
    await page.waitForFunction(() => !['weather','sun','chargers'].some(id => document.getElementById(id)?.textContent.includes('불러오는 중')), {timeout: 35000}).catch(() => {});
    await capture('03-attraction-detail.png', null, '가회동성당 사진·설명·지도·실제 부가 정보 응답');
    const info = await page.$$('section.panel');
    for (const section of info) {
      if ((await section.evaluate(element => element.textContent)).includes('Useful information')) {
        await section.screenshot({path: path.join(out, '04-travel-info.png')});
        manifest.screenshots.push({file: '04-travel-info.png', path: '/attractions/' + context.placeIds[0], description: '여행 정보 영역 실제 응답', viewport: desktop, element: '여행 정보 panel'});
      }
    }
    await capture('05-news.png', '/news', '실제 RSS 수집 뉴스와 원문 링크');
    await capture('06-login.png', '/login', '로그인 화면', false);
    if (!account.created) {
      await visit('/signup');
      await fill('loginId', account.login); await fill('email', account.login + '@example.test');
      await fill('name', '여행하는 개발자'); await fill('password', account.password); await submit();
      account.created = true; fs.writeFileSync(privateFile, JSON.stringify(account));
    }
    await visit('/login'); await fill('loginId', account.login); await fill('password', account.password); await submit();
    await capture('07-profile.png', '/me', '시연용 회원의 프로필 조회·수정·탈퇴');
    const today = new Intl.DateTimeFormat('sv-SE', {timeZone: 'Asia/Seoul'}).format(new Date());
    if (!account.plan) {
      await visit('/plans/new'); await fill('title', '서울 북촌에서 보내는 하루');
      await fill('startDate', today); await fill('endDate', today); await fill('budget', '50000');
      await fill('details', '가회동성당과 경복궁을 걸으며 북촌을 둘러보는 시연용 일정입니다. 방문 순서와 예산을 저장하고 공개 링크로 공유합니다.');
      await page.click('[name=isPublic]'); await submit(); account.plan = Number(page.url().match(/\/plans\/(\d+)/)[1]);
      for (const [index, id] of context.placeIds.entries()) {
        const result = await page.evaluate(async ({base, plan, id, day, index}) => {
          const csrf = document.querySelector('[name=csrf]').value;
          const response = await fetch(base + '/plans/' + plan + '/stops', {method:'POST', body:new URLSearchParams({csrf,attractionId:String(id),visitDate:day,budget:index?'30000':'20000',memo:index?'궁궐 산책과 식사':'북촌 골목과 성당 둘러보기'})});
          return response.status;
        }, {base:context.baseUrl,plan:account.plan,id,day:today,index});
        if (result !== 200) throw Error('Adding showcase stop failed');
      }
      fs.writeFileSync(privateFile, JSON.stringify(account));
    }
    await capture('08-plan-list.png', '/plans', '공개 계획과 내가 작성한 계획');
    await capture('09-plan-detail.png', '/plans/' + account.plan, '일정·경비·관광지 2개·동선·공개 공유');
    await capture('10-plan-form.png', '/plans/new', '일정·예산·상세 계획·공개 여부 입력');
    const photoPage = await browser.newPage();
    const photoResponse = await photoPage.goto(context.photoUrl, {waitUntil:'load', timeout:30000});
    if (photoResponse.status() !== 200 || !photoResponse.headers()['content-type'].includes('image/')) throw Error('Showcase photo unavailable');
    const photo = path.join(work, 'showcase-gahoe.jpg'); fs.writeFileSync(photo, await photoResponse.buffer()); await photoPage.close();
    if (!account.hot) {
      await visit('/hot-places/new'); await fill('title', '북촌에서 만난 가회동성당'); await fill('placeName', '가회동성당');
      await fill('placeType', '문화·역사'); await fill('visitedOn', today);
      await fill('description', 'EnjoyTrip 서비스 기능을 소개하기 위한 시연용 기록입니다. 북촌의 사진과 방문 위치·날짜를 함께 보관합니다. 사진 출처: 한국관광공사 국문관광정보.');
      await fill('latitude', '37.58208588'); await fill('longitude', '126.98466169');
      await (await page.$('[name=image]')).uploadFile(photo); await submit();
      account.hot = Number(page.url().match(/\/hot-places\/(\d+)/)[1]); fs.writeFileSync(privateFile, JSON.stringify(account));
    }
    await capture('11-hot-list.png', '/hot-places', '사진·장소·방문일을 공유하는 Hot Place 목록');
    await capture('12-hot-detail.png', '/hot-places/' + account.hot, '사진과 위치·방문일을 저장한 상세 화면');
    await capture('13-hot-form.png', '/hot-places/new', '사진 업로드와 지도 좌표 선택');
    if (!account.post) {
      await visit('/posts/new'); await fill('title', '북촌 여행 동선을 함께 공유해요');
      await fill('body', '가회동성당에서 경복궁으로 이어지는 하루 여행을 계획했습니다. 공개 여행 계획의 방문 순서와 예산을 참고해 나만의 일정을 만들어 보세요. (서비스 시연용 게시글)');
      await submit(); account.post = true; fs.writeFileSync(privateFile, JSON.stringify(account));
    }
    await capture('14-board.png', '/posts', '회원 간 여행 정보를 공유하는 게시판');
    await capture('15-notices.png', '/notices', '관리자가 작성하고 누구나 읽는 공지사항');
    // New isolated browser context avoids capturing a password field or changing the demo session.
    const adminContext = await browser.createBrowserContext(); const admin = await adminContext.newPage();
    await admin.setViewport(desktop); await admin.goto(context.baseUrl + '/login', {waitUntil:'networkidle2'});
    await admin.type('[name=loginId]', config.ENJOYTRIP_ADMIN_ID); await admin.type('[name=password]', config.ENJOYTRIP_ADMIN_PASSWORD);
    await Promise.all([admin.waitForNavigation({waitUntil:'networkidle2'}), admin.click('form button.primary')]);
    await admin.goto(context.baseUrl + '/admin', {waitUntil:'networkidle2'});
    await admin.screenshot({path:path.join(out,'16-admin.png'),fullPage:true});
    manifest.screenshots.push({file:'16-admin.png',path:'/admin',description:'현재 .env의 실제 관리자·수집 상태',viewport:desktop,fullPage:true});
    await adminContext.close();
    const mobileContext = await browser.createBrowserContext(); const mobile = await mobileContext.newPage();
    await mobile.setViewport({width:390,height:844});
    for (const [file,route] of [['17-home-mobile.png','/'],['18-attractions-mobile.png','/attractions?region=1&type=39']]) {
      await mobile.goto(context.baseUrl+route,{waitUntil:'networkidle2'});
      if(await mobile.evaluate(()=>document.documentElement.scrollWidth>innerWidth))throw Error('Mobile overflow');
      await mobile.screenshot({path:path.join(out,file),fullPage:false});
      manifest.screenshots.push({file,path:route,description:'모바일 실제 화면·가로 넘침 없음',viewport:mobile.viewport(),fullPage:false});
    }
    await mobile.click('.nav-toggle'); await mobile.screenshot({path:path.join(out,'19-mobile-menu.png')});
    manifest.screenshots.push({file:'19-mobile-menu.png',path:'/attractions?region=1&type=39',description:'모바일 메뉴 열기',viewport:mobile.viewport()});
    await mobileContext.close();
    manifest.demo = {plan:account.plan,hotPlace:account.hot,syntheticMember:true,photoSource:context.photoUrl};
    fs.writeFileSync(path.join(out,'capture-manifest.json'),JSON.stringify(manifest,null,2));
    console.log('Completed '+manifest.screenshots.length+' actual captures using project root .env.');
  } finally {await browser.close();}
}
main().catch(error=>{console.error(error.message);process.exit(1);});
