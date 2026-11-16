package co.edu.uis.siminversion.carga;

import co.edu.uis.siminversion.instrumento.Instrumento;
import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import co.edu.uis.siminversion.precio.PrecioHistoricoRepositorio;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, carga {@code SIMBOLO.csv} de la carpeta configurada para cada instrumento que aún no tiene
 * precios. Usa las mismas reglas que la carga masiva (REQ-018); después de la primera carga no hace nada,
 * así que las actualizaciones posteriores se hacen desde /admin/carga.
 */
@Component
public class CargaInicialHistoricos implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaInicialHistoricos.class);

    private final PropiedadesHistorico propiedades;
    private final InstrumentoRepositorio instrumentoRepositorio;
    private final PrecioHistoricoRepositorio precioHistoricoRepositorio;
    private final CargaHistoricoServicio cargaHistoricoServicio;

    public CargaInicialHistoricos(PropiedadesHistorico propiedades, InstrumentoRepositorio instrumentoRepositorio,
                                  PrecioHistoricoRepositorio precioHistoricoRepositorio,
                                  CargaHistoricoServicio cargaHistoricoServicio) {
        this.propiedades = propiedades;
        this.instrumentoRepositorio = instrumentoRepositorio;
        this.precioHistoricoRepositorio = precioHistoricoRepositorio;
        this.cargaHistoricoServicio = cargaHistoricoServicio;
    }

    @Override
    public void run(ApplicationArguments argumentos) throws IOException {
        if (!propiedades.estaConfigurado()) {
            return;
        }
        Path carpeta = Path.of(propiedades.directorio()).toAbsolutePath();
        if (!Files.isDirectory(carpeta)) {
            log.warn("No existe la carpeta de históricos {}; no se cargan precios iniciales.", carpeta);
            return;
        }
        for (Instrumento instrumento : instrumentoRepositorio.findAllByOrderBySimboloAsc()) {
            Path archivo = carpeta.resolve(instrumento.getSimbolo() + ".csv");
            if (Files.isRegularFile(archivo) && !precioHistoricoRepositorio.tienePrecios(instrumento.getId())) {
                cargar(instrumento.getSimbolo(), archivo);
            }
        }
    }

    private void cargar(String simbolo, Path archivo) throws IOException {
        ResultadoCarga resultado;
        try (Reader contenido = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            resultado = cargaHistoricoServicio.cargar(simbolo, contenido);
        }
        if (resultado.aceptada()) {
            log.info("Histórico inicial de {}: {} filas cargadas ({} advertencias de huecos).", simbolo,
                    resultado.filasInsertadas(), resultado.advertencias().size());
        } else {
            log.error("No se cargó el histórico inicial de {}: {}", simbolo, resultado.errores());
        }
    }
}
