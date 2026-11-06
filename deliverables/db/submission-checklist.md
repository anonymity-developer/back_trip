#  DB 프로젝트 제출 점검

| 항목 | 상태 |
| --- | --- |
| Git 프로젝트명 | dbtrip 기준으로 ZIP 프로젝트 폴더 구성. 실제 원격 프로젝트 생성은 제출자가 확인 |
| 파일명 | EnjoyTrip_db.zip |
| .mwb | EnjoyTrip_db.mwb. Workbench 네이티브 생성·재열기, 12개 테이블·다이어그램·FK 검증 |
| .sql | EnjoyTrip_db.sql. 전체 DDL＋공개 관광지 데이터, 빈 DB 복원 확인 |
| .png(ERD) | EnjoyTrip_db.png. 같은 Workbench 모델에서 직접 내보냄 |
| 실행 화면 | 현재 공통 .env·3306/enjoytrip에서 19장 새로 캡처 |
| README 서비스 소개 | 검색→계획→Hot Place→커뮤니티를 실행 화면과 함께 소개 |
| .git·bin 폴더 | ZIP 작성 시 제외. 작업 저장소 자체는 삭제하지 않음 |
| 비밀값·로컬 데이터 | 실제 .env·개인 업로드·로그·DB 백업·target·캐시 제외 |

## ZIP 재생성

프로젝트 루트에서 실행한다. 산출물·소스·문서를 복사하고 필수 3개 파일은 `dbtrip/` 최상위에도 둔다. 설정의 실제 비밀값이 제출 후보 파일에 포함되면 중단한다.

```powershell
python scripts/package-submission.py --names 개발자 사용자
```

출력은 Git 제외 폴더 `submission/EnjoyTrip_db.zip`이다. ZIP 무결성·필수 파일·캡처 19장·제외 폴더를 검사한다. ZIP을 열어 `dbtrip/README.md` 또는 `dbtrip/deliverables/db/README.md`부터 확인한다.

## 실제 제출자가 할 일

1. dbtrip Git 프로젝트 및 팀원의 제출 대상이 맞는지 확인한다.
3. 업로드된 파일명·압축 해제·필수 3개 파일을 최종 확인한다.

