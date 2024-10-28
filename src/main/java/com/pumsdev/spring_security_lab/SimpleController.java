package com.pumsdev.spring_security_lab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimpleController {
    private static final Logger logger = LoggerFactory.getLogger(SimpleController.class);
    @GetMapping("/")
    public String publicPage() {
        logger.debug("On public page");
        logger.debug("getPrincipal {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return "Public";
    }

    @GetMapping("/internal")
    public String internal(Authentication authentication) {
        logger.debug("Object after login");
        logger.debug("getPrincipal {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        logger.debug("getName {}", authentication.getName());
        return "Hallow internal";
    }
}
