package co.edu.uis.siminversion.rendimiento;

import co.edu.uis.siminversion.portafolio.ResumenPortafolio;
import co.edu.uis.siminversion.reloj.InstantaneaDiaria;
import java.math.BigDecimal;
import java.util.List;

/**
 * REQ-011: valoración y rendimiento del portafolio a su fecha simulada.
 * rendimiento total = P&L realizado + P&L no realizado − comisiones + intereses (salvo diferencias de centavos
 * por redondeo del costo base).
 */
public record Rendimiento(ResumenPortafolio resumen, BigDecimal gananciaRealizada, BigDecimal gananciaNoRealizada,
                          BigDecimal comisionesPagadas, BigDecimal interesesGanados,
                          List<ResultadoPorInstrumento> porInstrumento, List<InstantaneaDiaria> instantaneas) {

    public boolean tieneEvolucion() {
        return !instantaneas.isEmpty();
    }

    /** Clase CSS según el signo de un valor: "positivo", "negativo" o vacía. */
    public String tendenciaDe(BigDecimal valor) {
        return valor.signum() > 0 ? "positivo" : valor.signum() < 0 ? "negativo" : "";
    }

    public List<String> fechasGrafico() {
        return instantaneas.stream().map(instantanea -> instantanea.fecha().toString()).toList();
    }

    public List<String> valoresGrafico() {
        return instantaneas.stream().map(instantanea -> instantanea.valorTotal().toPlainString()).toList();
    }
}
