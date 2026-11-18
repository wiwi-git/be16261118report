package com.be16261118report.post.post.service;

import com.be16261118report.post.post.entity.Post;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 글 서비스 CRUD 테스트 (H2 인메모리, 트랜잭션 안에서 실행 후 롤백)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("글을 작성하면 저장되고 id가 부여된다")
    void write() {
        Post post = postService.write("제목", "내용");

        assertThat(post.getId()).isNotNull();
        assertThat(postService.findById(post.getId())).contains(post);
    }

    @Test
    @DisplayName("글 다건 조회는 최신 글(id 큰 순)부터 반환한다")
    void findAll() {
        Post post1 = postService.write("제목1", "내용1");
        Post post2 = postService.write("제목2", "내용2");

        List<Post> posts = postService.findAll();

        assertThat(posts).containsExactly(post2, post1);
    }

    @Test
    @DisplayName("글 단건 조회 - 없는 id면 빈 Optional을 반환한다")
    void findByIdNotFound() {
        assertThat(postService.findById(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    @DisplayName("글을 수정하면 더티체킹으로 DB에 반영된다")
    void modify() {
        Post post = postService.write("제목", "내용");

        postService.modify(post, "새 제목", "새 내용");
        em.flush();
        em.clear();

        Post reloaded = postService.findById(post.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("새 제목");
        assertThat(reloaded.getContent()).isEqualTo("새 내용");
    }

    @Test
    @DisplayName("글을 삭제하면 조회되지 않는다")
    void delete() {
        Post post = postService.write("제목", "내용");
        long before = postService.count();

        postService.delete(post);
        em.flush();
        em.clear();

        assertThat(postService.findById(post.getId())).isEmpty();
        assertThat(postService.count()).isEqualTo(before - 1);
    }

    @Test
    @DisplayName("flush를 호출하면 글에 추가한 댓글에 id가 부여된다")
    void flush() {
        Post post = postService.write("제목", "내용");
        var comment = post.addComment("댓글");

        postService.flush();

        assertThat(comment.getId()).isNotNull();
    }
}
