package co.edu.uis.siminversion.escenario;

/** Escenario con cuántos instrumentos del catálogo tienen precios en su período (por ejemplo, sin cripto en 2008). */
public record VistaEscenario(Escenario escenario, long instrumentosConPrecios, long instrumentosEnCatalogo) {
}
