package com.lootsafe.controller;

import com.lootsafe.dto.request.LoginRequestDTO;
import com.lootsafe.dto.request.UserRequestDTO;
import com.lootsafe.dto.response.TokenResponse;
import com.lootsafe.dto.response.UserResponseDTO;
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

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequestDTO request) {
        String token = userService.authenticate(request.email(), request.password());
        return ResponseEntity.ok(new TokenResponse(token));
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