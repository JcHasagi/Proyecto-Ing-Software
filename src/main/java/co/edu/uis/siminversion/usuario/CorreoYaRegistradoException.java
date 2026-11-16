package co.edu.uis.siminversion.usuario;

public class CorreoYaRegistradoException extends RuntimeException {

    public CorreoYaRegistradoException(String correo) {
        super("Ya existe una cuenta con el correo " + correo);
    }
}
