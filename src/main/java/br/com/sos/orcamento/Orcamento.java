package br.com.sos.orcamento;

import br.com.sos.auth.Usuario;
import br.com.sos.os.OrdemServico;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orcamento", indexes = @Index(name = "idx_orcamento_os", columnList = "os_id"))
@Getter
@Setter
@NoArgsConstructor
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "os_id", nullable = false)
    private OrdemServico os;

    @Column(nullable = false, length = 2000)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "prazo_dias", nullable = false)
    private Integer prazoDias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOrcamento status;

    /** Atendente que registrou a decisão do cliente (preenchido na aprovação/recusa). */
    @ManyToOne
    @JoinColumn(name = "decidido_por_id")
    private Usuario decididoPor;

    @Column(name = "data_decisao")
    private LocalDateTime dataDecisao;
}
