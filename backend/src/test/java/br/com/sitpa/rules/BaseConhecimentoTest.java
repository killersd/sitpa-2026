package br.com.sitpa.rules;

import br.com.sitpa.domain.OrigemDecisao;
import br.com.sitpa.domain.TipoRegra;
import br.com.sitpa.rules.model.FatoTriagem;
import br.com.sitpa.rules.model.PropostaClassificacao;
import br.com.sitpa.rules.model.SintomaObservado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Valida a compilacao em runtime da base de conhecimento: e o mecanismo que sustenta
 * o requisito de editar regras sem recompilar a aplicacao.
 */
class BaseConhecimentoTest {

    private final RegrasDoClasspath regrasDoClasspath = new RegrasDoClasspath();

    private BaseConhecimento baseCarregada() {
        BaseConhecimento base = new BaseConhecimento();
        base.recarregar(regrasDoClasspath.carregar());
        return base;
    }

    /** Dispara as regras e devolve as propostas na ordem de disparo. */
    private List<PropostaClassificacao> disparar(BaseConhecimento base, FatoTriagem fato,
                                                 SintomaObservado... observados) {
        KieSession sessao = base.novaSessao();
        try {
            List<PropostaClassificacao> coletadas = new ArrayList<>();
            sessao.setGlobal("propostas", coletadas);
            sessao.insert(fato);
            for (SintomaObservado observado : observados) {
                sessao.insert(observado);
            }
            sessao.fireAllRules();
            return coletadas;
        } finally {
            sessao.dispose();
        }
    }

    private Set<String> propostas(BaseConhecimento base, FatoTriagem fato, SintomaObservado... observados) {
        return disparar(base, fato, observados).stream()
                .map(PropostaClassificacao::getClassificacaoCodigo)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("compila os artefatos DRL e DMN entregues no classpath")
    void compilaArtefatosDoClasspath() {
        EstadoBaseConhecimento estado = baseCarregada().estado();

        assertThat(estado.versao()).isEqualTo(1L);
        assertThat(estado.regrasDrl()).contains("Base - gravidade cadastrada do sintoma");
        assertThat(estado.regrasDrl()).contains("Convulsoes - risco imediato de vida");
        assertThat(estado.modelosDmn()).contains("TriagemConvulsoes");
    }

    @Test
    @DisplayName("regra base propaga a gravidade cadastrada, sem conhecer o sintoma")
    void regraBasePropagaGravidadeCadastrada() {
        BaseConhecimento base = baseCarregada();
        FatoTriagem fato = FatoTriagem.novo()
                .grupo("ADULTOS", "Adultos")
                .fluxograma("QUEIXA_NOVA", "Queixa cadastrada hoje")
                .sintoma("SINTOMA_INVENTADO", "Sintoma inventado pela unidade")
                .construir();

        Set<String> codigos = propostas(base, fato,
                new SintomaObservado(1L, "SINTOMA_INVENTADO", "Sintoma inventado pela unidade", "AMARELO"));

        assertThat(codigos).containsExactly("AMARELO");
    }

    @Test
    @DisplayName("DRL especializado eleva a gravidade em combinacoes que a tabela plana nao expressa")
    void drlEspecializadoDetectaCombinacoes() {
        BaseConhecimento base = baseCarregada();

        // Isolados, os dois sintomas sao LARANJA no cadastro. Juntos, indicam estado de mal epileptico.
        FatoTriagem fato = FatoTriagem.novo()
                .grupo("ADULTOS", "Adultos")
                .fluxograma("CONVULSOES", "Convulsoes")
                .sintoma("CONVULSAO_EM_CURSO", "Convulsionando no momento")
                .sintoma("CONVULSAO_RECENTE", "Crise nas ultimas 24h")
                .construir();

        Set<String> codigos = propostas(base, fato,
                new SintomaObservado(1L, "CONVULSAO_EM_CURSO", "Convulsionando no momento", "LARANJA"),
                new SintomaObservado(2L, "CONVULSAO_RECENTE", "Crise nas ultimas 24h", "LARANJA"));

        assertThat(codigos).contains("VERMELHO", "LARANJA");
    }

    @Test
    @DisplayName("gestante com crise convulsiva vira VERMELHO por suspeita de eclampsia")
    void gestanteComConvulsaoEhVermelho() {
        BaseConhecimento base = baseCarregada();
        FatoTriagem fato = FatoTriagem.novo()
                .grupo("ADULTOS", "Adultos")
                .fluxograma("CONVULSOES", "Convulsoes")
                .sintoma("CONVULSAO_RECENTE", "Crise nas ultimas 24h")
                .gestante(true)
                .construir();

        Set<String> codigos = propostas(base, fato,
                new SintomaObservado(1L, "CONVULSAO_RECENTE", "Crise nas ultimas 24h", "LARANJA"));

        assertThat(codigos).contains("VERMELHO");
    }

    @Test
    @DisplayName("regras de um fluxograma nao vazam para outro")
    void regrasNaoVazamEntreFluxogramas() {
        BaseConhecimento base = baseCarregada();
        FatoTriagem fato = FatoTriagem.novo()
                .grupo("ADULTOS", "Adultos")
                .fluxograma("CEFALEIA", "Cefaleia")
                .sintoma("CONVULSAO_EM_CURSO", "Convulsionando no momento")
                .construir();

        Set<String> codigos = propostas(base, fato,
                new SintomaObservado(1L, "CONVULSAO_EM_CURSO", "Convulsionando no momento", "VERDE"));

        assertThat(codigos).containsExactly("VERDE");
    }

    @Test
    @DisplayName("tabela DMN de Convulsoes devolve o discriminador mais grave pela politica FIRST")
    void tabelaDmnAvaliaDiscriminadores() {
        BaseConhecimento base = baseCarregada();
        DMNModel modelo = base.modeloDmn("TriagemConvulsoes").orElseThrow();

        assertThat(avaliarDmn(base, modelo, List.of("CONVULSAO_EM_CURSO"))).isEqualTo("VERMELHO");
        assertThat(avaliarDmn(base, modelo, List.of("HIPOGLICEMIA"))).isEqualTo("LARANJA");
        assertThat(avaliarDmn(base, modelo, List.of("CEFALEIA_INTENSA"))).isEqualTo("AMARELO");
        assertThat(avaliarDmn(base, modelo, List.of("PROBLEMA_RECENTE"))).isEqualTo("VERDE");
        // FIRST le a tabela de cima para baixo: o discriminador mais grave vence.
        assertThat(avaliarDmn(base, modelo, List.of("PROBLEMA_RECENTE", "CHOQUE"))).isEqualTo("VERMELHO");
        // Sem discriminador reconhecido, a tabela nao opina.
        assertThat(avaliarDmn(base, modelo, List.of("SINTOMA_DESCONHECIDO"))).isNull();
    }

    private String avaliarDmn(BaseConhecimento base, DMNModel modelo, List<String> sintomas) {
        DMNContext contexto = base.dmnRuntime().newContext();
        contexto.set("Sintomas", sintomas);
        DMNResult resultado = base.dmnRuntime().evaluateAll(modelo, contexto);
        assertThat(resultado.hasErrors())
                .as("mensagens do DMN: %s", resultado.getMessages())
                .isFalse();
        return (String) resultado.getContext().get("Classificacao");
    }

    @Test
    @DisplayName("regra invalida e rejeitada e a base anterior continua no ar")
    void regraInvalidaNaoDerrubaBaseAtual() {
        BaseConhecimento base = baseCarregada();
        long versaoOriginal = base.estado().versao();

        List<ArtefatoRegra> comLixo = new java.util.ArrayList<>(regrasDoClasspath.carregar());
        comLixo.add(ArtefatoRegra.doBanco("quebrada.drl", TipoRegra.DRL, "rule \"sem when nem then\" isso nao compila"));

        assertThatThrownBy(() -> base.recarregar(comLixo))
                .isInstanceOf(ErroCompilacaoRegrasException.class);

        assertThat(base.estado().versao()).isEqualTo(versaoOriginal);
        assertThat(propostas(base,
                FatoTriagem.novo().grupo("ADULTOS", "Adultos").fluxograma("CONVULSOES", "Convulsoes")
                        .sintoma("CHOQUE", "Choque").construir(),
                new SintomaObservado(1L, "CHOQUE", "Choque", "VERMELHO")))
                .contains("VERMELHO");
    }

    @Test
    @DisplayName("regra nova vinda do banco entra em vigor sem recompilar a aplicacao")
    void regraDoBancoEntraEmVigorAQuente() {
        BaseConhecimento base = baseCarregada();

        FatoTriagem fato = FatoTriagem.novo()
                .grupo("ADULTOS", "Adultos")
                .fluxograma("CEFALEIA", "Cefaleia")
                .sintoma("DOR_MODERADA", "Dor moderada")
                .idadeAnos(80)
                .construir();
        SintomaObservado observado = new SintomaObservado(1L, "DOR_MODERADA", "Dor moderada", "VERDE");

        assertThat(propostas(base, fato, observado)).containsExactly("VERDE");

        List<ArtefatoRegra> comNova = new java.util.ArrayList<>(regrasDoClasspath.carregar());
        comNova.add(ArtefatoRegra.doBanco("idoso-cefaleia.drl", TipoRegra.DRL, """
                package br.com.sitpa.rules;

                import br.com.sitpa.rules.model.FatoTriagem;
                import br.com.sitpa.rules.model.PropostaClassificacao;

                global java.util.List propostas;

                rule "Cefaleia - idoso com dor"
                    when
                        $f : FatoTriagem( fluxogramaCodigo == "CEFALEIA", idadeMaiorOuIgualA(75) == true,
                                          temSintoma("DOR_MODERADA") == true )
                    then
                        propostas.add(new PropostaClassificacao("AMARELO", drools.getRule().getName(),
                                "Cefaleia em maior de 75 anos exige avaliacao antecipada."));
                end
                """));

        EstadoBaseConhecimento estado = base.recarregar(comNova);

        assertThat(estado.versao()).isEqualTo(2L);
        assertThat(propostas(base, fato, observado)).containsExactlyInAnyOrder("VERDE", "AMARELO");
    }

    @Test
    @DisplayName("proposta especializada declara origem DRL e a base declara origem BASE")
    void origemDaPropostaEhRastreavel() {
        BaseConhecimento base = baseCarregada();
        List<PropostaClassificacao> resultado = disparar(base,
                FatoTriagem.novo()
                        .grupo("ADULTOS", "Adultos")
                        .fluxograma("CONVULSOES", "Convulsoes")
                        .sintoma("CHOQUE", "Choque")
                        .construir(),
                new SintomaObservado(1L, "CHOQUE", "Choque", "VERMELHO"));

        assertThat(resultado).anyMatch(p -> p.getOrigem() == OrigemDecisao.BASE);
        assertThat(resultado).anyMatch(p -> p.getOrigem() == OrigemDecisao.DRL
                && p.getRegra().equals("Convulsoes - risco imediato de vida"));
    }

    @Test
    @DisplayName("a salience define a ordem de disparo, e com ela o desempate entre regras")
    void salienceDefineAOrdemDeDisparo() {
        BaseConhecimento base = baseCarregada();

        // "crise febril em lactente" (salience 110) e "comprometimento neurologico agudo"
        // (salience 90) propoem ambas LARANJA; a de maior salience deve vir antes na coleta,
        // e por isso e ela que fica com o credito pela classificacao.
        List<String> regras = disparar(base,
                FatoTriagem.novo()
                        .grupo("CRIANCAS", "Criancas")
                        .fluxograma("CONVULSOES", "Convulsoes")
                        .sintoma("CONVULSAO_RECENTE", "Crise nas ultimas 24h")
                        .sintoma("FEBRE_ALTA", "Febre alta")
                        .idadeAnos(1)
                        .construir(),
                new SintomaObservado(1L, "CONVULSAO_RECENTE", "Crise nas ultimas 24h", "LARANJA"),
                new SintomaObservado(2L, "FEBRE_ALTA", "Febre alta", "AMARELO"))
                .stream().map(PropostaClassificacao::getRegra).toList();

        assertThat(regras.indexOf("Convulsoes - crise febril em lactente"))
                .isLessThan(regras.indexOf("Convulsoes - comprometimento neurologico agudo"));
    }
}
