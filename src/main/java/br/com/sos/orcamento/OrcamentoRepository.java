package br.com.sos.orcamento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    List<Orcamento> findByOsId(Long osId);

    List<Orcamento> findByOsIdOrderByIdAsc(Long osId);
}
