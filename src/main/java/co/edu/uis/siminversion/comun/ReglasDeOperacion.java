package co.edu.uis.siminversion.comun;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Montos de RN-01 y RN-04 en USD, la moneda de referencia. Son los del SRS divididos entre 1.000
 * (decisión del 2026-11-15) para que guarden proporción con los precios reales en USD.
 */
public final class ReglasDeOperacion {

    public static final BigDecimal SALDO_INICIAL_POR_DEFECTO = new BigDecimal("10000.00");
    public static final BigDecimal SALDO_INICIAL_MINIMO = new BigDecimal("1000.00");
    public static final BigDecimal SALDO_INICIAL_MAXIMO = new BigDecimal("100000.00");
    public static final BigDecimal TASA_COMISION = new BigDecimal("0.003");
    public static final BigDecimal COMISION_MINIMA = new BigDecimal("5.00");
    /** RN-10: el dinero se redondea a 2 decimales con redondeo bancario. */
    public static final int ESCALA_DINERO = 2;

    private ReglasDeOperacion() {
    }

    public static BigDecimal redondearDinero(BigDecimal valor) {
        return valor.setScale(ESCALA_DINERO, RoundingMode.HALF_EVEN);
    }

    /** Monto operado: cantidad por precio de cierre, redondeado (RN-10). */
    public static BigDecimal montoOperado(long cantidad, BigDecimal precio) {
        return redondearDinero(precio.multiply(BigDecimal.valueOf(cantidad)));
    }

    /** RN-04: 0,3 % del monto operado, con un mínimo de 5 USD. */
    public static BigDecimal comision(BigDecimal montoOperado) {
        return redondearDinero(montoOperado.multiply(TASA_COMISION)).max(COMISION_MINIMA);
    }

    /** RN-01. */
    public static boolean saldoInicialValido(BigDecimal saldo) {
        return saldo != null && saldo.compareTo(SALDO_INICIAL_MINIMO) >= 0 && saldo.compareTo(SALDO_INICIAL_MAXIMO) <= 0;
    }
}
