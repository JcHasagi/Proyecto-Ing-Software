package co.edu.uis.siminversion.carga;

import co.edu.uis.siminversion.precio.PrecioDiario;
import java.util.List;

/** Filas válidas en orden cronológico y errores con número de línea. */
public record ResultadoLectura(List<PrecioDiario> filas, List<String> errores) {

    public boolean tieneErrores() {
        return !errores.isEmpty();
    }
}
