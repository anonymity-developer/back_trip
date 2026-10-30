# EnjoyTrip

Servlet/JSP와 **Servlet(Controller) → Service → Dao → Dto** 구조로 구현한 공공데이터 기반 여행 서비스입니다. 요구사항과 설계는 [개발 문서](docs/README.md)를 참고하세요.

## 제출용 산출물

[산출물 README](deliverables/README.md)에 개선된 요구사항 목록, 유즈케이스·클래스 다이어그램, 실제 8080 실행 화면 캡처 17장과 검증 범위를 모았습니다. [전체 화면 캡처](deliverables/screenshots/README.md)도 바로 확인할 수 있습니다.

## 요구 환경

- Java 21, Tomcat 11, MySQL 8. Maven은 포함된 Wrapper가 자동으로 준비합니다.
- 한국관광공사 국문 관광정보 API, 한국천문연구원 출몰시각 API, 한국환경공단 충전소 API, OpenWeather API 키
- 지도 사용 시 브라우저 인터넷 연결(OpenStreetMap 타일 로드)

## 로컬 실행

1. MySQL에서 `src/main/resources/schema.sql`을 실행합니다. 기본 DB 이름은 `enjoytrip`입니다.
2. [.env.example](.env.example)을 참고해 Git에서 제외되는 `.env`에 실제 값을 입력합니다. `replace-me`는 예시값이므로 실제 MySQL·관리자 비밀번호로 바꿉니다. 실제 비밀번호나 API 키를 커밋하지 않습니다.
3. Windows는 `.\run-local.ps1`을 실행합니다. 스크립트가 Maven 빌드와 테스트를 수행하고 `.env`를 Tomcat 프로세스에 전달합니다. macOS/Linux는 `./mvnw clean package` 후 환경 변수를 설정한 Tomcat 11의 `webapps`에 `target/enjoytrip.war`를 배포합니다.
4. `http://localhost:8080/enjoytrip/health`에서 `OK`를 확인한 다음 `/enjoytrip/`에서 홈을 열고, 첫 관리자 계정으로 로그인해 `/enjoytrip/admin`의 관광지 동기화를 시작합니다. 자세한 절차는 [로컬 실행 안내](docs/06-local-run.md)를 참고하세요.

| 환경 변수 | 용도 |
| --- | --- |
| `ENJOYTRIP_DB_URL`, `ENJOYTRIP_DB_USER`, `ENJOYTRIP_DB_PASSWORD` | MySQL JDBC 접속. Tomcat JNDI `jdbc/EnjoyTrip` DataSource가 있으면 이를 우선 사용 |
| `ENJOYTRIP_ADMIN_ID`, `ENJOYTRIP_ADMIN_EMAIL`, `ENJOYTRIP_ADMIN_PASSWORD` | 첫 실행 시 관리자 생성. 이미 존재하면 비밀번호를 변경하지 않음 |
| `ENJOYTRIP_TOUR_API_KEY` | 관광지·사진 수집 |
| `ENJOYTRIP_DATA_GO_API_KEY` | 일출·일몰과 충전소 조회. 각 API 활용 신청이 필요할 수 있음 |
| `ENJOYTRIP_WEATHER_API_KEY` | OpenWeather 현재 날씨·5일 예보 |
| `ENJOYTRIP_UPLOAD_DIR` | Hot Place 사진 저장 디렉터리. Tomcat 프로세스에 쓰기 권한 필요 |

애플리케이션은 DB 테이블을 자동으로 만들지 않습니다. 관리자 계정은 스키마 적용 후 앱을 시작해야 생성됩니다. 외부 API 키가 없으면 해당 수집·부가 정보 기능만 사용할 수 없습니다. 관광지 목록은 마지막으로 저장된 DB 자료를 계속 조회합니다.

## 주요 기능

- 지역·유형·이름별 관광지 검색, 사진·지도·날씨·일출·충전소 조회
- 회원 가입·프로필·로그인·탈퇴와 관리자 비밀번호 초기화
- 날짜·경비·방문 순서를 기록하고 공개 링크로 공유하는 여행 계획
- 사진과 지도 위치를 저장하는 Hot Place
- 관광 뉴스 RSS, 관리자 공지사항, 회원 공유게시판

## 검증

`.\mvnw.cmd test`로 비밀번호 해시와 계획 입력 검증을 실행합니다. 이 작업에서는 WAR 빌드와 단위 테스트 3건, 분리된 MySQL·Tomcat의 주요 HTTP 흐름을 확인했습니다. 실제 공공 API 호출은 서비스 키 설정 후 [검증 계획](docs/04-test-plan.md)에 따라 확인해야 합니다. 현재 상태는 [구현·검증 기록](docs/05-implementation-status.md)에 정리했습니다.

`temp/`는 `.gitignore`에 포함되어 프로젝트 명세서 원본이 Git에 올라가지 않습니다.
