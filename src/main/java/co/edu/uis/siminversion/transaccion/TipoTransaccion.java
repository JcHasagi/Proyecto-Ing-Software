package co.edu.uis.siminversion.transaccion;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tipos de movimiento del libro. El monto siempre es positivo; los signos indican cómo afecta al efectivo y
 * al saldo de la cuenta de ahorro. El efecto en la posición lo aplica {@link EstadoDelLibro}.
 */
public enum TipoTransaccion {

    DEPOSITO_INICIAL("Depósito inicial", 1, 0),
    COMPRA("Compra", -1, 0),
    VENTA("Venta", 1, 0),
    COMISION("Comisión", -1, 0),
    DEPOSITO_AHORRO("Depósito a ahorro", -1, 1),
    RETIRO_AHORRO("Retiro de ahorro", 1, -1),
    INTERES_AHORRO("Interés de ahorro", 0, 1);

    private final String etiqueta;
    private final int signoEfectivo;
    private final int signoAhorro;

    TipoTransaccion(String etiqueta, int signoEfectivo, int signoAhorro) {
        this.etiqueta = etiqueta;
        this.signoEfectivo = signoEfectivo;
        this.signoAhorro = signoAhorro;
    }

    public static Set<TipoTransaccion> entradasDeEfectivo() {
        return conSigno(TipoTransaccion::getSignoEfectivo, 1);
    }

    public static Set<TipoTransaccion> salidasDeEfectivo() {
        return conSigno(TipoTransaccion::getSignoEfectivo, -1);
    }

    public static Set<TipoTransaccion> entradasDeAhorro() {
        return conSigno(TipoTransaccion::getSignoAhorro, 1);
    }

    public static Set<TipoTransaccion> salidasDeAhorro() {
        return conSigno(TipoTransaccion::getSignoAhorro, -1);
    }

    private static Set<TipoTransaccion> conSigno(ToIntFunction<TipoTransaccion> signo, int valor) {
        return Stream.of(values())
                .filter(tipo -> signo.applyAsInt(tipo) == valor)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TipoTransaccion.class)));
    }

    public boolean esDeAhorro() {
        return signoAhorro != 0;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public int getSignoEfectivo() {
        return signoEfectivo;
    }

    public int getSignoAhorro() {
        return signoAhorro;
    }
}
