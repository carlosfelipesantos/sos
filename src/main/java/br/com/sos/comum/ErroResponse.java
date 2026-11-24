package br.com.sos.comum;

/** Formato único de erro da API: { "campo": "descricao", "mensagem": "..." }. campo é null quando o erro não é de um campo. */
public record ErroResponse(String campo, String mensagem) {
}
