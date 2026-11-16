package co.edu.uis.siminversion.instrumento;

import co.edu.uis.siminversion.precio.CoberturaHistorico;

/** Instrumento en la administración del catálogo, con su cobertura de precios (null si no tiene). */
public record FilaAdministracion(Instrumento instrumento, CoberturaHistorico cobertura) {
}
