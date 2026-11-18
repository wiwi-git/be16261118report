package com.be16261118report.post.postComment.dto;

import com.be16261118report.post.postComment.entity.PostComment;

import java.time.LocalDateTime;

// 댓글 응답 DTO (엔티티를 그대로 노출하지 않기 위함)
public record PostCommentDto(
        long id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        long postId,
        String content
) {
    public PostCommentDto(PostComment comment) {
        this(
                comment.getId(),
                comment.getCreateDate(),
                comment.getModifyDate(),
                comment.getPost().getId(),
                comment.getContent()
        );
    }
}
