package com.tv.tvapi.service;

import com.tv.tvapi.model.Post;
import com.tv.tvapi.model.PostComment;
import com.tv.tvapi.model.PostContent;
import com.tv.tvapi.model.User;
import com.tv.tvapi.repository.PostRepository;
import com.tv.tvapi.repository.PostCommentRepository;
import com.tv.tvapi.repository.PostContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostContentRepository postMediaFileRepo;
    private final PostCommentRepository postCommentRepo;

    public Post getCurrentUserPost(Long postId, User u) {
        return postRepository.findByIdAndUser(postId, u).orElse(null);
    }


    public Post savePostAndFlush(Post post) {
        return postRepository.saveAndFlush(post);
    }

    public Post save(Post post) {
        return postRepository.save(post);
    }

    public PostContent savePostContent(PostContent postContent) {
        return postMediaFileRepo.saveAndFlush(postContent);
    }

    public PostContent getPostMediaFile(Long mediaFileId, Post post) {
        return postMediaFileRepo.findByIdAndPost(mediaFileId, post).orElse(null);
    }

    public List<PostContent> getPostContents(Post post) {
        return postMediaFileRepo.findByPostAndActiveOrderByPos(post, 1);
    }

    public Post getById(Long postId) {
        return postRepository.findById(postId).orElse(null);
    }

    public List<Post> getPosts(User user, Pageable pageable) {
        return postRepository.getPostsNative(user.getId(), pageable);
    }

    public List<Post> getUserStories(User user, Pageable pageable) {
        return postRepository.getUserStoriesNative(user.getId(), pageable);
    }

    public List<PostComment> getPostComments(Post post) {
        return postCommentRepo.findByPost(post);
    }

    public List<Post> getUserPosts(User user, Integer status, Pageable pageable) {
        return postRepository.getUserPostsNative(user.getId(), status, null, 1, pageable);
    }

    public List<Post> getUserPosts(Long userId,Integer type, Pageable pageable) {
        return postRepository.findByUserAndTypeAndStatus(userId,type, pageable);
    }


}
