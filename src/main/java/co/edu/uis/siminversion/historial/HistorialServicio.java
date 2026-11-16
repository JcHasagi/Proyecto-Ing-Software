package co.edu.uis.siminversion.historial;

import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import co.edu.uis.siminversion.portafolio.Portafolio;
import co.edu.uis.siminversion.portafolio.PortafolioServicio;
import co.edu.uis.siminversion.transaccion.Transaccion;
import co.edu.uis.siminversion.transaccion.TransaccionRepositorio;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** REQ-012: todas las transacciones del portafolio en orden cronológico, filtrables por tipo, instrumento y fechas. */
@Service
public class HistorialServicio {

    private final PortafolioServicio portafolioServicio;
    private final TransaccionRepositorio transaccionRepositorio;

    public HistorialServicio(PortafolioServicio portafolioServicio, TransaccionRepositorio transaccionRepositorio) {
        this.portafolioServicio = portafolioServicio;
        this.transaccionRepositorio = transaccionRepositorio;
    }

    @Transactional(readOnly = true)
    public VistaHistorial consultar(String correo, long portafolioId, FiltroHistorial filtroRecibido) {
        Portafolio portafolio = portafolioServicio.obtener(correo, portafolioId);
        FiltroHistorial filtro = normalizar(filtroRecibido);
        if (filtro.desde() != null && filtro.hasta() != null && filtro.desde().isAfter(filtro.hasta())) {
            throw new OperacionInvalidaException("La fecha «desde» es posterior a la fecha «hasta»; corrige el rango.");
        }
        List<Transaccion> todas = transaccionRepositorio.findByPortafolioIdOrderByFechaSimAscIdAsc(portafolioId);
        List<String> simbolos = todas.stream()
                .map(Transaccion::getInstrumento)
                .filter(Objects::nonNull)
                .map(instrumento -> instrumento.getSimbolo())
                .distinct()
                .sorted()
                .toList();
        return new VistaHistorial(portafolio, filtro, todas.stream().filter(filtro::incluye).toList(), todas.size(),
                simbolos);
    }

    private static FiltroHistorial normalizar(FiltroHistorial filtro) {
        if (filtro == null) {
            return FiltroHistorial.SIN_FILTRO;
        }
        String simbolo = filtro.simbolo() == null || filtro.simbolo().isBlank()
                ? null
                : filtro.simbolo().trim().toUpperCase(Locale.ROOT);
        return new FiltroHistorial(filtro.tipo(), simbolo, filtro.desde(), filtro.hasta());
    }
}
