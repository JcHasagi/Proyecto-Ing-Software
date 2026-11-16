package co.edu.uis.siminversion.portafolio;

import co.edu.uis.siminversion.transaccion.EstadoDelLibro;

/** Resumen de un portafolio y el estado del libro con el que se calculó. */
public record ResumenConLibro(ResumenPortafolio resumen, EstadoDelLibro libro) {
}
