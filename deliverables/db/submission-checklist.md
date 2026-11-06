#  DB 프로젝트 제출 점검

| 항목 | 상태 |
| --- | --- |
| Git 프로젝트명 | dbtrip 기준으로 ZIP 프로젝트 폴더 구성. 실제 원격 프로젝트 생성은 제출자가 확인 |
| DB 산출물 파일명 | EnjoyTrip_db.zip |
| 소스코드 파일명 | EnjoyTrip_source.zip |
| .mwb | EnjoyTrip_db.mwb. Workbench 네이티브 생성·재열기, 12개 테이블·다이어그램·FK 검증 |
| .sql | EnjoyTrip_db.sql. 전체 DDL＋공개 관광지 데이터, 빈 DB 복원 확인 |
| .png(ERD) | EnjoyTrip_db.png. 같은 Workbench 모델에서 직접 내보냄 |
| 실행 화면 | 현재 공통 .env·3306/enjoytrip에서 19장 새로 캡처 |
| README 서비스 소개 | 검색→계획→Hot Place→커뮤니티를 실행 화면과 함께 소개 |
| .git·bin 폴더 | ZIP 작성 시 제외. 작업 저장소 자체는 삭제하지 않음 |
| 비밀값·로컬 데이터 | 실제 .env·개인 업로드·로그·DB 백업·target·캐시 제외 |

## ZIP 재생성

프로젝트 루트에서 실행하면 **DB 산출물과 소스코드 ZIP 두 개**를 만든다. 산출물 ZIP은 현재 `deliverables/db`만 최상위로 풀고, 소스 ZIP은 실행에 필요한 Maven 프로젝트만 `dbtrip/`에 담는다. 두 ZIP 모두 `deliverables/back`·`docs/back`을 포함하지 않는다.

```powershell
python scripts/package-submission.py --names 개발자 사용자
```

출력은 Git 제외 폴더 **`submission/db-final/`의 위 두 ZIP**이다. 이전 `submission/` 바로 아래의 혼합 ZIP은 사용하지 않는다. DB ZIP의 `README.md`, 소스 ZIP의 `dbtrip/README.md`부터 확인한다. 압축 무결성·산출물 필수 파일·캡처 19장·소스 파일·설정 예시·각 ZIP의 README 링크·제외 폴더를 검사한다. 실제 비밀값이 제출 후보 파일에서 발견되면 중단한다.

## 실제 제출자가 할 일

1. dbtrip Git 프로젝트 및 팀원의 제출 대상이 맞는지 확인한다.
3. 업로드된 두 파일의 이름·압축 해제·필수 3개 DB 파일과 실제 소스를 확인한다.

