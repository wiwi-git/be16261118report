package com.be16261118report.post.postComment.controller;

import com.be16261118report.post.post.entity.Post;
import com.be16261118report.post.post.service.PostService;
import com.be16261118report.post.postComment.dto.PostCommentDto;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 댓글 API
// 댓글은 별도 서비스/레포지토리 없이 컨트롤러에서 글(Post) 엔티티를 통해 다룬다.
// 모든 메서드는 트랜잭션 안에서 동작하며, 글의 댓글 목록(LAZY)도 트랜잭션 안에서 읽는다.
// 조회 API는 RsData로 감싸지 않고 DTO를 그대로 반환한다.
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
}
