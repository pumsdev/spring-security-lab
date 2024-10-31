package com.pumsdev.spring_security_lab;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import java.util.ArrayList;
import java.util.List;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(
                        authorizeHttp -> {
                            authorizeHttp.requestMatchers("/").permitAll();
                            authorizeHttp.anyRequest().authenticated();
                        }
                )
                .formLogin(l -> l.defaultSuccessUrl("/internal"))
                .logout(l -> l.logoutSuccessUrl("/"))
                .addFilterBefore(new SimpleFilter(), AuthorizationFilter.class)
                .authenticationProvider(new SimpleProvider())
                .build();
    }

    @Bean
    InMemoryUserDetailsManager inMemoryUserDetailsManager() {

        List<UserDetails> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Pbkdf2PasswordEncoder encoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
            String result = encoder.encode("myPassword");
            result = "{pbkdf2@SpringSecurity_v5_8}" + result;
            users.add(User.withUsername("user" + i)
                    .password(result)
                    .roles("user")
                    .build());

            System.out.println("user " + i + " password is " + users.get(i).getPassword());
        }

        return new InMemoryUserDetailsManager(users);
    }
}
