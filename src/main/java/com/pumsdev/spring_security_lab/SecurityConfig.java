package com.pumsdev.spring_security_lab;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.ArrayList;
import java.util.List;

@EnableWebSecurity
@Configuration
public class SecurityConfig {
    @Bean
    InMemoryUserDetailsManager inMemoryUserDetailsManager() {
        String customPassword = "{noop}halow";
        List<UserDetails> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            users.add(User.withUsername("user" + i)
                    .password(customPassword + i)
                    .roles("user")
                    .build());
        }

        return new InMemoryUserDetailsManager(users);
    }
}
