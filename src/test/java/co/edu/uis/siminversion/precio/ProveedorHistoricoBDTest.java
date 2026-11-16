package co.edu.uis.siminversion.precio;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Calendario por instrumento: RN-03 y RN-08 se evalúan con los cierres de cada instrumento. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProveedorHistoricoBDTest {

    // Marzo de 2024: el 1 es viernes, el 2 y 3 fin de semana, el 4 lunes.
    private static final LocalDate JUEVES = LocalDate.of(2024, 2, 29);
    private static final LocalDate VIERNES = LocalDate.of(2024, 3, 1);
    private static final LocalDate SABADO = LocalDate.of(2024, 3, 2);
    private static final LocalDate DOMINGO = LocalDate.of(2024, 3, 3);
    private static final LocalDate LUNES = LocalDate.of(2024, 3, 4);

    @Autowired
    private ProveedorDePrecios proveedorDePrecios;

    @Autowired
    private InstrumentoRepositorio instrumentoRepositorio;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long accion;
    private long cripto;

    @BeforeEach
    void cargarPrecios() {
        accion = instrumentoRepositorio.findBySimbolo("AAPL").orElseThrow().getId();
        cripto = instrumentoRepositorio.findBySimbolo("BTCUSD").orElseThrow().getId();
        insertar(accion, JUEVES, "100.00");
        insertar(accion, VIERNES, "101.00");
        insertar(accion, LUNES, "102.00");
        insertar(cripto, VIERNES, "500.00");
        insertar(cripto, SABADO, "510.00");
        insertar(cripto, DOMINGO, "520.00");
        insertar(cripto, LUNES, "530.00");
    }

    private void insertar(long instrumentoId, LocalDate fecha, String cierre) {
        BigDecimal valor = new BigDecimal(cierre);
        jdbcTemplate.update("""
                INSERT INTO precio_historico (instrumento_id, fecha, apertura, maximo, minimo, cierre, volumen)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, instrumentoId, fecha, valor, valor, valor, valor, 1000L);
    }

    @Test
    void elSiguienteDiaBursatilDeUnaAccionSaltaElFinDeSemana() {
        assertThat(proveedorDePrecios.siguienteDiaBursatil(accion, VIERNES)).contains(LUNES);
        assertThat(proveedorDePrecios.siguienteDiaBursatil(accion, SABADO)).contains(LUNES);
    }

    @Test
    void elSiguienteDiaBursatilDeUnaCriptomonedaIncluyeElSabado() {
        assertThat(proveedorDePrecios.siguienteDiaBursatil(cripto, VIERNES)).contains(SABADO);
    }

    @Test
    void noHaySiguienteDiaBursatilDespuesDelUltimoCierre() {
        assertThat(proveedorDePrecios.siguienteDiaBursatil(accion, LUNES)).isEmpty();
    }

    @Test
    void unaFechaSinCierreNoTienePrecio() {
        assertThat(proveedorDePrecios.precioCierre(accion, SABADO)).isEmpty();
        assertThat(proveedorDePrecios.precioCierre(accion, VIERNES))
                .hasValueSatisfying(cierre -> assertThat(cierre).isEqualByComparingTo("101.00"));
    }

    @Test
    void elUltimoCierreHastaUnDiaSinCotizacionEsElDelDiaBursatilAnterior() {
        assertThat(proveedorDePrecios.ultimoCierreHasta(accion, DOMINGO))
                .hasValueSatisfying(precio -> assertThat(precio.fecha()).isEqualTo(VIERNES));
        assertThat(proveedorDePrecios.ultimoCierreHasta(accion, JUEVES.minusDays(1))).isEmpty();
    }

    @Test
    void elHistoricoNoIncluyeFechasPosterioresAlLimite() {
        assertThat(proveedorDePrecios.historico(accion, JUEVES.minusMonths(1), VIERNES))
                .extracting(PrecioDiario::fecha)
                .containsExactly(JUEVES, VIERNES);
    }
}
