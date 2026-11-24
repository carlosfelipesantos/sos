package br.com.sos.cliente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByCpf(String cpf);

    Optional<Cliente> findByCpf(String cpf);

    /** Busca por parte do nome (sem diferenciar maiúsculas) ou pelo CPF/CNPJ. */
    List<Cliente> findByNomeClienteContainingIgnoreCaseOrCpfContainingOrderByNomeCliente(String nome, String cpf);
}
