package co.edu.uis.siminversion.reloj;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uis.siminversion.PreciosDePrueba;
import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import co.edu.uis.siminversion.orden.OrdenServicio;
import co.edu.uis.siminversion.orden.TipoOrden;
import co.edu.uis.siminversion.portafolio.PortafolioServicio;
import co.edu.uis.siminversion.usuario.RegistroUsuarioServicio;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * RNF-04: si algo falla mientras el reloj avanza, no queda nada a medias: ni las transacciones de la orden,
 * ni el estado de la orden, ni la fecha del reloj.
 *
 * <p>A diferencia de las demás, esta prueba no es transaccional: el servicio tiene que confirmar o revertir
 * de verdad. Por eso borra al final todo lo que creó.
 */
@SpringBootTest
@ActiveProfiles("test")
class AtomicidadDelAvanceTest {

    private static final String CORREO = "atomicidad@example.com";
    private static final LocalDate INICIO = LocalDate.of(2024, 3, 1);
    private static final LocalDate DESTINO = LocalDate.of(2024, 3, 5);

    @Autowired
    private PortafolioServicio portafolioServicio;

    @Autowired
    private OrdenServicio ordenServicio;

    @Autowired
    private RelojServicio relojServicio;

    @Autowired
    private RegistroUsuarioServicio registroUsuarioServicio;

    @Autowired
    private InstrumentoRepositorio instrumentoRepositorio;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long portafolioId;
    private Long instrumentoId;

    @AfterEach
    void borrarLoCreado() {
        if (portafolioId != null) {
            jdbcTemplate.update("DELETE FROM instantanea_portafolio WHERE portafolio_id = ?", portafolioId);
            jdbcTemplate.update("DELETE FROM transaccion WHERE portafolio_id = ?", portafolioId);
            jdbcTemplate.update("DELETE FROM orden WHERE portafolio_id = ?", portafolioId);
        }
        jdbcTemplate.update("UPDATE usuario SET portafolio_activo_id = NULL WHERE correo = ?", CORREO);
        jdbcTemplate.update("DELETE FROM portafolio WHERE usuario_id IN (SELECT id FROM usuario WHERE correo = ?)",
                CORREO);
        jdbcTemplate.update("DELETE FROM usuario WHERE correo = ?", CORREO);
        if (instrumentoId != null) {
            jdbcTemplate.update("DELETE FROM precio_historico WHERE instrumento_id = ?", instrumentoId);
        }
    }

    @Test
    void siElAvanceFallaNoQuedaNadaAMedias() {
        instrumentoId = instrumentoRepositorio.findBySimbolo("AAPL").orElseThrow().getId();
        PreciosDePrueba precios = new PreciosDePrueba(jdbcTemplate, instrumentoRepositorio);
        precios.insertar("AAPL", "2024-03-01", "100");
        precios.insertar("AAPL", "2024-03-04", "110");
        precios.insertar("AAPL", "2024-03-05", "120");
        registroUsuarioServicio.registrarInversionista(CORREO, "Atomicidad", "clave-segura-1");
        portafolioId = portafolioServicio.crear(CORREO, "Atomicidad", new BigDecimal("5000"), INICIO).getId();
        long ordenId = ordenServicio.emitir(CORREO, portafolioId, "AAPL", TipoOrden.COMPRA, 10L).getId();

        // Fallo provocado: la instantánea del último día ya existe, así que el avance falla al final, cuando
        // la orden ya se ejecutó el 4 de marzo y sus dos transacciones ya se enviaron a la base de datos.
        jdbcTemplate.update("""
                INSERT INTO instantanea_portafolio
                    (portafolio_id, fecha_sim, efectivo, valor_posiciones, saldo_ahorro, valor_total)
                VALUES (?, ?, 0, 0, 0, 0)
                """, portafolioId, DESTINO);

        assertThatThrownBy(() -> relojServicio.avanzar(CORREO, portafolioId, DESTINO))
                .isInstanceOf(DataAccessException.class);

        assertThat(transacciones()).as("solo el depósito inicial").isEqualTo(1);
        assertThat(estadoDeLaOrden(ordenId)).isEqualTo("PENDIENTE");
        assertThat(fechaDelReloj()).isEqualTo(INICIO);
        assertThat(portafolioServicio.efectivo(portafolioId)).isEqualByComparingTo("5000");

        // Sin el fallo, el mismo avance queda completo: operación y comisión juntas.
        jdbcTemplate.update("DELETE FROM instantanea_portafolio WHERE portafolio_id = ?", portafolioId);
        relojServicio.avanzar(CORREO, portafolioId, DESTINO);

        assertThat(transacciones()).as("depósito, compra y comisión").isEqualTo(3);
        assertThat(estadoDeLaOrden(ordenId)).isEqualTo("EJECUTADA");
        assertThat(fechaDelReloj()).isEqualTo(DESTINO);
        // 5.000 − 10 × 110 − comisión mínima de 5.
        assertThat(portafolioServicio.efectivo(portafolioId)).isEqualByComparingTo("3895");
    }

    private long transacciones() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transaccion WHERE portafolio_id = ?", Long.class,
                portafolioId);
    }

    private String estadoDeLaOrden(long ordenId) {
        return jdbcTemplate.queryForObject("SELECT estado FROM orden WHERE id = ?", String.class, ordenId);
    }

    private LocalDate fechaDelReloj() {
        return jdbcTemplate.queryForObject("SELECT fecha_actual_sim FROM portafolio WHERE id = ?", LocalDate.class,
                portafolioId);
    }
}
