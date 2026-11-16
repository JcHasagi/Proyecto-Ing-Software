package co.edu.uis.siminversion.instrumento;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstrumentoRepositorio extends JpaRepository<Instrumento, Long> {

    Optional<Instrumento> findBySimbolo(String simbolo);

    List<Instrumento> findAllByOrderBySimboloAsc();

    long countByActivoTrue();

    /**
     * Instrumentos activos filtrados por tipo y por texto en símbolo o nombre (ambos opcionales).
     * {@code patron} ya viene en minúsculas, con comodines y con '!' como carácter de escape.
     */
    @Query("""
            select i from Instrumento i
            where i.activo = true
              and (:tipo is null or i.tipo = :tipo)
              and (:patron is null
                   or lower(i.simbolo) like :patron escape '!'
                   or lower(i.nombre) like :patron escape '!')
            order by i.simbolo
            """)
    List<Instrumento> buscarActivos(@Param("tipo") TipoInstrumento tipo, @Param("patron") String patron);
}
