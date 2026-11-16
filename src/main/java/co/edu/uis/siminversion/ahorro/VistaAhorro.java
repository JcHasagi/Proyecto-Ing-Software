package co.edu.uis.siminversion.ahorro;

import co.edu.uis.siminversion.portafolio.Portafolio;
import co.edu.uis.siminversion.transaccion.Transaccion;
import java.math.BigDecimal;
import java.util.List;

/** Página de la cuenta de ahorro: saldos y movimientos (más recientes primero). */
public record VistaAhorro(Portafolio portafolio, CuentaAhorro cuenta, BigDecimal saldoAhorro,
                          BigDecimal efectivoDisponible, BigDecimal interesesGanados, List<Transaccion> movimientos) {

    public boolean tieneCuenta() {
        return cuenta != null;
    }
}
