package com.dev.taskmanagement.service.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JWTService {
    public String generateToken(UserDetails userDetails) {
        //TODO time
        long expirationTime = 1000 * 60 * 60 * 24; // 24 часа
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigenKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    public String extractUserName(String token){
        return extractClaim(token,Claims::getSubject);

    }
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String login = extractUserName(token);
        return login.equals(userDetails.getUsername()) && !isTokenExpired(token); // Обратите внимание на "!"
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token,Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolvers) {
        final Claims claims =  extractAllClaims(token);
        return claimsResolvers.apply(claims);
    }
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigenKey()).build().parseClaimsJws(token).getBody();
    }
    private Key getSigenKey() {
        //TODO key
        byte[] key = Decoders.BASE64.decode("pSFKIVO83YNCFKWcb9HqClroA5nPoR97U9ABVD55gVo=");
        return Keys.hmacShaKeyFor(key);
    }
}
