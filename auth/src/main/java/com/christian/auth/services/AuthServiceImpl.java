package com.christian.auth.services;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.christian.auth.dto.LoginRequest;
import com.christian.auth.dto.TokenResponse;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final CustomUserDetails userDetailsService;

    private final PasswordEncoder passwordEncoder;

    private final RSAKey rsaKey;

    @Value("${auth.jwt.issuer}")
    private String issuer;

    @Value("${auth.jwt.expiration-seconds:3600}")
    private long expirationSeconds;

    public AuthServiceImpl(CustomUserDetails userDetailsService,
                           PasswordEncoder passwordEncoder,
                           JWKSource<SecurityContext> jwkSource) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        try {
            JWKSelector jwkSelector = new JWKSelector(
                    new JWKMatcher.Builder().keyType(KeyType.RSA).build()
            );
            var jwks = jwkSource.get(jwkSelector, null);
            if (jwks == null || jwks.isEmpty())
                throw new IllegalStateException("No se encontró ninguna clave RSA");
            rsaKey = (RSAKey) jwks.get(0);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo obtener la clave RSA", e);
        }
    }

    @Override
    public TokenResponse autenticar(LoginRequest request) throws Exception {

        log.info("Cargando usuario {}", request.username());

        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(request.username());
        } catch (UsernameNotFoundException e) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        if (!passwordEncoder.matches(request.password(), userDetails.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        Instant now = Instant.now();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(userDetails.getUsername())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(expirationSeconds)))
                .jwtID(UUID.randomUUID().toString())
                .claim("roles", userDetails.getAuthorities()
                        .stream().map(GrantedAuthority::getAuthority).toList())
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                claimsSet
        );

        signedJWT.sign(new RSASSASigner(rsaKey.toPrivateKey()));

        return new TokenResponse(signedJWT.serialize());
    }
}
