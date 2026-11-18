package com.be16261118report.post.post.dto;

import com.be16261118report.post.post.entity.Post;

import java.time.LocalDateTime;

// 글 응답 DTO (엔티티를 그대로 노출하지 않기 위함)
public record PostDto(
        long id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        String title,
        String content
) {
    public PostDto(Post post) {
        this(
                post.getId(),
                post.getCreateDate(),
                post.getModifyDate(),
                post.getTitle(),
                post.getContent()
        );
    }
}
