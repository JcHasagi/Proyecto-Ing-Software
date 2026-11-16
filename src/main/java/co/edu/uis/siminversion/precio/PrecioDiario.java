package co.edu.uis.siminversion.precio;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Precio de un día, por unidad, en la moneda de referencia (USD).
 * {@code volumen} puede ser null: las divisas no tienen volumen centralizado.
 * Es serializable porque una carga validada espera en la sesión hasta que el administrador la confirma.
 */
public record PrecioDiario(LocalDate fecha, BigDecimal apertura, BigDecimal maximo, BigDecimal minimo,
                           BigDecimal cierre, Long volumen) implements Serializable {
}
