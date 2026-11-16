package co.edu.uis.siminversion.rendimiento;

import co.edu.uis.siminversion.instrumento.Instrumento;
import java.math.BigDecimal;

/** P&L de un instrumento operado: realizado por sus ventas y no realizado por lo que aún se tiene. */
public record ResultadoPorInstrumento(Instrumento instrumento, long cantidadActual, BigDecimal gananciaRealizada,
                                      BigDecimal gananciaNoRealizada) {

    public BigDecimal total() {
        return gananciaRealizada.add(gananciaNoRealizada);
    }
}
