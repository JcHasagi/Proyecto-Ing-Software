package co.edu.uis.siminversion.ahorro;

import co.edu.uis.siminversion.comun.ReglasDeOperacion;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Política de la cuenta de ahorro (decisión del 2026-11-15): tasa nominal anual fija, interés diario simple
 * sobre el saldo y capitalización en cada día bursátil que procesa el reloj. Si el reloj salta días sin
 * cotización, el interés cubre todos los días transcurridos.
 */
public final class CalculadoraDeIntereses {

    private static final BigDecimal DIAS_DEL_ANIO = BigDecimal.valueOf(365);

    private CalculadoraDeIntereses() {
    }

    /** saldo × tasa × días ÷ 365, redondeado a centavos (RN-10). */
    public static BigDecimal interes(BigDecimal saldo, BigDecimal tasaAnual, long dias) {
        if (saldo.signum() <= 0 || dias <= 0 || tasaAnual.signum() <= 0) {
            return ReglasDeOperacion.redondearDinero(BigDecimal.ZERO);
        }
        BigDecimal exacto = saldo.multiply(tasaAnual).multiply(BigDecimal.valueOf(dias))
                .divide(DIAS_DEL_ANIO, 10, RoundingMode.HALF_EVEN);
        return ReglasDeOperacion.redondearDinero(exacto);
    }
}
