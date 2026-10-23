package com.app.medicalquality;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class MedicalQualityApplication {

    public static void main(String[] a_args) {
        SpringApplication.run(MedicalQualityApplication.class, a_args);
    }
}
