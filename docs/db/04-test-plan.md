# DB 확장 검증 계획

## 검증 상태와 환경

**이번 수행 범위:** 두 PDF의 텍스트와 신규 PDF 주요 화면, 제공 ERD 이미지, SQL의 정적 분석, 기존 문서·실제 소스 대조, 계획 문서의 누락·일관성·링크 확인이다. 아래 시나리오는 **후속 구현 후 수행할 검증**이다. DB 접속·SQL 실행·Maven/Tomcat 실행·실제 외부 API 호출은 이번에 수행하지 않았다.

현재 단위 테스트는 `AppConfigTest` 1건, `PasswordHasherTest` 2건, `PlanServiceTest` 1건으로 총 4개 메서드다. 계획 Service 테스트는 잘못된 날짜·예산을 DB 저장 전에 거부하는 경계 검증이며 실제 DAO·DB 통합 검증이 아니다. `target/surefire-reports`와 기존 구현 기록은 과거 결과로만 참고한다.

- Java 21·Maven Wrapper·Tomcat 11·Q5에서 확인한 MySQL 서버를 사용한다. 분석 시 PATH의 Java는 8 설치 경로이므로 먼저 실행 JDK를 확인한다.
- 원본 적재용 격리 인스턴스와 신규 설치/업그레이드 검증용 앱 DB를 준비한다. 실제 사용 중인 DB와 분리되었는지 접속 대상·권한을 확인한다.
- 외부 연동은 고정 정상·빈·오류 응답으로 먼저 검증하고, 키·활용 승인이 준비되면 실제 응답 검증을 별도로 수행한다. 운영 데이터에 가짜 외부 데이터를 상시 혼합하지 않는다.
- 사용자 A/B·관리자, 공개/비공개 계획, 같은 구군 코드가 있는 서로 다른 시도, 사진이 있는/없는 관광지를 준비한다. 이전 ID·content_id·FK·사진 파일 목록을 기준선으로 보관한다.
- 검증 중 정기·관리자 동기화를 통제한다. `AppLifecycle`은 키가 없어도 뉴스를 예약하므로 키 제거만으로 모든 쓰기가 중지되었다고 보지 않는다.

## 초기 적재와 데이터 정합성

| ID·요구 | 시나리오 | 합격 기준 |
| --- | --- | --- |
| D01·DB-01/02 | 빈 앱 DB에 신규 설치 DDL·기준정보 적용, 전체 ERD 대조 | 12개 테이블, 컬럼·타입·NULL·DEFAULT·PK/FK/UNIQUE/INDEX·삭제 정책 일치 |
| D02·DB-02/04 | 기존 9개 테이블 데이터가 있는 검증 복제 DB에 업그레이드 | 기존 ID·회원·콘텐츠 유지, 신규 설치와 같은 목표 구조. CREATE IF NOT EXISTS 반복으로 대체하지 않음 |
| D03·DB-03/04 | 격리 환경에서 Schema→Dump 적재 후 COUNT | 시도 17·구군 234·유형 8·관광지 50,915. 입력 자료·환경·오류 유무 기록 |
| D04·DB-04/NF104 | 관광지 유형별 GROUP BY | 12=13,177 / 14=2,657 / 15=1,688 / 25=1,069 / 28=4,570 / 32=3,583 / 38=8,345 / 39=15,826 |
| D05·DB-02/04 | 원본·변환 데이터의 키 중복·결측 검사 | 관광지 content_id의 NULL·중복 0, 기준정보 자연키 중복 0. 오류를 임의 무시하지 않음 |
| D06·DB-02/NF104 | 시도·유형 FK, 구군이 있는 관광지의 지역·구군 쌍 검사 | 고아 0. 구군 NULL 54건은 허용하고 자동 삭제·임의 배정하지 않음 |
| D07·DB-02 | 같은 구군 코드를 가진 두 시도에서 올바른 쌍과 잘못된 쌍을 입력 | 올바른 쌍은 허용, 없는 지역·구군 쌍은 거부. 단독 구군 UNIQUE로 한 지역을 제거하지 않음 |
| D08·DB-04/NF104 | 주소·설명·사진·NULL·좌표 변환 대조 | 주소 결합·설명 텍스트화·이미지 없음 안내가 규칙과 일치. 좌표 소수 8자리 반올림 오차는 최대 0.000000005도 이내 |
| D09·DB-04/NF104 | 0 좌표 64건, NULL 설명 49,826건, 한글·따옴표·개행·긴 값 검사 | 승인한 결측 정책·기존 값 보호 적용, 원본 추적 가능, 잘림·문자 손상 없음 |
| D10·DB-04/F104/F112 | 기존 id와 덤프 no가 다른 동일 content_id 행, 신규 행, 덤프에 없는 기존 행 병합 | 기존 content_id↔id 및 계획·사진 FK 보존. 신규 행만 새 id, 통합 건수=기존 수＋신규 content_id 수 |
| D11·DB-04 | 같은 초기 자료 두 번 적재, 중간 실패 후 재실행 | 중복 증가 0, 기존 id/FK 변경 0, 실패 구간 진단 가능. 제약 오류를 IGNORE로 숨기지 않음 |
| D12·DB-04/NF104 | 오래된 덤프와 최신 기존 값 충돌, 빈 설명·사진·NULL 원본 갱신 시각 | Q3 정책 준수. 기존 비어 있지 않은 값 보호, 덤프 생성일을 source_updated_at로 기록하지 않음 |
| D13·DB-02 | 참조 중인 기준정보·관광지, 회원·계획 삭제 | 기준정보 RESTRICT, 방문지가 참조하는 관광지 삭제 거부, 회원·계획의 기존 CASCADE, processed_by의 SET NULL 유지 |
| D14·DB-06 | 새 DB·사진 디렉터리에서 시연 초기화 | 실제 개인정보 없는 계정·계획·게시글·대응 사진으로 재현 가능. 더미 계정도 해시 저장 |
| D15·DB-02/04 | 실제 MySQL 버전·설정에서 원본·개선 DDL의 FK 확인 | 비표준 FK 호환성·개선 이력 기록. FOREIGN_KEY_CHECKS 재설정만으로 합격 처리하지 않음 |

다음은 **후속 조회 예시**이며 이번에 실행하지 않았다. 첫 블록은 격리 원본 DB, 두 번째는 B안 앱 DB 기준이다. 실행 전에 연결 대상을 확인한다.

```sql
-- 원본 검증 DB의 기대 건수
SELECT 'sidos' AS entity, COUNT(*) AS cnt FROM trip_source.sidos
UNION ALL SELECT 'guguns', COUNT(*) FROM trip_source.guguns
UNION ALL SELECT 'contenttypes', COUNT(*) FROM trip_source.contenttypes
UNION ALL SELECT 'attractions', COUNT(*) FROM trip_source.attractions;

SELECT content_type_id, COUNT(*) AS cnt
FROM trip_source.attractions GROUP BY content_type_id ORDER BY content_type_id;

-- 구군 NULL은 제외하고 실제 고아 쌍 검사: 기대 0건
SELECT a.no, a.area_code, a.si_gun_gu_code
FROM trip_source.attractions a
LEFT JOIN trip_source.guguns g
  ON g.sido_code = a.area_code AND g.gugun_code = a.si_gun_gu_code
WHERE a.si_gun_gu_code IS NOT NULL AND g.no IS NULL;

SELECT content_id, COUNT(*) AS cnt FROM trip_source.attractions
GROUP BY content_id HAVING content_id IS NULL OR COUNT(*) > 1;
```

```sql
-- B안 앱 DB의 구군·계획·사진 참조: 모두 기대 0건
SELECT a.id FROM enjoytrip.attractions a
LEFT JOIN enjoytrip.guguns g
  ON g.sido_code = a.area_code AND g.gugun_code = a.sigungu_code
WHERE a.sigungu_code IS NOT NULL AND g.no IS NULL;

SELECT s.id FROM enjoytrip.plan_stops s
LEFT JOIN enjoytrip.attractions a ON a.id = s.attraction_id
WHERE a.id IS NULL;

SELECT i.id FROM enjoytrip.attraction_images i
LEFT JOIN enjoytrip.attractions a ON a.id = i.attraction_id
WHERE a.id IS NULL;
```

고아 0건만으로는 잘못된 대상으로 참조가 바뀐 문제를 찾지 못한다. 이전의 `stop.id → attraction_id → content_id`와 사진의 같은 대응을 전환 후 대조한다. DDL은 `SHOW CREATE TABLE`, `SHOW INDEX`, `information_schema.KEY_COLUMN_USAGE`도 확인한다. 서버 버전은 `SELECT VERSION()`, 설정은 `SHOW VARIABLES`로 확인하며 8.0에 없는 8.4 전용 변수를 무조건 참조하지 않는다.

## DAO·Service·API 검증

| ID·요구 | 정상 시나리오 | 경계·오류와 합격 조건 |
| --- | --- | --- |
| A01·F101~F103 | 전체 지역·8개 유형·키워드를 단독/조합 검색, DB 결과와 화면 대조 | 결과 없음 안내, 페이지 이동 시 조건 유지. SQL 삽입 문자열도 파라미터로 처리 |
| A02·F101/NF103 | 20건 표시·21건 선조회, 다음 페이지·동명 관광지 확인 | title,id 정렬 유지, 페이지 간 중복·누락 없음. OFFSET은 표시 수 20 기준 |
| A03·F101~F103 | DB 기준 지역·유형 선택지와 기존 URL 확인 | no가 아닌 코드 사용, 승인한 39번 명칭과 화면 일치. 잘못된 숫자·긴 값·미등록 코드 처리 검증 |
| A04·F104 | `/api/attractions/search?q=`→plan.js 선택→방문지 추가 | 기존 id/title JSON·최대 10건 유지, 빈 q·100자 초과는 400. 목록용 21건으로 늘어나지 않음 |
| A05·F101/F112 | 기존 URL·신규 id의 상세·대표 사진·저장된 갤러리 조회 | no/content_id를 내부 id로 혼동하지 않음. 미존재 404·형식 오류 400, 설명·사진·좌표 없어도 주요 화면 유지 |
| A06·DB-03/05 | LookupDao로 부모 시도별 구군 조회, Controller의 Service 경유 확인 | 같은 이름·코드여도 부모를 혼동하지 않음. InfoController는 읽기 전용 조회를 사용해 관광지 설명 수집·DB 쓰기를 새로 유발하지 않음 |
| A07·F101/DB-05 | API 대역에서 신규/기존/구군 NULL을 포함해 두 번 동기화 | id 유지·중복 0·코드 정합. 미등록 양수 코드·SQL 예외는 상태 기록, 실패 이후를 성공으로 처리하지 않음 |
| A08·DB-05 | upsert 배치 중간에 SQL 오류 주입 | 해당 트랜잭션 변경이 남지 않음. 앞서 커밋된 다른 페이지까지 롤백된다고 주장하지 않음 |
| A09·F111~F114 | 날씨·날짜별 일출/일몰·사진·주변 충전소 조회 | 키 누락·지연·잘못된 XML·좌표 결측 시 해당 정보만 실패 안내. 주요 DB 화면과 기존 JSON 형태 유지 |
| A10·DB-05 | 수동/정기 동기화와 앱 시작 직후 동작 | 전환 중 쓰기 중지, 평상시 중복 실행 방지·상태 표시 확인. 뉴스 예약도 포함 |

## 기존 기능·권한 회귀

| ID·요구 | 시나리오 | 합격 조건 |
| --- | --- | --- |
| R01·F107~F108 | 가입→로그인→프로필 수정→로그아웃→재로그인→탈퇴 | 영속화·중복 거부·탈퇴 후 로그인 불가. 기존 계정·해시 호환 유지 |
| R02·F108 | 초기화 요청→관리자 처리→임시 비밀번호→변경 | 중복 처리·일반회원 관리자 접근 거부, 임시 비밀번호 변경 강제 |
| R03·F104 | 기존 계획 조회, 공개/비공개 계획 작성, 방문지 추가·삭제·순서 변경·재조회 | 참조 content_id·일정·예산·메모·순서 유지. 관리자를 포함한 타인의 비공개 계획 조회 거부 |
| R04·F104 | 순서 변경에 중복·타 계획·누락 ID 전송, 저장 중 오류 | 기존 순서 보존. 날짜 변경·날짜별 재번호화가 구현되었다고 간주하지 않음 |
| R05·F105 | 기존 사진 조회, Hot Place 등록→수정→삭제 | DB 상대 경로·실제 파일 일치, MIME·서명·용량·소유권 검증 유지 |
| R06·F106 | 저장된 뉴스 조회, 같은 RSS 두 번 수집, 전체 RSS 장애 | 기존 목록 유지·URL 중복 방지. 실제 수집 성공은 실제 응답·추가 결과로 기록 |
| R07·F109~F110 | 관리자 공지·회원 게시글 CRUD, 검색·페이지 | 일반회원 공지 변경·타인 공유글 변경 거부, JSP 출력 이스케이프 유지 |
| R08·F107/DB-02 | 탈퇴 후 계획·방문지·Hot Place·공유글·초기화 요청·사진 확인 | 기존 연쇄 삭제 유지, 관광지·기준정보 보존. 사진 삭제 실패는 별도로 기록 |
| R09·공통 | 비회원·일반회원·작성자·타인·관리자의 직접 HTTP 요청 | 서버 권한·CSRF 검증. 기존 리다이렉트·순서 변경 204 계약 유지 |
| R10·NF101~NF102 | 데스크톱·모바일의 메뉴·검색·목록·상세·폼·지도 | 조건·빈 상태·한글·가로 넘침 확인. 기존 URL·컨텍스트 경로 유지 |

## 성능·접속·복구·산출물 검증

| ID·요구 | 검증 | 합격 기준 |
| --- | --- | --- |
| O01·NF103 | 원본 50,915건을 통합한 DB에서 전체/지역/유형/조합/LIKE/후반 페이지의 시간·EXPLAIN 측정 | 데이터량·조건·실행계획·측정값 기록. `%검색어%`가 일반 인덱스로 반드시 빠르다고 단정하지 않음. 수치 목표는 별도 합의 |
| O02·DB-05 | JNDI 우선·환경변수 대체 접속 | 같은 목표 DB 사용. `/health` 200과 별도로 실제 DAO 쿼리의 테이블·컬럼 확인 |
| O03·DB-04 | DDL·DML 중간 실패, 대상 오인 방지, 백업 복원 | 실패 구간 진단 가능. DB·이전 WAR·설정·사진을 일관되게 복원한 뒤 주요 기능 확인. DDL을 ROLLBACK만으로 되돌리지 않음 |
| O04·DB-06/07 | 새 환경에서 설치·초기/시연 데이터 준비·재실행 | 실제 개인정보·비밀값 없이 재현. 재실행 가능 범위와 초기화 대상을 명시 |
| O05·DB-07 | README·전체 ERD·DDL·최종 소스·실행 화면 대조 | 신규 PDF §1.5 항목 충족. temp 제외로 인한 참조 누락 없음. 실행 DB·일시·결과 기록 |

## 결과 기록과 완료 판정

기록에는 아래 항목을 포함한다. DB 자격증명·비밀번호·서비스 키·개인정보는 기록하지 않는다.

| 시나리오 ID | 실시 일시(KST) | 소스·DDL 버전·입력 해시 | 환경·비밀값 없는 DB 식별 | 기대/실측 | 성공/실패/미실시 | 증거·남은 작업 |
| --- | --- | --- | --- | --- | --- | --- |
| 예: D03 | 후속 실시 시 작성 | 대상 기록 | 실제 MySQL 버전·격리 환경 | COUNT 결과 | 이번에는 미실시 | SQL 결과 등 |

필수 F101~F103·F107~F108, DB-01~DB-07, ID/FK 보존·권한·복구의 중요 시나리오가 통과하고 기존 추가·심화 기능에 중대한 회귀가 없어야 완료다. 키 대기 중인 실제 API 검증은 이유·준비사항을 기록하며 미실시를 성공으로 표시하지 않는다.

문서 자체는 F101~F114·NF101~NF104·DB-01~DB-07·필수 산출물의 누락, 실제 파일 대응, 다섯 문서의 링크, Q1/Q2 답변 반영, 미결정 사항의 일관성을 검토한다. 이번 완료는 문서 검토까지이며 위 시나리오의 실행 합격을 의미하지 않는다.
