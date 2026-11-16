package co.edu.uis.siminversion.carga;

import co.edu.uis.siminversion.instrumento.Instrumento;
import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import co.edu.uis.siminversion.instrumento.TipoInstrumento;
import co.edu.uis.siminversion.precio.PrecioDiario;
import co.edu.uis.siminversion.precio.PrecioHistoricoRepositorio;
import java.io.IOException;
import java.io.Reader;
import java.sql.Types;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga masiva de histórico de precios (REQ-018, solo administrador), en dos pasos: {@link #validar} arma
 * el resumen sin guardar nada y {@link #confirmar} guarda cuando el administrador lo acepta.
 *
 * <ul>
 *   <li>Todo o nada: si una fila tiene formato inválido o una fecha repetida dentro del archivo,
 *       no se inserta ninguna.</li>
 *   <li>Las fechas que ya existen en la BD para el instrumento se omiten, no se sobrescriben, y se informan.</li>
 *   <li>Los huecos en la serie son advertencias, no errores: una fecha sin cierre no es día bursátil (RN-08).</li>
 * </ul>
 */
@Service
public class CargaHistoricoServicio {

    private static final int TAMANO_LOTE = 500;
    private static final int MAXIMO_HUECOS_REPORTADOS = 20;
    private static final String INSERTAR_PRECIO = """
            INSERT INTO precio_historico (instrumento_id, fecha, apertura, maximo, minimo, cierre, volumen)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private final InstrumentoRepositorio instrumentoRepositorio;
    private final PrecioHistoricoRepositorio precioHistoricoRepositorio;
    private final JdbcTemplate jdbcTemplate;
    private final LectorCsvHistorico lector = new LectorCsvHistorico();

    public CargaHistoricoServicio(InstrumentoRepositorio instrumentoRepositorio,
                                  PrecioHistoricoRepositorio precioHistoricoRepositorio, JdbcTemplate jdbcTemplate) {
        this.instrumentoRepositorio = instrumentoRepositorio;
        this.precioHistoricoRepositorio = precioHistoricoRepositorio;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<Instrumento> instrumentosDisponibles() {
        return instrumentoRepositorio.findAllByOrderBySimboloAsc();
    }

    /**
     * Paso 1 de REQ-018: revisa el formato, las fechas repetidas, las fechas que ya existen y los huecos.
     * No guarda nada: el resultado es el resumen que el administrador confirma o cancela.
     */
    @Transactional(readOnly = true)
    public ValidacionCarga validar(String simbolo, Reader contenido) throws IOException {
        String simboloNormalizado = simbolo == null ? "" : simbolo.trim().toUpperCase(Locale.ROOT);
        Optional<Instrumento> instrumento = instrumentoRepositorio.findBySimbolo(simboloNormalizado);
        if (instrumento.isEmpty()) {
            return ValidacionCarga.rechazada(simboloNormalizado,
                    List.of("No existe un instrumento con símbolo '" + simboloNormalizado + "'."));
        }
        ResultadoLectura lectura = lector.leer(contenido);
        if (lectura.tieneErrores()) {
            return ValidacionCarga.rechazada(simboloNormalizado, lectura.errores());
        }

        List<PrecioDiario> filas = lectura.filas();
        long instrumentoId = instrumento.get().getId();
        Set<LocalDate> fechasExistentes = fechasYaCargadas(instrumentoId, filas);
        List<PrecioDiario> filasNuevas = filas.stream()
                .filter(fila -> !fechasExistentes.contains(fila.fecha()))
                .toList();
        List<LocalDate> fechasOmitidas = filas.stream()
                .map(PrecioDiario::fecha)
                .filter(fechasExistentes::contains)
                .toList();
        return ValidacionCarga.aceptada(simboloNormalizado, instrumentoId, filas.size(), filasNuevas, fechasOmitidas,
                detectarHuecos(filas, instrumento.get().getTipo()));
    }

    /**
     * Paso 2 de REQ-018: guarda las filas nuevas de una validación aceptada, todas o ninguna. Si entre la
     * validación y la confirmación alguien cargó alguna de esas fechas, se omite y se informa; nunca se
     * sobrescribe un precio.
     */
    @Transactional
    public ResultadoCarga confirmar(ValidacionCarga validacion) {
        if (!validacion.aceptada()) {
            throw new IllegalArgumentException("Solo se puede confirmar una validación aceptada");
        }
        List<PrecioDiario> filasNuevas = validacion.filasNuevas();
        Set<LocalDate> fechasExistentes = filasNuevas.isEmpty()
                ? Set.of()
                : fechasYaCargadas(validacion.instrumentoId(), filasNuevas);
        List<PrecioDiario> filasPorGuardar = filasNuevas.stream()
                .filter(fila -> !fechasExistentes.contains(fila.fecha()))
                .toList();
        List<LocalDate> fechasOmitidas = Stream.concat(validacion.fechasOmitidas().stream(),
                        filasNuevas.stream().map(PrecioDiario::fecha).filter(fechasExistentes::contains))
                .sorted()
                .toList();

        insertarEnLotes(validacion.instrumentoId(), filasPorGuardar);
        return ResultadoCarga.aceptada(validacion.simbolo(), validacion.filasLeidas(), filasPorGuardar.size(),
                fechasOmitidas, validacion.advertencias());
    }

    /** Valida y guarda en un solo paso: lo usa la carga inicial al arrancar, donde no hay quien confirme. */
    @Transactional
    public ResultadoCarga cargar(String simbolo, Reader contenido) throws IOException {
        ValidacionCarga validacion = validar(simbolo, contenido);
        return validacion.aceptada()
                ? confirmar(validacion)
                : ResultadoCarga.rechazada(validacion.simbolo(), validacion.errores());
    }

    /** Fechas del instrumento que ya tienen precio dentro del rango de las filas (que vienen en orden). */
    private Set<LocalDate> fechasYaCargadas(long instrumentoId, List<PrecioDiario> filasEnOrden) {
        return new HashSet<>(precioHistoricoRepositorio.fechasEntre(instrumentoId,
                filasEnOrden.getFirst().fecha(), filasEnOrden.getLast().fecha()));
    }

    private void insertarEnLotes(long instrumentoId, List<PrecioDiario> filas) {
        jdbcTemplate.batchUpdate(INSERTAR_PRECIO, filas, TAMANO_LOTE, (sentencia, fila) -> {
            sentencia.setLong(1, instrumentoId);
            sentencia.setObject(2, fila.fecha());
            sentencia.setBigDecimal(3, fila.apertura());
            sentencia.setBigDecimal(4, fila.maximo());
            sentencia.setBigDecimal(5, fila.minimo());
            sentencia.setBigDecimal(6, fila.cierre());
            if (fila.volumen() == null) {
                sentencia.setNull(7, Types.BIGINT);
            } else {
                sentencia.setLong(7, fila.volumen());
            }
        });
    }

    /** Advierte separaciones mayores a las normales para el tipo (fines de semana y festivos no cuentan). */
    static List<String> detectarHuecos(List<PrecioDiario> filas, TipoInstrumento tipo) {
        List<String> advertencias = new ArrayList<>();
        int huecos = 0;
        for (int indice = 1; indice < filas.size(); indice++) {
            LocalDate anterior = filas.get(indice - 1).fecha();
            LocalDate actual = filas.get(indice).fecha();
            long dias = ChronoUnit.DAYS.between(anterior, actual);
            if (dias > tipo.getDiasMaximosEntreCierres()) {
                huecos++;
                if (advertencias.size() < MAXIMO_HUECOS_REPORTADOS) {
                    advertencias.add("Hueco de " + (dias - 1) + " días sin cierre entre " + anterior + " y "
                            + actual + ".");
                }
            }
        }
        if (huecos > advertencias.size()) {
            advertencias.add("... y " + (huecos - advertencias.size()) + " huecos más.");
        }
        return advertencias;
    }
}
