package co.edu.uis.siminversion.instrumento;

import co.edu.uis.siminversion.precio.PrecioDiario;

/** Fila del catálogo (REQ-004). {@code ultimoCierre} es null si no hay precios hasta la fecha límite. */
public record ResumenInstrumento(String simbolo, String nombre, TipoInstrumento tipo, String sector, boolean activo,
                                 PrecioDiario ultimoCierre) {

    static ResumenInstrumento de(Instrumento instrumento, PrecioDiario ultimoCierre) {
        return new ResumenInstrumento(instrumento.getSimbolo(), instrumento.getNombre(), instrumento.getTipo(),
                instrumento.getSector(), instrumento.isActivo(), ultimoCierre);
    }

    public String iniciales() {
        return Instrumento.inicialesDe(simbolo, tipo);
    }
}
