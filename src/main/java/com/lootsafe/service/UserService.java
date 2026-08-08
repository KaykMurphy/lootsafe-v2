package com.lootsafe.service;

import com.lootsafe.dto.request.UserRequestDTO;
import com.lootsafe.dto.response.UserResponseDTO;
import com.lootsafe.entity.User;
import com.lootsafe.enums.UserRole;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.mapper.UserMapper;
import com.lootsafe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private static final String MSG_USER_NOT_FOUND = "Usuário não encontrado.";

    public UserResponseDTO createUser(UserRequestDTO dto) {

        if (userRepository.existsByEmail(dto.email())){
            throw new BusinessException("Email em uso");
        }


        User user = userMapper.toEntity(dto);
        user.setRoles(Set.of(UserRole.BUYER, UserRole.SELLER));

        String passwordHash = passwordEncoder.encode(dto.passwordHash());
        user.setPasswordHash(passwordHash);

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    public User authenticateAndReturnUser(String email, String rawPassword) {

        User user = findEntityByEmail(email);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())){
            throw new BusinessException("Credenciais inválidas");
        }

        return user;
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

    public UserResponseDTO updateUser(UUID id, UserRequestDTO updateUser) {
        User existingUser = findEntityById(id);

        existingUser.setName(updateUser.name());
        existingUser.setPixKey(updateUser.pixKey());

        User savedUser = userRepository.save(existingUser);
        return userMapper.toResponse(savedUser);
    }











}
