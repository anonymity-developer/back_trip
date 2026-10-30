# Windows 로컬 실행

## 1. MySQL 준비

MySQL Workbench에서 관리자 계정(보통 `root`)으로 접속해 `src/main/resources/schema.sql`을 열고 전체 실행한다. 이 파일이 `enjoytrip` 데이터베이스와 테이블을 만든다.

가장 간단한 로컬 확인 방법은 프로젝트 루트의 `.env`에서 다음 값을 실제 MySQL 로그인 정보로 바꾸는 것이다.

```dotenv
ENJOYTRIP_DB_URL=jdbc:mysql://localhost:3306/enjoytrip?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
ENJOYTRIP_DB_USER=root
ENJOYTRIP_DB_PASSWORD=실제_MySQL_root_비밀번호
ENJOYTRIP_ADMIN_PASSWORD=새로_정한_관리자_비밀번호
```

`replace-me`는 작동하는 비밀번호가 아니다. 별도 `enjoytrip` DB 계정을 사용하려면 MySQL에서 그 계정을 만들고 `enjoytrip` 데이터베이스에 권한을 부여한 뒤, 같은 계정명과 비밀번호를 `.env`에 적어야 한다. `.env`는 Git에서 제외된다.

## 2. 빌드와 실행

프로젝트 루트에서 PowerShell을 열고 실행한다.

```powershell
.\run-local.ps1
```

`run-local.ps1`은 Maven 빌드와 테스트를 실행하고 `.env`를 읽어 **그 Tomcat 프로세스에만** 환경 변수로 전달한다. PowerShell 창에서 `Get-ChildItem Env:ENJOYTRIP_*`를 실행해도 값이 보이지 않는 것은 정상이다. `http://localhost:8080/enjoytrip/health`가 `OK`이면 DB 연결이 정상이다. 홈은 `http://localhost:8080/enjoytrip/`이다. 종료는 실행 창에서 Ctrl+C를 누른다.

PowerShell에서 스크립트 실행 정책 오류가 나면 이번 실행에만 적용되는 `powershell -NoProfile -ExecutionPolicy Bypass -File .\run-local.ps1`을 사용한다.

## 3. 증상별 확인

- 홈은 열리지만 목록이나 버튼에서 500 오류: `/health`를 확인한다. 503이면 Tomcat 로그 `target/local-tomcat-8080/logs/localhost.YYYY-MM-DD.log`의 DB 접속 오류를 확인한다.
- `Access denied for user 'enjoytrip'@'localhost'`: `.env`의 DB 사용자·비밀번호와 MySQL에 생성된 계정이 다르다. 기본 예시의 `replace-me`를 실제 비밀번호로 바꾼다.
- `Unknown database 'enjoytrip'` 또는 테이블 없음: `schema.sql`을 실행한다.
- 한글 깨짐: 수정된 WAR를 다시 빌드하고 Tomcat을 재시작한다. 공통 JSP 헤더는 UTF-8로 읽도록 설정되어 있다.
- 이미지 업로드 실패: `ENJOYTRIP_UPLOAD_DIR`의 폴더가 실제로 존재하고 Tomcat을 실행한 사용자에게 쓰기 권한이 있는지 확인한다.
- 지도 로딩 실패: 지도를 표시하는 브라우저가 인터넷에 연결되어 있는지 확인한다. Leaflet 파일은 앱에서 제공하며 OpenStreetMap 지도 타일만 브라우저에서 가져온다. 카카오맵 활성화, JavaScript 키, 도메인 등록은 필요하지 않다.
