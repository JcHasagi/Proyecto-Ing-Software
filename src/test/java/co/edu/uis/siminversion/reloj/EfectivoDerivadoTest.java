package co.edu.uis.siminversion.reloj;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uis.siminversion.PreciosDePrueba;
import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import co.edu.uis.siminversion.orden.OrdenServicio;
import co.edu.uis.siminversion.orden.TipoOrden;
import co.edu.uis.siminversion.portafolio.PortafolioServicio;
import co.edu.uis.siminversion.reloj.ResumenAvance.OrdenProcesada;
import co.edu.uis.siminversion.transaccion.EstadoDelLibro;
import co.edu.uis.siminversion.transaccion.TransaccionRepositorio;
import co.edu.uis.siminversion.usuario.RegistroUsuarioServicio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Criterio de aceptación 2 (RN-06): después de más de 50 operaciones, el efectivo que muestra la aplicación
 * coincide con el recalculado desde cero a partir del libro y con la cuenta independiente de las ejecuciones.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EfectivoDerivadoTest {

    private static final String CORREO = "cincuenta@example.com";
    private static final LocalDate INICIO = LocalDate.of(2023, 1, 2);
    private static final int DIAS = 200;
    private static final List<String> SIMBOLOS = List.of("AAPL", "BTCUSD");

    @Autowired
    private PortafolioServicio portafolioServicio;

    @Autowired
    private OrdenServicio ordenServicio;

    @Autowired
    private RelojServicio relojServicio;

    @Autowired
    private RegistroDeInstantaneas registroDeInstantaneas;

    @Autowired
    private TransaccionRepositorio transaccionRepositorio;

    @Autowired
    private RegistroUsuarioServicio registroUsuarioServicio;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InstrumentoRepositorio instrumentoRepositorio;

    @Test
    void elEfectivoRecalculadoDesdeLasTransaccionesCoincideConElMostrado() {
        registroUsuarioServicio.registrarInversionista(CORREO, "Prueba", "clave-segura-1");
        insertarPreciosAleatorios();
        long portafolioId = portafolioServicio.crear(CORREO, "Cincuenta", new BigDecimal("50000"), INICIO).getId();

        Random azar = new Random(7);
        BigDecimal efectivoEsperado = new BigDecimal("50000");
        Map<String, Long> cantidadesEsperadas = new HashMap<>();
        int operaciones = 0;
        for (int vuelta = 0; vuelta < DIAS && operaciones < 60; vuelta++) {
            for (String simbolo : SIMBOLOS) {
                emitirAlAzar(portafolioId, simbolo, azar);
            }
            LocalDate destino = relojServicio.destinoDeSalto(CORREO, portafolioId, RelojServicio.Salto.DIA);
            ResumenAvance avance = relojServicio.avanzar(CORREO, portafolioId, destino);
            for (OrdenProcesada ejecutada : avance.ejecutadas()) {
                boolean compra = ejecutada.tipo() == TipoOrden.COMPRA;
                BigDecimal neto = compra
                        ? ejecutada.monto().add(ejecutada.comision()).negate()
                        : ejecutada.monto().subtract(ejecutada.comision());
                efectivoEsperado = efectivoEsperado.add(neto);
                cantidadesEsperadas.merge(ejecutada.simbolo(), compra ? ejecutada.cantidad() : -ejecutada.cantidad(),
                        Long::sum);
                operaciones++;
            }
        }

        BigDecimal mostrado = portafolioServicio.efectivo(portafolioId);
        EstadoDelLibro recalculado = EstadoDelLibro.desde(
                transaccionRepositorio.findByPortafolioIdOrderByFechaSimAscIdAsc(portafolioId));
        BigDecimal ultimaInstantanea = registroDeInstantaneas.dePortafolio(portafolioId).getLast().efectivo();

        assertThat(operaciones).isGreaterThanOrEqualTo(50);
        assertThat(mostrado).isEqualByComparingTo(recalculado.efectivo());
        assertThat(mostrado).isEqualByComparingTo(efectivoEsperado);
        assertThat(mostrado).isEqualByComparingTo(ultimaInstantanea);
        for (String simbolo : SIMBOLOS) {
            assertThat(recalculado.cantidadDe(simbolo)).isEqualTo(cantidadesEsperadas.getOrDefault(simbolo, 0L));
        }
    }

    /** Compra o vende una cantidad al azar; si la orden no pasa la validación, se descarta. */
    private void emitirAlAzar(long portafolioId, String simbolo, Random azar) {
        long enPosicion = portafolioServicio.posicionEn(portafolioId, simbolo).map(p -> p.cantidad()).orElse(0L);
        boolean vender = enPosicion > 0 && azar.nextBoolean();
        long cantidad = vender ? 1 + azar.nextInt((int) Math.min(enPosicion, 20)) : 1 + azar.nextInt(20);
        try {
            ordenServicio.emitir(CORREO, portafolioId, simbolo, vender ? TipoOrden.VENTA : TipoOrden.COMPRA, cantidad);
        } catch (OperacionInvalidaException rechazadaAlEmitir) {
            // Por ejemplo, una venta que choca con otra pendiente: no cuenta como operación.
        }
    }

    /** Paseo aleatorio reproducible: AAPL en días hábiles y BTC todos los días. */
    private void insertarPreciosAleatorios() {
        PreciosDePrueba precios = new PreciosDePrueba(jdbcTemplate, instrumentoRepositorio);
        Random azar = new Random(42);
        double accion = 150;
        double cripto = 300;
        for (int dia = 0; dia < DIAS; dia++) {
            LocalDate fecha = INICIO.plusDays(dia);
            cripto *= 1 + (azar.nextDouble() - 0.5) * 0.06;
            precios.insertar("BTCUSD", fecha, BigDecimal.valueOf(cripto).setScale(2, RoundingMode.HALF_EVEN));
            if (fecha.getDayOfWeek() != DayOfWeek.SATURDAY && fecha.getDayOfWeek() != DayOfWeek.SUNDAY) {
                accion *= 1 + (azar.nextDouble() - 0.5) * 0.04;
                precios.insertar("AAPL", fecha, BigDecimal.valueOf(accion).setScale(2, RoundingMode.HALF_EVEN));
            }
        }
    }
}
