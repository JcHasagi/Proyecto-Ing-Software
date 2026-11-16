package co.edu.uis.siminversion.educacion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticuloRepositorio extends JpaRepository<Articulo, Long> {

    List<Articulo> findAllByOrderByOrdenVisualizacionAsc();
}
