package br.com.sos.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Usuário do sistema (diagrama de classes: Usuario, relacionamento "Possui" com Cargo).
 * login e senhaHash não estão no diagrama, mas são obrigatórios para o login (RNF001).
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    /** Pessoa ou equipe (SRS 1.3: o prestador pode ser uma pessoa ou uma equipe). */
    public static final String TIPO_PESSOA = "PESSOA";
    public static final String TIPO_EQUIPE = "EQUIPE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_usuario", nullable = false)
    private String nomeUsuario;

    private String email;

    /** CPF do usuário, somente dígitos. String porque CPF não cabe em int e pode começar com zero. */
    @Column(unique = true, length = 11)
    private String cpf;

    @Column(name = "tipo_usuario", nullable = false, length = 20)
    private String tipoUsuario = TIPO_PESSOA;

    private String telefone;

    @Column(nullable = false, unique = true)
    private String login;

    /** Hash BCrypt da senha. A senha nunca é gravada em texto puro (RNF001). */
    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_cargo",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "cargo_id"))
    private Set<Cargo> cargos = new HashSet<>();

    public boolean temCargo(String tipoCargo) {
        return cargos.stream().anyMatch(c -> c.getTipoCargo().equals(tipoCargo));
    }
}
