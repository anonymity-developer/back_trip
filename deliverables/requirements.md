# 개선된 요구사항 목록

이 문서는 `temp/`의 프로젝트 명세서를 정리한 [기존 요구사항](reference/docs/01-requirements.md)을 기준으로, 구현 중 확정된 권한·외부 연동·실패 처리 규칙을 반영했다. F101~F114 번호는 기존 문서와 일치한다. **구현 완료**는 코드와 화면이 존재한다는 뜻이며, 외부 기관 API의 모든 실시간 응답을 검증했다는 뜻은 아니다.

## 기능 요구사항

| ID | 사용자·목적 | 상세 완료 기준 | 구현 및 검증 근거 |
| --- | --- | --- | --- |
| F101 | 방문자가 지역별 관광지를 찾는다 | 한국관광공사 데이터를 17개 지역 단위로 수집·저장하고 지역별 목록과 상세를 제공한다. API가 중단돼도 저장된 목록을 조회할 수 있다. | `AttractionController → AttractionService → AttractionDao → AttractionDto`; [서울 목록](screenshots/02-attractions-seoul.png), [상세](screenshots/03-attraction-detail-map.png) |
| F102 | 방문자가 관광지·숙박·음식점을 찾는다 | 지역, 유형, 이름을 조합해 검색하고 페이지당 20건을 보여준다. 결과가 없으면 안내한다. | `/attractions?region=&type=&keyword=`; [검색 화면](screenshots/02-attractions-seoul.png) |
| F103 | 방문자가 문화시설·축제/공연·코스·레포츠·쇼핑을 찾는다 | F102의 검색 방식으로 유형 코드를 필터링한다. 선택 조건은 페이지 이동 후에도 유지된다. | `AttractionDao.search`; [검색 화면](screenshots/02-attractions-seoul.png) |
| F104 | 회원이 여행을 계획·공유한다 | 제목·기간(최대 91일)·예산·설명을 저장한다. 방문지의 날짜·경비·메모·순서를 관리하고 좌표가 있는 방문지를 지도 동선으로 표시한다. 비공개 계획은 소유자만 열 수 있으며 관리자도 예외가 아니다. | `PlanController → PlanService → PlanDao → PlanDto/PlanStopDto`; [목록](screenshots/04-plan-list.png), [상세](screenshots/05-plan-detail.png), [작성](screenshots/13-plan-form.png) |
| F105 | 회원이 Hot Place를 공유한다 | 방문일·유형·설명·좌표·사진을 등록하고 소유자만 수정·삭제한다. 신규 사진은 JPEG/PNG/WebP, 최대 5MB로 제한한다. 지도 클릭으로 좌표를 고른다. | `HotPlaceController → HotPlaceService → HotPlaceDao → HotPlaceDto`; [목록](screenshots/06-hot-place-list.png), [작성](screenshots/12-hot-place-form.png), [좌표 지도](screenshots/12b-hot-place-map.png) |
| F106 | 방문자가 관광 뉴스를 읽는다 | 뉴시스 문화·지방·수도권 RSS에서 관광 키워드와 일치하는 제목만 수집한다. URL 중복을 제거하고 제목·출처·발행일·원문 링크를 보여준다. 새 저장 건수와 관련 기사 확인 건수를 구분한다. | `NewsController → NewsService → NewsDao → NewsDto`; [뉴스 목록](screenshots/07-news.png) |
| F107 | 회원이 계정을 관리한다 | 가입·프로필 조회/수정·탈퇴를 제공하고 ID·이메일 중복 및 잘못된 입력을 거부한다. 탈퇴 시 사용자 콘텐츠를 함께 정리한다. | `AuthController → UserService → UserDao → UserDto`; [로그인](screenshots/10-login.png) |
| F108 | 회원이 인증·비밀번호를 관리한다 | 로그인/로그아웃, 비밀번호 초기화 요청, 관리자 본인 확인 후 임시 비밀번호 발급, 다음 로그인 시 비밀번호 변경을 제공한다. | `SecurityFilter`, `UserService`; [로그인](screenshots/10-login.png), [관리자](screenshots/11-admin.png) |
| F109 | 관리자가 공지를 관리한다 | 누구나 목록·상세를 열 수 있고 관리자만 작성·수정·삭제한다. | `PostController → PostService → PostDao`; [공지](screenshots/08-notices.png) |
| F110 | 회원이 게시판에 글을 쓴다 | 누구나 목록·상세를 열 수 있고 회원만 작성하며 작성자만 수정·삭제한다. | `PostController → PostService → PostDao`; [게시판](screenshots/09-board.png) |
| F111 | 방문자가 날씨를 확인한다 | 관광지 좌표의 현재 날씨와 최대 5일 예보를 상세 화면에 비동기로 표시한다. 실패하면 정보 영역에만 오류를 표시한다. | `InfoController → InfoService`; OpenWeather 키와 실응답 조건부 |
| F112 | 방문자가 관광지 사진을 본다 | 대표 사진과 추가 사진을 표시하고 사진 URL이 없거나 로드에 실패하면 자리표시 화면을 보여준다. | `AttractionService.images`, JSP/`ui.js`; [서울 목록](screenshots/02-attractions-seoul.png) |
| F113 | 방문자가 일출·일몰을 확인한다 | 선택 날짜와 관광지 좌표로 시각을 조회하고 한국 시간대로 표기한다. | `InfoController → InfoService`; 공공데이터 API 활용 승인·실응답 조건부 |
| F114 | 방문자가 주변 충전소를 찾는다 | 관광지 반경 10km의 충전소 최대 20곳을 상태·갱신 시각과 함께 목록·지도에 표시한다. | `InfoController → InfoService`; 공공데이터 API 활용 승인·실응답 조건부 |

## 운영·품질 요구사항

| ID | 개선된 규칙 | 완료 기준 |
| --- | --- | --- |
| N01 | 권한과 개인정보 | 비공개 계획은 소유자만 조회한다. 관리자 역할은 다른 회원의 비공개 계획 조회 권한을 주지 않는다. 변경 요청은 역할과 소유권을 서버에서 검사한다. |
| N02 | 인증과 입력 보호 | 비밀번호는 PBKDF2 해시로 저장한다. 로그인 시 세션 ID를 바꾸고 변경 요청에 CSRF 토큰을 요구한다. JSP 출력은 이스케이프하고 SQL은 PreparedStatement를 사용한다. |
| N03 | 외부 연동 실패 처리 | 관광지·뉴스는 저장된 DB 자료를 계속 보여준다. 날씨·일출·충전소 오류는 상세 화면의 해당 영역에만 표시한다. 관리자에게 동기화·수집 상태를 보여준다. |
| N04 | 지도 공급원 | 카카오맵 활성화 없이 Leaflet과 OpenStreetMap 타일을 사용한다. 지도 출처를 표시하고 타일을 대량 미리 받지 않는다. 타일 연결 실패 시 안내한다. |
| N05 | 반응형·접근성 | 390px 모바일부터 데스크톱까지 가로 넘침 없이 탐색한다. 모바일 메뉴는 버튼으로 열고 `aria-expanded`를 갱신한다. 입력·버튼의 키보드 포커스를 표시한다. |
| N06 | 반복 실행 | 관리자 수동 실행과 일일 스케줄러가 동일한 수집 Service를 사용한다. 중복 관광지·뉴스는 원본 ID 또는 URL 기준으로 갱신·제거한다. |
| N07 | 비밀 관리 | DB 비밀번호와 API 키는 `.env`로 전달하고 Git에서 제외한다. 제출 화면과 문서에 값을 넣지 않는다. |

## 원본에서 구체화한 결정

1. **비공개 계획:** 관리자에게도 소유자가 아닌 계획은 보이지 않는다.
2. **지도:** 카카오맵 활성화를 진행하지 않기로 한 결정에 따라 키가 필요 없는 Leaflet/OpenStreetMap으로 전환했다.
3. **뉴스:** 전체 기사를 복제하지 않고 제목 필터·URL 중복 제거·원문 링크를 사용한다. 새 뉴스가 없으면 추가 저장 건수가 0일 수 있다.
4. **동기화:** 관리자 화면에서 0건, 부분 저장, API 오류를 구분한다. 수집 결과는 DB에 남지만 관리자 상태 문구 자체는 재시작 후 초기화될 수 있다.
5. **실연동 검증:** 날씨·일출·충전소는 키·활용 승인·제공자 상태에 따라 결과가 달라지므로 코드 구현과 현재 실응답 검증을 구분한다.
