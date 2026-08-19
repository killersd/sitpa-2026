package br.com.sitpa.rules;

import br.com.sitpa.domain.TipoRegra;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Carrega os artefatos de regra entregues junto com o build (<code>resources/rules</code>).
 *
 * <p>Sao o conhecimento "de fabrica": funcionam sem nenhuma configuracao no banco e servem de
 * ponto de partida. Regras cadastradas pelo administrador chegam depois e podem sobrepor estas.
 * A ordenacao por nome de arquivo e proposital &mdash; o prefixo numerico (00-, 10-) deixa
 * explicita a leitura de baixo para cima, da camada generica para as especializacoes.</p>
 */
@Component
public class RegrasDoClasspath {

    private static final String PADRAO = "classpath*:rules/*";

    public List<ArtefatoRegra> carregar() {
        PathMatchingResourcePatternResolver resolvedor = new PathMatchingResourcePatternResolver(
                RegrasDoClasspath.class.getClassLoader());
        List<ArtefatoRegra> artefatos = new ArrayList<>();
        try {
            for (Resource recurso : resolvedor.getResources(PADRAO)) {
                String nome = recurso.getFilename();
                if (nome == null) {
                    continue;
                }
                TipoRegra tipo;
                if (nome.endsWith(".drl")) {
                    tipo = TipoRegra.DRL;
                } else if (nome.endsWith(".dmn")) {
                    tipo = TipoRegra.DMN;
                } else {
                    continue;
                }
                String conteudo;
                try (var entrada = recurso.getInputStream()) {
                    conteudo = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
                }
                artefatos.add(ArtefatoRegra.doClasspath(nome, tipo, conteudo));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler as regras do classpath (" + PADRAO + ")", e);
        }
        artefatos.sort(Comparator.comparing(ArtefatoRegra::nome));
        return artefatos;
    }
}
