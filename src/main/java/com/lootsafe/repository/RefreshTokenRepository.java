package com.lootsafe.repository;

import com.lootsafe.entity.RefreshToken;
import com.lootsafe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUser(User user);

    Optional<RefreshToken> findByUserId(UUID userId);

    void deleteByUser(User user);

    void deleteByToken(String token);


}
