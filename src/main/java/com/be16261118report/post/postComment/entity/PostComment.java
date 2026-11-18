package com.be16261118report.post.postComment.entity;

import com.be16261118report.global.jpa.entity.BaseEntity;
import com.be16261118report.post.post.entity.Post;
import jakarta.persistence.*;

// 댓글 엔티티
// 댓글의 생성·수정·삭제는 Post의 addComment / modifyComment / removeComment를 통해서 한다.
@Entity
public class PostComment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Post post;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    protected PostComment() {
    }

    public PostComment(Post post, String content) {
        this.post = post;
        this.content = content;
    }

    public void modify(String content) {
        this.content = content;
    }

    public Post getPost() {
        return post;
    }

    public String getContent() {
        return content;
    }
}
