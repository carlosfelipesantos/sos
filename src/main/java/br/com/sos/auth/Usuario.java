package br.com.sos.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String login;

    /** Hash BCrypt da senha. A senha nunca é gravada em texto puro (RNF001). */
    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_papel", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "papel", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Papel> papeis = new HashSet<>();

    public boolean temPapel(Papel papel) {
        return papeis.contains(papel);
    }
}
