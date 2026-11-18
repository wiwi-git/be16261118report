package com.be16261118report.post.postComment.controller;

import com.be16261118report.post.post.entity.Post;
import com.be16261118report.post.post.service.PostService;
import com.be16261118report.post.postComment.entity.PostComment;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 댓글 API 테스트 (H2 인메모리, 트랜잭션 안에서 실행 후 롤백)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class PostCommentControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PostService postService;

    @Autowired
    private EntityManager em;

    // 영속성 컨텍스트를 비우고 DB에서 다시 읽어 실제 반영 여부를 확인한다.
    private Post reload(Post post) {
        em.flush();
        em.clear();
        return postService.findById(post.getId()).orElseThrow();
    }

    // 글에 댓글을 추가하고 flush로 id를 받는다. (댓글은 글을 통해서만 저장)
    private PostComment addComment(Post post, String content) {
        PostComment comment = post.addComment(content);
        postService.flush();
        return comment;
    }

    @Test
    @DisplayName("댓글 다건 조회 - RsData 없이 해당 글의 댓글 목록을 작성순으로 반환한다")
    void getItems() throws Exception {
        Post post = postService.write("제목", "내용");
        PostComment comment1 = addComment(post, "댓글1");
        PostComment comment2 = addComment(post, "댓글2");
        Post other = postService.write("다른 글", "다른 내용");
        addComment(other, "다른 글의 댓글");

        mvc.perform(get("/api/v1/posts/%d/comments".formatted(post.getId())))
                .andDo(print())
                .andExpect(handler().handlerType(PostCommentController.class))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(comment1.getId()))
                .andExpect(jsonPath("$[0].postId").value(post.getId()))
                .andExpect(jsonPath("$[0].content").value("댓글1"))
                .andExpect(jsonPath("$[0].createDate").exists())
                .andExpect(jsonPath("$[0].modifyDate").exists())
                .andExpect(jsonPath("$[1].id").value(comment2.getId()));
    }

    @Test
    @DisplayName("댓글 다건 조회 - 없는 글이면 404-1을 반환한다")
    void getItemsPostNotFound() throws Exception {
        mvc.perform(get("/api/v1/posts/%d/comments".formatted(Long.MAX_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("댓글 단건 조회 - RsData 없이 댓글 하나를 반환한다")
    void getItem() throws Exception {
        Post post = postService.write("제목", "내용");
        PostComment comment = addComment(post, "댓글");

        mvc.perform(get("/api/v1/posts/%d/comments/%d".formatted(post.getId(), comment.getId())))
                .andDo(print())
                .andExpect(handler().handlerType(PostCommentController.class))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").doesNotExist())
                .andExpect(jsonPath("$.id").value(comment.getId()))
                .andExpect(jsonPath("$.postId").value(post.getId()))
                .andExpect(jsonPath("$.content").value("댓글"));
    }

    @Test
    @DisplayName("댓글 단건 조회 - 다른 글의 댓글이거나 없는 댓글이면 404-1을 반환한다")
    void getItemNotFound() throws Exception {
        Post post = postService.write("제목", "내용");
        Post other = postService.write("다른 글", "다른 내용");
        PostComment otherComment = addComment(other, "다른 글의 댓글");

        mvc.perform(get("/api/v1/posts/%d/comments/%d".formatted(post.getId(), otherComment.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));

        mvc.perform(get("/api/v1/posts/%d/comments/%d".formatted(post.getId(), Long.MAX_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("댓글 작성 - 201-1 RsData를 반환하고, 글에 추가한 댓글이 더티체킹으로 저장된다")
    void write() throws Exception {
        Post post = postService.write("제목", "내용");

        mvc.perform(post("/api/v1/posts/%d/comments".formatted(post.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "새 댓글"}
                                """))
                .andDo(print())
                .andExpect(handler().handlerType(PostCommentController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value(containsString("번 댓글이 작성되었습니다.")))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.postId").value(post.getId()))
                .andExpect(jsonPath("$.data.content").value("새 댓글"));

        assertThat(reload(post).getComments())
                .extracting(PostComment::getContent)
                .containsExactly("새 댓글");
    }

    @Test
    @DisplayName("댓글 작성 - 내용이 비어 있으면 400-1, 없는 글이면 404-1을 반환한다")
    void writeFail() throws Exception {
        Post post = postService.write("제목", "내용");

        mvc.perform(post("/api/v1/posts/%d/comments".formatted(post.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value(containsString("content-NotBlank")));

        mvc.perform(post("/api/v1/posts/%d/comments".formatted(Long.MAX_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "새 댓글"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("댓글 수정 - 200-1 RsData를 반환하고 더티체킹으로 반영된다")
    void modify() throws Exception {
        Post post = postService.write("제목", "내용");
        PostComment comment = addComment(post, "댓글");

        mvc.perform(put("/api/v1/posts/%d/comments/%d".formatted(post.getId(), comment.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "수정된 댓글"}
                                """))
                .andDo(print())
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 댓글이 수정되었습니다.".formatted(comment.getId())))
                .andExpect(jsonPath("$.data.content").value("수정된 댓글"));

        assertThat(reload(post).findCommentById(comment.getId()))
                .get()
                .extracting(PostComment::getContent)
                .isEqualTo("수정된 댓글");
    }

    @Test
    @DisplayName("댓글 수정 - 다른 글의 댓글이면 404-1을 반환한다")
    void modifyNotFound() throws Exception {
        Post post = postService.write("제목", "내용");
        Post other = postService.write("다른 글", "다른 내용");
        PostComment otherComment = addComment(other, "다른 글의 댓글");

        mvc.perform(put("/api/v1/posts/%d/comments/%d".formatted(post.getId(), otherComment.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "수정된 댓글"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("댓글 삭제 - 200-1 RsData를 반환하고 고아 객체 제거로 DB에서 삭제된다")
    void deleteComment() throws Exception {
        Post post = postService.write("제목", "내용");
        PostComment comment1 = addComment(post, "댓글1");
        addComment(post, "댓글2");

        mvc.perform(delete("/api/v1/posts/%d/comments/%d".formatted(post.getId(), comment1.getId())))
                .andDo(print())
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 댓글이 삭제되었습니다.".formatted(comment1.getId())));

        Post reloaded = reload(post);
        assertThat(em.find(PostComment.class, comment1.getId())).isNull();
        assertThat(reloaded.getComments())
                .extracting(PostComment::getContent)
                .containsExactly("댓글2");
    }

    @Test
    @DisplayName("댓글 삭제 - 없는 댓글이면 404-1을 반환한다")
    void deleteCommentNotFound() throws Exception {
        Post post = postService.write("제목", "내용");

        mvc.perform(delete("/api/v1/posts/%d/comments/%d".formatted(post.getId(), Long.MAX_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }
}
