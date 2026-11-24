package br.com.sos.os;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long>,
        JpaSpecificationExecutor<OrdemServico> {

    /** Próximo número de OS. Vem de uma sequence do banco: nunca repete, mesmo com aberturas simultâneas (RNF008). */
    @Query(value = "select nextval('os_numero_seq')", nativeQuery = true)
    Long proximoNumero();

    Optional<OrdemServico> findByNumero(Long numero);
}
