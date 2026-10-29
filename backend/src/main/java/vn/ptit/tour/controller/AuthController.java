package vn.ptit.tour.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import vn.ptit.tour.dto.ApiResponse;
import vn.ptit.tour.dto.LoginRequest;
import vn.ptit.tour.dto.LoginResponse;
import vn.ptit.tour.entity.AppUser;
import vn.ptit.tour.exception.ApiException;
import vn.ptit.tour.exception.ErrorCode;
import vn.ptit.tour.repository.AppUserRepository;
import vn.ptit.tour.security.AuthPrincipal;
import vn.ptit.tour.security.CurrentUser;
import vn.ptit.tour.security.JwtService;

import java.util.Map;

/** Dang nhap de lay JWT cho cac API noi bo. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(AppUserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        AppUser user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED,
                        "Tai khoan hoac mat khau khong dung"));

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "Tai khoan hoac mat khau khong dung");
        }

        String token = jwtService.generate(user);
        LoginResponse body = new LoginResponse(token, "Bearer", jwtService.expirationSeconds(),
                user.getUsername(), user.getFullName(), user.getRole().name());
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me() {
        AuthPrincipal p = CurrentUser.get();
        if (p == null) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "userId", String.valueOf(p.userId()),
                "username", p.username(),
                "fullName", p.fullName(),
                "role", p.role().name())));
    }
}
