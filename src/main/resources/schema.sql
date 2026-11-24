-- Sequence usada para gerar o número único da OS (OrdemServicoRepository.proximoNumero)
create sequence if not exists os_numero_seq start with 1 increment by 1;
