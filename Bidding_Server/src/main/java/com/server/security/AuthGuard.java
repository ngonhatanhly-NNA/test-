package com.server.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.server.model.Role;
import com.server.service.UserService;
import com.shared.network.Response;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AuthGuard {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final Gson gson = new Gson();

    public AuthGuard(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    public String requireLogin(Context ctx) {
        String token = extractBearerToken(ctx);
        if (token == null) {
            throwUnauthorized("Bạn chưa đăng nhập hoặc token không hợp lệ");
        }

        try {
            DecodedJWT decodedJWT = jwtUtil.verify(token);
            String username = decodedJWT.getSubject();
            Long userId = decodedJWT.getClaim("userId").asLong();
            String roleClaim = decodedJWT.getClaim("role").asString();

            if (username == null || roleClaim == null) {
                throwUnauthorized("Token thiếu thông tin người dùng");
            }

            Role tokenRole;
            try {
                tokenRole = Role.valueOf(roleClaim.toUpperCase());
            } catch (IllegalArgumentException e) {
                throwUnauthorized("Role trong token không hợp lệ");
                return null;
            }

            Role roleInDb = userService.getUserRole(username);
            if (roleInDb == null) {
                throwUnauthorized("Không tìm thấy thông tin người dùng");
            }

            if (roleInDb != tokenRole) {
                throw new ForbiddenResponse(gson.toJson(new Response("FAIL", "Role của bạn đã thay đổi, vui lòng đăng nhập lại", null)));
            }

            ctx.attribute("auth.userId", userId);
            ctx.attribute("auth.username", username);
            ctx.attribute("auth.role", roleInDb);
            return username;
        } catch (JWTVerificationException e) {
            throwUnauthorized("Token không hợp lệ hoặc đã hết hạn");
        }

        return null;
    }

    public Role requireRole(Context ctx, Role... allowedRoles) {
        requireLogin(ctx);
        Role role = ctx.attribute("auth.role");

        Set<Role> allowed = new HashSet<>(Arrays.asList(allowedRoles));
        if (!allowed.contains(role)) {
            String json = gson.toJson(new Response("FAIL", "Bạn không có quyền truy cập chức năng này", null));
            throw new ForbiddenResponse(json);
        }
        return role;
    }

    private String extractBearerToken(Context ctx) {
        String header = ctx.header("Authorization");
        if (header == null || header.trim().isEmpty()) {
            return null;
        }

        String prefix = "Bearer ";
        if (!header.startsWith(prefix)) {
            return null;
        }

        String token = header.substring(prefix.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private void throwUnauthorized(String message) {
        String json = gson.toJson(new Response("FAIL", message, null));
        throw new UnauthorizedResponse(json);
    }
}
