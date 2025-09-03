-- created_at 컬럼에 인덱스 추가 (ORDER BY 성능 향상)
--
-- 실행 방법:
-- 1. 개발 환경 (H2 Database):
--    - H2 Console에 접속: http://localhost:8080/h2-console
--    - 또는 IDE의 Database 도구에서 H2 데이터베이스 연결
--    - 이 SQL 파일 전체를 복사하여 실행
--
-- 2. 배포 환경 (MySQL/PostgreSQL):
--    - MySQL: mysql -u [username] -p [database_name] < optimize_created_at_index.sql
--    - PostgreSQL: psql -U [username] -d [database_name] -f optimize_created_at_index.sql
--    - 또는 배포 서버의 데이터베이스 관리 도구에서 실행
--
-- 주의사항:
-- - 데이터베이스 백업 후 실행 권장
-- - 대용량 테이블의 경우 인덱스 생성 시간이 오래 걸릴 수 있음
CREATE INDEX IF NOT EXISTS idx_fairytale_created_at ON fairytale(created_at DESC);

-- 복합 인덱스 추가 (검색 + 정렬 동시 최적화)
CREATE INDEX IF NOT EXISTS idx_fairytale_title_created_at ON fairytale(title, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_fairytale_content_created_at ON fairytale(content, created_at DESC);