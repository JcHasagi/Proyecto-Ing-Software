package co.edu.uis.siminversion.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos del formulario de registro (REQ-001). El servicio vuelve a validar las reglas. */
public class FormularioRegistro {

    @NotBlank(message = "Ingresa tu correo.")
    @Email(message = "El correo no es válido.")
    @Size(max = 254, message = "El correo es demasiado largo.")
    private String correo;

    @NotBlank(message = "Ingresa tu nombre.")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres.")
    private String nombre;

    @NotBlank(message = "Ingresa una contraseña.")
    @Size(min = RegistroUsuarioServicio.LONGITUD_MINIMA_CONTRASENA,
            message = "La contraseña debe tener al menos {min} caracteres.")
    private String contrasena;

    @NotBlank(message = "Confirma la contraseña.")
    private String confirmacionContrasena;

    public boolean contrasenasCoinciden() {
        return contrasena != null && contrasena.equals(confirmacionContrasena);
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getConfirmacionContrasena() {
        return confirmacionContrasena;
    }

    public void setConfirmacionContrasena(String confirmacionContrasena) {
        this.confirmacionContrasena = confirmacionContrasena;
    }
}
