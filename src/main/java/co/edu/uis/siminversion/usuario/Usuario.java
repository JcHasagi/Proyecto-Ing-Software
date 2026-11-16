package co.edu.uis.siminversion.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String correo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "hash_contrasena", nullable = false, length = 100)
    private String hashContrasena;

    // VARCHAR explícito: sin esto Hibernate espera un ENUM nativo de MySQL y falla la validación del esquema.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    /** Último portafolio usado; es el portafolio activo al iniciar sesión (REQ-002). */
    @Column(name = "portafolio_activo_id")
    private Long portafolioActivoId;

    protected Usuario() {
        // Requerido por JPA.
    }

    public Usuario(String correo, String nombre, String hashContrasena, Rol rol, LocalDateTime fechaRegistro) {
        this.correo = correo;
        this.nombre = nombre;
        this.hashContrasena = hashContrasena;
        this.rol = rol;
        this.fechaRegistro = fechaRegistro;
    }

    public Long getId() {
        return id;
    }

    public String getCorreo() {
        return correo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getHashContrasena() {
        return hashContrasena;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public Long getPortafolioActivoId() {
        return portafolioActivoId;
    }

    public void activarPortafolio(Long portafolioId) {
        this.portafolioActivoId = portafolioId;
    }
}
