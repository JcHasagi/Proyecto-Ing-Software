package co.edu.uis.siminversion.usuario;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/** REQ-001 (registro) y REQ-002 (inicio de sesión; el cierre lo maneja Spring Security en /logout). */
@Controller
public class CuentaControlador {

    private final RegistroUsuarioServicio registroUsuarioServicio;

    public CuentaControlador(RegistroUsuarioServicio registroUsuarioServicio) {
        this.registroUsuarioServicio = registroUsuarioServicio;
    }

    @GetMapping("/login")
    public String login() {
        return "cuenta/login";
    }

    @GetMapping("/registro")
    public String formularioRegistro(Model modelo) {
        modelo.addAttribute("formulario", new FormularioRegistro());
        return "cuenta/registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("formulario") FormularioRegistro formulario,
                            BindingResult errores) {
        if (!formulario.contrasenasCoinciden()) {
            errores.rejectValue("confirmacionContrasena", "noCoincide", "Las contraseñas no coinciden.");
        }
        if (errores.hasErrors()) {
            return "cuenta/registro";
        }
        try {
            registroUsuarioServicio.registrarInversionista(
                    formulario.getCorreo(), formulario.getNombre(), formulario.getContrasena());
        } catch (CorreoYaRegistradoException correoDuplicado) {
            errores.rejectValue("correo", "duplicado", "Ya existe una cuenta con ese correo.");
            return "cuenta/registro";
        } catch (RegistroInvalidoException datosInvalidos) {
            errores.reject("invalido", datosInvalidos.getMessage());
            return "cuenta/registro";
        }
        return "redirect:/login?registrado";
    }
}
