package co.edu.uis.siminversion.carga;

import java.time.LocalDate;
import java.util.List;

/**
 * Resultado de una carga masiva. Si no fue aceptada, no se insertó ninguna fila.
 * {@code fechasOmitidas} son las fechas que ya existían en la BD y no se sobrescribieron.
 */
public record ResultadoCarga(boolean aceptada, String simbolo, int filasLeidas, int filasInsertadas,
                             List<LocalDate> fechasOmitidas, List<String> errores, List<String> advertencias) {

    public static ResultadoCarga rechazada(String simbolo, List<String> errores) {
        return new ResultadoCarga(false, simbolo, 0, 0, List.of(), List.copyOf(errores), List.of());
    }

    public static ResultadoCarga aceptada(String simbolo, int filasLeidas, int filasInsertadas,
                                          List<LocalDate> fechasOmitidas, List<String> advertencias) {
        return new ResultadoCarga(true, simbolo, filasLeidas, filasInsertadas, List.copyOf(fechasOmitidas),
                List.of(), List.copyOf(advertencias));
    }
}
