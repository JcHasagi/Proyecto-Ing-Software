package co.edu.uis.siminversion.instrumento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "instrumento")
public class Instrumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String simbolo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoInstrumento tipo;

    /** No aplica a divisas ni criptomonedas. */
    @Column(length = 80)
    private String sector;

    @Column(nullable = false)
    private boolean activo = true;

    protected Instrumento() {
        // Requerido por JPA.
    }

    public Instrumento(String simbolo, String nombre, TipoInstrumento tipo, String sector) {
        this.simbolo = simbolo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.sector = sector;
    }

    public Long getId() {
        return id;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoInstrumento getTipo() {
        return tipo;
    }

    public String getSector() {
        return sector;
    }

    public boolean isActivo() {
        return activo;
    }

    /** REQ-017: edición desde la administración del catálogo (el servicio valida antes). */
    public void actualizar(String simbolo, String nombre, TipoInstrumento tipo, String sector) {
        this.simbolo = simbolo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.sector = sector;
    }

    /** Deshabilitado: no aparece en el catálogo ni admite compras; las posiciones existentes se conservan. */
    public void cambiarActivo(boolean activo) {
        this.activo = activo;
    }

    public String getIniciales() {
        return inicialesDe(simbolo, tipo);
    }

    /** Texto del ícono circular: la divisa o cripto base (EUR, BTC) o el símbolo de hasta 4 letras. */
    static String inicialesDe(String simbolo, TipoInstrumento tipo) {
        int largo = tipo == TipoInstrumento.DIVISA || tipo == TipoInstrumento.CRIPTOMONEDA ? 3 : 4;
        return simbolo.length() <= largo ? simbolo : simbolo.substring(0, largo);
    }
}
