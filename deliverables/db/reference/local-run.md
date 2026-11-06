# DB 산출물·소스코드 실행 안내

제출 파일은 두 개다. DB 산출물 ZIP은 문서·필수 DB 파일·실행 화면을, 소스코드 ZIP은 실행 가능한 Maven 프로젝트만 담는다. 이전 백엔드 산출물은 어느 ZIP에도 포함하지 않는다.

| 파일 | 내용 |
| --- | --- |
| EnjoyTrip_db.zip | README, MWB·SQL·ERD PNG, 현재 환경 캡처 19장, 검증·실행 안내 |
| EnjoyTrip_source.zip | dbtrip/src, pom.xml, Maven Wrapper, 실행·테스트 스크립트, .env.example |

## 새 환경 실행

1. DB 산출물 ZIP과 소스코드 ZIP을 각각 압축 해제한다. 소스 폴더는 `dbtrip/`이다.
2. Java 21 이상 JDK, MySQL 8, Tomcat 11을 준비하고 `JAVA_HOME`을 JDK 위치로 지정한다. 이번 검증 환경은 Java 25 JDK의 release 21, MySQL 8.0.46, Tomcat 11.0.26이었다.
3. MySQL에서 **새 빈 DB용** `EnjoyTrip_db.sql`을 실행한다. 앱 테이블 12개·기준정보·공개 관광지 51,622건을 준비하며 실제 회원·관리자·개인 콘텐츠는 포함하지 않는다.
4. 소스 폴더의 `.env.example`을 `.env`로 복사하고 DB 접속·관리자 계정·업로드 폴더·사용할 API 키를 설정한다. 참고용 [설정 예시](.env.example)는 실제 비밀번호를 포함하지 않는다.
5. 소스 폴더에서 실행한다. 기본 Tomcat 위치가 다르면 옵션으로 지정한다.

```powershell
.\run-local.ps1
# Tomcat 위치가 다른 경우
.\run-local.ps1 -TomcatHome 'C:\설치경로\apache-tomcat-11'
```

6. `http://localhost:8080/enjoytrip/health`의 `OK`를 확인하고 홈을 연다. 현재 개발 환경은 별도 포트 18080으로 실행되어 있으며 새 환경의 기본 포트는 8080이다.
7. 직접 회원 가입 후 여행 계획·방문지·Hot Place·게시글을 작성한다. 캡처의 시연 자료는 기능 소개를 위해 등록한 것으로 제출 SQL의 개인 데이터로 내보내지 않았다.

API 키가 없어도 저장된 관광지·회원·계획·커뮤니티는 사용할 수 있다. 날씨는 OpenWeather, 일출/일몰·충전소는 공공데이터 키·활용 승인과 실제 응답을 확인한다. 모든 실행 설정은 소스 폴더의 `.env` 하나에 넣고 변경 후 앱을 재시작한다.

## 기존 DB 업그레이드

기존 DB는 백업하고 앱·동기화를 중지한 뒤 [업그레이드 SQL](001-db-extension.sql)을 사용한다. 기존 관광지의 코드가 기준정보와 맞지 않으면 오류로 중지한다. `CREATE TABLE IF NOT EXISTS`만 반복해서는 기존 구조가 업그레이드되지 않는다. 기존 DB에 새 환경용 전체 데이터 SQL을 무작정 중복 실행하지 않는다.

기준정보가 이미 준비되었다면 관광지 동기화와 `content_id` 중복 갱신을 이용한다. 실제 운영 전환에는 DB·사진·WAR·설정을 같은 시점으로 백업하고, 별도 DB에서 복원과 참조를 확인해야 한다.

## 검증 명령

아래 명령은 압축 해제한 소스 폴더에서 실행한다. 실제 `.env`는 제출하지 않는다.

```powershell
# 단위·대역 테스트: 8건 통과, DB 통합 1건 생략
.\scripts\test.ps1

# 같은 서버·계정으로 DAO 전용 테스트 DB 준비 후 총 9건 실행
python scripts/db/setup-test.py --with-attractions
.\scripts\test.ps1 -Integration

# 앱 실행 후 HTTP 주요 흐름 확인: 정상 기대 PASS 5개
python scripts/db/http-smoke.py --base-url http://127.0.0.1:8080/enjoytrip
```

DAO 테스트는 실행 중에만 DB명을 `enjoytrip_test`로 바꾸고 원복한다. 별도 env·계정을 만들지 않으며 DB 계정에는 테스트 DB 생성·사용 권한이 필요하다. HTTP 검증은 고유한 임시 회원·콘텐츠만 생성하고 정리한다.

현재 화면·실제 응답과 제한 사항은 [검증 기록](../verification.md), [전체 캡처](../screenshots/README.md)를 따른다. 충전소는 응답 지연으로 이번 성공 증거에 포함하지 않았다.
