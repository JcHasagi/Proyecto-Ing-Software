package co.edu.uis.siminversion.portafolio;

import co.edu.uis.siminversion.transaccion.Posicion;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Posición valorada al último cierre disponible en la fecha simulada. */
public record PosicionValorada(Posicion posicion, BigDecimal precioActual, LocalDate fechaPrecio,
                               BigDecimal valorMercado, BigDecimal gananciaNoRealizada) {

    public String tendencia() {
        return gananciaNoRealizada.signum() > 0 ? "positivo" : gananciaNoRealizada.signum() < 0 ? "negativo" : "";
    }
}
