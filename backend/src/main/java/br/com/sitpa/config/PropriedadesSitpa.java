package br.com.sitpa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuracao propria do SITPa.
 *
 * @param jwtSegredo     chave HMAC do token; precisa de no minimo 32 bytes e <b>deve</b> vir do
 *                       ambiente em producao (variavel SITPA_JWT_SEGREDO)
 * @param jwtValidadeMinutos tempo de vida do token
 * @param corsOrigens    origens liberadas para o frontend; aceitam curinga, por exemplo
 *                       {@code http://localhost:*}
 * @param seedInicial    cria protocolo de demonstracao e usuarios padrao quando o banco esta vazio
 */
@ConfigurationProperties(prefix = "sitpa")
public record PropriedadesSitpa(String jwtSegredo,
                                Integer jwtValidadeMinutos,
                                List<String> corsOrigens,
                                Boolean seedInicial) {

    public int validadeMinutos() {
        return jwtValidadeMinutos == null ? 480 : jwtValidadeMinutos;
    }

    public List<String> origens() {
        return corsOrigens == null || corsOrigens.isEmpty()
                ? List.of("http://localhost", "http://localhost:*", "http://127.0.0.1:*")
                : corsOrigens;
    }

    public boolean deveSemear() {
        return seedInicial == null || seedInicial;
    }
}
