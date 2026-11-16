package co.edu.uis.siminversion.usuario;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroUsuarioServicio {

    // Política provisional: el SRS aún no la define.
    public static final int LONGITUD_MINIMA_CONTRASENA = 8;
    /** bcrypt solo usa los primeros 72 bytes de la contraseña. */
    public static final int BYTES_MAXIMOS_CONTRASENA = 72;

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder codificadorContrasenas;
    private final Clock relojSistema;

    public RegistroUsuarioServicio(UsuarioRepositorio usuarioRepositorio, PasswordEncoder codificadorContrasenas,
                                   Clock relojSistema) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.codificadorContrasenas = codificadorContrasenas;
        this.relojSistema = relojSistema;
    }

    /** REQ-001: el registro público siempre crea un inversionista. */
    @Transactional
    public Usuario registrarInversionista(String correo, String nombre, String contrasena) {
        return registrar(correo, nombre, contrasena, Rol.INVERSIONISTA);
    }

    @Transactional
    public Usuario registrar(String correo, String nombre, String contrasena, Rol rol) {
        String correoNormalizado = normalizarCorreo(correo);
        if (correoNormalizado.isEmpty()) {
            throw new RegistroInvalidoException("El correo es obligatorio.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new RegistroInvalidoException("El nombre es obligatorio.");
        }
        validarContrasena(contrasena);
        if (usuarioRepositorio.existsByCorreo(correoNormalizado)) {
            throw new CorreoYaRegistradoException(correoNormalizado);
        }
        Usuario usuario = new Usuario(correoNormalizado, nombre.trim(), codificadorContrasenas.encode(contrasena), rol,
                LocalDateTime.now(relojSistema));
        try {
            return usuarioRepositorio.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException rechazoDeLaBase) {
            if (esCorreoRepetido(rechazoDeLaBase)) {
                // Otro registro con el mismo correo ganó la carrera; la restricción UNIQUE lo detuvo.
                throw new CorreoYaRegistradoException(correoNormalizado);
            }
            // Cualquier otro rechazo es un fallo real: no se disfraza de correo repetido.
            throw rechazoDeLaBase;
        }
    }

    private static boolean esCorreoRepetido(DataIntegrityViolationException rechazo) {
        if (!(rechazo.getCause() instanceof ConstraintViolationException violacion)) {
            return false;
        }
        String restriccion = violacion.getConstraintName();
        return violacion.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE
                || (restriccion != null && restriccion.toLowerCase(Locale.ROOT).contains("uq_usuario_correo"));
    }

    /** Los correos se guardan sin espacios y en minúsculas para que la unicidad no dependa de mayúsculas. */
    public static String normalizarCorreo(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    private static void validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.length() < LONGITUD_MINIMA_CONTRASENA) {
            throw new RegistroInvalidoException(
                    "La contraseña debe tener al menos " + LONGITUD_MINIMA_CONTRASENA + " caracteres.");
        }
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS_CONTRASENA) {
            throw new RegistroInvalidoException(
                    "La contraseña no puede superar " + BYTES_MAXIMOS_CONTRASENA + " bytes.");
        }
    }
}
