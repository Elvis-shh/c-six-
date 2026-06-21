package com.smartreport.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.secret:SmartReport-JWT-Secret-Key-Must-Be-At-Least-256-Bits-Long}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration:7200000}")
    private long accessExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshExpiration;

    private SecretKey getKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String generateAccessToken(Long userId, String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpiration))
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpiration))
                .signWith(getKey())
                .compact();
    }

    public Long getUserId(String token) {
        return Long.parseLong(Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject());
    }

    public String getEmail(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("email", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return !isBlacklisted(token);
        } catch (JwtException e) {
            return false;
        }
    }

    private boolean isBlacklisted(String token) {
        String hash = DigestUtils.md5Hex(token);
        return Boolean.TRUE.equals(redisTemplate.hasKey("bl:" + hash));
    }

    public void blacklist(String token) {
        String hash = DigestUtils.md5Hex(token);
        long ttl = getExpiration(token);
        if (ttl > 0) {
            redisTemplate.opsForValue().set("bl:" + hash, "1", ttl, TimeUnit.MILLISECONDS);
        }
    }

    private long getExpiration(String token) {
        try {
            Date exp = Jwts.parser().verifyWith(getKey()).build()
                    .parseSignedClaims(token).getPayload().getExpiration();
            return exp.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0;
        }
    }
}
