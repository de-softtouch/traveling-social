package com.tv.tvapi.repository;

import com.tv.tvapi.model.Post;
import com.tv.tvapi.model.PostContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostContentRepository extends JpaRepository<PostContent,Long> {

    Optional<PostContent> findByIdAndPost(Long id, Post post);

    List<PostContent> findByPostAndActiveOrderByPos(Post post,int active);
}
