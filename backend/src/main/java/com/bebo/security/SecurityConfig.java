package com.bebo.security;

import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  public SecretKey jwtSecretKey(JwtProperties properties) {
    byte[] decodedSecret;

    try {
      decodedSecret = Base64.getDecoder().decode(properties.secret());
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("JWT_SECRET must be a valid Base64 value", exception);
    }

    if (decodedSecret.length < 32) {
      throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes");
    }

    return new SecretKeySpec(decodedSecret, "HmacSHA256");
  }

  @Bean
  public JwtEncoder jwtEncoder(SecretKey secretKey) {
    return NimbusJwtEncoder.withSecretKey(secretKey).algorithm(MacAlgorithm.HS256).build();
  }

  @Bean
  public JwtDecoder jwtDecoder(SecretKey secretKey, JwtProperties properties) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();

    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));

    return decoder;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      CorsConfigurationSource corsConfigurationSource,
      RestAuthenticationEntryPoint authenticationEntryPoint,
      RestAccessDeniedHandler accessDeniedHandler)
      throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(
                        "/api/auth/**",
                        "/api/public/**",
                        "/api/ping",
                        "/actuator/health",
                        "/actuator/info")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            resourceServer ->
                resourceServer
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                    .jwt(Customizer.withDefaults()));

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOrigins(properties.allowedOrigins());

    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration("/api/**", configuration);

    return source;
  }
}
