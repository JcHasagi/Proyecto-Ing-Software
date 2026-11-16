package co.edu.uis.siminversion.carga;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uis.siminversion.precio.PrecioDiario;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LectorCsvHistoricoTest {

    private final LectorCsvHistorico lector = new LectorCsvHistorico();

    private ResultadoLectura leer(String contenido) throws IOException {
        return lector.leer(new StringReader(contenido));
    }

    @Test
    void leeEncabezadoEnInglesYOrdenaPorFecha() throws IOException {
        ResultadoLectura resultado = leer("""
                Date,Open,High,Low,Close,Volume
                2024-03-04,10.50,12.00,10.00,11.75,2000
                2024-03-01,10.00,11.00,9.50,10.50,1000
                """);

        assertThat(resultado.errores()).isEmpty();
        assertThat(resultado.filas()).extracting(PrecioDiario::fecha)
                .containsExactly(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 4));
        PrecioDiario primera = resultado.filas().getFirst();
        assertThat(primera.apertura()).isEqualByComparingTo("10.00");
        assertThat(primera.maximo()).isEqualByComparingTo("11.00");
        assertThat(primera.minimo()).isEqualByComparingTo("9.50");
        assertThat(primera.cierre()).isEqualByComparingTo("10.50");
        assertThat(primera.volumen()).isEqualTo(1000L);
    }

    @Test
    void aceptaEncabezadoEnEspanolConTildesPuntoYComaYVolumenVacio() throws IOException {
        ResultadoLectura resultado = leer("""
                Fecha;Apertura;Máximo;Mínimo;Cierre;Volumen
                2024-03-01;1.0810;1.0850;1.0790;1.0838;
                """);

        assertThat(resultado.errores()).isEmpty();
        assertThat(resultado.filas()).singleElement().satisfies(fila -> {
            assertThat(fila.cierre()).isEqualByComparingTo("1.0838");
            assertThat(fila.volumen()).isNull();
        });
    }

    @Test
    void aceptaArchivoSinColumnaDeVolumenEIgnoraColumnasDesconocidas() throws IOException {
        ResultadoLectura resultado = leer("""
                Date,Open,High,Low,Close,Adj Close
                2024-03-01,10.00,11.00,9.50,10.50,10.40
                """);

        assertThat(resultado.errores()).isEmpty();
        assertThat(resultado.filas()).singleElement().satisfies(fila -> {
            assertThat(fila.cierre()).isEqualByComparingTo("10.50");
            assertThat(fila.volumen()).isNull();
        });
    }

    @Test
    void quitaLaMarcaBomDelEncabezado() throws IOException {
        ResultadoLectura resultado = leer("﻿Date,Open,High,Low,Close\n2024-03-01,1,2,1,2\n");

        assertThat(resultado.errores()).isEmpty();
        assertThat(resultado.filas()).hasSize(1);
    }

    @Test
    void reportaFormatoInvalidoConNumeroDeLinea() throws IOException {
        ResultadoLectura resultado = leer("""
                Date,Open,High,Low,Close,Volume
                2024-13-01,10.00,11.00,9.50,10.50,1000
                2024-03-04,10,50,12.00,10.00,11.75,2000
                2024-03-05,10.50,12.00
                2024-03-06,10.50,12.00,10.00,11.75,mil
                """);

        assertThat(resultado.errores()).hasSize(4);
        assertThat(resultado.errores().get(0)).startsWith("Línea 2:").contains("fecha inválida");
        assertThat(resultado.errores().get(1)).startsWith("Línea 3:");
        assertThat(resultado.errores().get(2)).startsWith("Línea 4:").contains("columnas");
        assertThat(resultado.errores().get(3)).startsWith("Línea 5:").contains("volumen inválido");
    }

    @Test
    void rechazaFechasRepetidasDentroDelArchivo() throws IOException {
        ResultadoLectura resultado = leer("""
                Date,Open,High,Low,Close
                2024-03-01,10.00,11.00,9.50,10.50
                2024-03-01,10.10,11.10,9.60,10.60
                """);

        assertThat(resultado.errores()).containsExactly(
                "Línea 3: la fecha 2024-03-01 está repetida (ya aparece en la línea 2).");
    }

    @Test
    void rechazaPreciosIncoherentesNoPositivosOVolumenNegativo() throws IOException {
        ResultadoLectura resultado = leer("""
                Date,Open,High,Low,Close,Volume
                2024-03-01,10.00,9.00,9.50,9.20,100
                2024-03-04,12.00,11.00,9.50,10.50,100
                2024-03-05,0,11.00,9.50,10.50,100
                2024-03-06,10.00,11.00,9.50,10.50,-5
                """);

        assertThat(resultado.errores()).hasSize(4);
        assertThat(resultado.errores().get(0)).contains("mínimo").contains("mayor que el máximo");
        assertThat(resultado.errores().get(1)).contains("entre el mínimo y el máximo");
        assertThat(resultado.errores().get(2)).contains("mayor que cero");
        assertThat(resultado.errores().get(3)).contains("negativo");
    }

    @Test
    void rechazaEncabezadoSinColumnasObligatorias() throws IOException {
        ResultadoLectura resultado = leer("""
                Date,Close
                2024-03-01,10.50
                """);

        assertThat(resultado.filas()).isEmpty();
        assertThat(resultado.errores()).singleElement().asString()
                .startsWith("Línea 1:").contains("apertura").contains("máximo").contains("mínimo");
    }

    @Test
    void rechazaArchivoVacioOSoloConEncabezado() throws IOException {
        assertThat(leer("").errores()).containsExactly("El archivo está vacío.");
        assertThat(leer("Date,Open,High,Low,Close\n").errores())
                .containsExactly("El archivo no contiene filas de datos.");
    }

    @Test
    void redondeaPreciosAOchoDecimalesConRedondeoBancario() throws IOException {
        // HALF_EVEN: ...125 queda en ...12 (2 es par) y ...135 sube a ...14 (3 es impar).
        ResultadoLectura resultado = leer("""
                Date,Open,High,Low,Close
                2024-03-01,0.000000125,0.000000125,0.000000125,0.000000125
                2024-03-04,0.000000135,0.000000135,0.000000135,0.000000135
                """);

        assertThat(resultado.errores()).isEmpty();
        assertThat(resultado.filas().get(0).cierre()).isEqualTo(new BigDecimal("0.00000012"));
        assertThat(resultado.filas().get(1).cierre()).isEqualTo(new BigDecimal("0.00000014"));
    }

    @Test
    void limitaLaCantidadDeErroresReportados() throws IOException {
        StringBuilder contenido = new StringBuilder("Date,Open,High,Low,Close\n");
        for (int indice = 0; indice < 60; indice++) {
            contenido.append("no-es-fecha,1,2,1,2\n");
        }

        ResultadoLectura resultado = leer(contenido.toString());

        assertThat(resultado.errores()).hasSize(LectorCsvHistorico.MAXIMO_ERRORES_REPORTADOS + 1);
        assertThat(resultado.errores().getLast()).isEqualTo("... y 10 errores más.");
    }
}
