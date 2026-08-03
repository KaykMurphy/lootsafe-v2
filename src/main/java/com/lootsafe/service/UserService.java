package com.lootsafe.service;

import com.lootsafe.entity.User;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(User user) {

        if (userRepository.existsByEmail(user.getEmail())){
            throw new BusinessException("Email em uso");
        }

        User newUser = new User();
        newUser.setName(user.getName());
        newUser.setEmail(user.getEmail());

        newUser.setRole(user.getRole());

        newUser.setPasswordHash(user.getPasswordHash());

        return userRepository.save(newUser);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    public User findById(UUID id){
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("E-mail não encontrado"));
    }

    public User updateUser(UUID id, User updateUser) {

        User existingUser = findById(id);

        existingUser.setName(updateUser.getName());
        existingUser.setPixKey(updateUser.getPixKey());


        return userRepository.save(existingUser);

    }











}
