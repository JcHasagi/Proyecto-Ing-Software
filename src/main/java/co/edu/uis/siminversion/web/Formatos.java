package co.edu.uis.siminversion.web;

import co.edu.uis.siminversion.comun.Textos;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/** Formatos para las vistas (se usa como ${@formatos...}). Convención colombiana: 1.234,56. */
@Component("formatos")
public class Formatos {

    private static final String SIN_DATO = "—";

    /** Precios de 1 o más con 2 decimales; menores (p. ej. 1 JPY en USD) con hasta 8. */
    public String precio(BigDecimal valor) {
        if (valor == null) {
            return SIN_DATO;
        }
        String patron = valor.abs().compareTo(BigDecimal.ONE) >= 0 ? "#,##0.00" : "#,##0.00######";
        return Textos.numero(patron, valor);
    }

    public String porcentaje(BigDecimal valor) {
        if (valor == null) {
            return SIN_DATO;
        }
        return (valor.signum() > 0 ? "+" : "") + Textos.numero("#,##0.00", valor) + " %";
    }

    /** Tasa expresada como fracción (0,04) mostrada como porcentaje sin signo (4 %). */
    public String tasa(BigDecimal fraccion) {
        return fraccion == null ? SIN_DATO : Textos.numero("#,##0.##", fraccion.movePointRight(2)) + " %";
    }

    public String entero(Long valor) {
        return valor == null ? SIN_DATO : Textos.numero("#,##0", BigDecimal.valueOf(valor));
    }

    /** Montos de dinero: siempre 2 decimales y prefijo de moneda (US$ 1.234,56). */
    public String dinero(BigDecimal valor) {
        return valor == null ? SIN_DATO : Textos.textoDinero(valor);
    }

    public String fecha(LocalDate fecha) {
        return fecha == null ? SIN_DATO : Textos.textoFecha(fecha);
    }
}
