package co.edu.uis.siminversion.reloj;

import co.edu.uis.siminversion.orden.Orden;
import co.edu.uis.siminversion.orden.TipoOrden;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Resumen que se muestra al terminar de avanzar el reloj (REQ-009). */
public record ResumenAvance(LocalDate desde, LocalDate hasta, int diasBursatiles, List<OrdenProcesada> ejecutadas,
                            List<OrdenProcesada> rechazadas, List<OrdenProcesada> canceladas, BigDecimal interesesGanados,
                            BigDecimal efectivo, BigDecimal valorTotal, boolean finalizado) implements Serializable {

    /** Una orden que se ejecutó, rechazó o canceló durante el avance. */
    public record OrdenProcesada(String simbolo, TipoOrden tipo, long cantidad, LocalDate fecha, BigDecimal precio,
                                 BigDecimal monto, BigDecimal comision, String motivo) implements Serializable {
    }

    static final class Constructor {

        private final List<OrdenProcesada> ejecutadas = new ArrayList<>();
        private final List<OrdenProcesada> rechazadas = new ArrayList<>();
        private final List<OrdenProcesada> canceladas = new ArrayList<>();
        private BigDecimal intereses = BigDecimal.ZERO;

        void interes(BigDecimal monto) {
            intereses = intereses.add(monto);
        }

        void ejecutada(Orden orden, LocalDate fecha, BigDecimal precio, BigDecimal monto, BigDecimal comision) {
            ejecutadas.add(new OrdenProcesada(orden.getInstrumento().getSimbolo(), orden.getTipo(), orden.getCantidad(),
                    fecha, precio, monto, comision, null));
        }

        void rechazada(Orden orden, LocalDate fecha) {
            rechazadas.add(sinPrecio(orden, fecha));
        }

        void cancelada(Orden orden, LocalDate fecha) {
            canceladas.add(sinPrecio(orden, fecha));
        }

        private static OrdenProcesada sinPrecio(Orden orden, LocalDate fecha) {
            return new OrdenProcesada(orden.getInstrumento().getSimbolo(), orden.getTipo(), orden.getCantidad(), fecha,
                    null, null, null, orden.getMotivo());
        }

        ResumenAvance construir(LocalDate desde, LocalDate hasta, int dias, InstantaneaDiaria ultimoDia,
                                boolean finalizado) {
            return new ResumenAvance(desde, hasta, dias, List.copyOf(ejecutadas), List.copyOf(rechazadas),
                    List.copyOf(canceladas), intereses, ultimoDia.efectivo(), ultimoDia.valorTotal(), finalizado);
        }
    }
}
