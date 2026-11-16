package co.edu.uis.siminversion.usuario;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    long countByFechaRegistroGreaterThanEqualAndFechaRegistroLessThan(LocalDateTime desde, LocalDateTime hasta);
}
