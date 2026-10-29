package vn.ptit.tour.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import vn.ptit.tour.dto.ApiError;
import vn.ptit.tour.dto.ApiResponse;
import vn.ptit.tour.exception.ErrorCode;
import vn.ptit.tour.security.JwtAuthenticationFilter;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Ma tran quyen (muc 7.2 dac ta):
 *
 *   Nhom chuc nang                            GUEST CUSTOMER PRODUCT MANAGER ADMIN
 *   Tra cuu va goi y tour cong khai             v      v        v       v      v
 *   Xem so thich cua phien ban Tour noi bo      -      -        v       v      -
 *   Gan va cap nhat so thich Tour               -      -        v       -      -
 *   Tao danh muc so thich                       -      -        v       -      -
 *   Cap nhat danh muc so thich                  -      -        v       -      -
 *
 * ADMIN quan tri tai khoan/phan quyen, KHONG mac nhien duoc sua noi dung
 * hay phe duyet nghiep vu.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final ObjectMapper objectMapper;
    private final String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          ObjectMapper objectMapper,
                          @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.jwtFilter = jwtFilter;
        this.objectMapper = objectMapper;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))   // cho H2 console
            .authorizeHttpRequests(reg -> reg
                // --- Dang nhap va tien ich
                .requestMatchers("/api/v1/auth/login", "/h2-console/**",
                                 "/actuator/health", "/error").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // --- API noi bo: xem so thich cua phien ban Tour (PRODUCT, MANAGER)
                .requestMatchers(HttpMethod.GET, "/api/v1/internal/tour-versions/*/interests")
                    .hasAnyRole("PRODUCT", "MANAGER")

                // --- API noi bo: gan/cap nhat so thich Tour (chi PRODUCT)
                .requestMatchers(HttpMethod.PUT, "/api/v1/internal/tour-versions/*/interests")
                    .hasRole("PRODUCT")

                // --- API noi bo: quan ly danh muc so thich (chi PRODUCT)
                .requestMatchers(HttpMethod.GET, "/api/v1/internal/interests").hasAnyRole("PRODUCT", "MANAGER")
                .requestMatchers(HttpMethod.POST, "/api/v1/internal/interests").hasRole("PRODUCT")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/internal/interests/*").hasRole("PRODUCT")

                // --- Moi API noi bo khac deu phai xac thuc
                .requestMatchers("/api/v1/internal/**").authenticated()

                // --- API cong khai: tat ca nguoi dung, ke ca chua dang nhap (GUEST)
                .requestMatchers("/api/v1/**").permitAll()

                .anyRequest().permitAll())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                        writeError(res, ErrorCode.UNAUTHENTICATED))
                .accessDeniedHandler((req, res, e) ->
                        writeError(res, ErrorCode.FORBIDDEN)))
            .addFilterBefore(jwtFilter,
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeError(HttpServletResponse res, ErrorCode code) throws java.io.IOException {
        res.setStatus(code.status().value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        ApiError error = new ApiError(code.name(), code.defaultMessage(), null,
                UUID.randomUUID().toString(), OffsetDateTime.now());
        objectMapper.writeValue(res.getWriter(), ApiResponse.fail(error));
    }
}
