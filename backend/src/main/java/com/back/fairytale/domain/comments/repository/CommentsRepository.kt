package com.back.fairytale.domain.comments.repository

import com.back.fairytale.domain.comments.entity.Comments
import com.back.fairytale.domain.fairytale.entity.Fairytale
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param


interface CommentsRepository : JpaRepository<Comments, Long> {
    // 특정 동화(fairytale)에 대한 comments를 조회하는 method
    fun findByFairytale(fairytale: Fairytale, pageable: Pageable): Page<Comments>

    // 특정 동화(fairytale)에 대한 댓글을 계층 구조로 페이징하여 조회
    @EntityGraph(attributePaths = ["user", "fairytale"]) // N+1 문제 해결을 위한 EntityGraph 사용
    @Query ("""
        SELECT c FROM Comments c
        WHERE c.fairytale.id = :fairytaleId
        ORDER BY 
            COALESCE(c.parent.id, c.id) ASC, /* 부모 댓글 기준 정렬 */
            c.parent.id ASC NULLS FIRST, /* 부모 댓글 우선 정렬 */
            c.createdAt ASC /* 같은 부모 내에서는 생성일 기준 정렬 */
    """)
    fun findByFairytaleIdOrderByHierarchy(
        @Param("fairytaleId") fairytaleId: Long,
        pageable: Pageable
    ): Page<Comments>

    // 부모 댓글 ID 리스트에 해당하는 자식 댓글 수를 그룹화하여 조회, N+1 문제 해결
    @Query("""
        SELECT c.parent.id as parentId, COUNT(c) as count 
        FROM Comments c 
        WHERE c.parent.id IN :parentIds 
        GROUP BY c.parent.id
    """)
    fun countChildrenByParentId(@Param("parentIds") parentIds: List<Long>): List<Array<Any>>
}
