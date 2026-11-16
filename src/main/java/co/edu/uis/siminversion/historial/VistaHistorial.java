package co.edu.uis.siminversion.historial;

import co.edu.uis.siminversion.portafolio.Portafolio;
import co.edu.uis.siminversion.transaccion.Transaccion;
import java.util.List;

/** Historial de transacciones (REQ-012) en orden cronológico, con lo necesario para armar los filtros. */
public record VistaHistorial(Portafolio portafolio, FiltroHistorial filtro, List<Transaccion> transacciones,
                             int totalDelPortafolio, List<String> simbolosOperados) {
}
