package co.edu.uis.siminversion.carga;

import co.edu.uis.siminversion.precio.PrecioDiario;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumen de la validación de un archivo de históricos (paso 1 de REQ-018). Todavía no se ha guardado nada:
 * {@code filasNuevas} son las que se guardarán si el administrador confirma y {@code fechasOmitidas}, las
 * que ya existían para el instrumento y no se sobrescriben. Si no fue aceptada, {@code errores} dice por qué.
 */
public record ValidacionCarga(boolean aceptada, String simbolo, Long instrumentoId, int filasLeidas,
                              List<PrecioDiario> filasNuevas, List<LocalDate> fechasOmitidas,
                              List<String> errores, List<String> advertencias) implements Serializable {

    public static ValidacionCarga rechazada(String simbolo, List<String> errores) {
        return new ValidacionCarga(false, simbolo, null, 0, List.of(), List.of(), List.copyOf(errores), List.of());
    }

    public static ValidacionCarga aceptada(String simbolo, long instrumentoId, int filasLeidas,
                                           List<PrecioDiario> filasNuevas, List<LocalDate> fechasOmitidas,
                                           List<String> advertencias) {
        return new ValidacionCarga(true, simbolo, instrumentoId, filasLeidas, List.copyOf(filasNuevas),
                List.copyOf(fechasOmitidas), List.of(), List.copyOf(advertencias));
    }

    /** Sin filas nuevas no hay nada que confirmar: todas las fechas del archivo ya estaban cargadas. */
    public boolean tieneFilasNuevas() {
        return !filasNuevas.isEmpty();
    }

    public int cantidadDeFilasNuevas() {
        return filasNuevas.size();
    }
}
