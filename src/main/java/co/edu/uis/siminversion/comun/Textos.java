package co.edu.uis.siminversion.comun;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Textos de dinero y fechas con la convención colombiana (1.234,56 y dd/MM/yyyy). Vive fuera de la capa web
 * porque los servicios lo usan para armar los mensajes de {@link OperacionInvalidaException}.
 */
public final class Textos {

    private static final Locale COLOMBIA = Locale.of("es", "CO");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Textos() {
    }

    /** Monto en la moneda de referencia: US$ 1.234,56. */
    public static String textoDinero(BigDecimal valor) {
        return "US$ " + numero("#,##0.00", valor);
    }

    public static String textoFecha(LocalDate fecha) {
        return fecha.format(FORMATO_FECHA);
    }

    /** Número con el patrón de {@link DecimalFormat} dado, redondeado con redondeo bancario (RN-10). */
    public static String numero(String patron, BigDecimal valor) {
        // DecimalFormat no es seguro entre hilos: se crea uno por llamada.
        DecimalFormat formato = new DecimalFormat(patron, DecimalFormatSymbols.getInstance(COLOMBIA));
        formato.setRoundingMode(RoundingMode.HALF_EVEN);
        return formato.format(valor);
    }
}
