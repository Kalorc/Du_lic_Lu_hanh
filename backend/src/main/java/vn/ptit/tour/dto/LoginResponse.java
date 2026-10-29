package vn.ptit.tour.dto;

public record LoginResponse(String accessToken,
                            String tokenType,
                            long expiresInSeconds,
                            String username,
                            String fullName,
                            String role) {
}
