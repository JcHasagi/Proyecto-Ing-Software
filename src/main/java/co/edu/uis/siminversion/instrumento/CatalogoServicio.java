package co.edu.uis.siminversion.instrumento;

import co.edu.uis.siminversion.precio.PrecioDiario;
import co.edu.uis.siminversion.precio.ProveedorDePrecios;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas del catálogo (REQ-004) y del detalle de un instrumento (REQ-005).
 * {@code hasta} es la fecha límite de precios visibles: nunca se muestra un precio posterior.
 */
@Service
@Transactional(readOnly = true)
public class CatalogoServicio {

    private final InstrumentoRepositorio instrumentoRepositorio;
    private final ProveedorDePrecios proveedorDePrecios;

    public CatalogoServicio(InstrumentoRepositorio instrumentoRepositorio, ProveedorDePrecios proveedorDePrecios) {
        this.instrumentoRepositorio = instrumentoRepositorio;
        this.proveedorDePrecios = proveedorDePrecios;
    }

    public List<ResumenInstrumento> consultarCatalogo(TipoInstrumento tipo, String texto, LocalDate hasta) {
        return instrumentoRepositorio.buscarActivos(tipo, patronDeBusqueda(texto)).stream()
                .map(instrumento -> ResumenInstrumento.de(instrumento, ultimoCierre(instrumento, hasta)))
                .toList();
    }

    /**
     * También responde para instrumentos deshabilitados (REQ-017): no salen en el catálogo, pero quien tenga
     * posición debe poder verlos y venderlos.
     */
    public DetalleInstrumento consultarDetalle(String simbolo, RangoHistorico rango, LocalDate hasta) {
        Instrumento instrumento = instrumentoRepositorio
                .findBySimbolo(simbolo.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new InstrumentoNoEncontradoException(simbolo));
        List<PrecioDiario> serie = proveedorDePrecios.historico(instrumento.getId(), rango.desde(hasta), hasta);
        return new DetalleInstrumento(ResumenInstrumento.de(instrumento, ultimoCierre(instrumento, hasta)),
                rango, hasta, serie);
    }

    private PrecioDiario ultimoCierre(Instrumento instrumento, LocalDate hasta) {
        return proveedorDePrecios.ultimoCierreHasta(instrumento.getId(), hasta).orElse(null);
    }

    /** Convierte el texto del usuario en un patrón LIKE literal: sus % y _ no actúan como comodines. */
    static String patronDeBusqueda(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String literal = texto.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + literal + "%";
    }
}
