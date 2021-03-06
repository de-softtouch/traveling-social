
package com.tv.tvapi.helper;

import com.tv.tvapi.dto.UserDto;
import com.tv.tvapi.model.FileUpload;
import com.tv.tvapi.model.Follow;
import com.tv.tvapi.model.User;
import com.tv.tvapi.request.BaseParamRequest;
import com.tv.tvapi.request.RegistrationRequest;
import com.tv.tvapi.request.UserUpdateRequest;

import com.tv.tvapi.exception.FileNotFoundException;
import com.tv.tvapi.exception.FileUploadException;
import com.tv.tvapi.response.*;
import com.tv.tvapi.service.*;
import com.tv.tvapi.utilities.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.core.io.FileUrlResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.stream.Collectors;

@Component("UserHelper")
@RequiredArgsConstructor
@Slf4j
public class UserHelper {

    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final ModelMapper modelMapper;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final FollowService followService;
    private final JwtService jwtService;

    public ResponseEntity<?> getUsers() {
        List<User> users = userService.getUsers();
        List<UserDto> data = users.stream().map(user ->
                modelMapper.map(user, UserDto.class)
        ).collect(Collectors.toList());
        return ResponseEntity.ok(data);
    }

    public ResponseEntity<?> searchingUsers(Map<String, String> params) {
        BaseParamRequest baseParam = new BaseParamRequest(params);
        String pUsername = params.get("username");
        String pFullName = params.get("fullName");
        String pPhone = params.get("phone");
        String pEmail = params.get("email");
        String username = null;
        String fullName = null;
        String email = null;
        String phone = null;
        if (!ValidationUtil.isNullOrBlank(pUsername))
            username = "%" + pUsername.trim() + "%";

        if (!ValidationUtil.isNullOrBlank(pFullName))
            fullName = "%" + pFullName.trim() + "%";

        if (!ValidationUtil.isNullOrBlank(pEmail))
            email = "%" + pEmail.trim() + "%";

        if (!ValidationUtil.isNullOrBlank(pPhone))
            phone = "%" + pPhone.trim() + "%";

        Pageable pageable = baseParam.toPageRequest();

        List<User> users = userService.search(username, fullName, phone, email, 1, pageable);

        User user = userService.getCurrentUser();
        List<UserInfoResponse> data = users.stream()
                .map(u -> {
                    UserInfoResponse userInfoResponse = modelMapper.map(u, UserInfoResponse.class);
                    userInfoResponse.setIsFollowed(followService.isFollowed(u, user, 1));
                    return userInfoResponse;
                })
                .collect(Collectors.toList());
        return BaseResponse.success(data, "Searching users success!");
    }

    public ResponseEntity<?> registrationAccount(RegistrationRequest registrationRequest) {
        String usernameRequest = registrationRequest.getUsername().trim();
        String emailRequest = registrationRequest.getEmail().trim();
        String passwordRequest = registrationRequest.getPassword().trim();

        if (userService.existByEmail(emailRequest))
            return BaseResponse.conflict("Email has been used");
        if (userService.existByUsername(usernameRequest))
            return BaseResponse.conflict("Username has been used");
        if (passwordRequest.length() < 6) {
            return BaseResponse.badRequest("Password is not valid");
        }
        User user = new User();
        try {
            String verificationCode = userService.generateUniqueCode();
            mailService.sendVerificationCode(emailRequest, verificationCode);
            user.setVerificationCode(verificationCode);
        } catch (MessagingException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        user.setUsername(usernameRequest);
        user.setEmail(emailRequest);
        user.setPassword(passwordEncoder.encode(passwordRequest));
        user.setActive(0);
        user.setRole(roleService.getMemberRole());
        userService.save(user);
        return BaseResponse.success(null, "Registration user success!");
    }


    public ResponseEntity<?> updateAvt(MultipartFile file) {
        try {
            FileUploadResponse fileUploadResponse = fileStorageService.saveImage(file);
            //save user avt
            User user = userService.getCurrentUser();
            user.setAvt(fileUploadResponse.getName());
            userService.save(user);
            return BaseResponse.success(fileUploadResponse, "Update avatar success!");
        } catch (FileUploadException e) {
            return BaseResponse.badRequest(e.getMessage());
        }
    }

    public ResponseEntity<?> updateBackground(MultipartFile file) {
        try {
            FileUploadResponse fileUploadResponse = fileStorageService.saveImage(file);
            //save user avt
            User user = userService.getCurrentUser();
            user.setBackground(fileUploadResponse.getName());
            userService.save(user);
            return BaseResponse.success(fileUploadResponse, "Update background success!");
        } catch (FileUploadException e) {
            return BaseResponse.badRequest(e.getMessage());
        }
    }

    public ResponseEntity<?> getImage(String fileName) {
        //check file upload info exist in db
        FileUpload fileUpload = fileStorageService.getFileUpload(fileName);
        if (fileUpload != null && fileUpload.getActive() == 1
                && (fileUpload.getContentType().equals("image/jpeg") || fileUpload.getContentType().equals("image/png"))) {
            //return file as resource
            try {
                File file = fileStorageService.getFileFromStorage(fileName);
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(new FileUrlResource(file.getPath()));
            } catch (FileNotFoundException | MalformedURLException e) {
                e.printStackTrace();
                return BaseResponse.badRequest(e.getMessage());
            }
        }

        return BaseResponse.badRequest("Could not find file with name : " + fileName);
    }

    public ResponseEntity<?> verificationAccount(String code) {
        User user = userService.getByCode(code);
        if (user == null)
            return BaseResponse.badRequest("Code is not valid");
        user.setVerificationCode(null);
        user.setActive(1);
        userService.save(user);
        String accessToken = jwtService.generateToken(user, 15);
        String refreshToken = jwtService.generateToken(user, 30);
        return BaseResponse.
                success(Map.of("accessToken", accessToken, "refreshToken", refreshToken),
                        "Active account success!");

    }

    public ResponseEntity<?> updateCurrentUserInfo(UserUpdateRequest userUpdateRequest) {
        String username = userUpdateRequest.getUsername();
        String fullName = userUpdateRequest.getFullName();
        User currentUser = userService.getCurrentUser();
        if (!currentUser.getUsername().equals(username)
                && userService.existByUsername(username))
            currentUser.setUsername(username);
        currentUser.setFullName(fullName);
        userService.save(currentUser);
        return BaseResponse.success(null, "update current user info success!");
    }

    public ResponseEntity<?> validationInput(String input, String value) {
        if (!ValidationUtil.isNullOrBlank(input)) {
            value = value.trim();
            if (ValidationUtil.isNullOrBlank(value)) {
                return BaseResponse.badRequest(input + " may not be blank!");
            }
            switch (input) {
                case "username": {
                    if (value.length() < 4 || value.length() > 15) {
                        return BaseResponse.badRequest("username must between 4 and 15");
                    } else if (userService.existByUsername(value)) {
                        return BaseResponse.conflict("username has been used!");
                    }
                    break;
                }
                case "email": {
                    if (ValidationUtil.isEmail(value)) {
                        if (userService.existByEmail(value)) {
                            return BaseResponse.conflict("email has been used");
                        }
                        break;
                    } else {
                        return BaseResponse.badRequest("email is not valid!");
                    }
                }
                case "phone": {
                    break;
                }
                default:
                    return ResponseEntity.ok().build();
            }
        }
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> getFollowingUsers(Map<String, String> param) {
        BaseParamRequest baseParam = new BaseParamRequest(param);
        List<Follow> follows = followService.getFollowingUsers(userService.getCurrentUser().getId(), baseParam.toPageRequest());
        List<UserInfoResponse> rs = follows.stream()
                .map(f -> modelMapper.map(f.getUser(), UserInfoResponse.class))
                .collect(Collectors.toList());
        return BaseResponse.success(rs, "get following users success!");
    }

    public ResponseEntity<?> getFollowers(Map<String, String> param) {
        BaseParamRequest baseParamRequest = new BaseParamRequest(param);
        List<Follow> follows = followService
                .getFollowers(userService.getCurrentUser().getId(), baseParamRequest.toPageRequest());
        List<UserInfoResponse> rs = follows.stream()
                .map(f -> modelMapper.map(f.getFollower(), UserInfoResponse.class))
                .toList();
        return BaseResponse.success(rs, "get followers success!");
    }

    public ResponseEntity<?> followUser(Long userId) {
        User followed = userService.getById(userId);
        User currentUser = userService.getCurrentUser();
        if (!Objects.equals(currentUser.getId(), userId) && followed != null) {
            Follow follow;
            if ((follow = followService.getByUserAndFollower(followed, currentUser)) == null) {
                follow = new Follow();
                follow.setFollower(currentUser);
                follow.setUser(followed);
            }
            follow.setActive(1);
            follow.setStatus(1);
            followService.saveAndFlush(follow);
            return BaseResponse.success(null, "Following user " + userId + " request success!");
        }
        return BaseResponse.badRequest("Can not follow user with id: " + userId);
    }

    public ResponseEntity<?> unFollowUser(Long id) {
        log.info("USER : " + id);
        log.info("FOLLOWER : " + userService.getCurrentUser().getId());
        Follow follow = followService.getByUserAndFollower(userService.getById(id), userService.getCurrentUser());
        if (follow != null) {
            follow.setActive(0);
            follow.setStatus(0);
            followService.saveAndFlush(follow);
            return BaseResponse.success(null, "unfollow user with id: " + id + "  success!");
        }
        return BaseResponse.badRequest("Can not unfollow user with id: " + id);
    }

    public ResponseEntity<?> getCurrentUserDetail() {
        User user = userService.getById(userService.getCurrentUser().getId());
        UserDetailResponse detail = modelMapper.map(user, UserDetailResponse.class);
        int followerCounts = followService.countFollowers(user, 1);
        int followingCounts = followService.countFollowingUsers(user, 1);
        detail.setFollowerCounts(followerCounts);
        detail.setFollowingCounts(followingCounts);
        return BaseResponse.success(detail, "get current user detail success!");
    }


    public ResponseEntity<?> getUserProfile(Long userId) {
        User user = userService.getById(userId, 1);
        if (user == null)
            return BaseResponse.badRequest("Can not find user with id: " + userId);
        UserProfileResponse userInfoResponse = modelMapper.map(user, UserProfileResponse.class);
        int followerCounts = followService.countFollowers(user, 1);
        int followingCounts = followService.countFollowingUsers(user, 1);
        userInfoResponse.setFollowerCounts(followerCounts);
        userInfoResponse.setFollowingCounts(followingCounts);
        userInfoResponse.setIsFollowed(followService.isFollowed(user, userService.getCurrentUser(), 1));
        return BaseResponse.success(userInfoResponse, "Get user with id: " + userId + " success");
    }

    public ResponseEntity<?> getTopUsers(Map<String, String> param) {
        Pageable pageable = new BaseParamRequest(param).toPageRequest();
        List<User> users = userService.getTopActiveUsers(pageable);
        List<UserInfoResponse> rs = users.stream()
                .map(u -> modelMapper.map(u, UserInfoResponse.class))
                .collect(Collectors.toList());
        return BaseResponse.success(rs, "get top posts users success");

    }


}
