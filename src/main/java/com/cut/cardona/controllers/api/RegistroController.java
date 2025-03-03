package com.cut.cardona.controllers.api;

import com.cut.cardona.errores.ErrorHandler;
import com.cut.cardona.modelo.usuarios.DtoRegistroUsuario;
import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.Roles;
import com.cut.cardona.modelo.usuarios.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller("api/registro")
public class RegistroController {
    @Autowired
    RepositorioUsuario repositorioUsuario;
    @Autowired
    PasswordEncoder passwordEncoder;




    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        // Añadir un nuevo objeto de usuario al modelo
        model.addAttribute("registroUsuario", new DtoRegistroUsuario("","","","","",false));
        return "registro"; // Devuelve la plantilla Thymeleaf
    }

    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute("registroUsuario") DtoRegistroUsuario registroUsuario,
                                   BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        // Validar campos (como si las contraseñas coinciden, los emails son iguales, etc.)
        validarDTO(registroUsuario, result);
        if (result.hasErrors()) {
            return "registro";
        }

        Usuario usuario = new Usuario(registroUsuario, passwordEncoder);
        try {
            usuario.setRol(Roles.ROLE_USER);
            repositorioUsuario.save(usuario);
            String successfulMessage = "Registro exitoso.<br>Ahora puedes iniciar sesión.<br>" + usuario.getUsername();
            redirectAttributes.addFlashAttribute("successfulRegistroUsuario", successfulMessage);
        } catch (DataIntegrityViolationException e) {
            ErrorHandler.handleDataIntegrityViolationException(e, redirectAttributes);
            return "redirect:/registro";
        } catch (Exception e) {
            ErrorHandler.handleGenericException(e, redirectAttributes);
            return "redirect:/registro";
        }
        // Guardar el usuario en la base de datos (lógica de persistencia aquí)

        // Redirigir a la página de éxito o de login

        return "redirect:/login";
    }

    private static void validarDTO(DtoRegistroUsuario registroUsuario, BindingResult result) {
        if (!registroUsuario.userName().matches("^[a-zA-Z0-9]*$")) {
            result.rejectValue("userName", "error.userName", "El nombre de usuario solo puede contener letras, números y no puede estar vacío");
        }
        if (!registroUsuario.email().equals(registroUsuario.confirmEmail())) {
            result.rejectValue("confirmEmail", "error.confirmEmail", "Los correos electrónicos no coinciden");
        }
        if (!registroUsuario.password().equals(registroUsuario.confirmPassword())) {
            result.rejectValue("confirmPassword", "error.confirmPassword", "Las contraseñas no coinciden");
        }
    }


}

