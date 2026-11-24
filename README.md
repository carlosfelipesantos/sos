# SoS – Sistema de Ordem de Serviço (backend)

Spring Boot 4.1 · Java 21 · Spring Data JPA · Spring Security · PostgreSQL · Swagger

## Como rodar

1. Banco: `docker compose up -d` (PostgreSQL em `localhost:5432`, banco `sos`, usuário e senha `postgres`).
   Se já tiver um PostgreSQL local, crie o banco `sos` nele ou ajuste `DB_URL`, `DB_USER` e `DB_PASSWORD`.
2. API: `./mvnw spring-boot:run` → http://localhost:8080
3. Swagger: http://localhost:8080/swagger-ui.html

As tabelas são criadas automaticamente pelo Hibernate (`ddl-auto: update`).

## Usuários de teste (senha `123`)

| Login | Cargo |
|---|---|
| `atendente` | ATENDENTE |
| `prestador` | PRESTADOR |
| `prestador2` | PRESTADOR (tipoUsuario EQUIPE) |
| `autonomo` | ATENDENTE e PRESTADOR |

## Login (para o front)

- `POST /auth/login` com `{ "login": "atendente", "senha": "123" }` → cria a sessão (cookie `JSESSIONID`, expira em 30 min sem uso).
- `GET /auth/me` → `{ id, nomeUsuario, login, cargos }`.
- `POST /auth/logout` → 204.
- O front precisa mandar o cookie: `credentials: 'include'` (fetch) ou `withCredentials: true` (axios).
- Origens liberadas no CORS: `sos.cors.origens` em `application.yml` (ou variável `CORS_ORIGENS`).

## Erros

Todos os erros voltam no formato `{ "campo": "descricao", "mensagem": "..." }`.

| Exceção | HTTP |
|---|---|
| `RegraNegocioException` | 400 |
| `AcessoNegadoException` | 403 |
| `NaoEncontradoException` | 404 |
| `ConflitoException` | 409 |
| Sem login | 401 |
| Cargo sem permissão (`@PreAuthorize`) | 403 |

## Para o resto do time

- Proteja cada endpoint com `@PreAuthorize("hasRole('ATENDENTE')")` ou `hasRole('PRESTADOR')`.
- Usuário logado no controller: `@AuthenticationPrincipal UsuarioAutenticado usuario`.
- Usuário logado como entidade (para gravar em `Historico`, `Orcamento.decididoPor`): `usuarioLogadoService.atual()`.
- Número da OS: `ordemServicoRepository.proximoNumero()` (sequence `os_numero_seq`).
- Prestadores para o select do front: `usuarioRepository.findByCargosTipoCargoOrderByNomeUsuario(Cargo.PRESTADOR)`.
- Cliente para abrir a OS: `clienteService.buscarPorId(clienteId)` (lança 404 se não existir).
- Ao encerrar ou cancelar a OS, preencher `dataFinalizacao`.
- `Historico` é `@Immutable`: depois de gravado não muda.

## Clientes (RF001) – só ATENDENTE

| Endpoint | O que faz | Respostas |
|---|---|---|
| `POST /clientes` | Cadastra | 201 · 400 campo inválido · 409 `Cliente já cadastrado.` |
| `PUT /clientes/{id}` | Edita | 200 · 400 · 404 · 409 se o CPF/CNPJ for de outro cliente |
| `GET /clientes?busca=` | Lista (sem `busca`) ou pesquisa por parte do nome ou do CPF/CNPJ | 200 |
| `GET /clientes/{id}` | Mostra um cliente | 200 · 404 |

Corpo do POST e do PUT:

```json
{ "nome": "Maria da Silva", "cpfCnpj": "529.982.247-25", "telefone": "(17) 99999-0000", "email": "maria@email.com" }
```

- `nome`, `cpfCnpj` e `telefone` são obrigatórios; `email` é opcional (pode mandar `null` ou `""`).
- `cpfCnpj` aceita CPF ou CNPJ, com ou sem máscara, e confere os dígitos verificadores (`CpfCnpjValidator`). É gravado e devolvido só com números.
- Resposta: `{ "id": 1, "nome": "...", "cpfCnpj": "52998224725", "telefone": "...", "email": "...", "ativo": true }`.
- No 409, para mostrar o cadastro que já existe (fluxo alternativo do RF001), o front chama `GET /clientes?busca={cpfCnpj}`.
- Prestador recebe 403 em todos os endpoints de clientes.

## Modelo de dados (segue o diagrama de classes)

| Classe | Atributos |
|---|---|
| Usuario | id, nomeUsuario, email, cpf, tipoUsuario (PESSOA/EQUIPE), telefone, login, senhaHash, cargos |
| Cargo | id, tipoCargo (ATENDENTE/PRESTADOR) |
| Cliente | id, nomeCliente, email, statusCliente, cpf (CPF ou CNPJ), telefone |
| OrdemServico | id, numero, tipoServico, status, dataAbertura, dataFinalizacao, cliente, prestador |
| Orcamento | id, valorOrcamento, descricao, dataCriacao, dataDecisao, prazoDias, status, os, decididoPor |
| Historico | id, descricao, dataHistorico, statusAnterior, statusNovo, os, usuario |

Campos que não estão no diagrama, mas o SRS exige: `login`/`senhaHash` (RNF001), `numero`/`status` da OS (RF002), `prazoDias`/`status` do orçamento (RF003), `statusAnterior`/`statusNovo` do histórico (RNF005).
CPF é `String` (não cabe em `int` e pode começar com zero) e o valor é `BigDecimal` (dinheiro não pode usar `float`).

## Estrutura

```
br.com.sos
├── auth        Usuario, Cargo, login, segurança, usuários de teste
├── cliente     Cliente, ClienteRepository
├── os          OrdemServico, StatusOS, Historico e repositories
├── orcamento   Orcamento, StatusOrcamento, OrcamentoRepository
└── comum       ErroResponse, exceções e GlobalExceptionHandler
```
