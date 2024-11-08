package com.pumsdev.spring_security_lab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;

import java.util.Objects;

public class ZimpleProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(ZimpleProvider.class);
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        logger.debug("ZimpleProvider yo!");
        var name = authentication.getName();
        if (Objects.equals(name, "user4")) {
            // add role to user4
            logger.debug("add role devops to user4");
            var user4 = User.withUsername("user4")
                    .password("SET_NEW_PASSWORD")
                    .roles("user", "devops")
                    .build();
            return UsernamePasswordAuthenticationToken.authenticated(
                    user4,
                    null,
                    user4.getAuthorities()
            );
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
