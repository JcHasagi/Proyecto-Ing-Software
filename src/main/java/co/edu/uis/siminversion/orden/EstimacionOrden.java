package co.edu.uis.siminversion.orden;

import co.edu.uis.siminversion.instrumento.Instrumento;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Estimación que se muestra antes de confirmar una orden (REQ-006/007). Usa el último cierre visible;
 * el precio real será el cierre del siguiente día bursátil (RN-03).
 *
 * @param totalEstimado       compra: monto + comisión; venta: monto − comisión (ingreso neto)
 * @param efectivoDisponible  efectivo del libro menos lo comprometido por compras pendientes
 * @param efectivoResultante  efectivo disponible después de la orden
 * @param unidadesDisponibles posición menos ventas pendientes
 */
public record EstimacionOrden(long portafolioId, Instrumento instrumento, TipoOrden tipo, long cantidad,
                              BigDecimal precioReferencia, LocalDate fechaPrecio, LocalDate fechaSimulada,
                              BigDecimal monto, BigDecimal comision, BigDecimal totalEstimado,
                              BigDecimal efectivoDisponible, BigDecimal efectivoResultante, long unidadesDisponibles) {

    public boolean esCompra() {
        return tipo == TipoOrden.COMPRA;
    }
}
