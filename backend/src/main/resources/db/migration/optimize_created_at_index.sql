-- created_at 컬럼에 인덱스 추가 (ORDER BY 성능 향상)
CREATE INDEX IF NOT EXISTS idx_fairytale_created_at ON fairytale(created_at DESC);

-- 복합 인덱스 추가 (검색 + 정렬 동시 최적화)
CREATE INDEX IF NOT EXISTS idx_fairytale_title_created_at ON fairytale(title, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_fairytale_content_created_at ON fairytale(content, created_at DESC);