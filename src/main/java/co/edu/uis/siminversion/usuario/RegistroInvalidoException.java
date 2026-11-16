package co.edu.uis.siminversion.usuario;

/** Datos de registro que no cumplen las reglas del servidor (por ejemplo, la política de contraseñas). */
public class RegistroInvalidoException extends RuntimeException {

    public RegistroInvalidoException(String mensaje) {
        super(mensaje);
    }
}
