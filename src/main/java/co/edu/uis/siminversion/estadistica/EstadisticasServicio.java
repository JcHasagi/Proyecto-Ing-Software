package co.edu.uis.siminversion.estadistica;

import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import co.edu.uis.siminversion.orden.EstadoOrden;
import co.edu.uis.siminversion.orden.OrdenRepositorio;
import co.edu.uis.siminversion.portafolio.PortafolioRepositorio;
import co.edu.uis.siminversion.usuario.UsuarioRepositorio;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** REQ-019: totales de uso entre dos fechas reales, ambas inclusive. */
@Service
public class EstadisticasServicio {

    private static final int INSTRUMENTOS_EN_RANKING = 5;

    private final UsuarioRepositorio usuarioRepositorio;
    private final PortafolioRepositorio portafolioRepositorio;
    private final OrdenRepositorio ordenRepositorio;

    public EstadisticasServicio(UsuarioRepositorio usuarioRepositorio, PortafolioRepositorio portafolioRepositorio,
                                OrdenRepositorio ordenRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.portafolioRepositorio = portafolioRepositorio;
        this.ordenRepositorio = ordenRepositorio;
    }

    @Transactional(readOnly = true)
    public EstadisticasDeUso calcular(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new OperacionInvalidaException("Indica las dos fechas del rango.");
        }
        if (desde.isAfter(hasta)) {
            throw new OperacionInvalidaException("La fecha «desde» es posterior a la fecha «hasta»; corrige el rango.");
        }
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();

        Map<EstadoOrden, Long> porEstado = new EnumMap<>(EstadoOrden.class);
        for (EstadoOrden estado : EstadoOrden.values()) {
            porEstado.put(estado, 0L);
        }
        for (Object[] fila : ordenRepositorio.contarPorEstado(inicio, finExclusivo)) {
            porEstado.put((EstadoOrden) fila[0], (Long) fila[1]);
        }
        List<EstadisticasDeUso.InstrumentoOperado> masOperados = ordenRepositorio
                .contarPorInstrumento(inicio, finExclusivo, PageRequest.of(0, INSTRUMENTOS_EN_RANKING)).stream()
                .map(fila -> new EstadisticasDeUso.InstrumentoOperado((String) fila[0], (Long) fila[1]))
                .toList();

        return new EstadisticasDeUso(desde, hasta,
                usuarioRepositorio.countByFechaRegistroGreaterThanEqualAndFechaRegistroLessThan(inicio, finExclusivo),
                portafolioRepositorio.countByCreadoEnGreaterThanEqualAndCreadoEnLessThan(inicio, finExclusivo),
                portafolioRepositorio.countByCreadoEnGreaterThanEqualAndCreadoEnLessThanAndEscenarioIdIsNotNull(
                        inicio, finExclusivo),
                porEstado, masOperados, usuarioRepositorio.count(), portafolioRepositorio.count());
    }
}
