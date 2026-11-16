package co.edu.uis.siminversion.transaccion;

import co.edu.uis.siminversion.comun.ReglasDeOperacion;
import co.edu.uis.siminversion.instrumento.Instrumento;
import java.math.BigDecimal;

/**
 * Posición derivada del libro (no se persiste). {@code costoBaseUnitario} es el promedio ponderado de los
 * precios de compra, sin comisiones (RN-05; requerimientos.md, anexo).
 */
public record Posicion(Instrumento instrumento, long cantidad, BigDecimal costoBaseUnitario) {

    public BigDecimal costoTotal() {
        return ReglasDeOperacion.montoOperado(cantidad, costoBaseUnitario);
    }
}
