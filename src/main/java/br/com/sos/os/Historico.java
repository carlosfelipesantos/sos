package br.com.sos.os;

import br.com.sos.auth.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Registro de andamento ou mudança de status da OS.
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

    @ManyToOne(optional = false)
    @JoinColumn(name = "os_id", nullable = false)
    private OrdemServico os;

    @Column(length = 2000)
    private String observacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 30)
    private StatusOS statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", length = 30)
    private StatusOS statusNovo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @PrePersist
    void aoCriar() {
        if (dataHora == null) {
            dataHora = LocalDateTime.now();
        }
    }
}
