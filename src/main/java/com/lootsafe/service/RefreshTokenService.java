package com.lootsafe.service;

import com.lootsafe.config.JwtProperties;
import com.lootsafe.entity.RefreshToken;
import com.lootsafe.entity.User;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.UnauthorizedException;
import com.lootsafe.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final UserService userService;

    @Transactional
    public RefreshToken createRefreshToken(UUID userId) {

        User user = userService.findEntityById(userId);

        // one device at a time
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs()));

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new UnauthorizedException("Refresh token expirado. Faça login novamente.");
        }
        if (token.isRevoked()) {
            throw new UnauthorizedException("Sessão revogada.");
        }
        return token;
    }

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido."));
    }
}