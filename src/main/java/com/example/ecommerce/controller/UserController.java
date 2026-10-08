package com.example.ecommerce.controller;

import com.example.ecommerce.dto.*;
import com.example.ecommerce.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserDto me(Principal principal) {
        return DtoMapper.toUserDto(userService.getByEmail(principal.getName()));
    }

    @PutMapping
    public UserDto update(Principal principal, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.getName(), request);
    }

    @PutMapping("/password")
    public MessageResponse changePassword(Principal principal, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getName(), request);
        return new MessageResponse("Password updated");
    }
}
