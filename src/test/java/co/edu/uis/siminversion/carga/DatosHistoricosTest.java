package co.edu.uis.siminversion.carga;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uis.siminversion.instrumento.Instrumento;
import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import co.edu.uis.siminversion.instrumento.TipoInstrumento;
import co.edu.uis.siminversion.precio.PrecioDiario;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Los CSV de datos/historico cubren todo el catálogo inicial y pasan las reglas de la carga masiva. */
@SpringBootTest
@ActiveProfiles("test")
class DatosHistoricosTest {

    private static final Path CARPETA = Path.of("datos", "historico");
    private static final int FILAS_MINIMAS = 1000;

    @Autowired
    private InstrumentoRepositorio instrumentoRepositorio;

    @Test
    void cadaInstrumentoDelCatalogoTieneUnArchivoValido() throws IOException {
        LectorCsvHistorico lector = new LectorCsvHistorico();
        for (Instrumento instrumento : instrumentoRepositorio.findAll()) {
            Path archivo = CARPETA.resolve(instrumento.getSimbolo() + ".csv");
            assertThat(archivo).as("histórico de %s", instrumento.getSimbolo()).isRegularFile();

            ResultadoLectura lectura;
            try (Reader contenido = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                lectura = lector.leer(contenido);
            }

            assertThat(lectura.errores()).as("errores en %s", archivo).isEmpty();
            assertThat(lectura.filas()).as("filas de %s", archivo).hasSizeGreaterThanOrEqualTo(FILAS_MINIMAS);
            if (instrumento.getTipo() == TipoInstrumento.DIVISA) {
                assertThat(lectura.filas()).extracting(PrecioDiario::volumen).containsOnlyNulls();
            }
            if (instrumento.getTipo() == TipoInstrumento.ACCION || instrumento.getTipo() == TipoInstrumento.ETF) {
                // Las acciones y los ETFs no cotizan los fines de semana.
                assertThat(lectura.filas()).extracting(fila -> fila.fecha().getDayOfWeek().getValue())
                        .allMatch(dia -> dia <= 5);
            }
        }
    }
}
