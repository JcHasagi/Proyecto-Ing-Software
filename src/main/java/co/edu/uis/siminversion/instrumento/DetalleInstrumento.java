package co.edu.uis.siminversion.instrumento;

import co.edu.uis.siminversion.precio.PrecioDiario;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Detalle e histórico de un instrumento (REQ-005). La serie va en orden cronológico y termina en {@code hasta}. */
public record DetalleInstrumento(ResumenInstrumento instrumento, RangoHistorico rango, LocalDate hasta,
                                 List<PrecioDiario> serie) {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public boolean tieneDatos() {
        return !serie.isEmpty();
    }

    public BigDecimal maximoDelRango() {
        return serie.stream().map(PrecioDiario::maximo).max(Comparator.naturalOrder()).orElse(null);
    }

    public BigDecimal minimoDelRango() {
        return serie.stream().map(PrecioDiario::minimo).min(Comparator.naturalOrder()).orElse(null);
    }

    /** Variación del cierre entre el primer y el último día del rango, en %, a 2 decimales (HALF_EVEN). */
    public BigDecimal variacionPorcentual() {
        if (serie.size() < 2) {
            return null;
        }
        BigDecimal primero = serie.getFirst().cierre();
        BigDecimal ultimo = serie.getLast().cierre();
        return ultimo.subtract(primero).multiply(CIEN).divide(primero, 2, RoundingMode.HALF_EVEN);
    }

    /** Clase CSS según el signo de la variación: "positivo", "negativo" o vacía. */
    public String tendencia() {
        BigDecimal variacion = variacionPorcentual();
        if (variacion == null || variacion.signum() == 0) {
            return "";
        }
        return variacion.signum() > 0 ? "positivo" : "negativo";
    }

    /** Los días más recientes primero, para la tabla. */
    public List<PrecioDiario> ultimosDias(int cantidad) {
        return serie.subList(Math.max(0, serie.size() - cantidad), serie.size()).reversed();
    }

    public List<String> fechasGrafico() {
        return serie.stream().map(precio -> precio.fecha().toString()).toList();
    }

    /** Como texto para no pasar por double en el servidor; el navegador los convierte solo para dibujar. */
    public List<String> cierresGrafico() {
        return serie.stream().map(precio -> precio.cierre().toPlainString()).toList();
    }

    /** Volumen diario para las barras bajo el gráfico; null en divisas. */
    public List<Long> volumenesGrafico() {
        return serie.stream().map(PrecioDiario::volumen).toList();
    }
}
