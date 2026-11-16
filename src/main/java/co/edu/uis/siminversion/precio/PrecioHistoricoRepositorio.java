package co.edu.uis.siminversion.precio;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrecioHistoricoRepositorio extends JpaRepository<PrecioHistorico, PrecioHistoricoId> {

    @Query("""
            select p from PrecioHistorico p
            where p.id.instrumentoId = :instrumentoId and p.id.fecha between :desde and :hasta
            order by p.id.fecha
            """)
    List<PrecioHistorico> buscarEntre(@Param("instrumentoId") long instrumentoId,
                                      @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            select min(p.id.fecha) from PrecioHistorico p
            where p.id.instrumentoId = :instrumentoId and p.id.fecha > :fecha
            """)
    Optional<LocalDate> primeraFechaPosterior(@Param("instrumentoId") long instrumentoId,
                                              @Param("fecha") LocalDate fecha);

    @Query("""
            select p from PrecioHistorico p
            where p.id.instrumentoId = :instrumentoId
              and p.id.fecha = (select max(q.id.fecha) from PrecioHistorico q
                                where q.id.instrumentoId = :instrumentoId and q.id.fecha <= :fecha)
            """)
    Optional<PrecioHistorico> ultimoHasta(@Param("instrumentoId") long instrumentoId,
                                          @Param("fecha") LocalDate fecha);

    @Query("""
            select distinct p.id.fecha from PrecioHistorico p
            where p.id.fecha > :despuesDe and p.id.fecha <= :hasta
            order by p.id.fecha
            """)
    List<LocalDate> fechasDelMercadoEntre(@Param("despuesDe") LocalDate despuesDe, @Param("hasta") LocalDate hasta);

    @Query("select min(p.id.fecha) from PrecioHistorico p where p.id.fecha >= :fecha")
    Optional<LocalDate> primeraFechaDelMercadoDesde(@Param("fecha") LocalDate fecha);

    @Query("select min(p.id.fecha) from PrecioHistorico p")
    Optional<LocalDate> primeraFecha();

    @Query("select max(p.id.fecha) from PrecioHistorico p")
    Optional<LocalDate> ultimaFecha();

    @Query("""
            select count(distinct p.id.instrumentoId) from PrecioHistorico p, Instrumento i
            where i.id = p.id.instrumentoId and i.activo = true and p.id.fecha between :desde and :hasta
            """)
    long contarInstrumentosActivosConPrecios(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Días con precio, primera y última fecha de cada instrumento (para la administración del catálogo). */
    @Query("""
            select new co.edu.uis.siminversion.precio.CoberturaHistorico(
                p.id.instrumentoId, count(p), min(p.id.fecha), max(p.id.fecha))
            from PrecioHistorico p group by p.id.instrumentoId
            """)
    List<CoberturaHistorico> coberturaPorInstrumento();

    @Query("select count(p) > 0 from PrecioHistorico p where p.id.instrumentoId = :instrumentoId")
    boolean tienePrecios(@Param("instrumentoId") long instrumentoId);

    @Query("""
            select p.id.fecha from PrecioHistorico p
            where p.id.instrumentoId = :instrumentoId and p.id.fecha between :desde and :hasta
            """)
    List<LocalDate> fechasEntre(@Param("instrumentoId") long instrumentoId,
                                @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
