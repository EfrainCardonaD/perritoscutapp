package com.cut.cardona.controllers.api;



import com.cut.cardona.errores.ValidacionDeIntegridad;
import com.cut.cardona.modelo.usuarios.DatosAutenticacionUsuario;
import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import com.cut.cardona.security.CustomUserDetails;
import com.cut.cardona.security.DatosJWTToken;
import com.cut.cardona.security.PasswordService;
import com.cut.cardona.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@RestController
@RequestMapping("api/login")
public class AutenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;


    //test
    @Autowired
    PasswordService passwordService ;
    @Autowired
    private RepositorioUsuario repositorioUsuario;

    @PostMapping
    public ResponseEntity autenticarUsuario(@RequestBody DatosAutenticacionUsuario datosAutenticacionUsuario) {
        try {
            Authentication authenticationToken = new UsernamePasswordAuthenticationToken(
                    datosAutenticacionUsuario.userName(),
                    datosAutenticacionUsuario.password()
            );

            Authentication authenticate = authenticationManager.authenticate(authenticationToken);
            CustomUserDetails userDetails = (CustomUserDetails) authenticate.getPrincipal();
            Usuario usuario = userDetails.getUsuario();

            var JWTtoken = tokenService.generarToken(usuario);
            if (JWTtoken != null) {
                usuario.setUltimoAcceso(Timestamp.from(ZonedDateTime.now(ZoneId.of("America/Mexico_City")).toInstant()));
                repositorioUsuario.save(usuario);
            }

            return ResponseEntity.ok(new DatosJWTToken(JWTtoken));

        } catch (BadCredentialsException e) {
            throw new ValidacionDeIntegridad("Usuario o contraseña incorrectos");
        }
    }

    @GetMapping
    public void test() {

        System.out.println(passwordService.comprobarContrasenia("efra105", "123456"));

    }

}
