package br.com.sos.os;

import br.com.sos.auth.Usuario;
import br.com.sos.cliente.Cliente;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Ordem de serviço (diagrama de classes: OrdemServico).
 * numero e status não estão no diagrama, mas são obrigatórios pelo SRS (RF002, seção 1.3).
 */
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

    /** Descrição do serviço solicitado (obrigatória, RF002). */
    @Column(name = "tipo_servico", nullable = false, length = 2000)
    private String tipoServico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOS status;

    /** Data automática e sem edição (RF002). */
    @Column(name = "data_abertura", nullable = false, updatable = false)
    private LocalDateTime dataAbertura;

    /** Preenchida quando a OS fica ENCERRADA ou CANCELADA (RF005). */
    @Column(name = "data_finalizacao")
    private LocalDateTime dataFinalizacao;

    /** Relacionamento "Solicita": o cliente solicita a OS. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    /** Relacionamento "Soluciona": prestador responsável pela OS. */
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
