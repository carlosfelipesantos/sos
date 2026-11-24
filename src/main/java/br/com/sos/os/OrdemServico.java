package br.com.sos.os;

import br.com.sos.auth.Usuario;
import br.com.sos.cliente.Cliente;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ordem_servico", indexes = {
        @Index(name = "idx_os_status", columnList = "status"),
        @Index(name = "idx_os_cliente", columnList = "cliente_id"),
        @Index(name = "idx_os_prestador", columnList = "prestador_id")
})
@Getter
@Setter
@NoArgsConstructor
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número único da OS, gerado por OrdemServicoRepository.proximoNumero(). */
    @Column(nullable = false, unique = true, updatable = false)
    private Long numero;

    @Column(nullable = false, length = 2000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOS status;

    /** Data automática e sem edição (RF002). */
    @Column(name = "data_abertura", nullable = false, updatable = false)
    private LocalDateTime dataAbertura;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Usuario prestador;

    @PrePersist
    void aoCriar() {
        if (dataAbertura == null) {
            dataAbertura = LocalDateTime.now();
        }
    }
}
