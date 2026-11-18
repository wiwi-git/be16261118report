package com.be16261118report.post.post.entity;

import com.be16261118report.global.jpa.entity.BaseEntity;
import com.be16261118report.post.postComment.entity.PostComment;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// 글 엔티티
// 댓글은 별도 서비스/레포지토리 없이 글(애그리거트 루트)을 통해서만 추가·수정·삭제한다.
// 트랜잭션 안에서 글의 댓글 목록을 바꾸면 더티체킹 + cascade/orphanRemoval로 DB에 반영된다.
@Entity
public class Post extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @OneToMany(mappedBy = "post", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PostComment> comments = new ArrayList<>();

    protected Post() {
    }

    public Post(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void modify(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public PostComment addComment(String content) {
        PostComment comment = new PostComment(this, content);
        comments.add(comment);
        return comment;
    }

    // 이 글에 달린 댓글 중에서만 찾는다. (다른 글의 댓글 id면 비어 있음)
    public Optional<PostComment> findCommentById(long id) {
        return comments.stream()
                .filter(comment -> comment.getId() != null && comment.getId() == id)
                .findFirst();
    }

    public void modifyComment(PostComment comment, String content) {
        comment.modify(content);
    }

    public boolean removeComment(PostComment comment) {
        return comments.remove(comment);
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    // 밖에서 목록을 직접 건드리지 못하게 읽기 전용으로 반환
    public List<PostComment> getComments() {
        return Collections.unmodifiableList(comments);
    }
}
