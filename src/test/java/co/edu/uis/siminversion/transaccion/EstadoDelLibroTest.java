package co.edu.uis.siminversion.transaccion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uis.siminversion.comun.ReglasDeOperacion;
import co.edu.uis.siminversion.instrumento.Instrumento;
import co.edu.uis.siminversion.instrumento.TipoInstrumento;
import co.edu.uis.siminversion.orden.Orden;
import co.edu.uis.siminversion.orden.TipoOrden;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Costo base (RN-05) y efectivo (RN-06) derivados del libro, sin base de datos. */
class EstadoDelLibroTest {

    private static final LocalDate FECHA = LocalDate.of(2024, 3, 1);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 15, 12, 0);
    private final Instrumento aapl = new Instrumento("AAPL", "Apple Inc.", TipoInstrumento.ACCION, "Tecnología");
    private final List<Transaccion> libro = new ArrayList<>(
            List.of(Transaccion.depositoInicial(1L, new BigDecimal("10000.00"), FECHA, AHORA)));

    /** Agrega la operación y su comisión, como lo hace el reloj al ejecutar. */
    private void operar(TipoOrden tipo, long cantidad, String precio) {
        Orden orden = new Orden(1L, aapl, tipo, cantidad, FECHA, AHORA);
        BigDecimal precioEjecucion = new BigDecimal(precio);
        BigDecimal monto = ReglasDeOperacion.montoOperado(cantidad, precioEjecucion);
        libro.add(Transaccion.operacion(orden, precioEjecucion, monto, FECHA, AHORA));
        libro.add(Transaccion.comision(orden, ReglasDeOperacion.comision(monto), FECHA, AHORA));
    }

    @Test
    void elCostoBaseEsElPromedioPonderadoSinComisiones() {
        operar(TipoOrden.COMPRA, 10, "100");   // 1.000 + 5,00 de comisión
        operar(TipoOrden.COMPRA, 30, "120");   // 3.600 + 10,80 de comisión

        EstadoDelLibro estado = EstadoDelLibro.desde(libro);

        assertThat(estado.posicion("AAPL")).hasValueSatisfying(posicion -> {
            assertThat(posicion.cantidad()).isEqualTo(40);
            assertThat(posicion.costoBaseUnitario()).isEqualByComparingTo("115");
            assertThat(posicion.costoTotal()).isEqualByComparingTo("4600.00");
        });
        assertThat(estado.efectivo()).isEqualByComparingTo("5384.20");
    }

    @Test
    void venderNoCambiaElCostoUnitarioYCerrarLaPosicionLaElimina() {
        operar(TipoOrden.COMPRA, 10, "100");
        operar(TipoOrden.COMPRA, 30, "120");
        operar(TipoOrden.VENTA, 20, "130");    // 2.600 − 7,80 de comisión

        EstadoDelLibro trasPrimeraVenta = EstadoDelLibro.desde(libro);
        assertThat(trasPrimeraVenta.cantidadDe("AAPL")).isEqualTo(20);
        assertThat(trasPrimeraVenta.posicion("AAPL").orElseThrow().costoBaseUnitario()).isEqualByComparingTo("115");
        assertThat(trasPrimeraVenta.efectivo()).isEqualByComparingTo("7976.40");

        operar(TipoOrden.VENTA, 20, "90");     // 1.800 − 5,40 de comisión
        EstadoDelLibro sinPosicion = EstadoDelLibro.desde(libro);
        assertThat(sinPosicion.posiciones()).isEmpty();
        assertThat(sinPosicion.posicion("AAPL")).isEmpty();
        assertThat(sinPosicion.efectivo()).isEqualByComparingTo("9771.00");
    }

    @Test
    void elPnLRealizadoSaleDeLasVentasContraElCostoBaseYLasComisionesSeAcumulanAparte() {
        operar(TipoOrden.COMPRA, 10, "100");
        operar(TipoOrden.COMPRA, 30, "120");   // costo base 115
        operar(TipoOrden.VENTA, 20, "130");    // +15 × 20 = +300
        operar(TipoOrden.VENTA, 20, "90");     // −25 × 20 = −500

        EstadoDelLibro estado = EstadoDelLibro.desde(libro);

        assertThat(estado.gananciaRealizada()).isEqualTo(new BigDecimal("-200.00"));
        assertThat(estado.gananciaRealizadaPorSimbolo()).containsEntry("AAPL", new BigDecimal("-200.00"));
        assertThat(estado.comisionesPagadas()).isEqualByComparingTo("29.00");
        assertThat(estado.instrumentosOperados()).containsOnlyKeys("AAPL");
    }

    @Test
    void losMovimientosDeAhorroMuevenEfectivoYSaldoDeAhorro() {
        libro.add(Transaccion.movimientoDeAhorro(1L, TipoTransaccion.DEPOSITO_AHORRO, new BigDecimal("1000.00"), FECHA, AHORA));
        libro.add(Transaccion.movimientoDeAhorro(1L, TipoTransaccion.INTERES_AHORRO, new BigDecimal("10.00"), FECHA, AHORA));
        libro.add(Transaccion.movimientoDeAhorro(1L, TipoTransaccion.RETIRO_AHORRO, new BigDecimal("300.00"), FECHA, AHORA));

        EstadoDelLibro estado = EstadoDelLibro.desde(libro);

        assertThat(estado.saldoAhorro()).isEqualByComparingTo("710.00");
        assertThat(estado.efectivo()).isEqualByComparingTo("9300.00");
        assertThat(estado.interesesGanados()).isEqualByComparingTo("10.00");
        assertThatThrownBy(() -> Transaccion.movimientoDeAhorro(1L, TipoTransaccion.COMPRA, BigDecimal.ONE, FECHA, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nuncaSeVendeMasDeLoQueHayEnPosicion() {
        operar(TipoOrden.COMPRA, 5, "100");
        operar(TipoOrden.VENTA, 6, "100");

        assertThatThrownBy(() -> EstadoDelLibro.desde(libro)).isInstanceOf(IllegalStateException.class);
    }
}
