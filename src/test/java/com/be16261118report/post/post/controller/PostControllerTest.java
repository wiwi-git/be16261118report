package com.be16261118report.post.post.controller;

import com.be16261118report.post.post.entity.Post;
import com.be16261118report.post.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
