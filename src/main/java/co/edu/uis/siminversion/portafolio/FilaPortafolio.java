package co.edu.uis.siminversion.portafolio;

import java.math.BigDecimal;

/** Portafolio en la lista del usuario, con su efectivo y si es el activo. */
public record FilaPortafolio(Portafolio portafolio, BigDecimal efectivo, boolean activo) {
}
