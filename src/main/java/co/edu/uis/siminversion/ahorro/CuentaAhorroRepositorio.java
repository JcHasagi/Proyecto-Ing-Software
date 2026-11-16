package co.edu.uis.siminversion.ahorro;

import org.springframework.data.jpa.repository.JpaRepository;

/** La clave es el id del portafolio: como mucho una cuenta por portafolio. */
public interface CuentaAhorroRepositorio extends JpaRepository<CuentaAhorro, Long> {
}
