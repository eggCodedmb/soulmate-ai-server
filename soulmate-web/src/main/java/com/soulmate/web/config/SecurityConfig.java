package com.soulmate.web.config;

import com.soulmate.common.config.JwtProperties;
import com.soulmate.common.constant.CommonConstants;
import com.soulmate.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /** 不需要认证的路径 */
    private static final List<String> WHITE_LIST = List.of(
            "/api/auth/**",
            "/actuator/**",
            "/ws/**",
            "/api/alipay/notify",
            "/files/**",
            "/error"
    );

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(new RequestAttributeSecurityContextRepository()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST.toArray(new String[0])).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * JWT 认证过滤器
     */
    @Slf4j
    @Component
    @RequiredArgsConstructor
    public static class JwtAuthFilter extends OncePerRequestFilter {

        private final JwtProperties jwtProperties;
        private final AntPathMatcher pathMatcher = new AntPathMatcher();
        private final SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                         HttpServletResponse response,
                                         FilterChain filterChain) throws ServletException, IOException {

            // 白名单路径直接放行
            String path = request.getRequestURI();
            for (String pattern : WHITE_LIST) {
                if (pathMatcher.match(pattern, path)) {
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            // 解析 Token
            String authHeader = request.getHeader(CommonConstants.TOKEN_HEADER);
            if (authHeader != null && authHeader.startsWith(CommonConstants.TOKEN_PREFIX)) {
                String token = authHeader.substring(CommonConstants.TOKEN_PREFIX.length());
                try {
                    Long userId = JwtUtil.getUserId(token, jwtProperties.getSecret());
                    request.setAttribute(CommonConstants.CURRENT_USER_ID, userId);
                    request.setAttribute("currentUserId", userId);

                    // 设置 Spring Security Authentication
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                    var authentication = new UsernamePasswordAuthenticationToken(
                            userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    // 保存到 request attribute，确保异步分派时 SecurityContext 不丢失
                    securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
                } catch (Exception e) {
                    log.warn("Token解析失败: {}", e.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":1001,\"message\":\"Token无效或已过期\"}");
                    return;
                }
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":1001,\"message\":\"未提供认证Token\"}");
                return;
            }

            filterChain.doFilter(request, response);
        }
    }
}
