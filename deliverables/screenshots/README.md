# 실제 실행 화면 캡처

2026-10-30(KST) 로컬 MySQL과 Tomcat 11에서 실행 중인 `http://localhost:8080/enjoytrip/`을 Chrome으로 직접 열어 **17장**을 캡처했다. 데스크톱은 **1440×900**, 모바일은 **390×844** 뷰포트다. 이미지는 화면 합성·내용 수정 없이 저장했다. 로그인 뒤 화면은 관리자 계정으로 열었지만 비밀번호와 API 키는 캡처에 포함하지 않았다.

| 파일 | 실제 경로 | 확인할 내용 |
| --- | --- | --- |
| [01-home.png](01-home.png) | `/` | 홈, 주요 이동 버튼 |
| [02-attractions-seoul.png](02-attractions-seoul.png) | `/attractions?region=1` | 서울 지역 필터, 실데이터 사진 카드 |
| [03-attraction-detail-map.png](03-attraction-detail-map.png) | `/attractions/1` | 관광지 상세, OpenStreetMap 타일과 위치 마커 |
| [04-plan-list.png](04-plan-list.png) | `/plans` | 공개 여행 계획 목록 |
| [05-plan-detail.png](05-plan-detail.png) | `/plans/1` | 여행 계획 상세; 캡처 시점에는 방문지가 없는 상태 |
| [06-hot-place-list.png](06-hot-place-list.png) | `/hot-places` | Hot Place 목록의 빈 상태 |
| [07-news.png](07-news.png) | `/news` | DB에 저장된 관광 뉴스 목록 |
| [08-notices.png](08-notices.png) | `/notices` | 공지 목록 |
| [09-board.png](09-board.png) | `/posts` | 게시판 목록 |
| [10-login.png](10-login.png) | `/login` | 로그인 화면 |
| [11-admin.png](11-admin.png) | `/admin` | 관리자 동기화·뉴스 수집·초기화 요청 영역 |
| [12-hot-place-form.png](12-hot-place-form.png) | `/hot-places/new` | 로그인 후 Hot Place 등록 폼 |
| [12b-hot-place-map.png](12b-hot-place-map.png) | `/hot-places/new` 아래쪽 | 지도 타일·마커와 좌표 선택 영역 |
| [13-plan-form.png](13-plan-form.png) | `/plans/new` | 로그인 후 여행 계획 작성 폼 |
| [14-home-mobile.png](14-home-mobile.png) | `/` | 모바일 홈 |
| [15-mobile-menu.png](15-mobile-menu.png) | `/`에서 메뉴 열기 | 모바일 접기식 메뉴 |
| [16-attractions-mobile.png](16-attractions-mobile.png) | `/attractions?region=1` | 모바일 검색 폼과 관광지 카드 |

## 캡처를 읽을 때

- `03`은 지도 타일과 마커가 실제 브라우저에서 로드된 화면이다. 지도는 카카오맵 활성화 없이 Leaflet/OpenStreetMap을 사용한다.
- `05`의 공개 계획은 당시 방문지가 없어 경로 지도가 나타나지 않는다. 방문지를 추가하면 동선 지도가 표시되는 구현이다. **이 이미지를 경로 지도 검증 증거로 사용하지 않는다.**
- `06`은 당시 DB에 Hot Place가 없어 빈 상태를 보여준다. 작성 폼은 `12`에서 확인할 수 있다.
- `11`의 관리자 상태 문구는 캡처 당시의 값이다. 재시작 직후 관광지 동기화 상태는 초기화됐고 RSS 연결 실패가 표시됐다. DB에 저장된 관광지·뉴스는 각각 `02`, `07`에서 조회된다. **관리자 캡처를 당일 동기화 성공 증거로 사용하지 않는다.**
- `12`는 폼 상단, `12b`는 같은 폼의 좌표 선택 지도를 스크롤해 캡처했다. 지도 타일과 마커가 실제로 표시된다.
