package co.edu.uis.siminversion.precio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Único punto de acceso a precios para la lógica de negocio. Hoy lo implementa el histórico en BD;
 * una fuente en vivo sería otra implementación sin tocar los servicios.
 *
 * <p>El calendario es por instrumento: una fecha es día bursátil para un instrumento si tiene
 * cierre de ese instrumento en el histórico (RN-08). Así las criptomonedas cotizan fines de semana
 * y las acciones no.
 */
public interface ProveedorDePrecios {

    /** Cierre del instrumento en esa fecha; vacío si no fue día bursátil para él. */
    Optional<BigDecimal> precioCierre(long instrumentoId, LocalDate fecha);

    /** Primera fecha posterior a {@code fecha} con cierre del instrumento (RN-03). */
    Optional<LocalDate> siguienteDiaBursatil(long instrumentoId, LocalDate fecha);

    /** Último precio en o antes de {@code fecha}; sirve para valorar en días sin cotización. */
    Optional<PrecioDiario> ultimoCierreHasta(long instrumentoId, LocalDate fecha);

    /** Serie diaria entre ambas fechas, inclusive, en orden cronológico. */
    List<PrecioDiario> historico(long instrumentoId, LocalDate desde, LocalDate hasta);

    /**
     * Días bursátiles del mercado en (despuesDe, hasta]: fechas con cierre de al menos un instrumento.
     * Son los días que recorre el reloj de simulación (RN-08).
     */
    List<LocalDate> diasBursatilesDelMercado(LocalDate despuesDe, LocalDate hasta);

    /** Primer día bursátil del mercado en o después de {@code fecha}. */
    Optional<LocalDate> primerDiaBursatilDelMercadoDesde(LocalDate fecha);

    Optional<LocalDate> primeraFechaDelHistorico();

    Optional<LocalDate> ultimaFechaDelHistorico();

    /** Cuántos instrumentos activos tienen al menos un cierre entre ambas fechas, inclusive (REQ-014). */
    long instrumentosActivosConPrecios(LocalDate desde, LocalDate hasta);

    /** Días con precio, primera y última fecha de cada instrumento que tiene histórico (REQ-017). */
    List<CoberturaHistorico> coberturaPorInstrumento();
}
