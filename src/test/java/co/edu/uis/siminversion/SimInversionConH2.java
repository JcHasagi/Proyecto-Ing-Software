package co.edu.uis.siminversion;

import org.springframework.boot.SpringApplication;

/**
 * Arranca la aplicación con H2 en memoria (perfil "test"), sin MySQL. Útil para revisar pantallas;
 * los datos se pierden al detenerla. Se ejecuta con: mvnw spring-boot:test-run
 */
public class SimInversionConH2 {

    public static void main(String[] args) {
        SpringApplication.from(SimInversionApplication::main).withAdditionalProfiles("test").run(args);
    }
}
