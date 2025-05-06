package com.grey.config;


import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class SsoConfig {

    /**
     * ① 让 Spring 能用私钥签 client_assertion（private_key_jwt）
     */
    @Bean
    public JwtEncoder jwtEncoder() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("keys/private-key.pem")) {
            if (inputStream == null) {
                throw new FileNotFoundException("Missing keys/private-key.pem in classpath");
            }

            String jwkJson = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JWK jwk = JWK.parse(jwkJson); // 支持 JWK JSON 格式

            JWKSource<SecurityContext> source = (selector, ctx) -> selector.select(new JWKSet(jwk));
            return new NimbusJwtEncoder(source);
        }
    }



    /**
     * ② 如果你在后台代码里需要主动拿 token，可注入这个 manager。
     *    对于普通浏览器登录流程，其实不写也没问题。
     */
    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientService clientService) {      // ⬅️ 保持原来的 svc

        return new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                registrations, clientService);
    }

}
