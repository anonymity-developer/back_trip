"""Exercise the isolated app; creates temporary members/content and deletes them after verification."""
import argparse
import base64
from datetime import date
from http.cookiejar import CookieJar
import json
import re
import secrets
from urllib.error import HTTPError
from urllib.parse import urlencode, urlparse
from urllib.request import build_opener, HTTPCookieProcessor, Request
from settings import load_settings


class Browser:
    def __init__(self, base):
        self.base = base
        self.opener = build_opener(HTTPCookieProcessor(CookieJar()))

    def request(self, path, fields=None, raw=None, content_type=None, expected=200):
        data = raw if raw is not None else urlencode(fields, doseq=True).encode() if fields is not None else None
        headers = {'Content-Type': content_type} if content_type else {}
        request = Request(self.base + path, data=data, headers=headers)
        try:
            response = self.opener.open(request, timeout=20)
        except HTTPError as error:
            response = error
        with response:
            body = response.read()
            assert response.code == expected, f'{path}: expected {expected}, got {response.code}'
            return body.decode('utf-8', errors='replace'), response.geturl()

    def csrf(self, path='/login'):
        html, _ = self.request(path)
        return re.search(r'name="csrf" value="([^"]+)"', html).group(1)

    def post(self, path, fields, expected=200):
        return self.request(path, {'csrf': self.csrf('/me'), **fields}, expected=expected)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', required=True)
    args = parser.parse_args()
    base = args.base_url.rstrip('/')
    assert urlparse(base).hostname in ('127.0.0.1', 'localhost'), 'Only isolated local servers are supported'
    config = load_settings()
    guest = Browser(base)
    for path in ('/health', '/', '/attractions', '/plans', '/hot-places', '/news', '/notices', '/posts'):
        guest.request(path)
    html, _ = guest.request('/attractions?region=1&type=39')
    assert '음식점' in html and html.count('class="card"') == 20
    guest.request('/attractions?region=999', expected=400)
    guest.request('/attractions?type=999', expected=400)
    guest.request('/attractions?page=0', expected=400)
    guest.request('/api/attractions/search?q=', expected=400)
    values = json.loads(guest.request('/api/attractions/search?q=' + urlencode({'q': '서울'})[2:])[0])
    assert len(values) == 10 and set(values[0]) == {'id', 'title'}
    attraction = values[0]['id']
    guest.request(f'/attractions/{attraction}')
    guest.request(f'/api/attractions/{attraction}/photos')
    if not config.get('ENJOYTRIP_WEATHER_API_KEY'):
        guest.request(f'/api/attractions/{attraction}/weather', expected=503)
    print('PASS: public pages, filters, 20 cards, 10-item JSON, detail/photos')

    member = Browser(base)
    other = Browser(base)
    admin = Browser(base)
    created = []
    accounts = {}
    password = secrets.token_hex(16)
    today = date.today().isoformat()
    try:
        for browser in (member, other):
            login = 'verify_' + secrets.token_hex(5)
            token = browser.csrf('/signup')
            browser.request('/signup', {'csrf': token, 'loginId': login, 'email': login + '@example.test', 'name': 'DB 검증 회원', 'password': password})
            browser.request('/login', {'csrf': browser.csrf(), 'loginId': login, 'password': password})
            browser.request('/me')
            created.append(browser)
            accounts[browser] = (login, login + '@example.test')
        member.post('/me', {'email': 'updated_' + secrets.token_hex(4) + '@example.test', 'name': 'DB 수정 회원'})
        member.request('/logout', {}, expected=403)
        member.request('/admin', expected=403)
        member.request('/notices/new', expected=403)
        _, url = member.post('/plans/new', {'title': 'DB 회귀 계획', 'startDate': today, 'endDate': today, 'budget': '1000', 'details': '테스트', 'isPublic': 'on'})
        plan = int(re.search(r'/plans/(\d+)$', url).group(1))
        guest.request(f'/plans/{plan}')
        for place in values[:2]:
            member.post(f'/plans/{plan}/stops', {'attractionId': place['id'], 'visitDate': today, 'budget': '500', 'memo': '회귀'})
        html, _ = member.request(f'/plans/{plan}')
        stops = re.findall(r'<li data-id="(\d+)"', html)
        assert len(stops) == 2
        member.post(f'/api/plans/{plan}/stops/order', {'stopId': list(reversed(stops))}, expected=204)
        changed, _ = member.request(f'/plans/{plan}')
        assert re.findall(r'<li data-id="(\d+)"', changed) == list(reversed(stops))
        other.post(f'/plans/{plan}/delete', {}, expected=400)
        member.post(f'/plans/{plan}/edit', {'title': 'DB 비공개 계획', 'startDate': today, 'endDate': today, 'budget': '1000', 'details': 'private'})
        guest.request(f'/plans/{plan}', expected=404)
        other.request(f'/plans/{plan}', expected=404)
        post_title = 'DB 검증 게시글 ' + secrets.token_hex(8)
        member.post('/posts/new', {'title': post_title, 'body': '<script>test</script>'})
        listing, _ = member.request('/posts?' + urlencode({'keyword': post_title}))
        post = int(re.search(r'/posts/(\d+)', listing).group(1))
        detail, _ = guest.request(f'/posts/{post}')
        assert '&lt;script&gt;test&lt;/script&gt;' in detail
        member.post(f'/posts/{post}/edit', {'title': 'DB 수정 게시글', 'body': '수정 확인'})
        other.post(f'/posts/{post}/delete', {}, expected=403)
        print('PASS: signup/login/profile, CSRF, member permissions, plan stops/order, private visibility, board escaping/ownership')

        boundary = 'dbverify' + secrets.token_hex(8)
        fields = {'csrf': member.csrf('/me'), 'title': 'DB 검증 Hot Place', 'placeName': '검증 장소', 'placeType': '관광지', 'visitedOn': today, 'description': '검증 사진', 'latitude': '37.5', 'longitude': '126.9'}
        body = b''
        for key, value in fields.items():
            body += f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'.encode()
        png = base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aX1kAAAAASUVORK5CYII=')
        body += f'--{boundary}\r\nContent-Disposition: form-data; name="image"; filename="test.png"\r\nContent-Type: image/png\r\n\r\n'.encode() + png + f'\r\n--{boundary}--\r\n'.encode()
        _, url = member.request('/hot-places/new', raw=body, content_type='multipart/form-data; boundary=' + boundary)
        hot = int(re.search(r'/hot-places/(\d+)$', url).group(1))
        guest.request(f'/photos/{hot}')
        other.post(f'/hot-places/{hot}/delete', {}, expected=400)
        member.request(f'/hot-places/{hot}/edit', raw=body, content_type='multipart/form-data; boundary=' + boundary)
        guest.request(f'/photos/{hot}')
        member.post(f'/hot-places/{hot}/delete', {})
        guest.request(f'/photos/{hot}', expected=404)

        admin.request('/login', {'csrf': admin.csrf(), 'loginId': config['ENJOYTRIP_ADMIN_ID'], 'password': config['ENJOYTRIP_ADMIN_PASSWORD']})
        admin.request('/admin')
        admin.request(f'/plans/{plan}', expected=404)
        if config.get('ENJOYTRIP_SYNC_ENABLED', 'true').lower() == 'false':
            admin.post('/admin/sync/tour', {}, expected=503)
            admin.post('/admin/sync/news', {}, expected=503)
        notice_title = 'DB 검증 공지 ' + secrets.token_hex(8)
        admin.post('/notices/new', {'title': notice_title, 'body': '검증'})
        listing, _ = admin.request('/notices?' + urlencode({'keyword': notice_title}))
        notice = int(re.search(r'/notices/(\d+)', listing).group(1))
        admin.post(f'/notices/{notice}/edit', {'title': 'DB 수정 공지', 'body': '수정 확인'})
        admin.post(f'/notices/{notice}/delete', {})
        guest.request('/password-reset', {'csrf': guest.csrf('/password-reset'), 'email': accounts[other][1]})
        admin_page, _ = admin.request('/admin')
        request_row = next(row for row in re.findall(r'<tr>(.*?)</tr>', admin_page, re.S) if accounts[other][0] in row)
        reset_id = int(re.search(r'/admin/password-resets/(\d+)/complete', request_row).group(1))
        reset_page, _ = admin.post(f'/admin/password-resets/{reset_id}/complete', {})
        temporary = re.search(r'임시 비밀번호: <strong>([^<]+)</strong>', reset_page).group(1)
        repeated, _ = admin.post(f'/admin/password-resets/{reset_id}/complete', {})
        assert '처리할 수 없는 요청' in repeated
        other.post('/logout', {})
        _, changed_url = other.request('/login', {'csrf': other.csrf(), 'loginId': accounts[other][0], 'password': temporary})
        assert changed_url.endswith('/password-change')
        other.post('/password-change', {'oldPassword': temporary, 'newPassword': password})
        other.request('/me')
        print('PASS: photo upload/read/delete, administrator notice, private plan protection, configured sync policy')
        print('PASS: password reset request/admin completion, repeated completion rejection, forced password change')
    finally:
        for browser in reversed(created):
            browser.post('/me/delete', {'password': password})
    guest.request(f'/plans/{plan}', expected=404)
    guest.request(f'/posts/{post}', expected=404)
    print('PASS: member withdrawal cascade and temporary-data cleanup')


if __name__ == '__main__':
    main()
