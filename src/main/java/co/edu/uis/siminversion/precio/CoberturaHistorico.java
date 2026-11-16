package co.edu.uis.siminversion.precio;

import java.time.LocalDate;

/** Cuántos días con precio tiene un instrumento y entre qué fechas. */
public record CoberturaHistorico(Long instrumentoId, Long dias, LocalDate primeraFecha, LocalDate ultimaFecha) {
}
