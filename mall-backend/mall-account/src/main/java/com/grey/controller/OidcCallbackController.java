package com.grey.controller;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/sg/wb/v1/common/oidc")
public class OidcCallbackController {
 
    /* ---------- 配置参数 ---------- */
    @Value("${spring.security.oauth2.client.registration.lhubsso.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.provider.lhubsso.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.lhubsso.user-info-uri:}")
    private Optional<String> userInfoUri;   // 可选：若 SSO 提供 userinfo 端点

    @Value("${spring.security.oauth2.client.registration.lhubsso.redirect-uri}")
    private String redirectUri;

    /* ---------- OIDC 回调 ---------- */
    @PostMapping("/callback")
    public void handleCallback(@RequestParam(value = "code", required = false) String code,
                               @RequestParam(value = "state", required = false) String state,
                               HttpServletResponse response) throws Exception {

        if (code == null || state == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing authorization code or state");
            return;
        }
        log.info("-----------------OIDC callback: code={}, state={}-------------------", code, state);

        /* ---------- 1. 换取 Token ---------- */
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, "authorization_code");
        params.add(OAuth2ParameterNames.CODE, code);
        params.add(OAuth2ParameterNames.REDIRECT_URI, redirectUri);
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        params.add("client_assertion", generateClientAssertion());

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Map> tokenResp = restTemplate.exchange(
                tokenUri, HttpMethod.POST, new HttpEntity<>(params, headers), Map.class);

        if (!tokenResp.getStatusCode().is2xxSuccessful() || tokenResp.getBody() == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token exchange failed");
            return;
        }
        Map<?,?> body = tokenResp.getBody();
        String accessToken = (String) body.get("access_token");
        String idToken     = (String) body.get("id_token");
        log.info("----------------access_token={}, id_token(length={})---------------", accessToken != null, idToken != null ? idToken.length() : 0);

        /* ---------- 2. 解析 (并可能解密) ID Token ---------- */
        JWT jwtParsed = JWTParser.parse(idToken);
        if (jwtParsed instanceof EncryptedJWT enc) {
            JWEDecrypter dec = new ECDHDecrypter(loadPrivateECKey());
            enc.decrypt(dec);
            jwtParsed = enc.getPayload().toSignedJWT();
        }
        if (!(jwtParsed instanceof SignedJWT signed)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid ID token");
            return;
        }
        String username = signed.getJWTClaimsSet().getSubject();

        /* ---------- 3. 可选：调用 UserInfo 端点取得更多用户信息 ---------- */

        /* ---------- 4. 建立登录会话 ---------- */
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        /* ---------- 5. 跳转首页 ---------- */
        String cookieValue = "id_token=" + idToken
                + "; Path=/"
                + "; HttpOnly"
                + "; Secure"
                + "; Max-Age=3600"
                + "; SameSite=None";

        response.setHeader("Set-Cookie", cookieValue);
        response.sendRedirect("http://localhost:3000/callback");


    }

    /* ---------------------------------------------------------------- */
    /** 读取本地 EC 私钥（JWK JSON） */
    private ECPrivateKey loadPrivateECKey() throws Exception {
        String json = new String(new ClassPathResource("keys/private-key.pem").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ECKey.parse(json).toECPrivateKey();
    }

    /** 生成 client_assertion(JWT) 供私钥jwt认证 */
    private String generateClientAssertion() throws Exception {
        String json = new String(new ClassPathResource("keys/private-key.pem").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        ECKey ecKey = ECKey.parse(json);

        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(clientId)
                .subject(clientId)
                .audience(tokenUri)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .jwtID(UUID.randomUUID().toString())
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(ecKey.getKeyID())
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT signed = new SignedJWT(header, claims);
        signed.sign(new ECDSASigner(ecKey));
        return signed.serialize();
    }


    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {

        log.info("---------------------getCurrentUser-----------------");

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }



        return ResponseEntity.ok(Map.of("username", authentication.getName()));
    }

}
