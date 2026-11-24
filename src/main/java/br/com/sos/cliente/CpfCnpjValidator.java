package br.com.sos.cliente;

/**
 * Valida CPF e CNPJ pelos dígitos verificadores (RF001).
 * Aceita o número com ou sem máscara: 529.982.247-25, 52998224725, 11.222.333/0001-81 ou 11222333000181.
 */
public final class CpfCnpjValidator {

    private CpfCnpjValidator() {
    }

    /** Remove pontos, traços, barra e espaços. Ex.: "529.982.247-25" vira "52998224725". */
    public static String limpar(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.replaceAll("[.\\-/\\s]", "");
    }

    /** true se for um CPF (11 dígitos) ou CNPJ (14 dígitos) com os dígitos verificadores corretos. */
    public static boolean valido(String valor) {
        String numero = limpar(valor);
        if (numero == null || !numero.matches("\\d+")) {
            return false;
        }
        return switch (numero.length()) {
            case 11 -> cpfValido(numero);
            case 14 -> cnpjValido(numero);
            default -> false;
        };
    }

    private static boolean cpfValido(String cpf) {
        if (todosIguais(cpf)) {
            return false; // 000.000.000-00, 111.111.111-11... passam na conta, mas não existem
        }
        return digitoCpf(cpf, 9) == valorDe(cpf, 9)
                && digitoCpf(cpf, 10) == valorDe(cpf, 10);
    }

    /** Pesos 10..2 para o 1º dígito (9 números) e 11..2 para o 2º (10 números). */
    private static int digitoCpf(String cpf, int quantidade) {
        int soma = 0;
        int peso = quantidade + 1;
        for (int i = 0; i < quantidade; i++) {
            soma += valorDe(cpf, i) * peso--;
        }
        return digito(soma);
    }

    private static boolean cnpjValido(String cnpj) {
        if (todosIguais(cnpj)) {
            return false;
        }
        return digitoCnpj(cnpj, 12) == valorDe(cnpj, 12)
                && digitoCnpj(cnpj, 13) == valorDe(cnpj, 13);
    }

    /** Pesos 5,4,3,2,9..2 para o 1º dígito (12 números) e 6,5,4,3,2,9..2 para o 2º (13 números). */
    private static int digitoCnpj(String cnpj, int quantidade) {
        int soma = 0;
        int peso = quantidade - 7;
        for (int i = 0; i < quantidade; i++) {
            soma += valorDe(cnpj, i) * peso;
            peso = peso == 2 ? 9 : peso - 1;
        }
        return digito(soma);
    }

    private static int digito(int soma) {
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static int valorDe(String numero, int posicao) {
        return numero.charAt(posicao) - '0';
    }

    private static boolean todosIguais(String numero) {
        return numero.chars().allMatch(c -> c == numero.charAt(0));
    }
}
