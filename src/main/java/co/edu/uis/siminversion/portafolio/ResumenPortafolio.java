package co.edu.uis.siminversion.portafolio;

import co.edu.uis.siminversion.escenario.Escenario;
import co.edu.uis.siminversion.orden.Orden;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/** Vista del portafolio a su fecha simulada. El valor total incluye el saldo de la cuenta de ahorro. */
public record ResumenPortafolio(Portafolio portafolio, BigDecimal efectivo, List<PosicionValorada> posiciones,
                                BigDecimal valorPosiciones, boolean tieneCuentaAhorro, BigDecimal saldoAhorro,
                                List<Orden> ordenesPendientes, List<Orden> ordenesRecientes, Escenario escenario,
                                LocalDate fechaLimiteDelReloj) {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public BigDecimal valorTotal() {
        return efectivo.add(valorPosiciones).add(saldoAhorro);
    }

    /** Ganancia o pérdida total frente al saldo inicial, en USD. */
    public BigDecimal rendimiento() {
        return valorTotal().subtract(portafolio.getSaldoInicial());
    }

    /** Variación del valor total frente al saldo inicial, en %. */
    public BigDecimal variacionPorcentual() {
        return rendimiento().multiply(CIEN).divide(portafolio.getSaldoInicial(), 2, RoundingMode.HALF_EVEN);
    }

    public String tendencia() {
        int signo = variacionPorcentual().signum();
        return signo > 0 ? "positivo" : signo < 0 ? "negativo" : "";
    }
}
