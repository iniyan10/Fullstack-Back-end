package com.example.ecommerce.service;

import com.example.ecommerce.dto.ChangePasswordRequest;
import com.example.ecommerce.dto.DtoMapper;
import com.example.ecommerce.dto.UpdateProfileRequest;
import com.example.ecommerce.dto.UserDto;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.entity.UserStatus;
import com.example.ecommerce.exception.ApiException;
import com.example.ecommerce.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Loads the authenticated user by the email stored in the JWT. */
    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("User no longer exists"));
    }

    @Transactional(readOnly = true)
    public List<UserDto> listAll() {
        return userRepository.findAll().stream().map(DtoMapper::toUserDto).toList();
    }

    @Transactional
    public UserDto verify(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        user.setStatus(UserStatus.VERIFIED);
        return DtoMapper.toUserDto(userRepository.save(user));
    }

    /** Email is intentionally not editable: it is the login identity stored inside the JWT. */
    @Transactional
    public UserDto updateProfile(String email, UpdateProfileRequest request) {
        User user = getByEmail(email);
        user.setName(request.name().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setAddress(blankToNull(request.address()));
        user.setCity(blankToNull(request.city()));
        user.setZip(blankToNull(request.zip()));
        return DtoMapper.toUserDto(userRepository.save(user));
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getByEmail(email);
        // 400 (not 401) on purpose: a 401 would make the frontend treat the session as expired.
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw ApiException.badRequest("New password must be different from the current one");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
