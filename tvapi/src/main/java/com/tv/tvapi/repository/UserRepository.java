package com.tv.tvapi.repository;

import com.tv.tvapi.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByVerificationCode(String code);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Query(nativeQuery = true,
            value = "SELECT u.* FROM user u WHERE (:username IS NULL OR u.username LIKE :username) " +
                    "AND (:fullName IS NULL OR u.full_name LIKE :fullName) " +
                    "AND (:phone IS NULL OR u.phone LIKE :phone) " +
                    "AND (:email IS NULL OR u.email LIKE :email) " +
                    "AND (u.active = :active)")
    List<User> searchUsersNative(@Param("username") String username,
                                 @Param("fullName") String fullName,
                                 @Param("phone") String phone,
                                 @Param("email") String email,
                                 @Param("active") int active,
                                 Pageable pageable);
}