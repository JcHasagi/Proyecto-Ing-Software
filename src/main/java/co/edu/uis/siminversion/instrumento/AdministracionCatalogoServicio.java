package co.edu.uis.siminversion.instrumento;

import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import co.edu.uis.siminversion.precio.CoberturaHistorico;
import co.edu.uis.siminversion.precio.ProveedorDePrecios;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** REQ-017: el administrador agrega, edita y habilita o deshabilita instrumentos. */
@Service
public class AdministracionCatalogoServicio {

    private final InstrumentoRepositorio instrumentoRepositorio;
    private final ProveedorDePrecios proveedorDePrecios;

    public AdministracionCatalogoServicio(InstrumentoRepositorio instrumentoRepositorio,
                                          ProveedorDePrecios proveedorDePrecios) {
        this.instrumentoRepositorio = instrumentoRepositorio;
        this.proveedorDePrecios = proveedorDePrecios;
    }

    /** Todos los instrumentos, también los deshabilitados, con sus días de precios. */
    @Transactional(readOnly = true)
    public List<FilaAdministracion> listar() {
        Map<Long, CoberturaHistorico> coberturas = proveedorDePrecios.coberturaPorInstrumento().stream()
                .collect(Collectors.toMap(CoberturaHistorico::instrumentoId, Function.identity()));
        return instrumentoRepositorio.findAllByOrderBySimboloAsc().stream()
                .map(instrumento -> new FilaAdministracion(instrumento, coberturas.get(instrumento.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Instrumento obtener(long id) {
        return instrumentoRepositorio.findById(id)
                .orElseThrow(() -> InstrumentoNoEncontradoException.conId(id));
    }

    @Transactional
    public Instrumento crear(FormularioInstrumento datos) {
        String simbolo = normalizarSimbolo(datos.getSimbolo());
        exigirSimboloLibre(simbolo, null);
        Instrumento instrumento = new Instrumento(simbolo, nombre(datos), exigirTipo(datos), sector(datos));
        return instrumentoRepositorio.save(instrumento);
    }

    @Transactional
    public Instrumento editar(long id, FormularioInstrumento datos) {
        Instrumento instrumento = obtener(id);
        String simbolo = normalizarSimbolo(datos.getSimbolo());
        exigirSimboloLibre(simbolo, id);
        instrumento.actualizar(simbolo, nombre(datos), exigirTipo(datos), sector(datos));
        return instrumento;
    }

    @Transactional
    public Instrumento cambiarActivo(long id, boolean activo) {
        Instrumento instrumento = obtener(id);
        instrumento.cambiarActivo(activo);
        return instrumento;
    }

    private void exigirSimboloLibre(String simbolo, Long idPropio) {
        instrumentoRepositorio.findBySimbolo(simbolo)
                .filter(existente -> !existente.getId().equals(idPropio))
                .ifPresent(existente -> {
                    throw new OperacionInvalidaException("Ya existe un instrumento con el símbolo " + simbolo + ".");
                });
    }

    private static String normalizarSimbolo(String simbolo) {
        String normalizado = simbolo == null ? "" : simbolo.trim().toUpperCase(Locale.ROOT);
        if (!normalizado.matches("[A-Z0-9.\\-]{1,20}")) {
            throw new OperacionInvalidaException("El símbolo admite letras, números, punto y guion (máximo 20).");
        }
        return normalizado;
    }

    private static String nombre(FormularioInstrumento datos) {
        String nombre = datos.getNombre() == null ? "" : datos.getNombre().trim();
        if (nombre.isEmpty() || nombre.length() > 120) {
            throw new OperacionInvalidaException("El nombre es obligatorio y puede tener hasta 120 caracteres.");
        }
        return nombre;
    }

    private static TipoInstrumento exigirTipo(FormularioInstrumento datos) {
        if (datos.getTipo() == null) {
            throw new OperacionInvalidaException("Elige el tipo de instrumento.");
        }
        return datos.getTipo();
    }

    private static String sector(FormularioInstrumento datos) {
        String sector = datos.getSector() == null ? "" : datos.getSector().trim();
        if (sector.length() > 80) {
            throw new OperacionInvalidaException("El sector puede tener hasta 80 caracteres.");
        }
        return sector.isEmpty() ? null : sector;
    }
}
