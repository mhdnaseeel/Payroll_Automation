package com.fci.automation.security.jwt;

import com.fci.automation.security.services.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${fci.app.jwtSecret:SecretKeyMustBeLongEnoughToLookSecureAndItShouldBeStoredInEnvVariablesButForNowHardcodedIsOkayForDev}")
    private String jwtSecret;

    @Value("${fci.app.jwtExpirationMs:900000}")
    private int jwtExpirationMs;

    public String generateJwtToken(UserDetailsImpl userPrincipal) {
        // Determine Realm from Context or Username logic
        // For simplicity, we rely on the current Context which should be set during
        // login
        String realm = com.fci.automation.config.RealmContext.getRealm() != null
                ? com.fci.automation.config.RealmContext.getRealm().name()
                : "REAL";

        String role = userPrincipal.getAuthorities().stream().findFirst().map(item -> item.getAuthority())
                .orElse("USER");

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .claim("realm", realm) // Embed Realm
                .claim("role", role) // Embed Role
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateTokenFromUsername(String username) {
        // Fallback or Test usage - defaulting to USER role if unknown (or should update
        // signature)
        // For refresh token flow, we might need to fetch user again if we want role,
        // but for now let's assume this method is used for simple cases or testing.
        return Jwts.builder()
                .setSubject(username)
                .claim("realm", "REAL") // Default
                .claim("role", "USER") // Default
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String getRoleFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().get("role", String.class);
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public String getRealmFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().get("realm", String.class);
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parse(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }

        return false;
    }
}
