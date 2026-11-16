package co.edu.uis.siminversion.carga;

import java.io.Serializable;
import java.util.UUID;

/**
 * Validación aceptada que espera en la sesión la confirmación del administrador (REQ-018). El código viaja
 * en el formulario de confirmación: así una página vieja no puede confirmar una validación distinta de la
 * que muestra.
 */
public record CargaPendiente(String codigo, ValidacionCarga validacion) implements Serializable {

    public static CargaPendiente de(ValidacionCarga validacion) {
        return new CargaPendiente(UUID.randomUUID().toString(), validacion);
    }
}
