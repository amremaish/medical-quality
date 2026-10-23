package com.app.medicalquality.auth.service;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.repository.UserRepository;
import com.app.medicalquality.exception.AppException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository a_userRepository) {
        this.userRepository = a_userRepository;
    }

    public User require(Authentication a_authentication) {
        if (a_authentication == null || a_authentication.getName() == null) {
            throw new AppException("error.auth.unauthorized");
        }
        return userRepository
                .findByUsername(a_authentication.getName())
                .filter(User::isEnabled)
                .filter(a_user -> a_user.getDeletedAt() == null)
                .orElseThrow(() -> new AppException("error.auth.unauthorized"));
    }
}
