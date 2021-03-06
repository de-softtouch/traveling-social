package com.tv.tvapi.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "user")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String fullName;

    private String phone;

    private String email;

    private String password;

    private String verificationCode;

    private String avt;

    private int active;

    private int status;

    private String bio;

    private String background;

    @Temporal(TemporalType.TIMESTAMP)
    @CreationTimestamp
    private Date createDate;

    @Temporal(TemporalType.TIMESTAMP)
    @UpdateTimestamp
    private Date updateDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    //
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "uploadBy")
    private List<FileUpload> fileUploads = new ArrayList<>();

    public User(Long id, String username, String fullName, String phone, String email, String password, String verificationCode, String avt, int active, int status, Date createDate, Date updateDate, Role role) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.verificationCode = verificationCode;
        this.avt = avt;
        this.active = active;
        this.status = status;
        this.createDate = createDate;
        this.updateDate = updateDate;
        this.role = role;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private Set<Follow> followers = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "follower")
    private Set<Follow> following = new HashSet<>();


    @OneToMany(fetch = FetchType.LAZY, targetEntity = Post.class, mappedBy = "user")
    private List<Post> posts = new ArrayList<>();


    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<PostComment> postComments = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<PostReaction> postReactions = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private Collection<Review> reviews = new ArrayList<>();


    @OneToMany(mappedBy = "user",fetch = FetchType.LAZY)
    private Collection<UserReviewVisit> visitReviewPosts = new HashSet<>();

}
