package com.grey.controller;


import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;

@RestController
public class HelloController {

    @GetMapping("/api/userinfo")
    public HashMap<String, Object> getUserInfo() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        HashMap<String, Object> result = new HashMap<>();
        result.put("username", auth != null ? auth.getName() : null);
        return result;
    }

    /** ① 试访问就会被 Spring Security 重定向到 /login → SSO */
    /*

    @GetMapping("/")
    public String home() {
        return "Hello, you are authenticated!";
    }

    */

    /** ② 直接跳转到 /oauth2/authorization/{registrationId} 也行 */
    @GetMapping("/to-lhubsso")
    public void toLhub(HttpServletResponse resp) throws IOException {
        resp.sendRedirect("/oauth2/authorization/lhubsso");
    }
}
