package br.com.sos.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cargo (papel) do usuário no sistema, conforme o diagrama de classes.
 * Os cargos usados pelo sistema são ATENDENTE e PRESTADOR (SRS 2.2).
 * Um usuário pode ter os dois (prestador autônomo).
 */
@Entity
@Table(name = "cargo")
@Getter
@Setter
@NoArgsConstructor
public class Cargo {

    public static final String ATENDENTE = "ATENDENTE";
    public static final String PRESTADOR = "PRESTADOR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_cargo", nullable = false, unique = true, length = 30)
    private String tipoCargo;

    public Cargo(String tipoCargo) {
        this.tipoCargo = tipoCargo;
    }
}
