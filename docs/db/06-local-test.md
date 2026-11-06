# 직접 실행·테스트 안내

**앱과 개발 도구는 프로젝트 루트 `.env` 하나를 사용한다.** API 키·DB 계정·관리자 계정·업로드 경로는 이 파일에서만 관리한다. `target/db/app.env`와 `-EnvFile`은 더 이상 사용하지 않는다.

## 0. 이미 준비된 환경에서는 바로 테스트

현재 앱은 루트 `.env`의 MySQL 3306·`enjoytrip`에 연결되어 있다. 제공 관광지 50,915건을 바탕으로 기준정보·기존 시연 계획을 준비했으므로 초기화·적재를 반복하지 않는다. 공통 설정의 자동 동기화가 활성화되어 있으면 이후 실제 API 데이터로 총수가 증가할 수 있다. 이전부터 있던 `members` 테이블도 보존했다.

```powershell
Invoke-RestMethod http://127.0.0.1:18080/enjoytrip/health
```

`OK`면 [앱](http://127.0.0.1:18080/enjoytrip/)을 바로 열어 5절의 브라우저 확인 순서로 진행한다. 관리자 ID·비밀번호는 루트 `.env`의 `ENJOYTRIP_ADMIN_ID`, `ENJOYTRIP_ADMIN_PASSWORD`를 확인한다.

앱이 꺼졌다면 JDK를 지정하고 다음처럼 시작한다. 18080 대신 기본 8080을 쓰려면 `-Port`를 생략하고 접속 URL도 8080으로 바꾼다.

```powershell
$env:JAVA_HOME='C:/tools/jdk-21'
.\run-local.ps1 -Port 18080
```

위 경로는 예시이므로 실제 설치한 Java 21 이상 JDK의 경로를 사용한다. 프로젝트는 Java 21 대상으로 컴파일한다. PATH의 Java 8은 사용할 수 없다.

## 1. 공통 설정과 서버 상태

`.env`는 Git에서 제외된다. `.env.example`은 설정 이름을 보여주는 예시이며 실제 비밀번호·키를 넣어 커밋하지 않는다. 기존 `.env`가 있으면 예시 파일로 덮어쓰지 않는다.

| 설정 | 용도 |
| --- | --- |
| `ENJOYTRIP_DB_URL/USER/PASSWORD` | 같은 앱 DB 접속. 현재 URL은 localhost:3306/enjoytrip |
| `ENJOYTRIP_ADMIN_ID/EMAIL/PASSWORD` | 최초 관리자 생성. 기존 계정의 비밀번호를 매번 변경하지 않음 |
| `ENJOYTRIP_UPLOAD_DIR` | 실제 사진 파일 위치 |
| `ENJOYTRIP_TOUR_API_KEY` | 관광지·사진 API |
| `ENJOYTRIP_WEATHER_API_KEY` | 날씨 API |
| `ENJOYTRIP_DATA_GO_API_KEY` | 일출·일몰·충전소 API |
| `ENJOYTRIP_SYNC_ENABLED` | 기본 true. false이면 정기·수동 관광지/뉴스 동기화 중지 |

**설정 변경은 앱 재시작 후 반영된다.** API 활용 승인·제공자 오류는 키 존재 여부와 별도로 확인한다. 동기화 설정은 관광지/뉴스 수집에만 적용되며, 날씨·일출·충전소 조회는 해당 키로 요청한다.

```powershell
Get-NetTCPConnection -State Listen -LocalPort 3306,18080 -ErrorAction SilentlyContinue |
    Select-Object LocalAddress,LocalPort,OwningProcess
```

MySQL이 꺼져 있으면 설치된 기존 MySQL 서비스를 시작한다. `mysqld --initialize-insecure`는 새 서버 데이터 폴더를 만들 때만 쓰는 명령이며 기존 DB를 실행하는 명령이 아니다. `data directory has files`, `ibdata1 must be writable` 오류가 나면 초기화·중복 기동을 멈추고 기존 서버 상태부터 확인한다. 데이터 파일을 삭제하지 않는다.

개발 초기에 만들었던 13307 검증 인스턴스는 현재 앱과 무관하다. 현재 앱의 연결 대상은 `.env`를 기준으로 확인한다.

## 2. 신규 DB 또는 기존 구조 업그레이드

**현재 DB에는 이미 적용되어 있어 실행할 필요 없다.** 새 환경의 DB를 준비할 때만 루트 `.env`를 먼저 확인한 뒤 다음을 실행한다. SQL 도구는 같은 `.env`의 계정·호스트·포트를 읽으며 비밀번호를 출력하지 않는다.

```powershell
# 처음 만드는 앱 DB
python scripts/db/run-sql.py --file src/main/resources/schema.sql --file src/main/resources/db/seed/lookup-data.sql
```

기존 앱 테이블이 있다면 앱·동기화를 중지하고 DB·사진을 백업한 뒤 업그레이드를 사용한다. 기존 관광지 id·사용자 콘텐츠는 삭제하지 않는다.

```powershell
python scripts/db/run-sql.py --file src/main/resources/db/migrations/001-db-extension.sql
```

등록되지 않은 기존 지역·유형·구군 코드는 명시적 오류로 중지한다. 임의 삭제나 제약 검사 해제로 우회하지 않는다. DDL은 전체 트랜잭션 롤백 대상이 아니므로 적용 전에 백업한다. SQL 파일의 기본 앱 DB 이름은 `enjoytrip`이다.

## 3. 제공 원본과 관광지 적재

**현재 데이터는 이미 준비되어 있다.** 새 환경에서만 원본 두 SQL을 `temp/trip-schema.sql`, `temp/trip-dump.sql`로 준비하고 다음 순서로 적재한다. 파일명이 다르면 `prepare-source.py`의 `--schema-file`, `--dump-file`로 경로를 지정한다. 별도 설정 파일은 만들지 않는다.

```powershell
python scripts/db/prepare-source.py
python scripts/db/run-sql.py --file target/db/source.sql
python scripts/db/run-sql.py --file src/main/resources/db/import/import-trip-source.sql
```

생성한 원본 로더는 새 `trip_source`에만 적재하며 이미 존재하면 첫 CREATE에서 중지한다. `--force`로 계속 실행하지 않는다. 앱 DB에는 원본을 직접 넣지 않고 명시적 변환·병합 SQL을 사용한다.

새 앱 DB의 초기 기대값은 추가 50,915건·총 50,915건이다. 기존 데이터가 있으면 총수는 다르다. 반복 병합 시 추가·보충 0건과 기존 id/FK 보존을 확인한다. 기존 비어 있지 않은 값은 앞뒤 공백까지 유지한다. 원본 파일은 변경하지 않는다.

## 4. DB 정합성 확인

```powershell
python scripts/db/run-sql.py --file src/main/resources/db/validation/verify-db.sql
```

기준정보는 17/234/8건이고 39번 이름은 ‘음식점’이다. `duplicate_content_id_errors`, `region_errors`, `type_errors`, `gugun_errors`, 방문지·사진 참조 오류와 좌표 오류는 0을 확인한다. 원본 50,915건 외 데이터가 추가되면 총수는 증가할 수 있다.

기존 사용자 데이터가 있는 경우, 고아 0건뿐 아니라 전후의 `plan_stops.id → attraction_id → content_id`와 사진의 같은 매핑도 대조한다. 기존 프로젝트와 무관한 테이블을 삭제하거나 이름이 비슷하다는 이유로 회원 테이블에 자동 병합하지 않는다.

## 5. 앱·회귀 테스트

### 브라우저 확인

1. [상태 확인](http://127.0.0.1:18080/enjoytrip/health): `OK`.
2. [관광지 목록](http://127.0.0.1:18080/enjoytrip/attractions): 지역 17개·유형 8개 및 전체 선택지, 카드 20개.
3. [서울 음식점](http://127.0.0.1:18080/enjoytrip/attractions?region=1&type=39): 다음 페이지에서도 지역·유형 조건과 선택 상태 유지.
4. `/enjoytrip/attractions?region=999`: 400. `/enjoytrip/api/attractions/search?q=서울`: id/title만 가진 최대 10개 JSON.
5. 관광지 상세: 제목·주소·사진·설명·지도 또는 결측 안내. 외부 정보는 공통 `.env`의 키·승인·응답에 따라 표시된다.
6. 직접 회원 가입·로그인·내 정보 수정. 계획을 만들고 관광지 2개 추가→같은 날짜 내 순서 변경→새로고침하여 저장 확인.
7. 계획을 비공개로 바꾼 후 시크릿 창·다른 회원·관리자로 조회하면 404.
8. Hot Place 사진 등록·수정·조회·삭제. 공유글 CRUD와 일반회원 공지 작성 거부 확인.
9. 루트 `.env`의 관리자 계정으로 `/enjoytrip/admin` 확인. 동기화 버튼은 같은 파일의 `ENJOYTRIP_SYNC_ENABLED`를 따른다.

### 자동 테스트

0절처럼 `JAVA_HOME`을 지정한 후 실행한다. 같은 `.env`의 설정을 읽는다.

```powershell
# 단위·대역 테스트: 8건 통과, DB 통합 1건 생략
.\scripts\test.ps1

# DAO 통합 테스트도 실행: 같은 DB 서버·계정으로 테스트 DB만 별도 준비
python scripts/db/setup-test.py --with-attractions
.\scripts\test.ps1 -Integration
```

통합 실행은 총 9건·실패/오류/생략 0이 기대값이다. 별도 env나 계정을 생성하지 않는다. 테스트를 실행하는 동안만 접속 DB명을 `enjoytrip_test`로 바꿔 앱 데이터가 테스트에 의해 삭제되지 않도록 하며, 호스트·인증정보·나머지 옵션은 공통 설정을 재사용한다. 완료 후 테스트용 URL·플래그는 원복한다. DB 계정에는 테스트 DB 생성·사용 권한이 필요하다.

실행 중인 앱에 대한 HTTP 회귀도 같은 `.env`를 사용한다.

```powershell
python scripts/db/http-smoke.py --base-url http://127.0.0.1:18080/enjoytrip
```

기대 결과는 PASS 5개다. 스크립트는 고유한 임시 회원·게시글·계획·사진을 만들고 정리한다. 공통 설정의 관리자 계정으로 초기화·공지 기능을 검증하며, 실제 다른 사용자의 초기화 요청이나 게시글을 선택하지 않는다. 키가 설정된 날씨 호출과 활성화된 동기화는 성공/중지 응답을 강제로 가정하거나 대량 수집을 실행하지 않는다.

## 6. 시연 데이터

기존 공개 시연 계획·방문지 2개·공지·공유글은 공통 DB로 옮겨 두었다. [계획 목록](http://127.0.0.1:18080/enjoytrip/plans)에서 조회할 수 있다. 직접 회원을 만들어 작성한 내용은 앱 DB에 유지된다. Hot Place는 실제 사진을 업로드해야 한다.

`demo-data.sql`은 별도 `enjoytrip_test`에만 사용하는 선택적 시연 자료다. 일반 앱 실행이나 회귀 테스트에는 실행할 필요 없다. 기존 캡처와 당시 검증 결과는 [DB 산출물](../../deliverables/db/README.md)을 참고한다.

## 7. 백업·복구

설정 통합 직전의 기본 DB 백업은 로컬 `target/db/before-shared-env.sql`, 이전 검증 앱 데이터는 `target/db/previous-app-data.sql`에 보관했다. 실제 계정·내용이 포함될 수 있으므로 커밋하지 않는다.

기존 DB 적용 전에는 앱·동기화를 멈추고 DB와 업로드 디렉터리·WAR·설정을 같은 시점으로 백업한다. 인증정보는 `.env`에서만 관리하며 덤프 인수나 로그에 넣지 않는다. PowerShell 텍스트 리다이렉션으로 덤프 인코딩을 바꾸지 말고 mysqldump의 `--result-file` 또는 Python의 바이트 출력을 사용한다.

복원은 새 DB에서 먼저 확인한다. 12개 앱 테이블의 건수·관광지 ID·계획/사진 참조를 비교하고, 실제 환경에는 별도 작업 후 적용한다. 기존 `members` 같은 추가 테이블은 앱 ERD에 없더라도 백업·보존 대상이다.

## 8. 종료·재시작

직접 실행한 앱은 해당 PowerShell의 Ctrl+C로 종료한다. 포트가 이미 사용 중이면 중복 실행하지 않는다. 설정을 바꿨으면 기존 앱을 종료하고 다음 명령으로 재시작한다.

```powershell
.\run-local.ps1 -Port 18080
```

현재는 공통 DB를 사용하므로 이전 안내의 13307 검증 DB 종료 명령으로 앱의 DB를 제어하지 않는다. MySQL은 기존 서비스의 시작·중지 방식으로 관리한다.
