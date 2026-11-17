package co.edu.uis.siminversion.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * RNF-07 y criterio de aceptación 5: la demostración corre sin internet. Ninguna plantilla, hoja de estilos
 * o script puede pedir un recurso a otro servidor (CDN, fuentes, imágenes, analítica).
 *
 * <p>No reemplaza la demostración con la red desconectada, pero impide que alguien agregue una dependencia
 * externa sin darse cuenta.
 */
class SinRecursosExternosTest {

    private static final Path RECURSOS = Path.of("src", "main", "resources");
    /** Una dirección absoluta o sin protocolo ("//cdn...") dentro de un atributo, un url(...) o un @import. */
    private static final Pattern DIRECCION_EXTERNA = Pattern.compile(
            "(?i)(?:src|href|action|data|poster|srcset)\\s*=\\s*[\"']\\s*(?:https?:)?//[^\"']+"
                    + "|url\\(\\s*[\"']?\\s*(?:https?:)?//[^)]+"
                    + "|@import\\s+[\"'](?:https?:)?//[^\"']+"
                    + "|(?:fetch|import|importScripts)\\(\\s*[\"'](?:https?:)?//[^\"']+");

    @Test
    void ningunaPlantillaNiRecursoEstaticoPideNadaAOtroServidor() throws IOException {
        List<String> hallazgos = new ArrayList<>();
        try (Stream<Path> archivos = Files.walk(RECURSOS)) {
            for (Path archivo : archivos.filter(SinRecursosExternosTest::esDeLaInterfaz).toList()) {
                Matcher coincidencia = DIRECCION_EXTERNA.matcher(Files.readString(archivo, StandardCharsets.UTF_8));
                while (coincidencia.find()) {
                    hallazgos.add(RECURSOS.relativize(archivo) + ": " + coincidencia.group());
                }
            }
        }

        assertThat(hallazgos).as("recursos externos en la interfaz").isEmpty();
    }

    @Test
    void laRevisionCubreLasPlantillasYLosRecursosEstaticos() throws IOException {
        try (Stream<Path> archivos = Files.walk(RECURSOS)) {
            List<String> revisados = archivos.filter(SinRecursosExternosTest::esDeLaInterfaz)
                    .map(archivo -> archivo.getFileName().toString())
                    .toList();

            assertThat(revisados).contains("plantilla.html", "estilos.css", "grafico.js").hasSizeGreaterThan(20);
        }
    }

    private static boolean esDeLaInterfaz(Path archivo) {
        String nombre = archivo.getFileName().toString();
        return Files.isRegularFile(archivo)
                && (nombre.endsWith(".html") || nombre.endsWith(".css") || nombre.endsWith(".js"));
    }
}
