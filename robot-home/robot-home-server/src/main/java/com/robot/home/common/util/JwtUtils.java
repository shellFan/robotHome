package com.robot.home.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * JWT 工具：生成与解析 Token
 */
@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /** JWT密钥最小长度（位） */
    private static final int MIN_SECRET_LENGTH = 32;

    /** 常见弱密钥示例值 */
    private static final String[] WEAK_SECRETS = {
            "secret", "jwt-secret", "your-secret", "change-me", "123456",
            "password", "jwt_secret_key", "my-secret", "test", "example"
    };

    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT_SECRET 未设置，应用无法启动。请通过环境变量 JWT_SECRET 设置至少32字符的强密钥。");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET 长度不足（当前" + secret.length() + "字符），至少需要" + MIN_SECRET_LENGTH + "字符。");
        }
        String lower = secret.toLowerCase();
        for (String weak : WEAK_SECRETS) {
            if (lower.contains(weak)) {
                throw new IllegalStateException(
                        "JWT_SECRET 包含弱密钥模式 '" + weak + "'，请使用强随机密钥。");
            }
        }
        log.info("JWT_SECRET 校验通过（长度={}）", secret.length());
    }

    /**
     * 生成 Token
     *
     * @param type user / admin —— 用于区分前台用户与后台管理员
     */
    public String generateToken(Long userId, String username, String role, String type) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .setSubject(username)
                .claim("uid", userId)
                .claim("role", role)
                .claim("type", type)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /**
     * 前台用户 Token
     */
    public String generateToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, "user");
    }

    /**
     * 后台管理员 Token
     */
    public String generateAdminToken(Long userId, String username, String roleCodes) {
        return generateToken(userId, username, roleCodes, "admin");
    }

    public String getType(String token) {
        return parseToken(token).get("type", String.class);
    }

    public String generateRefreshToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpiration);
        return Jwts.builder()
                .setSubject(username)
                .claim("uid", userId)
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserId(String token) {
        return parseToken(token).get("uid", Long.class);
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public boolean isExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }
}
