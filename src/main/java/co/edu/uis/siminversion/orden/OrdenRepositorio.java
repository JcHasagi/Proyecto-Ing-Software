package co.edu.uis.siminversion.orden;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdenRepositorio extends JpaRepository<Orden, Long> {

    /** En orden de emisión: es el orden en que se ejecutan (REQ-010). El instrumento llega en la misma consulta. */
    @EntityGraph(attributePaths = "instrumento")
    List<Orden> findByPortafolioIdAndEstadoOrderByIdAsc(Long portafolioId, EstadoOrden estado);

    @EntityGraph(attributePaths = "instrumento")
    List<Orden> findTop20ByPortafolioIdAndEstadoNotOrderByIdDesc(Long portafolioId, EstadoOrden estado);

    @EntityGraph(attributePaths = "instrumento")
    Optional<Orden> findByIdAndPortafolioId(Long id, Long portafolioId);

    /** REQ-019: órdenes emitidas en el rango, por estado. Cada fila: [EstadoOrden, Long]. */
    @Query("""
            select o.estado, count(o) from Orden o
            where o.creadoEn >= :desde and o.creadoEn < :hasta
            group by o.estado
            """)
    List<Object[]> contarPorEstado(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** REQ-019: instrumentos con más órdenes en el rango. Cada fila: [símbolo, Long]. */
    @Query("""
            select o.instrumento.simbolo, count(o) from Orden o
            where o.creadoEn >= :desde and o.creadoEn < :hasta
            group by o.instrumento.simbolo
            order by count(o) desc, o.instrumento.simbolo
            """)
    List<Object[]> contarPorInstrumento(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                                        Pageable pagina);
}
