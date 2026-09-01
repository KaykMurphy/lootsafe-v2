package com.lootsafe.service;

import com.lootsafe.dto.request.LogoutRequestDTO;
import com.lootsafe.dto.request.UserRequestDTO;
import com.lootsafe.dto.request.UserUpdateRequestDTO;
import com.lootsafe.dto.response.UserResponseDTO;
import com.lootsafe.entity.User;
import com.lootsafe.enums.UserRole;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.exception.UnauthorizedException;
import com.lootsafe.mapper.UserMapper;
import com.lootsafe.repository.RefreshTokenRepository;
import com.lootsafe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    private static final String MSG_USER_NOT_FOUND = "Usuário não encontrado.";
    private static final String MSG_EMAIL_IN_USE = "Email em uso";
    private static final String MSG_INVALID_CREDENTIALS = "Credenciais inválidas";
    private static final String MSG_UNAUTHORIZED_ACCESS =
            "Você não tem permissão para acessar este recurso.";

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO dto) {

        if (userRepository.existsByEmail(dto.email())){
            throw new BusinessException(MSG_EMAIL_IN_USE);
        }


        User user = userMapper.toEntity(dto);
        user.setRoles(Set.of(UserRole.BUYER, UserRole.SELLER));

        String passwordHash = passwordEncoder.encode(dto.passwordHash());
        user.setPasswordHash(passwordHash);

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public void logout(LogoutRequestDTO request, UUID currentUserId) {

        refreshTokenRepository.deleteByToken(request.refreshToken());

        log.info("Logout realizado e tokens revogados para o usuário ID: {}", currentUserId);

    }

    public UserResponseDTO getUserForUser(UUID id, UUID currentUserId) {
        User currentUser = findEntityById(currentUserId);
        if (!id.equals(currentUserId) && !currentUser.hasRole(UserRole.ADMIN)) {
            throw new UnauthorizedException(MSG_UNAUTHORIZED_ACCESS);
        }
        User target = findEntityById(id);
        return userMapper.toResponse(target);
    }


    public UserResponseDTO authenticateAndReturnUser(String email, String rawPassword) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(MSG_INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())){
            throw new BusinessException(MSG_INVALID_CREDENTIALS);
        }

        return userMapper.toResponse(user);
    }

    public List<UserResponseDTO> findAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    public UserResponseDTO findByEmail(String email) {
        User user = findEntityByEmail(email);
        return userMapper.toResponse(user);
    }

    public User findEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_USER_NOT_FOUND));
    }

    public UserResponseDTO getUserById(UUID id) {
        User user = findEntityById(id);
        return userMapper.toResponse(user);
    }

    public User findEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_USER_NOT_FOUND));
    }

    @Transactional
    public UserResponseDTO updateUser(UUID id, UserUpdateRequestDTO updateUser) {
        User existingUser = findEntityById(id);

        existingUser.setName(updateUser.name());
        existingUser.setPixKey(updateUser.pixKey());

        User savedUser = userRepository.save(existingUser);
        return userMapper.toResponse(savedUser);
    }











}
