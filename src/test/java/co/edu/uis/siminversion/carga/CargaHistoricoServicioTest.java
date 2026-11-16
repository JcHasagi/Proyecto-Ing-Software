package co.edu.uis.siminversion.carga;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import co.edu.uis.siminversion.precio.PrecioDiario;
import co.edu.uis.siminversion.precio.ProveedorDePrecios;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CargaHistoricoServicioTest {

    private static final LocalDate INICIO = LocalDate.of(2000, 1, 1);
    private static final LocalDate FIN = LocalDate.of(2100, 1, 1);

    @Autowired
    private CargaHistoricoServicio cargaHistoricoServicio;

    @Autowired
    private InstrumentoRepositorio instrumentoRepositorio;

    @Autowired
    private ProveedorDePrecios proveedorDePrecios;

    private ResultadoCarga cargar(String simbolo, String contenido) throws IOException {
        return cargaHistoricoServicio.cargar(simbolo, new StringReader(contenido));
    }

    private List<PrecioDiario> historicoDe(String simbolo) {
        long id = instrumentoRepositorio.findBySimbolo(simbolo).orElseThrow().getId();
        return proveedorDePrecios.historico(id, INICIO, FIN);
    }

    @Test
    void cargaLasFilasDeUnArchivoValido() throws IOException {
        ResultadoCarga resultado = cargar("msft", """
                Date,Open,High,Low,Close,Volume
                2024-03-01,10.00,11.00,9.50,10.50,1000
                2024-03-04,10.50,12.00,10.00,11.75,2000
                """);

        assertThat(resultado.aceptada()).isTrue();
        assertThat(resultado.simbolo()).isEqualTo("MSFT");
        assertThat(resultado.filasLeidas()).isEqualTo(2);
        assertThat(resultado.filasInsertadas()).isEqualTo(2);
        assertThat(historicoDe("MSFT")).extracting(PrecioDiario::cierre)
                .usingElementComparator(java.math.BigDecimal::compareTo)
                .containsExactly(new java.math.BigDecimal("10.50"), new java.math.BigDecimal("11.75"));
    }

    @Test
    void omiteFechasYaCargadasSinSobrescribirlas() throws IOException {
        cargar("MSFT", """
                Date,Open,High,Low,Close
                2024-03-01,10.00,11.00,9.50,10.50
                """);

        ResultadoCarga segunda = cargar("MSFT", """
                Date,Open,High,Low,Close
                2024-03-01,20.00,21.00,19.50,20.50
                2024-03-04,10.50,12.00,10.00,11.75
                """);

        assertThat(segunda.aceptada()).isTrue();
        assertThat(segunda.filasInsertadas()).isEqualTo(1);
        assertThat(segunda.fechasOmitidas()).containsExactly(LocalDate.of(2024, 3, 1));
        assertThat(historicoDe("MSFT").getFirst().cierre()).isEqualByComparingTo("10.50");
    }

    @Test
    void noInsertaNadaSiUnaFilaEsInvalida() throws IOException {
        ResultadoCarga resultado = cargar("MSFT", """
                Date,Open,High,Low,Close
                2024-03-01,10.00,11.00,9.50,10.50
                2024-03-04,diez,12.00,10.00,11.75
                """);

        assertThat(resultado.aceptada()).isFalse();
        assertThat(resultado.errores()).singleElement().asString().startsWith("Línea 3:");
        assertThat(historicoDe("MSFT")).isEmpty();
    }

    @Test
    void rechazaUnSimboloQueNoExiste() throws IOException {
        ResultadoCarga resultado = cargar("NOEXISTE", "Date,Open,High,Low,Close\n2024-03-01,1,2,1,2\n");

        assertThat(resultado.aceptada()).isFalse();
        assertThat(resultado.errores()).singleElement().asString().contains("NOEXISTE");
    }

    @Test
    void aceptaUnaDivisaSinVolumen() throws IOException {
        ResultadoCarga resultado = cargar("EURUSD", """
                Date,Open,High,Low,Close
                2024-03-01,1.0810,1.0850,1.0790,1.0838
                """);

        assertThat(resultado.aceptada()).isTrue();
        assertThat(historicoDe("EURUSD")).singleElement().satisfies(fila -> assertThat(fila.volumen()).isNull());
    }

    @Test
    void advierteHuecosSegunElCalendarioDelTipo() throws IOException {
        // Viernes 1 a lunes 4 de marzo de 2024: normal para una acción.
        ResultadoCarga accionSinHueco = cargar("AAPL", """
                Date,Open,High,Low,Close
                2024-03-01,10,11,9,10
                2024-03-04,10,11,9,10
                """);
        // Una semana sin cierres sí es un hueco para una acción.
        ResultadoCarga accionConHueco = cargar("KO", """
                Date,Open,High,Low,Close
                2024-03-01,10,11,9,10
                2024-03-11,10,11,9,10
                """);
        // Las criptomonedas cotizan todos los días: faltar el sábado ya es un hueco.
        ResultadoCarga criptoConHueco = cargar("ETHUSD", """
                Date,Open,High,Low,Close
                2024-03-01,10,11,9,10
                2024-03-03,10,11,9,10
                """);

        assertThat(accionSinHueco.advertencias()).isEmpty();
        assertThat(accionConHueco.advertencias()).singleElement().asString()
                .contains("9 días").contains("2024-03-01").contains("2024-03-11");
        assertThat(criptoConHueco.advertencias()).singleElement().asString().contains("1 días");
        assertThat(criptoConHueco.aceptada()).isTrue();
    }
}
