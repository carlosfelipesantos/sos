package br.com.sos.os;

import br.com.sos.auth.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Registro de andamento ou mudança de status da OS (diagrama de classes: Historico).
 * Depois de gravado não pode ser alterado (RF004, RNF005): por isso @Immutable.
 * statusAnterior e statusNovo ficam nulos quando é só uma observação de andamento.
 */
@Entity
@Immutable
@Table(name = "historico", indexes = @Index(name = "idx_historico_os", columnList = "os_id"))
@Getter
@Setter
@NoArgsConstructor
public class Historico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 2000)
    private String descricao;

    @Column(name = "data_historico", nullable = false)
    private LocalDateTime dataHistorico;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 30)
    private StatusOS statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", length = 30)
    private StatusOS statusNovo;

    /** Relacionamento "Possui": a OS possui o histórico. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "os_id", nullable = false)
    private OrdemServico os;

    /** Usuário que fez o registro (RNF005). */
    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @PrePersist
    void aoCriar() {
        if (dataHistorico == null) {
            dataHistorico = LocalDateTime.now();
        }
    }
}
