package co.edu.uis.siminversion.reloj;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Valor del portafolio al cierre de un día bursátil (fila de instantanea_portafolio).
 * valorTotal = efectivo + valorPosiciones + saldoAhorro.
 */
public record InstantaneaDiaria(long portafolioId, LocalDate fecha, BigDecimal efectivo, BigDecimal valorPosiciones,
                                BigDecimal saldoAhorro, BigDecimal valorTotal) {
}
