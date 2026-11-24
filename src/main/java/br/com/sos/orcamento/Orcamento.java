package br.com.sos.orcamento;

import br.com.sos.auth.Usuario;
import br.com.sos.os.OrdemServico;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Orçamento da OS (diagrama de classes: Orcamento).
 * prazoDias e status não estão no diagrama, mas são obrigatórios pelo RF003.
 */
@Entity
@Table(name = "orcamento", indexes = @Index(name = "idx_orcamento_os", columnList = "os_id"))
@Getter
@Setter
@NoArgsConstructor
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** BigDecimal em vez de float: valor em dinheiro precisa ser exato. */
    @Column(name = "valor_orcamento", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorOrcamento;

    @Column(nullable = false, length = 2000)
    private String descricao;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_decisao")
    private LocalDateTime dataDecisao;

    @Column(name = "prazo_dias", nullable = false)
    private Integer prazoDias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOrcamento status;

    /** Relacionamento "Contem": a OS contém os orçamentos. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "os_id", nullable = false)
    private OrdemServico os;

    /** Relacionamento "Define": atendente que registrou a decisão do cliente. */
    @ManyToOne
    @JoinColumn(name = "decidido_por_id")
    private Usuario decididoPor;

    @PrePersist
    void aoCriar() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }
}
