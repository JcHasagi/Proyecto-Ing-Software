package co.edu.uis.siminversion;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uis.siminversion.ahorro.AhorroServicio;
import co.edu.uis.siminversion.historial.FiltroHistorial;
import co.edu.uis.siminversion.historial.HistorialServicio;
import co.edu.uis.siminversion.instrumento.Instrumento;
import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import co.edu.uis.siminversion.instrumento.TipoInstrumento;
import co.edu.uis.siminversion.orden.OrdenServicio;
import co.edu.uis.siminversion.orden.TipoOrden;
import co.edu.uis.siminversion.portafolio.PortafolioServicio;
import co.edu.uis.siminversion.reloj.RelojServicio;
import co.edu.uis.siminversion.rendimiento.RendimientoServicio;
import co.edu.uis.siminversion.usuario.RegistroUsuarioServicio;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * RNF-01: presupuesto de consultas SQL de las operaciones principales. No mide tiempo, que depende del equipo,
 * sino cuántas sentencias prepara Hibernate: es lo que crece si alguien reintroduce una consulta por fila
 * (por transacción, por orden o por día). Las lecturas con JdbcTemplate (instantáneas) no se cuentan.
 *
 * <p>Escenario: un portafolio con 10 posiciones, cuenta de ahorro y un año de reloj (más de 400 transacciones).
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Transactional
class PresupuestoDeConsultasTest {

    private static final String CORREO = "presupuesto@example.com";
    private static final LocalDate INICIO = LocalDate.of(2023, 1, 2);
    private static final int DIAS_CON_PRECIOS = 420;
    private static final int DIAS_DEL_ANIO = 365;
    private static final int POSICIONES = 10;
    private static final int COMPRAS_PENDIENTES = 3;

    @Autowired
    private PortafolioServicio portafolioServicio;

    @Autowired
    private OrdenServicio ordenServicio;

    @Autowired
    private RelojServicio relojServicio;

    @Autowired
    private AhorroServicio ahorroServicio;

    @Autowired
    private RendimientoServicio rendimientoServicio;

    @Autowired
    private HistorialServicio historialServicio;

    @Autowired
    private RegistroUsuarioServicio registroUsuarioServicio;

    @Autowired
    private InstrumentoRepositorio instrumentoRepositorio;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void lasOperacionesPrincipalesNoHacenUnaConsultaPorFila() {
        List<Instrumento> instrumentos = instrumentoRepositorio.findAllByOrderBySimboloAsc();
        insertarPrecios(instrumentos);
        registroUsuarioServicio.registrarInversionista(CORREO, "Presupuesto", "clave-segura-1");
        long id = portafolioServicio.crear(CORREO, "Presupuesto", new BigDecimal("100000"), INICIO).getId();
        ahorroServicio.abrir(CORREO, id);
        ahorroServicio.depositar(CORREO, id, new BigDecimal("20000"));
        List<String> comprados = instrumentos.stream().limit(POSICIONES).map(Instrumento::getSimbolo).toList();
        for (String simbolo : comprados) {
            ordenServicio.emitir(CORREO, id, simbolo, TipoOrden.COMPRA, 5L);
        }
        relojServicio.avanzar(CORREO, id, INICIO.plusDays(7));

        // Un año de reloj: lo único que crece con los días es el interés diario de la cuenta de ahorro.
        assertThat(consultas(() -> relojServicio.avanzar(CORREO, id, INICIO.plusDays(7 + DIAS_DEL_ANIO))))
                .as("avanzar un año").isLessThanOrEqualTo(DIAS_DEL_ANIO + 50);

        for (String simbolo : comprados.subList(0, COMPRAS_PENDIENTES)) {
            ordenServicio.emitir(CORREO, id, simbolo, TipoOrden.COMPRA, 1L);
        }
        assertThat(portafolioServicio.resumen(CORREO, id).posiciones()).hasSize(POSICIONES);

        assertThat(consultas(() -> portafolioServicio.resumenDelActivo(CORREO)))
                .as("barra superior: usuario, portafolio y dos sumas").isLessThanOrEqualTo(4);
        assertThat(consultas(() -> historialServicio.consultar(CORREO, id, FiltroHistorial.SIN_FILTRO)))
                .as("historial: usuario, portafolio y el libro con sus instrumentos").isLessThanOrEqualTo(3);
        assertThat(consultas(() -> portafolioServicio.resumen(CORREO, id)))
                .as("detalle del portafolio: siete fijas y un precio por posición")
                .isLessThanOrEqualTo(7 + POSICIONES);
        assertThat(consultas(() -> rendimientoServicio.rendimiento(CORREO, id)))
                .as("rendimiento: las mismas del detalle, con un solo recorrido del libro")
                .isLessThanOrEqualTo(7 + POSICIONES);
        assertThat(consultas(() -> ahorroServicio.vista(CORREO, id)))
                .as("cuenta de ahorro").isLessThanOrEqualTo(5 + COMPRAS_PENDIENTES);
        assertThat(consultas(() -> ordenServicio.estimar(CORREO, id, comprados.get(5), TipoOrden.COMPRA, 1L)))
                .as("estimación de una orden").isLessThanOrEqualTo(6 + COMPRAS_PENDIENTES);

        // Un día de reloj: 11 fijas, dos por instrumento (sus cierres) y tres por orden ejecutada.
        assertThat(consultas(() -> relojServicio.avanzar(CORREO, id, INICIO.plusDays(8 + DIAS_DEL_ANIO))))
                .as("avanzar un día").isLessThanOrEqualTo(11 + 2 * POSICIONES + 3 * COMPRAS_PENDIENTES);
    }

    /** Sentencias que prepara Hibernate para la operación, partiendo de un contexto de persistencia vacío. */
    private long consultas(Runnable operacion) {
        entityManager.flush();
        entityManager.clear();
        Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estadisticas.clear();
        operacion.run();
        entityManager.flush();
        return estadisticas.getPrepareStatementCount();
    }

    /** Acciones, ETFs y divisas en días hábiles; criptomonedas todos los días. */
    private void insertarPrecios(List<Instrumento> instrumentos) {
        List<Object[]> filas = new ArrayList<>();
        for (Instrumento instrumento : instrumentos) {
            for (int dia = 0; dia < DIAS_CON_PRECIOS; dia++) {
                LocalDate fecha = INICIO.plusDays(dia);
                boolean finDeSemana = fecha.getDayOfWeek() == DayOfWeek.SATURDAY
                        || fecha.getDayOfWeek() == DayOfWeek.SUNDAY;
                if (finDeSemana && instrumento.getTipo() != TipoInstrumento.CRIPTOMONEDA) {
                    continue;
                }
                BigDecimal cierre = BigDecimal.valueOf(100 + (dia % 17) + instrumento.getId() % 5);
                filas.add(new Object[] {instrumento.getId(), fecha, cierre, cierre, cierre, cierre, 1000L});
            }
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO precio_historico (instrumento_id, fecha, apertura, maximo, minimo, cierre, volumen)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, filas);
    }
}
