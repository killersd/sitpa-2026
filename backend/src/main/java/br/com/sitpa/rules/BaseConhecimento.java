package br.com.sitpa.rules;

import br.com.sitpa.domain.TipoRegra;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieRuntimeFactory;
import org.kie.api.runtime.KieSession;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Guarda a base de conhecimento ativa e permite troca-la em tempo de execucao.
 *
 * <p>Como o requisito e alterar regras <b>sem recompilar a aplicacao</b>, o DRL e o DMN nunca
 * sao compilados pelo Maven: sao texto que o Drools compila em memoria via {@code KieBuilder}.
 * A recarga monta um {@link KieContainer} inteiramente novo e so entao o publica em uma
 * referencia atomica &mdash; triagens em andamento terminam na base antiga e as seguintes ja usam
 * a nova, sem janela de indisponibilidade. Se a compilacao falhar, a base anterior continua
 * ativa e o erro sobe para quem tentou publicar.</p>
 */
@Component
public class BaseConhecimento {

    private static final Logger log = LoggerFactory.getLogger(BaseConhecimento.class);

    private static final String GROUP_ID = "br.com.sitpa";
    private static final String ARTIFACT_ID = "sitpa-regras";

    private final AtomicReference<Publicacao> publicada = new AtomicReference<>();
    private final AtomicLong contadorVersao = new AtomicLong();

    /**
     * Compila os artefatos e publica a base resultante.
     *
     * @throws ErroCompilacaoRegrasException se algum artefato nao compilar; a base atual e mantida
     */
    public synchronized EstadoBaseConhecimento recarregar(List<ArtefatoRegra> artefatos) {
        long versao = contadorVersao.get() + 1;
        KieServices ks = KieServices.get();
        ClassLoader classLoader = BaseConhecimento.class.getClassLoader();

        KieModuleModel moduleModel = ks.newKieModuleModel();
        KieBaseModel kieBaseModel = moduleModel.newKieBaseModel("sitpa-kbase")
                .setDefault(true)
                .setEqualsBehavior(EqualityBehaviorOption.EQUALITY);
        kieBaseModel.newKieSessionModel("sitpa-ksession").setDefault(true);

        ReleaseId releaseId = ks.newReleaseId(GROUP_ID, ARTIFACT_ID, "1.0." + versao);
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.writeKModuleXML(moduleModel.toXML());
        kfs.generateAndWritePomXML(releaseId);
        for (ArtefatoRegra artefato : artefatos) {
            kfs.write(artefato.caminhoNoKieFileSystem(), artefato.conteudo());
        }

        KieBuilder kieBuilder = ks.newKieBuilder(kfs, classLoader).buildAll();
        List<Message> erros = kieBuilder.getResults().getMessages(Message.Level.ERROR);
        if (!erros.isEmpty()) {
            List<String> problemas = erros.stream()
                    .map(m -> m.getPath() + ":" + m.getLine() + " " + m.getText())
                    .toList();
            log.error("Recarga da base de conhecimento rejeitada; base versao {} mantida no ar. Problemas: {}",
                    contadorVersao.get(), problemas);
            throw new ErroCompilacaoRegrasException(problemas);
        }
        kieBuilder.getResults().getMessages(Message.Level.WARNING)
                .forEach(m -> log.warn("Aviso do compilador de regras em {}:{} - {}", m.getPath(), m.getLine(), m.getText()));

        KieContainer container = ks.newKieContainer(releaseId, classLoader);
        DMNRuntime dmnRuntime = KieRuntimeFactory.of(container.getKieBase()).get(DMNRuntime.class);

        Publicacao nova = new Publicacao(versao, Instant.now(), container, dmnRuntime, List.copyOf(artefatos));
        Publicacao anterior = publicada.getAndSet(nova);
        contadorVersao.set(versao);

        if (anterior != null) {
            // O container antigo so e descartado depois que o novo esta publicado.
            try {
                anterior.container().dispose();
            } catch (RuntimeException e) {
                log.warn("Falha ao liberar a base de conhecimento anterior (versao {}).", anterior.versao(), e);
            }
        }

        EstadoBaseConhecimento estado = nova.estado();
        log.info("Base de conhecimento versao {} publicada: {} regra(s) DRL, {} modelo(s) DMN, {} artefato(s).",
                estado.versao(), estado.regrasDrl().size(), estado.modelosDmn().size(), estado.artefatos().size());
        return estado;
    }

    /** Nova sessao de inferencia sobre a base publicada. O chamador e responsavel por {@code dispose()}. */
    public KieSession novaSessao() {
        return exigirPublicada().container().newKieSession();
    }

    /** Modelo DMN pelo nome declarado no arquivo, se existir na base publicada. */
    public Optional<DMNModel> modeloDmn(String nome) {
        if (nome == null || nome.isBlank()) {
            return Optional.empty();
        }
        DMNRuntime runtime = exigirPublicada().dmnRuntime();
        return runtime.getModels().stream()
                .filter(m -> nome.equals(m.getName()))
                .findFirst();
    }

    public DMNRuntime dmnRuntime() {
        return exigirPublicada().dmnRuntime();
    }

    public boolean estaCarregada() {
        return publicada.get() != null;
    }

    public EstadoBaseConhecimento estado() {
        return exigirPublicada().estado();
    }

    private Publicacao exigirPublicada() {
        Publicacao atual = publicada.get();
        if (atual == null) {
            throw new IllegalStateException("Base de conhecimento ainda nao foi carregada.");
        }
        return atual;
    }

    private record Publicacao(long versao,
                              Instant carregadaEm,
                              KieContainer container,
                              DMNRuntime dmnRuntime,
                              List<ArtefatoRegra> artefatos) {

        EstadoBaseConhecimento estado() {
            List<String> regras = new ArrayList<>();
            container.getKieBase().getKiePackages()
                    .forEach(pacote -> pacote.getRules().forEach(regra -> regras.add(regra.getName())));
            regras.sort(Comparator.naturalOrder());

            List<String> modelos = dmnRuntime.getModels().stream()
                    .map(DMNModel::getName)
                    .sorted()
                    .toList();

            List<String> descricaoArtefatos = artefatos.stream()
                    .map(a -> a.nome() + " [" + a.tipo() + ", " + a.origem() + "]")
                    .toList();

            return new EstadoBaseConhecimento(versao, carregadaEm, regras, modelos, descricaoArtefatos);
        }
    }

    /** Conveniencia para testes e para o seeder: monta um artefato a partir de texto solto. */
    public static ArtefatoRegra artefato(String nome, TipoRegra tipo, String conteudo) {
        return ArtefatoRegra.doClasspath(nome, tipo, conteudo);
    }
}
