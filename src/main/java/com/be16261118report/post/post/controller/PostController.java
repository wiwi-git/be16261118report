package com.be16261118report.post.post.controller;

import com.be16261118report.global.rsData.RsData;
import com.be16261118report.post.post.dto.PostDto;
import com.be16261118report.post.post.entity.Post;
import com.be16261118report.post.post.service.PostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 글 API
// 조회 API는 RsData로 감싸지 않고 DTO를 그대로 반환한다.
// 작성/수정/삭제 API는 RsData로 반환하고, 상태 코드는 resultCode로 후처리된다.
@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<PostDto> getItems() {
        return postService.findAll()
                .stream()
                .map(PostDto::new)
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public PostDto getItem(@PathVariable long id) {
        return new PostDto(postService.findById(id).orElseThrow());
    }

    record PostWriteReqBody(
            @NotBlank String title,
            @NotBlank String content
    ) {
    }

    @PostMapping
    @Transactional
    public RsData<PostDto> write(@Valid @RequestBody PostWriteReqBody reqBody) {
        Post post = postService.write(reqBody.title(), reqBody.content());

        return new RsData<>(
                "201-1",
                "%d번 글이 작성되었습니다.".formatted(post.getId()),
                new PostDto(post)
        );
    }

    record PostModifyReqBody(
            @NotBlank String title,
            @NotBlank String content
    ) {
    }

    @PutMapping("/{id}")
    @Transactional
    public RsData<PostDto> modify(@PathVariable long id, @Valid @RequestBody PostModifyReqBody reqBody) {
        Post post = postService.findById(id).orElseThrow();

        postService.modify(post, reqBody.title(), reqBody.content());
        // 더티체킹 변경을 즉시 반영해 응답의 수정일(modifyDate)을 최신으로 맞춘다.
        postService.flush();

        return new RsData<>(
                "200-1",
                "%d번 글이 수정되었습니다.".formatted(id),
                new PostDto(post)
        );
    }

    @DeleteMapping("/{id}")
    @Transactional
    public RsData<Void> delete(@PathVariable long id) {
        Post post = postService.findById(id).orElseThrow();

        postService.delete(post);

        return new RsData<>(
                "200-1",
                "%d번 글이 삭제되었습니다.".formatted(id)
        );
    }
}
