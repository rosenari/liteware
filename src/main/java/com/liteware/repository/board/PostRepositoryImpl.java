package com.liteware.repository.board;

import com.liteware.model.dto.PostSearchCriteria;
import com.liteware.model.entity.board.Post;
import com.liteware.model.entity.board.Board;
import com.liteware.model.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {
    
    private final EntityManager entityManager;
    
    @Override
    public Page<Post> searchWithCriteria(PostSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        
        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Post> countRoot = countQuery.from(Post.class);
        countQuery.select(cb.count(countRoot));
        
        List<Predicate> countPredicates = buildPredicates(criteria, cb, countRoot);
        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }
        
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();
        
        // Data query
        CriteriaQuery<Post> dataQuery = cb.createQuery(Post.class);
        Root<Post> dataRoot = dataQuery.from(Post.class);
        
        // Fetch joins for eager loading
        dataRoot.fetch("writer", JoinType.LEFT);
        dataRoot.fetch("board", JoinType.LEFT);
        
        dataQuery.select(dataRoot);
        
        List<Predicate> dataPredicates = buildPredicates(criteria, cb, dataRoot);
        if (!dataPredicates.isEmpty()) {
            dataQuery.where(cb.and(dataPredicates.toArray(new Predicate[0])));
        }
        
        // Apply sorting
        applySorting(criteria, cb, dataQuery, dataRoot, pageable);
        
        // Execute query with pagination
        List<Post> posts = entityManager.createQuery(dataQuery)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        
        return new PageImpl<>(posts, pageable, totalCount);
    }
    
    @Override
    public SearchResult searchWithStats(PostSearchCriteria criteria, Pageable pageable) {
        Page<Post> posts = searchWithCriteria(criteria, pageable);
        
        // Calculate statistics
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        
        // Notice count
        CriteriaQuery<Long> noticeCountQuery = cb.createQuery(Long.class);
        Root<Post> noticeRoot = noticeCountQuery.from(Post.class);
        noticeCountQuery.select(cb.count(noticeRoot));
        List<Predicate> noticePredicates = buildPredicates(criteria, cb, noticeRoot);
        noticePredicates.add(cb.equal(noticeRoot.get("isNotice"), true));
        noticeCountQuery.where(cb.and(noticePredicates.toArray(new Predicate[0])));
        Long noticeCount = entityManager.createQuery(noticeCountQuery).getSingleResult();
        
        // Secret count
        CriteriaQuery<Long> secretCountQuery = cb.createQuery(Long.class);
        Root<Post> secretRoot = secretCountQuery.from(Post.class);
        secretCountQuery.select(cb.count(secretRoot));
        List<Predicate> secretPredicates = buildPredicates(criteria, cb, secretRoot);
        secretPredicates.add(cb.equal(secretRoot.get("isSecret"), true));
        secretCountQuery.where(cb.and(secretPredicates.toArray(new Predicate[0])));
        Long secretCount = entityManager.createQuery(secretCountQuery).getSingleResult();
        
        // Attachment count (posts with attachments)
        CriteriaQuery<Long> attachmentCountQuery = cb.createQuery(Long.class);
        Root<Post> attachmentRoot = attachmentCountQuery.from(Post.class);
        attachmentCountQuery.select(cb.countDistinct(attachmentRoot));
        Join<Post, ?> attachmentJoin = attachmentRoot.join("attachments", JoinType.INNER);
        List<Predicate> attachmentPredicates = buildPredicates(criteria, cb, attachmentRoot);
        attachmentCountQuery.where(cb.and(attachmentPredicates.toArray(new Predicate[0])));
        Long attachmentCount = entityManager.createQuery(attachmentCountQuery).getSingleResult();
        
        return new SearchResult(posts, posts.getTotalElements(), noticeCount, 
                               secretCount, attachmentCount);
    }
    
    private List<Predicate> buildPredicates(PostSearchCriteria criteria, 
                                           CriteriaBuilder cb, 
                                           Root<Post> root) {
        List<Predicate> predicates = new ArrayList<>();
        
        // 기본적으로 삭제되지 않은 게시글만
        if (criteria.getIsDeleted() == null || !criteria.getIsDeleted()) {
            predicates.add(cb.equal(root.get("isDeleted"), false));
        }
        
        // 키워드 검색
        if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
            String keyword = "%" + criteria.getKeyword().trim() + "%";
            
            if (criteria.getSearchField() == null || 
                criteria.getSearchField() == PostSearchCriteria.SearchField.ALL) {
                predicates.add(cb.or(
                    cb.like(root.get("title"), keyword),
                    cb.like(root.get("content"), keyword)
                ));
            } else if (criteria.getSearchField() == PostSearchCriteria.SearchField.TITLE) {
                predicates.add(cb.like(root.get("title"), keyword));
            } else if (criteria.getSearchField() == PostSearchCriteria.SearchField.CONTENT) {
                predicates.add(cb.like(root.get("content"), keyword));
            } else if (criteria.getSearchField() == PostSearchCriteria.SearchField.WRITER) {
                Join<Post, User> writerJoin = root.join("writer", JoinType.LEFT);
                predicates.add(cb.like(writerJoin.get("name"), keyword));
            }
        }
        
        // 게시판 필터
        if (criteria.getBoardId() != null) {
            predicates.add(cb.equal(root.get("board").get("boardId"), criteria.getBoardId()));
        }
        
        // 작성자 필터
        if (criteria.getWriterId() != null) {
            predicates.add(cb.equal(root.get("writer").get("userId"), criteria.getWriterId()));
        }
        
        // 작성자 이름 필터
        if (criteria.getWriterName() != null && !criteria.getWriterName().trim().isEmpty()) {
            Join<Post, User> writerJoin = root.join("writer", JoinType.LEFT);
            predicates.add(cb.like(writerJoin.get("name"), "%" + criteria.getWriterName() + "%"));
        }
        
        // 부서 필터
        if (criteria.getDepartmentId() != null) {
            Join<Post, User> writerJoin = root.join("writer", JoinType.LEFT);
            predicates.add(cb.equal(writerJoin.get("department").get("deptId"), criteria.getDepartmentId()));
        }
        
        // 날짜 범위 필터
        if (criteria.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.getStartDate()));
        }
        if (criteria.getEndDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.getEndDate()));
        }
        
        // 공지사항 필터
        if (criteria.getIsNotice() != null) {
            predicates.add(cb.equal(root.get("isNotice"), criteria.getIsNotice()));
        }
        
        // 비밀글 필터
        if (criteria.getIsSecret() != null) {
            predicates.add(cb.equal(root.get("isSecret"), criteria.getIsSecret()));
        }
        
        // 첨부파일 포함 여부
        if (criteria.getHasAttachment() != null && criteria.getHasAttachment()) {
            Subquery<Long> attachmentSubquery = cb.createQuery(Long.class).subquery(Long.class);
            Root<Post> subRoot = attachmentSubquery.from(Post.class);
            attachmentSubquery.select(subRoot.get("postId"));
            subRoot.join("attachments", JoinType.INNER);
            attachmentSubquery.where(cb.equal(subRoot.get("postId"), root.get("postId")));
            
            predicates.add(cb.exists(attachmentSubquery));
        }
        
        // 최소 조회수
        if (criteria.getMinViewCount() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("viewCount"), criteria.getMinViewCount()));
        }
        
        // 최소 좋아요 수
        if (criteria.getMinLikeCount() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("likeCount"), criteria.getMinLikeCount()));
        }
        
        return predicates;
    }
    
    private void applySorting(PostSearchCriteria criteria, 
                             CriteriaBuilder cb, 
                             CriteriaQuery<Post> query, 
                             Root<Post> root,
                             Pageable pageable) {
        List<Order> orders = new ArrayList<>();
        
        // 공지사항을 항상 상단에 표시
        orders.add(cb.desc(root.get("isNotice")));
        
        // 사용자 정의 정렬
        if (criteria.getSortType() != null) {
            switch (criteria.getSortType()) {
                case LATEST:
                    orders.add(cb.desc(root.get("createdAt")));
                    break;
                case OLDEST:
                    orders.add(cb.asc(root.get("createdAt")));
                    break;
                case VIEW_COUNT:
                    orders.add(cb.desc(root.get("viewCount")));
                    break;
                case LIKE_COUNT:
                    orders.add(cb.desc(root.get("likeCount")));
                    break;
                case COMMENT_COUNT:
                    // 댓글 수로 정렬하려면 서브쿼리가 필요
                    // 여기서는 간단히 createdAt으로 대체
                    orders.add(cb.desc(root.get("createdAt")));
                    break;
                default:
                    orders.add(cb.desc(root.get("createdAt")));
            }
        } else if (pageable.getSort().isSorted()) {
            // Pageable에서 정렬 정보가 있으면 적용
            pageable.getSort().forEach(order -> {
                if (order.isAscending()) {
                    orders.add(cb.asc(root.get(order.getProperty())));
                } else {
                    orders.add(cb.desc(root.get(order.getProperty())));
                }
            });
        } else {
            // 기본 정렬: 최신순
            orders.add(cb.desc(root.get("createdAt")));
        }
        
        query.orderBy(orders);
    }
}