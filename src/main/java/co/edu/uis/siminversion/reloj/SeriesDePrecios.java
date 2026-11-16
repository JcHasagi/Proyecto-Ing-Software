package co.edu.uis.siminversion.reloj;

import co.edu.uis.siminversion.precio.ProveedorDePrecios;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Cierres de los instrumentos que toca un avance del reloj, cargados una sola vez por instrumento.
 * Evita una consulta por instrumento y por día al avanzar meses o años.
 */
final class SeriesDePrecios {

    private final ProveedorDePrecios proveedorDePrecios;
    private final LocalDate desde;
    private final LocalDate hasta;
    private final Map<Long, NavigableMap<LocalDate, BigDecimal>> cierres = new HashMap<>();

    SeriesDePrecios(ProveedorDePrecios proveedorDePrecios, LocalDate desde, LocalDate hasta) {
        this.proveedorDePrecios = proveedorDePrecios;
        this.desde = desde;
        this.hasta = hasta;
    }

    /** Cierre exacto de ese día; vacío si el instrumento no cotizó (RN-08). */
    Optional<BigDecimal> cierreEn(long instrumentoId, LocalDate dia) {
        return Optional.ofNullable(serie(instrumentoId).get(dia));
    }

    /** Último cierre en o antes de ese día, para valorar posiciones en días sin cotización. */
    Optional<BigDecimal> ultimoCierreHasta(long instrumentoId, LocalDate dia) {
        return Optional.ofNullable(serie(instrumentoId).floorEntry(dia)).map(Map.Entry::getValue);
    }

    private NavigableMap<LocalDate, BigDecimal> serie(long instrumentoId) {
        return cierres.computeIfAbsent(instrumentoId, id -> {
            NavigableMap<LocalDate, BigDecimal> serie = new TreeMap<>();
            proveedorDePrecios.ultimoCierreHasta(id, desde).ifPresent(precio -> serie.put(precio.fecha(), precio.cierre()));
            proveedorDePrecios.historico(id, desde.plusDays(1), hasta)
                    .forEach(precio -> serie.put(precio.fecha(), precio.cierre()));
            return serie;
        });
    }
}
