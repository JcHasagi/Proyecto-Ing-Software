package co.edu.uis.siminversion.portafolio;

import co.edu.uis.siminversion.comun.ReglasDeOperacion;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Datos del formulario de REQ-003. El servicio vuelve a validar las reglas. */
public class FormularioPortafolio {

    @NotBlank(message = "Ponle un nombre al portafolio.")
    @Size(max = 80, message = "El nombre puede tener hasta 80 caracteres.")
    private String nombre = "Mi portafolio";

    @NotNull(message = "Indica el saldo inicial.")
    @DecimalMin(value = "1000", message = "El saldo inicial mínimo es US$ 1.000.")
    @DecimalMax(value = "100000", message = "El saldo inicial máximo es US$ 100.000.")
    private BigDecimal saldoInicial = ReglasDeOperacion.SALDO_INICIAL_POR_DEFECTO;

    @NotNull(message = "Elige la fecha en la que empieza el reloj.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaInicial = PortafolioServicio.FECHA_INICIAL_POR_DEFECTO;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public LocalDate getFechaInicial() {
        return fechaInicial;
    }

    public void setFechaInicial(LocalDate fechaInicial) {
        this.fechaInicial = fechaInicial;
    }
}
