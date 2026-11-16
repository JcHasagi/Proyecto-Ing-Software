package co.edu.uis.siminversion.comun;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** RN-01, RN-04 y RN-10 (montos en USD, divididos entre 1.000 respecto al SRS). */
class ReglasDeOperacionTest {

    private static BigDecimal usd(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void laComisionEsElTresPorMilConUnMinimoDeCincoDolares() {
        assertThat(ReglasDeOperacion.comision(usd("1000.00"))).isEqualTo(usd("5.00"));
        assertThat(ReglasDeOperacion.comision(usd("1666.67"))).isEqualTo(usd("5.00"));
        assertThat(ReglasDeOperacion.comision(usd("1700.00"))).isEqualTo(usd("5.10"));
        assertThat(ReglasDeOperacion.comision(usd("10000.00"))).isEqualTo(usd("30.00"));
    }

    @Test
    void laComisionSeRedondeaConRedondeoBancario() {
        // 5,625 queda en 5,62 (el 2 es par) y 5,475 sube a 5,48 (el 7 es impar).
        assertThat(ReglasDeOperacion.comision(usd("1875.00"))).isEqualTo(usd("5.62"));
        assertThat(ReglasDeOperacion.comision(usd("1825.00"))).isEqualTo(usd("5.48"));
    }

    @Test
    void elMontoOperadoSeRedondeaADosDecimalesConRedondeoBancario() {
        assertThat(ReglasDeOperacion.montoOperado(1, usd("2.675"))).isEqualTo(usd("2.68"));
        assertThat(ReglasDeOperacion.montoOperado(1, usd("2.665"))).isEqualTo(usd("2.66"));
        assertThat(ReglasDeOperacion.montoOperado(3, usd("0.335"))).isEqualTo(usd("1.00"));
        assertThat(ReglasDeOperacion.montoOperado(1_000_000, usd("0.00026068"))).isEqualTo(usd("260.68"));
    }

    @Test
    void elSaldoInicialDebeEstarEntreMilYCienMilDolares() {
        assertThat(ReglasDeOperacion.saldoInicialValido(usd("1000"))).isTrue();
        assertThat(ReglasDeOperacion.saldoInicialValido(usd("100000"))).isTrue();
        assertThat(ReglasDeOperacion.saldoInicialValido(usd("999.99"))).isFalse();
        assertThat(ReglasDeOperacion.saldoInicialValido(usd("100000.01"))).isFalse();
        assertThat(ReglasDeOperacion.saldoInicialValido(null)).isFalse();
        assertThat(ReglasDeOperacion.SALDO_INICIAL_POR_DEFECTO).isEqualTo(usd("10000.00"));
    }
}
