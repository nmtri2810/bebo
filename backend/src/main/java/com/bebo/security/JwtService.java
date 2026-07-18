package com.bebo.security;

import com.bebo.user.User;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final JwtEncoder jwtEncoder;
  private final JwtProperties jwtProperties;

  public JwtService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
    this.jwtEncoder = jwtEncoder;
    this.jwtProperties = jwtProperties;
  }

  public TokenResult createAccessToken(User user) {
    Instant now = Instant.now();

    Instant expiresAt = now.plus(jwtProperties.accessTokenTtl());

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();

    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(jwtProperties.issuer())
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("scope", "USER")
            .build();

    String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

    return new TokenResult(token, expiresAt);
  }

  public record TokenResult(String value, Instant expiresAt) {}
}
