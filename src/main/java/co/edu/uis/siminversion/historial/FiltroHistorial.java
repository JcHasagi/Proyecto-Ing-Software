package co.edu.uis.siminversion.historial;

import co.edu.uis.siminversion.transaccion.TipoTransaccion;
import co.edu.uis.siminversion.transaccion.Transaccion;
import java.time.LocalDate;

/** Filtro de REQ-012; cualquier campo puede venir vacío. */
public record FiltroHistorial(TipoTransaccion tipo, String simbolo, LocalDate desde, LocalDate hasta) {

    public static final FiltroHistorial SIN_FILTRO = new FiltroHistorial(null, null, null, null);

    public boolean estaActivo() {
        return tipo != null || simbolo != null || desde != null || hasta != null;
    }

    boolean incluye(Transaccion transaccion) {
        return (tipo == null || transaccion.getTipo() == tipo)
                && (simbolo == null || (transaccion.getInstrumento() != null
                        && transaccion.getInstrumento().getSimbolo().equals(simbolo)))
                && (desde == null || !transaccion.getFechaSim().isBefore(desde))
                && (hasta == null || !transaccion.getFechaSim().isAfter(hasta));
    }
}
