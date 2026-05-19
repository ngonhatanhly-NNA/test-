package com.server.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.server.model.Role;

import java.time.Instant;
import java.util.Date;

public class JwtUtil {
    private static final String ISSUER = "team13-bidding-system";
    private static final long DEFAULT_EXPIRES_SECONDS = 8 * 60 * 60;

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long expiresSeconds;

    public JwtUtil(String secret, long expiresSeconds) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
        this.expiresSeconds = expiresSeconds;
    }

    public static JwtUtil fromEnvironment() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.trim().isEmpty()) {
            secret = "chon-nham-nganh-roi";
        }
        return new JwtUtil(secret, DEFAULT_EXPIRES_SECONDS);
    }

    public String generateToken(long userId, String username, Role role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expiresSeconds);

        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(username)
                .withClaim("userId", userId)
                .withClaim("role", role.name())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) throws JWTVerificationException {
        return verifier.verify(token);
    }
}