package com.topviec.topviec_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.topviec.topviec_be.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt, u.lastLoginIp = :lastLoginIp WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId,
            @Param("lastLoginAt") LocalDateTime lastLoginAt,
            @Param("lastLoginIp") String lastLoginIp);
}