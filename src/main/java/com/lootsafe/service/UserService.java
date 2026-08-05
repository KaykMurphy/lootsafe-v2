package com.lootsafe.service;

import com.lootsafe.dto.request.UserRequestDTO;
import com.lootsafe.dto.response.UserResponseDTO;
import com.lootsafe.entity.User;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.mapper.UserMapper;
import com.lootsafe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponseDTO createUser(UserRequestDTO dto) {

        if (userRepository.existsByEmail(dto.email())){
            throw new BusinessException("Email em uso");
        }

        User user = userMapper.toEntity(dto);

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    public UserResponseDTO findByEmail(String email) {
        User user =  userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        return userMapper.toResponse(user);

    }

    public UserResponseDTO getUserById(UUID id) {
        User user = findEntityById(id);
        return userMapper.toResponse(user);
    }

    public User findEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    public UserResponseDTO updateUser(UUID id, UserRequestDTO updateUser) {

        User existingUser = findEntityById(id);

        existingUser.setName(updateUser.name());
        existingUser.setPixKey(updateUser.pixKey());

        User savedUser = userRepository.save(existingUser);

        return userMapper.toResponse(savedUser);

    }











}
