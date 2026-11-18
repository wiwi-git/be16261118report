package com.be16261118report.post.postComment.controller;

import com.be16261118report.global.rsData.RsData;
import com.be16261118report.post.post.entity.Post;
import com.be16261118report.post.post.service.PostService;
import com.be16261118report.post.postComment.dto.PostCommentDto;
import com.be16261118report.post.postComment.entity.PostComment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 댓글 API
// 댓글은 별도 서비스/레포지토리 없이 컨트롤러에서 글(Post) 엔티티를 통해 다룬다.
// 모든 메서드는 트랜잭션 안에서 동작한다.
// - 작성/수정/삭제: 글 엔티티의 addComment / modifyComment / removeComment 호출
//   → 트랜잭션 커밋(또는 flush) 시 더티체킹 + cascade/orphanRemoval로 DB에 반영
// - 조회 API는 RsData로 감싸지 않고, 쓰기 API는 RsData로 반환한다.
@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
public class PostCommentController {

    private final PostService postService;

    public PostCommentController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<PostCommentDto> getItems(@PathVariable long postId) {
        Post post = postService.findById(postId).orElseThrow();

        return post.getComments()
                .stream()
                .map(PostCommentDto::new)
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public PostCommentDto getItem(@PathVariable long postId, @PathVariable long id) {
        Post post = postService.findById(postId).orElseThrow();

        return new PostCommentDto(post.findCommentById(id).orElseThrow());
    }

    record PostCommentWriteReqBody(
            @NotBlank String content
    ) {
    }

    @PostMapping
    @Transactional
    public RsData<PostCommentDto> write(
            @PathVariable long postId,
            @Valid @RequestBody PostCommentWriteReqBody reqBody
    ) {
        Post post = postService.findById(postId).orElseThrow();

        PostComment comment = post.addComment(reqBody.content());
        // 댓글 저장 호출 없이 더티체킹(cascade)으로 INSERT되며, 응답에 id를 담기 위해 flush한다.
        postService.flush();

        return new RsData<>(
                "201-1",
                "%d번 댓글이 작성되었습니다.".formatted(comment.getId()),
                new PostCommentDto(comment)
        );
    }

    record PostCommentModifyReqBody(
            @NotBlank String content
    ) {
    }

    @PutMapping("/{id}")
    @Transactional
    public RsData<PostCommentDto> modify(
            @PathVariable long postId,
            @PathVariable long id,
            @Valid @RequestBody PostCommentModifyReqBody reqBody
    ) {
        Post post = postService.findById(postId).orElseThrow();
        PostComment comment = post.findCommentById(id).orElseThrow();

        post.modifyComment(comment, reqBody.content());
        // 더티체킹 변경을 즉시 반영해 응답의 수정일(modifyDate)을 최신으로 맞춘다.
        postService.flush();

        return new RsData<>(
                "200-1",
                "%d번 댓글이 수정되었습니다.".formatted(id),
                new PostCommentDto(comment)
        );
    }

    @DeleteMapping("/{id}")
    @Transactional
    public RsData<Void> delete(@PathVariable long postId, @PathVariable long id) {
        Post post = postService.findById(postId).orElseThrow();
        PostComment comment = post.findCommentById(id).orElseThrow();

        // 글의 댓글 목록에서 빼면 orphanRemoval로 DELETE된다.
        post.removeComment(comment);

        return new RsData<>(
                "200-1",
                "%d번 댓글이 삭제되었습니다.".formatted(id)
        );
    }
}
