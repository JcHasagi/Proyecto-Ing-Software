package co.edu.uis.siminversion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uis.siminversion.usuario.CorreoYaRegistradoException;
import co.edu.uis.siminversion.usuario.RegistroUsuarioServicio;
import co.edu.uis.siminversion.usuario.UsuarioRepositorio;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * La aplicación sigue funcionando después de que el pool renueva sus conexiones, cosa que hace solo a los
 * 30 minutos. Con H2 2.4.240, al cerrarse la conexión que creó las tablas, todas las restricciones CHECK
 * fallaban y el registro de usuarios respondía "ya existe una cuenta con ese correo". Por eso el pom fija
 * la versión de H2.
 *
 * <p>No es transaccional (necesita conexiones nuevas de verdad), así que borra al final lo que creó.
 */
@SpringBootTest
@ActiveProfiles("test")
class RenovacionDeConexionesTest {

    private static final String CORREO = "renovacion@example.com";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RegistroUsuarioServicio registroUsuarioServicio;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void borrarLoCreado() {
        jdbcTemplate.update("DELETE FROM usuario WHERE correo = ?", CORREO);
    }

    @Test
    void elRegistroFuncionaDespuesDeRenovarLasConexiones() throws Exception {
        dataSource.unwrap(HikariDataSource.class).getHikariPoolMXBean().softEvictConnections();
        // El pool cierra las conexiones libres en segundo plano.
        Thread.sleep(1000);

        registroUsuarioServicio.registrarInversionista(CORREO, "Renovacion", "clave-segura-1");

        assertThat(usuarioRepositorio.existsByCorreo(CORREO)).isTrue();
        assertThatThrownBy(() -> registroUsuarioServicio.registrarInversionista(CORREO, "Otra vez", "clave-segura-1"))
                .isInstanceOf(CorreoYaRegistradoException.class);
    }
}
