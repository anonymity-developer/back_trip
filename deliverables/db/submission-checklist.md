# 배포 파일 점검

| 항목 | 확인 내용 |
| --- | --- |
| DB 자료 | EnjoyTrip_db.mwb, EnjoyTrip_db.sql, EnjoyTrip_db.png |
| 실행 화면 | 공통 .env의 실제 앱에서 촬영한 19장 |
| 서비스 소개 | README에 검색·계획·Hot Place·커뮤니티 화면과 사용 흐름 설명 |
| 소스코드 | Java·JSP·정적 자원·SQL·테스트·Maven Wrapper·실행 스크립트 |
| 설정 | .env.example만 포함. 실제 .env·API 키·비밀번호 제외 |
| 제외 파일 | .git·bin·target·로그·백업·개인 업로드·캐시 |

## ZIP 생성

프로젝트 루트에서 실행한다.

```powershell
python scripts/package-submission.py
```

Git 제외 경로 `submission/db-final/EnjoyTrip_db.zip` 하나를 만든다. 최상위에는 DB 자료·서비스 소개·실행 화면·검증 안내를, `dbtrip/`에는 실행 가능한 Maven 프로젝트를 담는다. 이전 백엔드 자료는 포함하지 않는다.

패키징 도구는 압축 무결성·필수 DB 파일·실행 화면 19장·소스 구성·ZIP 안의 Markdown 상대 링크·제외 대상을 검사한다. 실제 비밀값이 후보 파일에서 발견되면 생성을 중단한다.

## 압축 해제 확인

1. EnjoyTrip_db.zip을 새 폴더에 압축 해제한다.
2. 최상위의 MWB·SQL·ERD PNG와 README의 화면을 연다.
3. dbtrip/의 .env.example을 참고해 .env를 설정하고 README에 따라 빌드·실행한다.
4. DB 복원·API·기능 검증의 실제 확인 범위는 [검증 기록](verification.md)을 참고한다.
