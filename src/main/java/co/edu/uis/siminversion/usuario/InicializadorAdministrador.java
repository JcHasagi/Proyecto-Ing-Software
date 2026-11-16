package co.edu.uis.siminversion.usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Crea la primera cuenta de administrador al arrancar. El registro público solo crea inversionistas
 * y el SRS aún no tiene un requerimiento para crear administradores.
 */
@Component
public class InicializadorAdministrador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorAdministrador.class);

    private final PropiedadesAdministrador propiedades;
    private final UsuarioRepositorio usuarioRepositorio;
    private final RegistroUsuarioServicio registroUsuarioServicio;

    public InicializadorAdministrador(PropiedadesAdministrador propiedades, UsuarioRepositorio usuarioRepositorio,
                                      RegistroUsuarioServicio registroUsuarioServicio) {
        this.propiedades = propiedades;
        this.usuarioRepositorio = usuarioRepositorio;
        this.registroUsuarioServicio = registroUsuarioServicio;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        if (!propiedades.estaConfigurado()) {
            log.info("Sin administrador inicial: define SIMINVERSION_ADMIN_CORREO y SIMINVERSION_ADMIN_CONTRASENA.");
            return;
        }
        String correo = RegistroUsuarioServicio.normalizarCorreo(propiedades.correo());
        if (usuarioRepositorio.existsByCorreo(correo)) {
            return;
        }
        registroUsuarioServicio.registrar(correo, "Administrador", propiedades.contrasena(), Rol.ADMINISTRADOR);
        log.info("Administrador inicial creado: {}", correo);
    }
}
