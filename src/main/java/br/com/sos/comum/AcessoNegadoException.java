package br.com.sos.comum;

/** Usuário logado sem direito sobre o registro (ex.: OS de outro prestador). Vira HTTP 403. */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
