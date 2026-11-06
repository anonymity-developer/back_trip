# dbtrip 소스코드

EnjoyTrip 소스코드다. Servlet/JSP·JDBC·MySQL로 관광지 탐색, 회원·인증, 여행 계획, Hot Place, 뉴스와 커뮤니티를 구현한다.

배포 ZIP의 `dbtrip/`은 Maven 프로젝트와 실행·테스트 파일을 담는다. DB 모델·전체 데이터 SQL·ERD·서비스 소개·실행 화면은 같은 ZIP의 최상위에 있다.

## 구성

```text
dbtrip/
├── src/                 Java·JSP·JavaScript·SQL 리소스·테스트
├── .mvn/                Maven Wrapper
├── scripts/             공통 설정 로딩·테스트·DB 실행 도구
├── pom.xml
├── mvnw / mvnw.cmd
├── run-local.ps1
└── .env.example
```

## 실행

1. Java 21 이상의 JDK, MySQL 8, Tomcat 11을 준비하고 `JAVA_HOME`을 지정한다.
2. 상위 폴더의 `EnjoyTrip_db.sql`을 새 빈 환경에 실행한다. 기본 DB는 `enjoytrip`이며 기준정보·공개 관광지 데이터를 포함한다. 기존 DB는 백업 후 별도 업그레이드 절차를 따른다.
3. 이 소스의 `.env.example`을 `.env`로 복사하고 DB 접속·관리자 계정·업로드 디렉터리와 사용할 API 키를 설정한다. 실제 `.env`는 제출에 포함하지 않았다.
4. 이 폴더에서 실행한다.

```powershell
.\run-local.ps1
# 기본 경로와 다른 Tomcat을 사용할 때
.\run-local.ps1 -TomcatHome 'C:\설치경로\apache-tomcat-11'
```

5. `http://localhost:8080/enjoytrip/health`에서 `OK`를 확인한다. 다른 포트는 `-Port 18080`처럼 지정한다. 관리자 계정은 `.env`의 값으로 최초 생성된다.

모든 설정은 이 폴더의 `.env` 하나를 사용하고 변경 후 앱을 재시작한다. 외부 키가 없어도 저장된 관광지·회원·계획·커뮤니티는 사용할 수 있다. 날씨·일출·충전소 등 실제 외부 정보는 해당 키·승인·제공자 응답이 필요하다.

## 테스트

```powershell
.\scripts\test.ps1
python scripts/db/setup-test.py --with-attractions
.\scripts\test.ps1 -Integration
python scripts/db/http-smoke.py --base-url http://127.0.0.1:8080/enjoytrip
```

기본 테스트는 8건 통과·DB 통합 1건 생략, 통합 실행은 9건 통과가 기대값이다. DAO 테스트는 같은 설정에서 테스트 DB명만 `enjoytrip_test`로 바꿔 사용한다. HTTP 검증은 실행 중인 앱에 임시 회원·콘텐츠를 만들고 정리하며 PASS 5개가 기대값이다.

이 소스에는 `.git`·`bin`·`target`·실제 `.env`·로그·개인 업로드를 포함하지 않았다. 서비스 소개·화면·모델 설명과 실행 안내는 상위 폴더의 README를 확인한다.
