-- 댓글 성능 최적화를 위한 인덱스 생성 (H2 Database)
-- 대댓글 기능 및 계층 구조 조회 최적화
--
-- !! 개발환경(H2) 수동 실행 방법 !!
-- 1. Spring Boot 애플리케이션 실행 (IDE에서 메인 클래스 실행 또는 ./gradlew bootRun)
-- 2. 브라우저에서 H2 Console 접속: http://localhost:8080/h2-console
-- 3. 로그인 화면에서 설정 입력:
--    - JDBC URL: jdbc:h2:./db_dev
--    - User Name: sa
--    - Password: (비워두기)
--    - Connect 버튼 클릭
-- 4. 아래 SQL문들을 복사해서 H2 Console에 붙여넣고 Run 버튼 클릭
-- 5. 인덱스 생성 확인: SHOW INDEXES FROM comments;
--
-- !! 운영환경(PostgreSQL) 실행 방법 !!
-- 
-- [방법 1: 수동 실행 (권장)]
-- 1. PostgreSQL 클라이언트 접속 (pgAdmin, DBeaver, psql 등)
-- 2. 아래 PostgreSQL용 SQL 실행:
--    CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_comments_hierarchy_query ON comments(fairytale_id, parent_id, created_at);
--    CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_comments_parent_count ON comments(parent_id) WHERE parent_id IS NOT NULL;
--    CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_comments_fairytale_parent ON comments(fairytale_id, parent_id);
--    CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_comments_user_created ON comments(user_id, created_at);
-- 3. 인덱스 생성 확인: \d comments (psql) 또는 SELECT * FROM pg_indexes WHERE tablename = 'comments';
--
-- [방법 2: Flyway 자동 실행]
-- 1. application-prod.yml에 Flyway 설정 추가:
--    spring:
--      flyway:
--        enabled: true
--        locations: classpath:db/migration
--        baseline-on-migrate: true
-- 2. 이 파일명을 V1__create_comments_performance_indexes.sql로 변경 (기존 마이그레이션 번호에 맞게)
-- 3. 현재 파일의 SQL을 CONCURRENTLY 키워드 추가한 PostgreSQL 버전으로 수정 (현재 파일의 SQL은 H2용이므로 PostgreSQL에 맞게 변경 필요)
-- 4. 애플리케이션 배포 시 자동 실행됨

-- 1. 계층 구조 조회 최적화 인덱스
-- findByFairytaleIdOrderByHierarchy 쿼리 최적화
CREATE INDEX IF NOT EXISTS idx_comments_hierarchy_query 
ON comments(fairytale_id, parent_id, created_at);

-- 2. 부모 댓글별 자식 수 조회 최적화 인덱스  
-- countChildrenByParentId 쿼리 최적화
CREATE INDEX IF NOT EXISTS idx_comments_parent_count 
ON comments(parent_id);

-- 3. 복합 인덱스 (선택적 최적화)
-- fairytale_id + parent_id 조합으로 특정 동화의 부모/자식 댓글 빠른 조회
CREATE INDEX IF NOT EXISTS idx_comments_fairytale_parent 
ON comments(fairytale_id, parent_id);

-- 4. 사용자별 댓글 조회 최적화 (기존 기능 지원)
-- 사용자가 작성한 댓글 조회 시 성능 향상
CREATE INDEX IF NOT EXISTS idx_comments_user_created 
ON comments(user_id, created_at);