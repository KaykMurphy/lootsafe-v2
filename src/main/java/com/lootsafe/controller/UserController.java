package com.lootsafe.controller;

import com.lootsafe.dto.request.LoginRequestDTO;
import com.lootsafe.dto.request.TokenRefreshRequestDTO;
import com.lootsafe.dto.request.UserRequestDTO;
import com.lootsafe.dto.response.TokenResponse;
import com.lootsafe.dto.response.UserResponseDTO;
import com.lootsafe.entity.RefreshToken;
import com.lootsafe.entity.User;
import com.lootsafe.service.JwtService;
import com.lootsafe.service.RefreshTokenService;
import com.lootsafe.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequestDTO request) {

        UserResponseDTO user = userService.authenticateAndReturnUser(request.email(), request.password());

        String accessToken = jwtService.generateToken(user.id(), user.email(), user.roles());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.id());

        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken.getToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid TokenRefreshRequestDTO request) {

        RefreshToken validToken = refreshTokenService.verifyExpiration(
                refreshTokenService.findByToken(request.refreshToken())
        );

        User user = validToken.getUser();

        String newAccessToken = jwtService.generateToken(user.getId(), user.getEmail(), user.getRoles());

        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

        return ResponseEntity.ok(new TokenResponse(newAccessToken, newRefreshToken.getToken()));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO register(@RequestBody @Valid UserRequestDTO request) {
        return userService.createUser(request);
    }

    @GetMapping("/{id}")
    public UserResponseDTO getUserById(@PathVariable UUID id) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.principal.toString()")
    public UserResponseDTO updateUser(@PathVariable UUID id,
                                      @RequestBody @Valid UserRequestDTO request,
                                      @AuthenticationPrincipal UUID currentUserId) {
        return userService.updateUser(id, request);
    }
}