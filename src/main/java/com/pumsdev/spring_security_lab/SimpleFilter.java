package com.pumsdev.spring_security_lab;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class SimpleFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(SimpleFilter.class);
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.debug("principal: {}", authentication.getPrincipal());
        logger.debug("credential: {}", authentication.getCredentials());
        logger.debug("authorities: {}", authentication.getAuthorities());

        logger.debug("hola SimpleFilter");

        filterChain.doFilter(request, response);
    }
}
