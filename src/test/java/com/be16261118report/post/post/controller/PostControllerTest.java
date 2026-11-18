package com.be16261118report.post.post.controller;

import com.be16261118report.post.post.entity.Post;
import com.be16261118report.post.post.service.PostService;
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

// 글 API 테스트 (H2 인메모리, 트랜잭션 안에서 실행 후 롤백)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class PostControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PostService postService;

    @Test
    @DisplayName("글 다건 조회 - RsData 없이 글 목록을 최신순으로 반환한다")
    void getItems() throws Exception {
        Post post1 = postService.write("제목1", "내용1");
        Post post2 = postService.write("제목2", "내용2");

        mvc.perform(get("/api/v1/posts"))
                .andDo(print())
                .andExpect(handler().handlerType(PostController.class))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(post2.getId()))
                .andExpect(jsonPath("$[0].title").value("제목2"))
                .andExpect(jsonPath("$[0].content").value("내용2"))
                .andExpect(jsonPath("$[0].createDate").exists())
                .andExpect(jsonPath("$[0].modifyDate").exists())
                .andExpect(jsonPath("$[1].id").value(post1.getId()));
    }

    @Test
    @DisplayName("글 단건 조회 - RsData 없이 글 하나를 반환한다")
    void getItem() throws Exception {
        Post post = postService.write("제목", "내용");

        mvc.perform(get("/api/v1/posts/" + post.getId()))
                .andDo(print())
                .andExpect(handler().handlerType(PostController.class))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").doesNotExist())
                .andExpect(jsonPath("$.id").value(post.getId()))
                .andExpect(jsonPath("$.title").value("제목"))
                .andExpect(jsonPath("$.content").value("내용"))
                .andExpect(jsonPath("$.createDate").exists())
                .andExpect(jsonPath("$.modifyDate").exists());
    }

    @Test
    @DisplayName("글 단건 조회 - 없는 글이면 404-1을 RsData로 반환한다")
    void getItemNotFound() throws Exception {
        mvc.perform(get("/api/v1/posts/" + Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("글 작성 - 201-1 RsData와 작성된 글을 반환하고 DB에 저장된다")
    void write() throws Exception {
        long before = postService.count();

        mvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "새 제목", "content": "새 내용"}
                                """))
                .andDo(print())
                .andExpect(handler().handlerType(PostController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value(containsString("번 글이 작성되었습니다.")))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.title").value("새 제목"))
                .andExpect(jsonPath("$.data.content").value("새 내용"));

        assertThat(postService.count()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("글 작성 - 제목/내용이 비어 있으면 400-1을 반환한다")
    void writeInvalid() throws Exception {
        mvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "", "content": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value(containsString("content-NotBlank")))
                .andExpect(jsonPath("$.msg").value(containsString("title-NotBlank")));
    }

    @Test
    @DisplayName("글 수정 - 200-1 RsData와 수정된 글을 반환하고 더티체킹으로 반영된다")
    void modify() throws Exception {
        Post post = postService.write("제목", "내용");

        mvc.perform(put("/api/v1/posts/" + post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "수정 제목", "content": "수정 내용"}
                                """))
                .andDo(print())
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글이 수정되었습니다.".formatted(post.getId())))
                .andExpect(jsonPath("$.data.title").value("수정 제목"))
                .andExpect(jsonPath("$.data.content").value("수정 내용"));

        Post modified = postService.findById(post.getId()).orElseThrow();
        assertThat(modified.getTitle()).isEqualTo("수정 제목");
        assertThat(modified.getContent()).isEqualTo("수정 내용");
    }

    @Test
    @DisplayName("글 수정 - 없는 글이면 404-1을 반환한다")
    void modifyNotFound() throws Exception {
        mvc.perform(put("/api/v1/posts/" + Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "수정 제목", "content": "수정 내용"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("글 삭제 - 200-1 RsData를 반환하고 더 이상 조회되지 않는다")
    void deletePost() throws Exception {
        Post post = postService.write("제목", "내용");

        mvc.perform(delete("/api/v1/posts/" + post.getId()))
                .andDo(print())
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글이 삭제되었습니다.".formatted(post.getId())));

        assertThat(postService.findById(post.getId())).isEmpty();
    }

    @Test
    @DisplayName("글 삭제 - 없는 글이면 404-1을 반환한다")
    void deletePostNotFound() throws Exception {
        mvc.perform(delete("/api/v1/posts/" + Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }
}
