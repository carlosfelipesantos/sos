package br.com.sos.cliente;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CpfCnpjValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-25", "52998224725", "111.444.777-35", "123.456.789-09"})
    void aceitaCpfValido(String cpf) {
        assertThat(CpfCnpjValidator.valido(cpf)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11.222.333/0001-81", "11222333000181", "45.997.418/0001-53", "06.990.590/0001-23"})
    void aceitaCnpjValido(String cnpj) {
        assertThat(CpfCnpjValidator.valido(cnpj)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "529.982.247-26",        // dígito verificador errado
            "123.456.789-00",
            "111.111.111-11",        // todos iguais
            "00000000000",
            "11.222.333/0001-82",    // dígito verificador errado
            "11.111.111/1111-11",
            "1234567890",            // 10 dígitos
            "123456789012",          // 12 dígitos
            "5299822472a",           // letra
            "529,982,247-25",        // vírgula não é máscara
            "abc"})
    void recusaInvalido(String valor) {
        assertThat(CpfCnpjValidator.valido(valor)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void recusaVazio(String valor) {
        assertThat(CpfCnpjValidator.valido(valor)).isFalse();
    }

    @Test
    void limpaMascara() {
        assertThat(CpfCnpjValidator.limpar("529.982.247-25")).isEqualTo("52998224725");
        assertThat(CpfCnpjValidator.limpar("11.222.333/0001-81")).isEqualTo("11222333000181");
        assertThat(CpfCnpjValidator.limpar(" 529 982 247 25 ")).isEqualTo("52998224725");
        assertThat(CpfCnpjValidator.limpar(null)).isNull();
    }
}
