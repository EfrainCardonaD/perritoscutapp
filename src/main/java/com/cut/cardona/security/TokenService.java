package com.cut.cardona.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.cut.cardona.modelo.usuarios.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class TokenService {

    @Value("${app.security.jwt.secret:changeit}")
    private String apiSecret;

    @Value("${app.security.jwt.issuer:perritoscutapp}")
    private String issuer;

    public String generarToken(Usuario usuario) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(apiSecret);
            return JWT.create()
                    .withIssuer(issuer)
                    .withSubject(usuario.getUsername())
                    .withClaim("roles", usuario.getRol().toString())
                    .withClaim("id", String.valueOf(usuario.getId()))
                    .withExpiresAt(generarFechaDeExpiacion())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException();
        }

    }

    public String getSubject(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(apiSecret);
            JWTVerifier verifier  = JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build();
            DecodedJWT decodedJWT = verifier.verify(token);
            return decodedJWT.getSubject();
        } catch (JWTVerificationException exception) {
            throw  new RuntimeException("token invalido");
        }
    }

    private Instant generarFechaDeExpiacion(){

        ZoneId zonaHoraria = ZoneId.of("America/New_York");
        LocalDateTime fechaActual = LocalDateTime.now(zonaHoraria);
        LocalDateTime fechaExpiracion = fechaActual.plusDays(1);
        return fechaExpiracion.toInstant(zonaHoraria.getRules().getOffset(fechaExpiracion));
    }

    public String generarRefreshToken(Usuario usuario) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(apiSecret);
            return JWT.create()
                    .withIssuer(issuer)
                    .withSubject(usuario.getUsername())
                    .withClaim("roles", usuario.getRol().toString())
                    .withClaim("id", String.valueOf(usuario.getId()))
                    .withExpiresAt(generarFechaDeExpiacionRefreshToken())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException();
        }
    }

    private Instant generarFechaDeExpiacionRefreshToken() {
        ZoneId zonaHoraria = ZoneId.of("America/New_York");
        LocalDateTime fechaActual = LocalDateTime.now(zonaHoraria);
        LocalDateTime fechaExpiracion = fechaActual.plusDays(30);
        return fechaExpiracion.toInstant(zonaHoraria.getRules().getOffset(fechaExpiracion));

    }

    public long getExpirationTime() {
        ZoneId zonaHoraria = ZoneId.of("America/New_York");
        LocalDateTime fechaActual = LocalDateTime.now(zonaHoraria);
        LocalDateTime fechaExpiracion = fechaActual.plusDays(1);
        return fechaExpiracion.toInstant(zonaHoraria.getRules().getOffset(fechaExpiracion)).toEpochMilli();
    }

    public void revokeToken(String token) {
        // TODO: Implementar blacklist de tokens revocados
    }

    // Método alias para compatibilidad
    public String generateToken(Usuario usuario) {
        return generarToken(usuario);
    }

}
