package co.edu.uis.siminversion.reloj;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Instantáneas diarias del portafolio. Se insertan en lote: un avance de un año son cientos de filas. */
@Repository
public class RegistroDeInstantaneas {

    private static final int TAMANO_LOTE = 500;
    private static final String INSERTAR = """
            INSERT INTO instantanea_portafolio
                (portafolio_id, fecha_sim, efectivo, valor_posiciones, saldo_ahorro, valor_total)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public RegistroDeInstantaneas(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertar(List<InstantaneaDiaria> instantaneas) {
        jdbcTemplate.batchUpdate(INSERTAR, instantaneas, TAMANO_LOTE, (sentencia, instantanea) -> {
            sentencia.setLong(1, instantanea.portafolioId());
            sentencia.setObject(2, instantanea.fecha());
            sentencia.setBigDecimal(3, instantanea.efectivo());
            sentencia.setBigDecimal(4, instantanea.valorPosiciones());
            sentencia.setBigDecimal(5, instantanea.saldoAhorro());
            sentencia.setBigDecimal(6, instantanea.valorTotal());
        });
    }

    /** En orden cronológico; es la serie del gráfico de evolución (REQ-011). */
    public List<InstantaneaDiaria> dePortafolio(long portafolioId) {
        return jdbcTemplate.query("""
                SELECT portafolio_id, fecha_sim, efectivo, valor_posiciones, saldo_ahorro, valor_total
                FROM instantanea_portafolio WHERE portafolio_id = ? ORDER BY fecha_sim
                """, (fila, numero) -> new InstantaneaDiaria(fila.getLong(1), fila.getObject(2, LocalDate.class),
                fila.getBigDecimal(3), fila.getBigDecimal(4), fila.getBigDecimal(5), fila.getBigDecimal(6)),
                portafolioId);
    }
}
