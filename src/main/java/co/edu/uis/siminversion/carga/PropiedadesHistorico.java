package co.edu.uis.siminversion.carga;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Carpeta con un CSV por instrumento (SIMBOLO.csv) para la carga inicial; vacía = desactivada. */
@ConfigurationProperties(prefix = "siminversion.historico")
public record PropiedadesHistorico(String directorio) {

    public boolean estaConfigurado() {
        return directorio != null && !directorio.isBlank();
    }
}
