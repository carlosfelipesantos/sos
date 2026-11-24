package br.com.sos.os;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoRepository extends JpaRepository<Historico, Long> {

    List<Historico> findByOsIdOrderByDataHistoricoAsc(Long osId);
}
