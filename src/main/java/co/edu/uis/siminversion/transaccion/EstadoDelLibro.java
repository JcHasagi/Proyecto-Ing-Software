package co.edu.uis.siminversion.transaccion;

import co.edu.uis.siminversion.comun.ReglasDeOperacion;
import co.edu.uis.siminversion.instrumento.Instrumento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Efectivo, posiciones, saldo de ahorro y resultados calculados desde cero recorriendo el libro de
 * transacciones en orden (RN-06). Es el único lugar donde se aplica el efecto de una transacción; el reloj
 * lo usa también para llevar el estado mientras ejecuta órdenes día a día.
 */
public final class EstadoDelLibro {

    /** Escala del costo base unitario: la misma de los precios. */
    private static final int ESCALA_COSTO = 8;

    private BigDecimal efectivo = BigDecimal.ZERO;
    private BigDecimal saldoAhorro = BigDecimal.ZERO;
    private BigDecimal comisionesPagadas = BigDecimal.ZERO;
    private BigDecimal interesesGanados = BigDecimal.ZERO;
    private final Map<String, PosicionAbierta> posiciones = new TreeMap<>();

    public static EstadoDelLibro desde(List<Transaccion> transaccionesEnOrden) {
        EstadoDelLibro estado = new EstadoDelLibro();
        transaccionesEnOrden.forEach(estado::aplicar);
        return estado;
    }

    public void aplicar(Transaccion transaccion) {
        TipoTransaccion tipo = transaccion.getTipo();
        BigDecimal monto = transaccion.getMonto();
        efectivo = efectivo.add(monto.multiply(BigDecimal.valueOf(tipo.getSignoEfectivo())));
        saldoAhorro = saldoAhorro.add(monto.multiply(BigDecimal.valueOf(tipo.getSignoAhorro())));
        switch (tipo) {
            case COMPRA -> posicionDe(transaccion.getInstrumento())
                    .comprar(transaccion.getCantidad(), transaccion.getPrecioEjecucion());
            case VENTA -> posicionDe(transaccion.getInstrumento())
                    .vender(transaccion.getCantidad(), transaccion.getPrecioEjecucion());
            case COMISION -> comisionesPagadas = comisionesPagadas.add(monto);
            case INTERES_AHORRO -> interesesGanados = interesesGanados.add(monto);
            default -> {
                // Depósitos y movimientos de ahorro solo mueven saldos.
            }
        }
    }

    public BigDecimal efectivo() {
        return efectivo;
    }

    public BigDecimal saldoAhorro() {
        return saldoAhorro;
    }

    public BigDecimal comisionesPagadas() {
        return comisionesPagadas;
    }

    public BigDecimal interesesGanados() {
        return interesesGanados;
    }

    /** P&L realizado de todas las ventas (RN-05): precio de venta menos costo base, sin comisiones. */
    public BigDecimal gananciaRealizada() {
        return ReglasDeOperacion.redondearDinero(posiciones.values().stream()
                .map(posicion -> posicion.gananciaRealizada)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** P&L realizado por símbolo, solo de los instrumentos que tuvieron ventas. */
    public Map<String, BigDecimal> gananciaRealizadaPorSimbolo() {
        Map<String, BigDecimal> resultado = new LinkedHashMap<>();
        posiciones.forEach((simbolo, posicion) -> {
            if (posicion.tuvoVentas) {
                resultado.put(simbolo, ReglasDeOperacion.redondearDinero(posicion.gananciaRealizada));
            }
        });
        return resultado;
    }

    public long cantidadDe(String simbolo) {
        PosicionAbierta posicion = posiciones.get(simbolo);
        return posicion == null ? 0 : posicion.cantidad;
    }

    public Optional<Posicion> posicion(String simbolo) {
        PosicionAbierta posicion = posiciones.get(simbolo);
        return posicion == null || posicion.cantidad == 0 ? Optional.empty() : Optional.of(posicion.comoPosicion());
    }

    /** Posiciones con cantidad mayor que cero, ordenadas por símbolo. */
    public List<Posicion> posiciones() {
        return posiciones.values().stream()
                .filter(posicion -> posicion.cantidad > 0)
                .map(PosicionAbierta::comoPosicion)
                .toList();
    }

    /** Instrumentos que alguna vez se compraron, por símbolo (incluye posiciones ya cerradas). */
    public Map<String, Instrumento> instrumentosOperados() {
        Map<String, Instrumento> resultado = new LinkedHashMap<>();
        posiciones.forEach((simbolo, posicion) -> resultado.put(simbolo, posicion.instrumento));
        return resultado;
    }

    private PosicionAbierta posicionDe(Instrumento instrumento) {
        return posiciones.computeIfAbsent(instrumento.getSimbolo(), simbolo -> new PosicionAbierta(instrumento));
    }

    private static final class PosicionAbierta {

        private final Instrumento instrumento;
        private long cantidad;
        private BigDecimal costoBaseUnitario = BigDecimal.ZERO;
        private BigDecimal gananciaRealizada = BigDecimal.ZERO;
        private boolean tuvoVentas;

        private PosicionAbierta(Instrumento instrumento) {
            this.instrumento = instrumento;
        }

        /** RN-05: promedio ponderado entre lo que ya había y lo comprado. */
        private void comprar(long cantidadComprada, BigDecimal precio) {
            BigDecimal costoTotal = costoBaseUnitario.multiply(BigDecimal.valueOf(cantidad))
                    .add(precio.multiply(BigDecimal.valueOf(cantidadComprada)));
            cantidad += cantidadComprada;
            costoBaseUnitario = costoTotal.divide(BigDecimal.valueOf(cantidad), ESCALA_COSTO, RoundingMode.HALF_EVEN);
        }

        /** RN-05: vender no cambia el costo unitario, solo la cantidad. RN-02: nunca por debajo de cero. */
        private void vender(long cantidadVendida, BigDecimal precio) {
            if (cantidadVendida > cantidad) {
                throw new IllegalStateException("Venta de " + cantidadVendida + " " + instrumento.getSimbolo()
                        + " con solo " + cantidad + " en posición");
            }
            gananciaRealizada = gananciaRealizada.add(
                    precio.subtract(costoBaseUnitario).multiply(BigDecimal.valueOf(cantidadVendida)));
            tuvoVentas = true;
            cantidad -= cantidadVendida;
            if (cantidad == 0) {
                costoBaseUnitario = BigDecimal.ZERO;
            }
        }

        private Posicion comoPosicion() {
            return new Posicion(instrumento, cantidad, costoBaseUnitario);
        }
    }
}
