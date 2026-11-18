package com.be16261118report.post.post.entity;

import com.be16261118report.post.postComment.entity.PostComment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 글 엔티티의 도메인 로직 단위 테스트 (DB 없이 순수 자바로 검증)
class PostTest {

    @Test
    @DisplayName("글을 생성하면 제목과 내용이 설정되고 댓글은 비어 있다")
    void create() {
        Post post = new Post("제목", "내용");

        assertThat(post.getTitle()).isEqualTo("제목");
        assertThat(post.getContent()).isEqualTo("내용");
        assertThat(post.getComments()).isEmpty();
    }

    @Test
    @DisplayName("글의 제목과 내용을 수정할 수 있다")
    void modify() {
        Post post = new Post("제목", "내용");

        post.modify("새 제목", "새 내용");

        assertThat(post.getTitle()).isEqualTo("새 제목");
        assertThat(post.getContent()).isEqualTo("새 내용");
    }

    @Test
    @DisplayName("글에 댓글을 추가하면 댓글이 글을 참조하고 글의 댓글 목록에 들어간다")
    void addComment() {
        Post post = new Post("제목", "내용");

        PostComment comment = post.addComment("댓글");

        assertThat(comment.getContent()).isEqualTo("댓글");
        assertThat(comment.getPost()).isSameAs(post);
        assertThat(post.getComments()).containsExactly(comment);
    }

    @Test
    @DisplayName("글에서 댓글을 수정할 수 있다")
    void modifyComment() {
        Post post = new Post("제목", "내용");
        PostComment comment = post.addComment("댓글");

        post.modifyComment(comment, "수정된 댓글");

        assertThat(comment.getContent()).isEqualTo("수정된 댓글");
    }

    @Test
    @DisplayName("글에서 댓글을 삭제하면 댓글 목록에서 빠진다")
    void removeComment() {
        Post post = new Post("제목", "내용");
        PostComment comment1 = post.addComment("댓글1");
        PostComment comment2 = post.addComment("댓글2");

        boolean removed = post.removeComment(comment1);

        assertThat(removed).isTrue();
        assertThat(post.getComments()).containsExactly(comment2);
    }

    @Test
    @DisplayName("글의 댓글 목록은 밖에서 직접 수정할 수 없다")
    void commentsAreReadOnly() {
        Post post = new Post("제목", "내용");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> post.getComments().add(null)
        ).isInstanceOf(UnsupportedOperationException.class);
    }
}
