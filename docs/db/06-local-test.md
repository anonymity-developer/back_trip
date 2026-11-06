# 직접 실행·테스트 안내

프로젝트 루트의 PowerShell에서 실행한다. 실제 사용 중인 3306 DB를 자동 변경하는 스크립트는 제공하지 않는다. 아래는 별도 13307 포트의 검증용 MySQL 기준이다. 기존 DB 적용은 백업·앱/동기화 중지 후 동일한 업그레이드 절차로 수행한다.

## 1. 검증용 MySQL 시작

Python 3, MySQL 8.0 또는 8.4의 서버·클라이언트가 필요하다. 다음은 현재 설치 경로와 저장소 경로 기준이다. 데이터 디렉터리 초기화는 **처음 한 번만** 한다. 이미 디렉터리에 DB 파일이 있으면 초기화 명령을 반복하지 않는다.

```powershell
New-Item -ItemType Directory -Force target/db/mysql-data | Out-Null
& 'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysqld.exe' --no-defaults --initialize-insecure --basedir='C:/Program Files/MySQL/MySQL Server 8.0' --datadir='C:/tools/workspace/backtrip/target/db/mysql-data' --console
```

첫 창에서 다음 서버를 실행하고 창을 유지한다. 비밀번호 없는 root는 이 로컬 검증 인스턴스의 초기 계정이며 기존 MySQL 계정과 별개다. 접속은 127.0.0.1로 제한한다.

```powershell
& 'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysqld.exe' --no-defaults --basedir='C:/Program Files/MySQL/MySQL Server 8.0' --datadir='C:/tools/workspace/backtrip/target/db/mysql-data' --bind-address=127.0.0.1 --port=13307 --mysqlx=OFF --max-allowed-packet=64M --console
```

다른 PowerShell 창에서 이후 명령을 실행한다. MySQL 클라이언트가 PATH에 없다면 `run-sql.py`에 `--mysql '전체 mysql.exe 경로'`를 추가한다. 서버 설치 경로나 작업 폴더가 다르면 위 경로를 실제 위치에 맞춘다.

## 2. 신규 DB 또는 기존 구조 업그레이드

**처음 만드는 앱 DB:**

```powershell
python scripts/db/run-sql.py --port 13307 --file src/main/resources/schema.sql --file src/main/resources/db/seed/lookup-data.sql
```

**기존 구조의 검증 복제 DB:** 위 신규 설치 대신 다음을 사용한다. 업그레이드 파일은 기준정보 생성·입력을 포함하므로 별도 초기화가 필요 없다. 기존 id·회원·계획·사진을 삭제하지 않는다.

```powershell
python scripts/db/run-sql.py --port 13307 --file src/main/resources/db/migrations/001-db-extension.sql
```

정상 결과는 기준정보 테이블 3개와 관광지 FK 3개 추가다. 기존 관광지의 지역·유형·구군 코드가 기준정보에 없으면 명시적 오류로 멈춘다. 잘못된 코드를 삭제하거나 제약 검사를 꺼서 우회하지 말고 데이터를 확인한다. 실패 전에 생성된 기준정보는 남을 수 있으며 문제 해결 후 재실행할 수 있다. DDL 전체가 롤백되는 구조는 아니다.

## 3. 제공 원본과 관광지 적재

`temp/trip-schema.sql`, `temp/trip-dump.sql` 두 파일이 필요하다. 원본은 수정하지 않는다. 생성기는 복합 구군 FK·키, DROP 제거, 접속 utf8mb4를 적용한 검증용 파일을 만든다.

```powershell
python scripts/db/prepare-source.py
python scripts/db/run-sql.py --port 13307 --file target/db/source.sql
python scripts/db/run-sql.py --port 13307 --file src/main/resources/db/import/import-trip-source.sql
```

`trip_source`이 이미 있으면 원본 로드는 첫 CREATE SCHEMA에서 중지한다. `--force`로 계속 실행하지 않는다. 원본이 이미 정상 적재되어 있다면 source.sql 재실행을 생략하고 변환 적재만 실행한다.

새 앱 DB에서는 최초 `added_attractions=50915`, `total_attractions=50915`가 기대값이다. 기존 관광지와 함께 적재하면 총수는 달라진다. 개발 검증에서는 기존 전용 관광지 1건을 남겨 총 50,916건을 확인했다.

변환 적재 명령을 한 번 더 실행한다. 정상 기대값은 `added_attractions=0`, `fill_operations=0`, 총수와 기존 id/FK 유지다. 기존의 비어 있지 않은 제목·주소·설명·사진·원본 갱신 시각은 유지한다. 기존 구군이 비어 있으면 같은 시도일 때만 보충하고, 좌표는 둘 다 NULL인 경우 유효한 원본 좌표 쌍으로 보충한다.

## 4. DB 정합성 확인

```powershell
python scripts/db/run-sql.py --port 13307 --file src/main/resources/db/validation/verify-db.sql
```

확인할 결과는 다음과 같다.

- 기준정보: 시도 17, 구군 234, 유형 8. 39번 이름은 ‘음식점’.
- `duplicate_content_id_errors`, `region_errors`, `type_errors`, `gugun_errors`, 계획·사진 참조 오류는 모두 0.
- 새 덤프 적재 기준 `coordinate_errors=0`. 기존 유효하지 않은 좌표가 있으면 별도 데이터 점검이 필요하며 기존 값을 강제로 덮어쓰지 않는다.
- 마지막 FK 목록에서 관광지의 시도·유형·복합 구군 FK, 기존 계획·사진 FK를 확인한다.

기존 데이터 보존은 MySQL Workbench에서 적재 전후 다음 값도 비교한다. 아래 예시 id는 실제 기존 행의 값으로 바꾼다. 참조 오류가 0이어도 잘못된 관광지를 가리킬 수 있으므로 content_id까지 대조한다.

```sql
SELECT id,content_id,title,address,description,image_url,source_updated_at
FROM enjoytrip.attractions WHERE id=42;
SELECT s.id,s.attraction_id,a.content_id
FROM enjoytrip.plan_stops s JOIN enjoytrip.attractions a ON a.id=s.attraction_id;
SELECT i.id,i.attraction_id,a.content_id,i.image_url
FROM enjoytrip.attraction_images i JOIN enjoytrip.attractions a ON a.id=i.attraction_id;
```

## 5. 앱·회귀 테스트

먼저 위 3절까지 원본 `trip_source`을 준비한 상태에서 앱 전용 테스트 DB·계정을 만든다. 다음 명령은 `enjoytrip_test`만 준비하며 기존 `.env`를 바꾸지 않는다. root 인증이 필요하면 해당 터미널의 `MYSQL_PWD`에 직접 설정하고 작업 후 제거한다. 비밀번호를 명령 인수에 넣지 않는다.

```powershell
python scripts/db/setup-test.py --port 13307 --import-source
```

생성된 `target/db/app.env`에 전용 DB 계정·관리자 계정·업로드 디렉터리와 `ENJOYTRIP_SYNC_ENABLED=false`가 저장된다. 무작위 로컬 테스트 비밀번호는 화면에 출력하지 않는다. 파일을 로컬 편집기로 열어 확인하고 Git에 추가하지 않는다. 이미 생성한 파일은 삭제하지 말고 같은 계정으로 재사용한다. 계정만 남기고 설정 파일을 잃어버린 경우에는 검증용 MySQL에서 `enjoytrip_verify` 계정을 정리하거나 비밀번호를 맞춰야 한다.

**단위 테스트:** Java 21 이상의 JDK로 `JAVA_HOME`을 지정하고 실행한다. 아래 경로는 현재 설치된 STS의 Java 25이며 Java 21 바이트코드로 빌드한다. Java 21 JDK가 있으면 그 경로를 사용해도 된다.

```powershell
$env:JAVA_HOME='C:/tools/app/sts-5.4.0.RELEASE/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_25.0.4.v20260826-0822/jre'
$env:JAVA_TOOL_OPTIONS="-Duser.home=$env:USERPROFILE"
.\mvnw.cmd -B -ntp "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" test
```

기본 실행의 기대 결과는 8건 통과·DB 통합 테스트 1건 생략이다. **실제 DB 통합 테스트**를 포함하려면 설정을 이 터미널에만 읽고 실행한다.

```powershell
foreach($entry in Get-Content -Encoding UTF8 target/db/app.env) {
    if($entry -match '^([^=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1],$Matches[2],'Process')
    }
}
$env:ENJOYTRIP_DB_TEST='true'
.\mvnw.cmd -B -ntp "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" test package
Remove-Item Env:ENJOYTRIP_DB_TEST
```

기대 결과는 총 9건·실패 0·오류 0·생략 0이다. 통합 테스트는 접속 DB명이 `enjoytrip_test`인지 먼저 확인한다. 구군 NULL→정상 코드 변경, 관광지 id·설명 보존, 잘못된 구군이 포함된 배치 전체 롤백을 확인하고 자체 테스트 행을 정리한다.

**앱 시작:** 새 PowerShell에서 같은 `JAVA_HOME`을 지정한 다음 실행한다. 이 창을 유지한다.

```powershell
.\run-local.ps1 -Port 18080 -EnvFile target/db/app.env
```

다른 창에서 자동 HTTP 회귀를 실행한다. 별도 라이브러리 설치 없이 Python 표준 라이브러리를 사용한다. 임시 회원·계획·게시글·사진을 실제 생성하고 마지막에 탈퇴·삭제하므로 반드시 검증용 앱에 실행한다.

```powershell
python scripts/db/http-smoke.py --base-url http://127.0.0.1:18080/enjoytrip
```

정상 결과는 4개 `PASS` 줄과 종료 코드 0이다. 검색·응답 건수, CSRF·관리자/작성자 권한, 비공개 계획, 순서 변경, 파일 업로드·삭제, 탈퇴 연쇄 삭제를 포함한다. 외부 키가 비어 있어 날씨 API의 503은 이 검증에서 예상한 결과다.

브라우저에서 직접 확인할 순서는 다음과 같다.

1. `http://127.0.0.1:18080/enjoytrip/health`: `OK`.
2. `/enjoytrip/attractions`: 지역 17개·유형 8개와 전체 선택지가 있고, 관광지 카드가 20개 표시된다.
3. `/enjoytrip/attractions?region=1&type=39`: 서울 음식점만 표시된다. 다음 페이지를 눌러 지역·유형 조건과 선택 상태가 유지되는지 확인한다.
4. `/enjoytrip/attractions?region=999`: HTTP 400. `/enjoytrip/api/attractions/search?q=서울`: id/title만 가진 최대 10개 JSON.
5. 관광지 카드 상세: 제목·주소·사진·지도 또는 결측 안내가 나온다. 덤프 신규 데이터의 갱신 시각은 ‘원본 갱신 시각 미제공’으로 표시된다. 외부 키가 없는 날씨·일출·충전소의 이용 불가 안내는 주요 화면을 막지 않는다.
6. 직접 회원 가입·로그인 후 내 정보 수정. 여행 계획을 만들고 ‘서울’로 방문지 2개를 추가한 뒤 순서를 바꾸고 새로고침한다. 같은 날짜의 방문 순서와 선택한 관광지가 유지되어야 한다.
7. 계획을 비공개로 바꾸고 주소를 시크릿 창에서 연다. 404가 기대값이다. 다른 회원·관리자에게도 비공개다.
8. Hot Place에서 JPG/PNG/WebP 사진을 등록하고 상세·사진을 확인한 뒤 삭제한다. 본인 게시글 CRUD와 일반회원의 공지 작성 거부도 확인한다.
9. `target/db/app.env`의 관리자 ID·비밀번호로 로그인해 `/enjoytrip/admin`을 연다. 검증 설정에서는 동기화 중지 안내와 비활성 버튼이 표시된다. 활성화는 설정의 `ENJOYTRIP_SYNC_ENABLED=true`로 바꾸고 앱을 재시작해야 하며, 실제 API 검증 시에만 키를 설정한다.

테스트 데이터·DB를 유지한 채 화면 테스트를 계속할 수 있다. 종료는 앱 실행 창의 Ctrl+C다. 개발자가 남긴 자동 검증 인스턴스가 18080을 사용 중이면 먼저 종료하거나 `-Port 18081`로 시작하고 모든 URL·HTTP 스크립트의 포트를 동일하게 바꾼다.

## 6. 검증 인스턴스 종료

```powershell
& 'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysqladmin.exe' --no-defaults --host=127.0.0.1 --port=13307 --user=root shutdown
```

다시 테스트할 때는 초기화 없이 서버 시작 명령만 실행한다. 파일은 Git에서 제외된 `target/db/`에 남는다. 실패 결과·원본 해시·적재 건수는 보관하되 비밀번호·서비스 키는 기록하지 않는다.
