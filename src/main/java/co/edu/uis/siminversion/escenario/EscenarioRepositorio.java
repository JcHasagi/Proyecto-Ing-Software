package co.edu.uis.siminversion.escenario;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EscenarioRepositorio extends JpaRepository<Escenario, Long> {

    List<Escenario> findAllByOrderByOrdenVisualizacionAsc();
}
