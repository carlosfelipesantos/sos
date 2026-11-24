package br.com.sos.comum;

import lombok.Getter;

/** Registro duplicado (ex.: CPF/CNPJ já cadastrado). Vira HTTP 409. */
@Getter
public class ConflitoException extends RuntimeException {

    private final String campo;

    public ConflitoException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }
}
