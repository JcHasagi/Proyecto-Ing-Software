package co.edu.uis.siminversion.ahorro;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Política de la cuenta de ahorro: saldo × tasa × días ÷ 365, a centavos con redondeo bancario. */
class CalculadoraDeInteresesTest {

    private static BigDecimal usd(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void elInteresCubreLosDiasTranscurridos() {
        assertThat(CalculadoraDeIntereses.interes(usd("10000"), usd("0.04"), 1)).isEqualTo(usd("1.10"));
        assertThat(CalculadoraDeIntereses.interes(usd("10000"), usd("0.04"), 3)).isEqualTo(usd("3.29"));
        assertThat(CalculadoraDeIntereses.interes(usd("10000"), usd("0.04"), 365)).isEqualTo(usd("400.00"));
    }

    @Test
    void sinSaldoSinDiasOSinTasaNoHayInteres() {
        assertThat(CalculadoraDeIntereses.interes(usd("0"), usd("0.04"), 5)).isEqualTo(usd("0.00"));
        assertThat(CalculadoraDeIntereses.interes(usd("1000"), usd("0.04"), 0)).isEqualTo(usd("0.00"));
        assertThat(CalculadoraDeIntereses.interes(usd("1000"), usd("0"), 30)).isEqualTo(usd("0.00"));
    }

    @Test
    void seRedondeaACentavosConRedondeoBancario() {
        // 0,005 queda en 0,00 (el 0 es par) y 0,015 sube a 0,02 (el 1 es impar).
        assertThat(CalculadoraDeIntereses.interes(usd("91.25"), usd("0.02"), 1)).isEqualTo(usd("0.00"));
        assertThat(CalculadoraDeIntereses.interes(usd("273.75"), usd("0.02"), 1)).isEqualTo(usd("0.02"));
    }
}
