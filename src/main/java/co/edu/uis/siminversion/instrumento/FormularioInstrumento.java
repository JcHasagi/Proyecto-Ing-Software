package co.edu.uis.siminversion.instrumento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos del formulario de REQ-017. El servicio vuelve a validar y normaliza el símbolo. */
public class FormularioInstrumento {

    @NotBlank(message = "Indica el símbolo.")
    @Pattern(regexp = "\\s*[A-Za-z0-9.\\-]{1,20}\\s*", message = "El símbolo admite letras, números, punto y guion (máximo 20).")
    private String simbolo;

    @NotBlank(message = "Indica el nombre.")
    @Size(max = 120, message = "El nombre puede tener hasta 120 caracteres.")
    private String nombre;

    @NotNull(message = "Elige el tipo de instrumento.")
    private TipoInstrumento tipo;

    @Size(max = 80, message = "El sector puede tener hasta 80 caracteres.")
    private String sector;

    public static FormularioInstrumento de(Instrumento instrumento) {
        FormularioInstrumento formulario = new FormularioInstrumento();
        formulario.setSimbolo(instrumento.getSimbolo());
        formulario.setNombre(instrumento.getNombre());
        formulario.setTipo(instrumento.getTipo());
        formulario.setSector(instrumento.getSector());
        return formulario;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public void setSimbolo(String simbolo) {
        this.simbolo = simbolo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoInstrumento getTipo() {
        return tipo;
    }

    public void setTipo(TipoInstrumento tipo) {
        this.tipo = tipo;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }
}
