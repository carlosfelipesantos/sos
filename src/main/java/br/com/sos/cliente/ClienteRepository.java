package br.com.sos.cliente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByCpfCnpj(String cpfCnpj);

    Optional<Cliente> findByCpfCnpj(String cpfCnpj);

    /** Busca por parte do nome (sem diferenciar maiúsculas) ou pelo CPF/CNPJ. */
    List<Cliente> findByNomeContainingIgnoreCaseOrCpfCnpjContainingOrderByNome(String nome, String cpfCnpj);
}
