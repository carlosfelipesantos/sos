package br.com.sos.os;

/** Etapas da ordem de serviço (SRS 1.3). */
public enum StatusOS {
    ABERTA,
    AGUARDANDO_APROVACAO,
    EM_EXECUCAO,
    PRONTA,
    ENCERRADA,
    CANCELADA
}
