package co.edu.uis.siminversion.usuario;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Administrador inicial, tomado de SIMINVERSION_ADMIN_CORREO y SIMINVERSION_ADMIN_CONTRASENA. */
@ConfigurationProperties(prefix = "siminversion.admin")
public record PropiedadesAdministrador(String correo, String contrasena) {

    public boolean estaConfigurado() {
        return correo != null && !correo.isBlank() && contrasena != null && !contrasena.isBlank();
    }
}
