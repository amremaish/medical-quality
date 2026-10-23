package com.app.medicalquality.auth.service;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.repository.UserRepository;
import com.app.medicalquality.config.AdminProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminSeedService implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AdminProperties adminProperties;
    private final PasswordEncoder passwordEncoder;

    public AdminSeedService(
            UserRepository a_userRepository,
            AdminProperties a_adminProperties,
            PasswordEncoder a_passwordEncoder) {
        this.userRepository = a_userRepository;
        this.adminProperties = a_adminProperties;
        this.passwordEncoder = a_passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments a_args) {
        if (userRepository.findByUsername(adminProperties.getUsername()).isEmpty()) {
            User user = new User();
            user.setUsername(adminProperties.getUsername());
            user.setPasswordHash(passwordEncoder.encode(adminProperties.getPassword()));
            user.setEnabled(true);
            user.setSuperAdmin(true);
            userRepository.save(user);
        }
    }
}
