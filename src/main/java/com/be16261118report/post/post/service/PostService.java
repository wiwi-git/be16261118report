package com.be16261118report.post.post.service;

import com.be16261118report.post.post.entity.Post;
import com.be16261118report.post.post.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// 글 서비스
// 기본은 읽기 전용 트랜잭션, 쓰기 메서드는 쓰기 트랜잭션으로 동작한다.
// 수정은 save 호출 없이 트랜잭션 안의 더티체킹으로 반영된다.
@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public long count() {
        return postRepository.count();
    }

    public List<Post> findAll() {
        return postRepository.findAllByOrderByIdDesc();
    }

    public Optional<Post> findById(long id) {
        return postRepository.findById(id);
    }

    @Transactional
    public Post write(String title, String content) {
        return postRepository.save(new Post(title, content));
    }

    @Transactional
    public void modify(Post post, String title, String content) {
        post.modify(title, content);
    }

    @Transactional
    public void delete(Post post) {
        postRepository.delete(post);
    }

    // 글에 추가한 댓글처럼 아직 INSERT되지 않은 변경을 즉시 반영해 id를 받을 때 사용
    @Transactional
    public void flush() {
        postRepository.flush();
    }
}
