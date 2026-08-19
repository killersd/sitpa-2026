package br.com.sitpa.rules;

import br.com.sitpa.domain.TipoRegra;

/**
 * Uma unidade de conhecimento a ser compilada na base: um arquivo DRL ou um modelo DMN.
 *
 * @param nome     identificacao unica dentro da base (vira o caminho do recurso no KieFileSystem)
 * @param tipo     DRL ou DMN
 * @param conteudo texto integral do artefato
 * @param origem   de onde veio, apenas para diagnostico ("classpath" ou "banco")
 */
public record ArtefatoRegra(String nome, TipoRegra tipo, String conteudo, String origem) {

    private static final char BOM = '﻿';

    public ArtefatoRegra {
        // Um BOM no inicio do texto faz o parser do Drools falhar com "no viable alternative at
        // input ''", mensagem que nao ajuda ninguem. Como o conteudo pode chegar colado pela API
        // ou de um arquivo salvo no Windows, a normalizacao fica aqui, na fronteira do modelo.
        if (conteudo != null && !conteudo.isEmpty() && conteudo.charAt(0) == BOM) {
            conteudo = conteudo.substring(1);
        }
    }

    public static ArtefatoRegra doClasspath(String nome, TipoRegra tipo, String conteudo) {
        return new ArtefatoRegra(nome, tipo, conteudo, "classpath");
    }

    public static ArtefatoRegra doBanco(String nome, TipoRegra tipo, String conteudo) {
        return new ArtefatoRegra(nome, tipo, conteudo, "banco");
    }

    /** Caminho do recurso dentro do KieFileSystem; a extensao determina o compilador usado. */
    public String caminhoNoKieFileSystem() {
        String extensao = tipo == TipoRegra.DMN ? ".dmn" : ".drl";
        String base = nome.endsWith(extensao) ? nome.substring(0, nome.length() - extensao.length()) : nome;
        return "src/main/resources/br/com/sitpa/regras/" + base.replaceAll("[^A-Za-z0-9._-]", "_") + extensao;
    }
}
