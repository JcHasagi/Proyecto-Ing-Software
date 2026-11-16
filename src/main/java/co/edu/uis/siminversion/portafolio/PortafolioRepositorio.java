package co.edu.uis.siminversion.portafolio;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PortafolioRepositorio extends JpaRepository<Portafolio, Long> {

    /** Siempre se busca con el dueño: así un usuario no puede llegar al portafolio de otro. */
    Optional<Portafolio> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Portafolio> findByUsuarioIdOrderByCreadoEnDescIdDesc(Long usuarioId);

    boolean existsByUsuarioId(Long usuarioId);

    long countByCreadoEnGreaterThanEqualAndCreadoEnLessThan(LocalDateTime desde, LocalDateTime hasta);

    long countByCreadoEnGreaterThanEqualAndCreadoEnLessThanAndEscenarioIdIsNotNull(LocalDateTime desde,
                                                                                   LocalDateTime hasta);

    /** Para operaciones que modifican el portafolio: incrementa la versión aunque no cambie ningún campo. */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select p from Portafolio p where p.id = :id and p.usuarioId = :usuarioId")
    Optional<Portafolio> buscarParaModificar(@Param("id") long id, @Param("usuarioId") long usuarioId);
}
