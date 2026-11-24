package br.com.sos.comum;

import lombok.Getter;

/** Regra de negócio violada. Vira HTTP 400. */
@Getter
public class RegraNegocioException extends RuntimeException {

    private final String campo;

    public RegraNegocioException(String mensagem) {
        this(null, mensagem);
    }

    public RegraNegocioException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }
}
