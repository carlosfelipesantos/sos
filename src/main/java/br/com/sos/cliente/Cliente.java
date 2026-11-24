package br.com.sos.cliente;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cliente que solicita a OS (diagrama de classes: Cliente, relacionamento "Solicita"). */
@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_cliente", nullable = false)
    private String nomeCliente;

    private String email;

    /** true = cliente ativo. */
    @Column(name = "status_cliente", nullable = false)
    private boolean statusCliente = true;

    /**
     * CPF (11 dígitos) ou CNPJ (14 dígitos), somente números. Não pode repetir (RF001).
     * String porque não cabe em int e pode começar com zero.
     */
    @Column(nullable = false, unique = true, length = 14)
    private String cpf;

    @Column(nullable = false)
    private String telefone;
}
