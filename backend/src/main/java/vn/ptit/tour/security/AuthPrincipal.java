package vn.ptit.tour.security;

import vn.ptit.tour.entity.Role;

/** Thong tin nguoi dung da xac thuc, gan vao SecurityContext. */
public record AuthPrincipal(Long userId, String username, String fullName, Role role) {
}
