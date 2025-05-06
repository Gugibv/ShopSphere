package com.grey.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.ECPrivateKey;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CookieTokenAuthenticationFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;

    public CookieTokenAuthenticationFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        String token = null;

        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if ("id_token".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }

        if (token != null) {
            log.info("Found id_token in cookie, trying to decode...");

            try {
                // 1. Parse JWT
                JWT jwtParsed = JWTParser.parse(token);

                // 2. 如果是加密 JWT，解密得到 SignedJWT
                if (jwtParsed instanceof EncryptedJWT encryptedJWT) {
                    JWEDecrypter decrypter = new ECDHDecrypter(loadPrivateECKey());
                    encryptedJWT.decrypt(decrypter);
                    jwtParsed = encryptedJWT.getPayload().toSignedJWT();
                }

                if (!(jwtParsed instanceof SignedJWT signed)) {
                    throw new IllegalStateException("Not a signed JWT");
                }

                JWTClaimsSet claimSet = signed.getJWTClaimsSet();

                // 3. 处理 claims，确保时间字段是 Instant 类型
                Map<String, Object> claims = new HashMap<>(claimSet.getClaims());
                if (claimSet.getExpirationTime() != null)
                    claims.put("exp", claimSet.getExpirationTime().toInstant());
                if (claimSet.getIssueTime() != null)
                    claims.put("iat", claimSet.getIssueTime().toInstant());
                if (claimSet.getNotBeforeTime() != null)
                    claims.put("nbf", claimSet.getNotBeforeTime().toInstant());

                // 4. 构建 Spring 的 Jwt 对象
                Jwt jwt = Jwt.withTokenValue(signed.serialize())
                        .headers(h -> h.putAll(signed.getHeader().toJSONObject()))
                        .claims(c -> c.putAll(claims))
                        .build();

                // 5. 注入到 Spring Security 上下文
                JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("JWT valid, subject: {}", jwt.getSubject());

            } catch (Exception e) {
                log.error("JWT 解密 / 转换失败: {}", e.getMessage());
            }
        } else {
            log.warn(" No id_token found in cookies");
        }

        chain.doFilter(req, res);
    }

    /**
     * 加载 JWE 解密所需的 EC 私钥
     */
    private ECPrivateKey loadPrivateECKey() throws IOException, ParseException, JOSEException {
        String json = new String(new ClassPathResource("keys/private-key.pem")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ECKey.parse(json).toECPrivateKey();
    }
}
