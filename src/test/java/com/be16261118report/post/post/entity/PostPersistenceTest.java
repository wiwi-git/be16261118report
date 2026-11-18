package com.be16261118report.post.post.entity;

import com.be16261118report.post.postComment.entity.PostComment;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

// 글/댓글 엔티티가 JPA로 저장·더티체킹·고아 삭제되는지 검증 (H2 인메모리, 트랜잭션 안에서 실행)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostPersistenceTest {

    @Autowired
    private EntityManager em;

    private Post persistAndReload(Post post) {
        em.persist(post);
        em.flush();
        em.clear();
        return em.find(Post.class, post.getId());
    }

    @Test
    @DisplayName("글을 저장하면 id와 생성일/수정일이 자동으로 채워진다")
    void persist() {
        Post post = persistAndReload(new Post("제목", "내용"));

        assertThat(post.getId()).isNotNull();
        assertThat(post.getCreateDate()).isNotNull();
        assertThat(post.getModifyDate()).isNotNull();
        assertThat(post.getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("트랜잭션 안에서 글을 수정하면 더티체킹으로 반영된다")
    void modifyByDirtyChecking() {
        Post post = persistAndReload(new Post("제목", "내용"));

        post.modify("새 제목", "새 내용");
        em.flush();
        em.clear();

        Post reloaded = em.find(Post.class, post.getId());
        assertThat(reloaded.getTitle()).isEqualTo("새 제목");
        assertThat(reloaded.getContent()).isEqualTo("새 내용");
    }

    @Test
    @DisplayName("저장된 글에 댓글을 추가하면 댓글 저장 없이 더티체킹(cascade)으로 저장된다")
    void addCommentByDirtyChecking() {
        Post post = persistAndReload(new Post("제목", "내용"));

        PostComment comment = post.addComment("댓글");
        em.flush();

        assertThat(comment.getId()).isNotNull();
        assertThat(comment.getCreateDate()).isNotNull();

        em.clear();
        Post reloaded = em.find(Post.class, post.getId());
        assertThat(reloaded.getComments())
                .extracting(PostComment::getContent)
                .containsExactly("댓글");
    }

    @Test
    @DisplayName("글을 통해 댓글을 수정하면 더티체킹으로 반영된다")
    void modifyCommentByDirtyChecking() {
        Post post = new Post("제목", "내용");
        post.addComment("댓글");
        post = persistAndReload(post);

        PostComment comment = post.getComments().getFirst();
        post.modifyComment(comment, "수정된 댓글");
        em.flush();
        em.clear();

        assertThat(em.find(PostComment.class, comment.getId()).getContent()).isEqualTo("수정된 댓글");
    }

    @Test
    @DisplayName("글에서 댓글을 빼면 고아 객체 제거로 DB에서도 삭제된다")
    void removeCommentByOrphanRemoval() {
        Post post = new Post("제목", "내용");
        post.addComment("댓글1");
        post.addComment("댓글2");
        post = persistAndReload(post);

        PostComment target = post.getComments().getFirst();
        post.removeComment(target);
        em.flush();
        em.clear();

        assertThat(em.find(PostComment.class, target.getId())).isNull();
        assertThat(em.find(Post.class, post.getId()).getComments()).hasSize(1);
    }

    @Test
    @DisplayName("글을 삭제하면 달린 댓글도 함께 삭제된다")
    void removePostWithComments() {
        Post post = new Post("제목", "내용");
        post.addComment("댓글");
        post = persistAndReload(post);
        Long commentId = post.getComments().getFirst().getId();

        em.remove(post);
        em.flush();
        em.clear();

        assertThat(em.find(Post.class, post.getId())).isNull();
        assertThat(em.find(PostComment.class, commentId)).isNull();
    }

    @Test
    @DisplayName("글에서 id로 댓글을 찾을 수 있고, 다른 글의 댓글이나 없는 id면 비어 있다")
    void findCommentById() {
        Post post = new Post("제목", "내용");
        post.addComment("댓글");
        Post other = new Post("다른 제목", "다른 내용");
        other.addComment("다른 댓글");
        em.persist(other);
        post = persistAndReload(post);
        other = em.find(Post.class, other.getId());

        PostComment comment = post.getComments().getFirst();
        Long otherCommentId = other.getComments().getFirst().getId();

        assertThat(post.findCommentById(comment.getId())).contains(comment);
        assertThat(post.findCommentById(otherCommentId)).isEmpty();
        assertThat(post.findCommentById(Long.MAX_VALUE)).isEmpty();
    }
}
