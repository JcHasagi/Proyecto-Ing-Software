package co.edu.uis.siminversion.portafolio;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos del portafolio activo que muestra la barra superior en todas las páginas. */
public record ResumenBarra(Long id, String nombre, LocalDate fechaActual, BigDecimal efectivo, EstadoPortafolio estado) {

    public boolean admiteOperaciones() {
        return estado == EstadoPortafolio.EN_CURSO;
    }
}
