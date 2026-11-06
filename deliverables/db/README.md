# EnjoyTrip DB 단계 산출물

기존 Servlet/JSP·JDBC 서비스를 유지하면서 제공 관광지 데이터를 연계했다. 기존 관광지의 내부 ID와 사용자 콘텐츠 참조를 보존하고, 시도·구군·유형 기준정보와 복합 구군 FK를 추가했다. 덤프는 신규 관광지와 누락 정보 보충에 사용하며 원본 SQL은 수정하지 않았다.

## 산출물과 명세 대응

| 신규 DB 명세 §1.5 | 실제 자료 |
| --- | --- |
| README | 이 문서, [직접 실행·테스트](../../docs/db/06-local-test.md) |
| ER 다이어그램 | [전체 ERD·데이터 정의](erd.md), [SVG](erd.svg), [Mermaid 편집본](erd.mmd) |
| DB Schema·DDL SQL | [최종 DDL](schema.sql), [실행 원본](../../src/main/resources/schema.sql), [기존 DB 업그레이드](../../src/main/resources/db/migrations/001-db-extension.sql) |
| 구현 소스·실행 화면 | [Java 소스](../../src/main/java/kr/co/enjoytrip/), [테스트 소스](../../src/test/java/kr/co/enjoytrip/), [실제 화면 6장](screenshots/README.md) |

초기·더미 데이터 및 생성형 AI 검토 목표(p.3)를 위한 자료도 제공한다.

- [기준정보 초기 데이터](../../src/main/resources/db/seed/lookup-data.sql): 시도 17·구군 234·유형 8. 승인된 39번 이름 보정 포함.
- [제공 관광지 변환 적재](../../src/main/resources/db/import/import-trip-source.sql), [검증 조회](../../src/main/resources/db/validation/verify-db.sql).
- [검증 DB 준비](../../scripts/db/setup-test.py), [시연 계획·공지·게시글](../../src/main/resources/db/seed/demo-data.sql), [HTTP 시연·회귀 스크립트](../../scripts/db/http-smoke.py).
- [원본 검증 파일 생성](../../scripts/db/prepare-source.py), [UTF-8 SQL 실행](../../scripts/db/run-sql.py), [ERD·데이터 정의 재생성](../../scripts/db/export-model.py).
- [실제 검증 결과·남은 범위](verification.md), [원본 대비 검토·개선 근거](../../docs/db/01-requirements.md#제공-erdddl덤프-정적-검토).

## 실행 요약

검증 환경은 MySQL 8.0.46, Tomcat 11.0.26, Java 25 JDK(`--release 21`)다. 앱 요구 대상은 Java 21 이상이다. 원본 수집과 기존 데이터 변경을 제어하도록 별도 검증 인스턴스를 사용했다.

이미 준비된 작업 폴더에서는 [상태 확인·바로 테스트](../../docs/db/06-local-test.md#0-이미-준비된-환경에서는-바로-테스트)를 먼저 따른다. 아래는 새 환경을 구축하는 순서다.

현재는 프로젝트 루트 `.env` 하나를 사용한다. 같은 파일의 키·DB 계정·관리자 계정을 앱과 도구가 재사용하며 테스트용 설정 파일을 생성하지 않는다. 이전 13307 격리 환경은 최초 검증 기록이고 현재 앱은 기본 DB(3306/enjoytrip)를 사용한다.

1. [상세 안내 1절](../../docs/db/06-local-test.md#1-공통-설정과-서버-상태)에서 공통 `.env`와 MySQL 상태를 확인한다.
2. 원본 두 SQL을 `temp/`에 준비하고 `python scripts/db/prepare-source.py`로 검증용 SQL을 만든다. 원본은 Git에서 제외되어 있으므로 별도 제공받아야 한다.
3. `python scripts/db/run-sql.py --file target/db/source.sql`로 공통 설정의 서버에 새 `trip_source` 원본을 적재한다. 같은 스키마가 있으면 중단한다. 현재 개발 DB에는 이미 관광지가 있어 재적재할 필요 없다.
4. 새 앱 DB라면 DDL·기준정보·변환 적재 SQL을 [상세 안내](../../docs/db/06-local-test.md)에 따라 적용한다. DAO 테스트만 실행할 때는 `python scripts/db/setup-test.py --with-attractions`를 사용하며 별도 env·계정을 만들지 않는다.
5. `JAVA_HOME` 지정 후 `.\run-local.ps1 -Port 18080`으로 실행한다. 키 없이도 저장된 관광지를 조회할 수 있다.
6. 다른 창에서 `python scripts/db/http-smoke.py --base-url http://127.0.0.1:18080/enjoytrip`를 실행한다. 기대 결과는 5개 PASS다. 임시 회원·콘텐츠는 정리된다.
7. 관리자 로그인 정보는 루트 `.env`에서 확인한다. 기존 공개 시연 계획은 기본 DB로 옮겨 두었다. `demo-data.sql`은 별도 테스트 DB용이며 일반 실행에 필요 없다.

신규 운영 DB는 DDL→기준정보 순으로 생성하고, 기존 DB는 백업·앱 중지 후 업그레이드 SQL을 사용한다. 제공 원본의 DROP/컬럼 없는 INSERT를 앱 DB에 직접 실행하지 않는다. 정확한 절차·기대 결과·복구는 [직접 테스트 안내](../../docs/db/06-local-test.md)에 있다.

## 검증 범위

단위·대역·DAO 통합 9건, HTTP 회원·인증·초기화·관광지·계획·Hot Place·공지·공유글·권한 흐름, 12개 테이블 백업 복원, 기존 ID/FK·반복 적재를 확인했다. 실제 기관 API·RSS 수집과 MySQL 8.4 실행은 미검증이다. 키 없는 날씨 503과 주요 DB 화면 유지까지 검증했다.

이 폴더는 저장소 내부 제출 자료다. GitLab 업로드·ZIP 형식·원본 자료 재배포 규칙은 반별 제출 기준에 따라 마지막에 정한다. 실제 계정 비밀번호·키·로컬 백업·원본 덤프를 포함하지 않았다.
