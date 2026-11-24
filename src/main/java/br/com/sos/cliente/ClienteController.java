package br.com.sos.cliente;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Cadastro de clientes (RF001). Só o ATENDENTE usa (RNF002): outros cargos recebem 403.
 * POST /clientes         -> cadastra (201)
 * PUT  /clientes/{id}    -> edita
 * GET  /clientes?busca=  -> lista ou pesquisa por nome ou CPF/CNPJ
 * GET  /clientes/{id}    -> mostra um cliente
 */
@Tag(name = "Clientes", description = "Cadastro de clientes (RF001). Somente ATENDENTE.")
@RestController
@RequestMapping("/clientes")
@PreAuthorize("hasRole('ATENDENTE')")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(summary = "Cadastra um cliente",
            description = "CPF/CNPJ com ou sem máscara. 400 se algum campo for inválido; 409 se o CPF/CNPJ já estiver cadastrado.")
    @PostMapping
    public ResponseEntity<ClienteResponse> cadastrar(@Valid @RequestBody ClienteRequest dados) {
        Cliente cliente = clienteService.cadastrar(dados);
        URI endereco = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(cliente.getId()).toUri();
        return ResponseEntity.created(endereco).body(ClienteResponse.de(cliente));
    }

    @Operation(summary = "Edita um cliente",
            description = "Mesmas validações do cadastro. 404 se o cliente não existir; 409 se o CPF/CNPJ for de outro cliente.")
    @PutMapping("/{id}")
    public ClienteResponse editar(@PathVariable("id") Long id, @Valid @RequestBody ClienteRequest dados) {
        return ClienteResponse.de(clienteService.editar(id, dados));
    }

    @Operation(summary = "Lista ou pesquisa clientes",
            description = "busca = parte do nome ou do CPF/CNPJ (com ou sem máscara). Sem busca, lista todos em ordem de nome.")
    @GetMapping
    public List<ClienteResponse> buscar(@RequestParam(name = "busca", required = false) String busca) {
        return clienteService.buscar(busca).stream().map(ClienteResponse::de).toList();
    }

    @Operation(summary = "Mostra um cliente", description = "404 se o cliente não existir.")
    @GetMapping("/{id}")
    public ClienteResponse buscarPorId(@PathVariable("id") Long id) {
        return ClienteResponse.de(clienteService.buscarPorId(id));
    }
}
