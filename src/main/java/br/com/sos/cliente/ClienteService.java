package br.com.sos.cliente;

import br.com.sos.comum.ConflitoException;
import br.com.sos.comum.NaoEncontradoException;
import br.com.sos.comum.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras do cadastro de clientes (RF001).
 * Para abrir OS (RF002), use {@link #buscarPorId(Long)}: lança 404 se o cliente não existir.
 */
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    /** Cadastra o cliente. CPF/CNPJ inválido dá 400; já cadastrado dá 409. */
    @Transactional
    public Cliente cadastrar(ClienteRequest dados) {
        String cpfCnpj = validarCpfCnpj(dados.cpfCnpj());
        if (clienteRepository.existsByCpf(cpfCnpj)) {
            throw clienteJaCadastrado();
        }
        Cliente cliente = new Cliente();
        preencher(cliente, dados, cpfCnpj);
        return salvar(cliente);
    }

    /** Edita o cliente com as mesmas validações do cadastro. O CPF/CNPJ do próprio cliente não conta como repetido. */
    @Transactional
    public Cliente editar(Long id, ClienteRequest dados) {
        Cliente cliente = buscarPorId(id);
        String cpfCnpj = validarCpfCnpj(dados.cpfCnpj());
        boolean usadoPorOutroCliente = clienteRepository.findByCpf(cpfCnpj)
                .filter(outro -> !outro.getId().equals(id))
                .isPresent();
        if (usadoPorOutroCliente) {
            throw clienteJaCadastrado();
        }
        preencher(cliente, dados, cpfCnpj);
        return salvar(cliente);
    }

    /**
     * Procura por parte do nome (sem diferenciar maiúsculas) ou por parte do CPF/CNPJ, com ou sem máscara.
     * Sem texto, lista todos em ordem de nome.
     */
    @Transactional(readOnly = true)
    public List<Cliente> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return clienteRepository.findAll(Sort.by("nomeCliente"));
        }
        String busca = texto.trim();
        String digitos = busca.replaceAll("\\D", "");
        if (digitos.isEmpty()) {
            // Sem números: busca só pelo nome (CpfContaining("") traria todos os clientes).
            return clienteRepository.findByNomeClienteContainingIgnoreCaseOrderByNomeCliente(busca);
        }
        return clienteRepository.findByNomeClienteContainingIgnoreCaseOrCpfContainingOrderByNomeCliente(busca, digitos);
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Cliente não encontrado."));
    }

    private String validarCpfCnpj(String valor) {
        if (!CpfCnpjValidator.valido(valor)) {
            throw new RegraNegocioException("cpfCnpj", "CPF ou CNPJ inválido. Confira os números digitados.");
        }
        return CpfCnpjValidator.limpar(valor);
    }

    private void preencher(Cliente cliente, ClienteRequest dados, String cpfCnpj) {
        cliente.setNomeCliente(dados.nome().trim());
        cliente.setCpf(cpfCnpj);
        cliente.setTelefone(dados.telefone().trim());
        cliente.setEmail(dados.email() == null || dados.email().isBlank() ? null : dados.email().trim());
    }

    private Cliente salvar(Cliente cliente) {
        try {
            return clienteRepository.saveAndFlush(cliente);
        } catch (DataIntegrityViolationException e) {
            // Dois cadastros com o mesmo CPF/CNPJ ao mesmo tempo: o unique do banco barra o segundo.
            throw clienteJaCadastrado();
        }
    }

    private ConflitoException clienteJaCadastrado() {
        return new ConflitoException("cpfCnpj", "Cliente já cadastrado.");
    }
}
