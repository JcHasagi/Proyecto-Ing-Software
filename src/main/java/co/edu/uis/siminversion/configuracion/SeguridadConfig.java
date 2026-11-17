package co.edu.uis.siminversion.configuracion;

import co.edu.uis.siminversion.usuario.Rol;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SeguridadConfig {

    /** Costo de bcrypt; el SRS exige al menos 10. */
    public static final int COSTO_BCRYPT = 12;

    @Bean
    public PasswordEncoder codificadorContrasenas() {
        return new BCryptPasswordEncoder(COSTO_BCRYPT);
    }

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(peticiones -> peticiones
                .requestMatchers("/login", "/registro", "/css/**", "/js/**", "/error", "/favicon.ico",
                        "/favicon.svg").permitAll()
                // Las funciones de administrador se bloquean en el servidor, no solo en la interfaz.
                .requestMatchers("/admin/**").hasRole(Rol.ADMINISTRADOR.name())
                .anyRequest().authenticated())
            .formLogin(formulario -> formulario
                .loginPage("/login")
                .usernameParameter("correo")
                .passwordParameter("contrasena")
                // REQ-002: tras iniciar sesión, el último portafolio usado (o la invitación a crear uno).
                .defaultSuccessUrl("/portafolio")
                .permitAll())
            .logout(salida -> salida
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?salida")
                .permitAll());
        return http.build();
    }
}
