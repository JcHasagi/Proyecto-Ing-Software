package co.edu.uis.siminversion.usuario;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Puente entre la tabla usuario y Spring Security (REQ-002). El nombre de usuario es el correo. */
@Service
public class ServicioDetallesUsuario implements UserDetailsService {

    private final UsuarioRepositorio usuarioRepositorio;

    public ServicioDetallesUsuario(UsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) {
        Usuario usuario = usuarioRepositorio.findByCorreo(RegistroUsuarioServicio.normalizarCorreo(correo))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return User.withUsername(usuario.getCorreo())
                .password(usuario.getHashContrasena())
                .roles(usuario.getRol().name())
                .disabled(!usuario.isActivo())
                .build();
    }
}
