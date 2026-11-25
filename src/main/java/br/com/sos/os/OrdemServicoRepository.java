package br.com.sos.os;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long>,
        JpaSpecificationExecutor<OrdemServico> {

    /** Próximo número de OS. Vem de uma sequence do banco: nunca repete, mesmo com aberturas simultâneas (RNF008). */
    @Query(value = "select nextval('os_numero_seq')", nativeQuery = true)
    Long proximoNumero();

    Optional<OrdemServico> findByNumero(Long numero);

    /**
     * Busca a OS travando a linha até o fim da transação (select ... for update).
     * Usado no andamento e no encerramento (RF004, RF005): duas mudanças de status ao mesmo tempo
     * na mesma OS esperam uma pela outra, e a segunda já enxerga o status novo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select os from OrdemServico os where os.id = :id")
    Optional<OrdemServico> buscarParaAlterar(@Param("id") Long id);
}
