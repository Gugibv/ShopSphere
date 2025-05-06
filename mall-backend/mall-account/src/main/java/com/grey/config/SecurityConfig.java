package com.grey.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;
import java.util.UUID;

@Configuration
public class SecurityConfig {


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ClientRegistrationRepository clientRepo ,
                                           JwtDecoder jwtDecoder) throws Exception {

        DefaultOAuth2AuthorizationRequestResolver delegate =
                new DefaultOAuth2AuthorizationRequestResolver(clientRepo, "/oauth2/authorization");

        OAuth2AuthorizationRequestResolver customResolver = new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                return customize(delegate.resolve(request));
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientId) {
                return customize(delegate.resolve(request, clientId));
            }

            private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest orig) {
                if (orig == null) return null;
                return OAuth2AuthorizationRequest.from(orig)
                        .state(UUID.randomUUID().toString().replaceAll("[;=]", ""))
                        .attributes(attrs -> attrs.put("nonce", UUID.randomUUID().toString()))
                        .build();
            }
        };

        http
                .addFilterBefore(
                        new com.grey.config.CookieTokenAuthenticationFilter(jwtDecoder),
                        BearerTokenAuthenticationFilter.class
                )
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/favicon.ico", "/api/sg/wb/v1/common/oidc/callback" ,"/api/sg/wb/v1/common/oidc/me").permitAll()
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(ep -> ep.authorizationRequestResolver(customResolver))
                        .redirectionEndpoint(redir -> redir.baseUri("/no-match"))
                        .successHandler(oauth2SuccessHandler())
                        .failureHandler(oauth2FailureHandler())
                )

                //  .csrf(csrf -> csrf.ignoringRequestMatchers("/api/sg/wb/v1/common/oidc/callback"))
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));

        return http.build();
    }


    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "https://sc.lhubsg.com",
                "https://cls.loc.lhubsg.com"
        ));

        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }


    @Bean
    public AuthenticationSuccessHandler oauth2SuccessHandler() {
        return (request, response, authentication) -> {
            // ✅ 登录成功后可设置 Cookie 或跳转前端页面
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":\"success\",\"user\":\"" + authentication.getName() + "\"}");
            // 也可用：response.sendRedirect("http://localhost:3000/home");
        };
    }

    @Bean
    public AuthenticationFailureHandler oauth2FailureHandler() {
        return (request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":\"fail\",\"message\":\"" + exception.getMessage() + "\"}");
        };
    }



    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri("https://sc.lhubsg.com/sam/idp/a/lhubstg/oidc/.well-known/keys").build();
    }


}
