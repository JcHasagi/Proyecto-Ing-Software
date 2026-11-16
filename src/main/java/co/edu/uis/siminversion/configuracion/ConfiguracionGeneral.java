package co.edu.uis.siminversion.configuracion;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionGeneral {

    /** Hora real del sistema (no confundir con el reloj de simulación de cada portafolio). */
    @Bean
    public Clock relojSistema() {
        return Clock.systemDefaultZone();
    }
}
