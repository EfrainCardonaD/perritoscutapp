package com.cut.cardona.controllers.service;
import com.cut.cardona.modelo.dto.usuarios.DtoUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    public DtoUsuario toDto(Usuario usuario) {
        return new DtoUsuario(usuario);
    }

    // Otros métodos relacionados con usuarios
}