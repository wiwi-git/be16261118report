package com.be16261118report.post.postComment.controller;

import com.be16261118report.post.post.entity.Post;
import com.be16261118report.post.post.service.PostService;
import com.be16261118report.post.postComment.entity.PostComment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
}
