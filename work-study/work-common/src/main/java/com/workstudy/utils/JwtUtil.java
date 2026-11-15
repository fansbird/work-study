package com.workstudy.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类（jjwt 0.12.x）
 * <p>0.12 起 API 变化：签名用 SecretKey、解析用 verifyWith + parseSignedClaims；
 * 同时要求 HS256 密钥长度不少于 256 bit（32 字节）。</p>
 */
public class JwtUtil {

    /**
     * 由字符串密钥生成 HMAC-SHA 密钥（长度必须 >= 32 字节）
     */
    private static SecretKey buildKey(String secretKey) {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 jwt
     *
     * @param secretKey 秘钥（>= 32 字节）
     * @param ttlMillis 过期时间（毫秒）
     * @param claims    载荷
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        Date exp = new Date(System.currentTimeMillis() + ttlMillis);
        return Jwts.builder()
                .claims(claims)
                .expiration(exp)
                .signWith(buildKey(secretKey))
                .compact();
    }

    /**
     * 解析 jwt（签名校验 + 过期校验，失败会抛 JwtException）
     */
    public static Claims parseJWT(String secretKey, String token) {
        return Jwts.parser()
                .verifyWith(buildKey(secretKey))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
