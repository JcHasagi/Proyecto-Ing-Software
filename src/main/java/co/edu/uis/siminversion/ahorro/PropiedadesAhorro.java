package co.edu.uis.siminversion.ahorro;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Tasa nominal anual de las cuentas nuevas (siminversion.ahorro.tasa-anual; 0,04 por defecto). */
@ConfigurationProperties(prefix = "siminversion.ahorro")
public record PropiedadesAhorro(BigDecimal tasaAnual) {

    private static final BigDecimal TASA_POR_DEFECTO = new BigDecimal("0.04");

    public BigDecimal tasaVigente() {
        return tasaAnual == null ? TASA_POR_DEFECTO : tasaAnual;
    }
}
