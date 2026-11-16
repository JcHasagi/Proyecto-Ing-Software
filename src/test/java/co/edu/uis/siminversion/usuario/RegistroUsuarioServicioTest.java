package co.edu.uis.siminversion.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegistroUsuarioServicioTest {

    private static final Pattern COSTO_BCRYPT = Pattern.compile("^\\$2[aby]?\\$(\\d{2})\\$.+");

    @Autowired
    private RegistroUsuarioServicio registroUsuarioServicio;

    @Autowired
    private PasswordEncoder codificadorContrasenas;

    @Test
    void registraUnInversionistaConLaContrasenaCifradaConBcryptDeCostoAlMenos10() {
        Usuario usuario = registroUsuarioServicio.registrarInversionista("ana@example.com", "Ana", "clave-segura-1");

        assertThat(usuario.getId()).isNotNull();
        assertThat(usuario.getRol()).isEqualTo(Rol.INVERSIONISTA);
        assertThat(usuario.getHashContrasena()).isNotEqualTo("clave-segura-1");
        assertThat(codificadorContrasenas.matches("clave-segura-1", usuario.getHashContrasena())).isTrue();
        Matcher costo = COSTO_BCRYPT.matcher(usuario.getHashContrasena());
        assertThat(costo.matches()).isTrue();
        assertThat(Integer.parseInt(costo.group(1))).isGreaterThanOrEqualTo(10);
    }

    @Test
    void guardaElCorreoSinEspaciosYEnMinusculas() {
        Usuario usuario = registroUsuarioServicio.registrarInversionista("  Beto@Example.COM ", "Beto", "clave-segura-1");

        assertThat(usuario.getCorreo()).isEqualTo("beto@example.com");
    }

    @Test
    void rechazaUnCorreoYaRegistradoSinImportarLasMayusculas() {
        registroUsuarioServicio.registrarInversionista("caro@example.com", "Caro", "clave-segura-1");

        assertThatThrownBy(() ->
                registroUsuarioServicio.registrarInversionista("CARO@example.com", "Otra", "clave-segura-2"))
                .isInstanceOf(CorreoYaRegistradoException.class);
    }

    @Test
    void rechazaContrasenasCortasOMayoresA72Bytes() {
        assertThatThrownBy(() -> registroUsuarioServicio.registrarInversionista("dani@example.com", "Dani", "corta"))
                .isInstanceOf(RegistroInvalidoException.class)
                .hasMessageContaining("8");
        assertThatThrownBy(() ->
                registroUsuarioServicio.registrarInversionista("dani@example.com", "Dani", "ñ".repeat(37)))
                .isInstanceOf(RegistroInvalidoException.class)
                .hasMessageContaining("72");
    }
}
