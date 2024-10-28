package com.pumsdev.spring_security_lab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;

import java.util.Objects;

public class SimpleProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(SimpleProvider.class);
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        logger.debug("SimpleProvider yo!");
        var name = authentication.getName();
        if (Objects.equals(name, "user2")) {
            // add role to user2
            logger.debug("add role devops to user2");
            var user2 = User.withUsername("user2")
                    .password("SET_NEW_PASSWORD")
                    .roles("user", "devops")
                    .build();
            return UsernamePasswordAuthenticationToken.authenticated(
                    user2,
                    null,
                    user2.getAuthorities()
            );
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
