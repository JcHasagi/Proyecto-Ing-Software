package co.edu.uis.siminversion.estadistica;

import co.edu.uis.siminversion.orden.EstadoOrden;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** REQ-019: solo totales del rango, sin datos personales. Las fechas son reales (no simuladas). */
public record EstadisticasDeUso(LocalDate desde, LocalDate hasta, long usuariosRegistrados, long portafoliosCreados,
                                long portafoliosDeEscenario, Map<EstadoOrden, Long> ordenesPorEstado,
                                List<InstrumentoOperado> instrumentosMasOperados, long totalUsuarios,
                                long totalPortafolios) {

    public long ordenesEmitidas() {
        return ordenesPorEstado.values().stream().mapToLong(Long::longValue).sum();
    }

    public long ordenesEjecutadas() {
        return ordenesPorEstado.getOrDefault(EstadoOrden.EJECUTADA, 0L);
    }

    /** Instrumento y cuántas órdenes tuvo en el rango. */
    public record InstrumentoOperado(String simbolo, long ordenes) {
    }
}
