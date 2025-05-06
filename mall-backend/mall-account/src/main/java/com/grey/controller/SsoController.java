package com.grey.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 *   App Login URL：你只需要跳转这个地址
 *   <a href="/oauth2/authorization/lhubsso">使用 SSO 登录</a>
 */
@RestController
@RequestMapping("/sso")
public class SsoController {

    // 🔄 前端登出回调 URL：SSO 登录页面点击“登出”后浏览器访问
    @GetMapping("/front-channel-logout")
    public ResponseEntity<?> frontChannelLogout(HttpServletRequest request, HttpServletResponse response) {
        request.getSession().invalidate(); // 清除本地 session
        return ResponseEntity.ok("✅ Front-Channel logout handled.");
    }

    // 🔐 后端登出通知：SSO 通过后台 POST 通知你“这个用户要注销”
    @PostMapping("/back-channel-logout")
    public ResponseEntity<?> backChannelLogout(@RequestBody String logoutToken) {
        // TODO：验证并解析 logoutToken（通常为 JWT），清理本地会话
        System.out.println("🛡️ Back-Channel logout received: " + logoutToken);
        return ResponseEntity.ok("✅ Back-Channel logout accepted.");
    }
}