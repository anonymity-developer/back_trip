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

페이즈 2에서 실제 실행 명령·URL·기대 결과를 추가한다. 페이즈 1은 DB 적재 기반이며 화면 코드의 연계 완료를 의미하지 않는다.

## 6. 검증 인스턴스 종료

```powershell
& 'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysqladmin.exe' --no-defaults --host=127.0.0.1 --port=13307 --user=root shutdown
```

다시 테스트할 때는 초기화 없이 서버 시작 명령만 실행한다. 파일은 Git에서 제외된 `target/db/`에 남는다. 실패 결과·원본 해시·적재 건수는 보관하되 비밀번호·서비스 키는 기록하지 않는다.
