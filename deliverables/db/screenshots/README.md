# 현재 .env 기준 실제 실행 화면 19장

2026-11-06(KST) Chrome에서 `http://127.0.0.1:18080/enjoytrip`을 열어 **전부 새로 캡처**했습니다. 앱은 루트 `.env`의 MySQL 3306/enjoytrip에 연결되었고 관광지 51,622건·뉴스 10건을 사용했습니다. 기존 enjoytrip_test·동기화 중지 환경의 캡처는 교체했습니다.

데스크톱 뷰포트는 1440×1000, 모바일은 390×844입니다. 데스크톱은 주로 전체 페이지를 저장했으므로 파일 높이는 화면 길이에 따라 다릅니다. 여행 정보는 브라우저의 해당 요소를 직접 캡처했습니다. 합성·내용 편집·가짜 API 응답은 사용하지 않았습니다.

| 파일 | 실제 기능·조건 |
| --- | --- |
| [01-home.png](01-home.png) | 비회원 홈·서비스 진입점 |
| [02-attractions-seoul.png](02-attractions-seoul.png) | DB 기준 서울 관광지 검색·20건 페이지 |
| [03-attraction-detail.png](03-attraction-detail.png) | 가회동성당 사진·설명·지도·여행 정보 |
| [04-travel-info.png](04-travel-info.png) | 실제 날씨·예보·일출/일몰 성공 응답. 충전소 성공 증거는 아님 |
| [05-news.png](05-news.png) | 수집된 관광 뉴스·원문 링크 |
| [06-login.png](06-login.png) | 비밀번호를 입력하지 않은 로그인 폼 |
| [07-profile.png](07-profile.png) | 가상 시연 회원의 프로필 수정·탈퇴 화면 |
| [08-plan-list.png](08-plan-list.png) | 공개 계획과 본인 계획 목록 |
| [09-plan-detail.png](09-plan-detail.png) | 실제 저장한 ‘서울 북촌에서 보내는 하루’, 2개 방문지·경비·지도 동선 |
| [10-plan-form.png](10-plan-form.png) | 기간·예산·상세 계획·공개 여부 입력 |
| [11-hot-list.png](11-hot-list.png) | 실제 사진을 업로드한 Hot Place 목록 |
| [12-hot-detail.png](12-hot-detail.png) | 사진·방문일·설명·좌표·본인 수정/삭제 |
| [13-hot-form.png](13-hot-form.png) | 사진 업로드·방문 정보·지도 좌표 선택 |
| [14-board.png](14-board.png) | 시연 회원이 등록한 여행 정보 공유글 |
| [15-notices.png](15-notices.png) | 관리자 공지 목록 |
| [16-admin.png](16-admin.png) | 현재 공통 설정의 실제 관광지·뉴스 수집 상태와 관리자 기능 |
| [17-home-mobile.png](17-home-mobile.png) | 모바일 홈 |
| [18-attractions-mobile.png](18-attractions-mobile.png) | 모바일 지역·유형 검색·가로 넘침 없음 |
| [19-mobile-menu.png](19-mobile-menu.png) | 모바일 메뉴를 연 상태 |

‘여행하는 개발자’와 관련 계획·Hot Place·게시글은 기능을 보여주려고 실제 폼으로 등록한 가상 시연 자료입니다. Hot Place 사진은 공개 관광지의 한국관광공사 제공 사진이며 설명에도 출처를 표시했습니다. 실제 관리자 비밀번호·API 키는 캡처하지 않았습니다.

날씨·일출/일몰은 이번 브라우저 요청에서 HTTP 200을 확인했습니다. 충전소는 별도 확인 요청도 응답 지연으로 시간 초과되어 성공했다고 표시하지 않았습니다. 일부 원본 사진·타일의 외부 로딩 실패는 있는 그대로 남겼습니다.

파일별 URL·뷰포트·실제 이미지 크기·캡처 시각은 [capture-manifest.json](capture-manifest.json)에 있습니다. 이 기록에는 비밀번호·키가 없습니다. 회원·권한·데이터 보존 등은 [검증 기록](../verification.md)도 함께 확인합니다.
