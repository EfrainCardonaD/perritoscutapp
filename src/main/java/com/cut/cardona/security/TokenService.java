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

    //@Value("${JWT_SECRET}")
    @Value("123456")
    private String apiSecret;

    public String generarToken(Usuario usuario) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(apiSecret);

            String token = JWT.create()
                    .withIssuer("graficos")
                    .withSubject(usuario.getUsername())
                    .withClaim("roles", usuario.getRol().toString())
                    .withClaim("id", usuario.getId())
                    .withExpiresAt(generarFechaDeExpiacion())
                    .sign(algorithm);
            return token;
        } catch (JWTCreationException exception) {
            // Invalid Signing configuration / Couldn't convert Claims.
            throw new RuntimeException();
        }

    }

    public String getSubject(String token) {
        DecodedJWT decodedJWT =  null;
        try {
            Algorithm algorithm = Algorithm.HMAC256(apiSecret);
            JWTVerifier verifier  = JWT.require(algorithm)
                    .withIssuer("graficos")
                    .build();
            decodedJWT = verifier.verify(token);
        } catch (JWTVerificationException exception) {
            throw  new RuntimeException("token invalido");
        }

        return decodedJWT.getSubject();

    }

    private Instant generarFechaDeExpiacion(){

        ZoneId zonaHoraria = ZoneId.of("America/New_York"); // Otra forma de representar UTC-5:00 (zona horaria de Nueva York)
        LocalDateTime fechaActual = LocalDateTime.now(zonaHoraria);
        LocalDateTime fechaExpiracion = fechaActual.plusDays(1);
        return fechaExpiracion.toInstant(zonaHoraria.getRules().getOffset(fechaExpiracion));
    }

    }



