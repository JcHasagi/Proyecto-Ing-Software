package co.edu.uis.siminversion.transaccion;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransaccionRepositorio extends JpaRepository<Transaccion, Long> {

    /**
     * Orden cronológico del libro: fecha simulada y, dentro del día, orden de registro. El instrumento llega
     * en la misma consulta; sin eso habría una consulta más por cada instrumento distinto del libro.
     */
    @EntityGraph(attributePaths = "instrumento")
    List<Transaccion> findByPortafolioIdOrderByFechaSimAscIdAsc(Long portafolioId);

    @Query("""
            select coalesce(sum(t.monto), 0) from Transaccion t
            where t.portafolioId = :portafolioId and t.tipo in :tipos
            """)
    BigDecimal sumarMontos(@Param("portafolioId") long portafolioId, @Param("tipos") Collection<TipoTransaccion> tipos);
}
