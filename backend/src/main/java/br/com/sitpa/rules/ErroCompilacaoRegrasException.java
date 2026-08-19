package br.com.sitpa.rules;

import java.util.List;

/**
 * Lancada quando um conjunto de regras nao compila. A base de conhecimento anterior
 * permanece ativa: uma regra invalida publicada por engano nao pode derrubar a triagem.
 */
public class ErroCompilacaoRegrasException extends RuntimeException {

    private final List<String> problemas;

    public ErroCompilacaoRegrasException(List<String> problemas) {
        super("Falha ao compilar as regras: " + String.join(" | ", problemas));
        this.problemas = List.copyOf(problemas);
    }

    public List<String> getProblemas() {
        return problemas;
    }
}
