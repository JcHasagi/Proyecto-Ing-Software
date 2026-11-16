package co.edu.uis.siminversion.web;

import java.math.BigDecimal;

/** Lectura de los campos numéricos que llegan como texto desde los formularios. */
public final class Entradas {

    private Entradas() {
    }

    /** El monto escrito por el usuario, o null si está vacío o no es un número: el servicio explica el error. */
    public static BigDecimal comoMonto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(texto.trim());
        } catch (NumberFormatException noEsNumero) {
            return null;
        }
    }
}
