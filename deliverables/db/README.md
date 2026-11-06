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

1. [상세 안내 1절](../../docs/db/06-local-test.md#1-검증용-mysql-시작)에 따라 격리 MySQL을 13307에 시작한다.
2. 원본 두 SQL을 `temp/`에 준비하고 `python scripts/db/prepare-source.py`로 검증용 SQL을 만든다. 원본은 Git에서 제외되어 있으므로 별도 제공받아야 한다.
3. `python scripts/db/run-sql.py --port 13307 --file target/db/source.sql`로 새 `trip_source`에 원본을 적재한다. 같은 스키마가 이미 있으면 중단한다.
4. `python scripts/db/setup-test.py --port 13307 --import-source`로 전용 `enjoytrip_test`와 로컬 설정을 준비한다.
5. `JAVA_HOME` 지정 후 `.\run-local.ps1 -Port 18080 -EnvFile target/db/app.env`로 앱을 실행한다. 외부 키가 없어도 저장된 50,915건을 검색할 수 있다.
6. 다른 창에서 `python scripts/db/http-smoke.py --base-url http://127.0.0.1:18080/enjoytrip`를 실행한다. 기대 결과는 5개 PASS다. 임시 회원·콘텐츠는 정리된다.
7. 공개 시연 데이터를 남기려면 앱 최초 실행 후 `python scripts/db/run-sql.py --port 13307 --file src/main/resources/db/seed/demo-data.sql`을 실행한다. 계획 id가 출력된다. 관리자 로그인 정보는 로컬 `target/db/app.env`에서 확인한다.

신규 운영 DB는 DDL→기준정보 순으로 생성하고, 기존 DB는 백업·앱 중지 후 업그레이드 SQL을 사용한다. 제공 원본의 DROP/컬럼 없는 INSERT를 앱 DB에 직접 실행하지 않는다. 정확한 절차·기대 결과·복구는 [직접 테스트 안내](../../docs/db/06-local-test.md)에 있다.

## 검증 범위

단위·대역·DAO 통합 9건, HTTP 회원·인증·초기화·관광지·계획·Hot Place·공지·공유글·권한 흐름, 12개 테이블 백업 복원, 기존 ID/FK·반복 적재를 확인했다. 실제 기관 API·RSS 수집과 MySQL 8.4 실행은 미검증이다. 키 없는 날씨 503과 주요 DB 화면 유지까지 검증했다.

이 폴더는 저장소 내부 제출 자료다. GitLab 업로드·ZIP 형식·원본 자료 재배포 규칙은 반별 제출 기준에 따라 마지막에 정한다. 실제 계정 비밀번호·키·로컬 백업·원본 덤프를 포함하지 않았다.
