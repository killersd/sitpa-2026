package br.com.sitpa.service;

import br.com.sitpa.domain.*;
import br.com.sitpa.dto.*;
import br.com.sitpa.exception.RequisicaoInvalidaException;
import br.com.sitpa.repository.ClassificacaoRealizadaRepository;
import br.com.sitpa.repository.ClassificacaoRepository;
import br.com.sitpa.repository.FluxogramaSintomaRepository;
import br.com.sitpa.rules.BaseConhecimento;
import br.com.sitpa.rules.model.FatoTriagem;
import br.com.sitpa.rules.model.PropostaClassificacao;
import br.com.sitpa.rules.model.SintomaObservado;
import org.kie.api.runtime.KieSession;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Fluxo de classificacao de paciente: grupo, fluxograma, sintomas, inferencia e resultado.
 *
 * <p>A avaliacao e a confirmacao usam exatamente o mesmo caminho de inferencia. A tela de revisao
 * chama {@link #avaliar}, que nao grava nada; ao confirmar, {@link #registrar} refaz a inferencia
 * e persiste. Reinferir em vez de confiar no resultado enviado pelo cliente evita que um payload
 * adulterado grave no historico uma classificacao que o motor nunca produziu.</p>
 */
@Service
@Transactional(readOnly = true)
public class TriagemService {

    private static final Logger log = LoggerFactory.getLogger(TriagemService.class);

    /**
     * Desempate entre propostas de mesma gravidade: uma regra especializada explica melhor a
     * decisao do que a propagacao generica do cadastro, entao vem primeiro.
     */
    private static final Map<OrigemDecisao, Integer> PESO_ORIGEM = Map.of(
            OrigemDecisao.DRL, 0,
            OrigemDecisao.DMN, 1,
            OrigemDecisao.BASE, 2,
            OrigemDecisao.NENHUMA, 3);

    private final BaseConhecimento baseConhecimento;
    private final ClassificacaoRepository classificacaoRepository;
    private final FluxogramaSintomaRepository associacaoRepository;
    private final ClassificacaoRealizadaRepository historicoRepository;
    private final FluxogramaService fluxogramaService;
    private final GrupoService grupoService;

    public TriagemService(BaseConhecimento baseConhecimento,
                          ClassificacaoRepository classificacaoRepository,
                          FluxogramaSintomaRepository associacaoRepository,
                          ClassificacaoRealizadaRepository historicoRepository,
                          FluxogramaService fluxogramaService,
                          GrupoService grupoService) {
        this.baseConhecimento = baseConhecimento;
        this.classificacaoRepository = classificacaoRepository;
        this.associacaoRepository = associacaoRepository;
        this.historicoRepository = historicoRepository;
        this.fluxogramaService = fluxogramaService;
        this.grupoService = grupoService;
    }

    /** Infere a classificacao sem gravar nada. Alimenta a tela de revisao. */
    public ResultadoTriagemResponse avaliar(TriagemRequest request) {
        return inferir(request).resposta();
    }

    /** Reinfere e grava o resultado no historico. */
    @Transactional
    public ClassificacaoRealizadaResponse registrar(TriagemRequest request, String usuarioLogin) {
        Inferencia inferencia = inferir(request);
        ResultadoTriagemResponse resultado = inferencia.resposta();
        ClassificacaoResponse vencedora = resultado.classificacao();

        ClassificacaoRealizada registro = new ClassificacaoRealizada();
        registro.setGrupoId(inferencia.grupo().getId());
        registro.setFluxogramaId(inferencia.fluxograma().getId());
        registro.setClassificacaoId(vencedora.id());
        registro.setGrupoNome(inferencia.grupo().getNome());
        registro.setFluxogramaNome(inferencia.fluxograma().getNome());
        registro.setClassificacaoCodigo(vencedora.codigo());
        registro.setClassificacaoNome(vencedora.nome());
        registro.setClassificacaoCor(vencedora.cor());
        registro.setClassificacaoPrioridade(vencedora.prioridade());
        registro.setTempoMaximoEsperaMinutos(vencedora.tempoMaximoEsperaMinutos());
        registro.setLocalAtendimento(vencedora.localAtendimento());
        registro.setTipoAtendimento(vencedora.tipoAtendimento());
        registro.setSintomas(resultado.sintomasSelecionados().stream().map(ReferenciaResponse::nome).toList());
        registro.setOrigemDecisao(resultado.origemDecisao());
        registro.setRegraAplicada(resultado.regraAplicada());
        registro.setJustificativa(resultado.justificativa());
        registro.setIdentificadorPaciente(request.identificadorPaciente());
        registro.setIdadeAnos(request.idadeAnos());
        registro.setObservacoes(request.observacoes());
        registro.setUsuarioLogin(usuarioLogin);

        ClassificacaoRealizada salvo = historicoRepository.save(registro);

        log.info("Triagem registrada: id={} fluxograma={} classificacao={} origem={} regra={} usuario={}",
                salvo.getId(), inferencia.fluxograma().getCodigo(), vencedora.codigo(),
                resultado.origemDecisao(), resultado.regraAplicada(), usuarioLogin);

        return Mapeadores.paraResponse(salvo);
    }

    // --- Inferencia ---

    private Inferencia inferir(TriagemRequest request) {
        if (!baseConhecimento.estaCarregada()) {
            throw new IllegalStateException("Base de conhecimento indisponivel; verifique as regras cadastradas.");
        }

        Grupo grupo = grupoService.exigir(request.grupoId());
        Fluxograma fluxograma = fluxogramaService.exigir(request.fluxogramaId());
        if (!Objects.equals(fluxograma.getGrupo().getId(), grupo.getId())) {
            throw new RequisicaoInvalidaException("O fluxograma \"" + fluxograma.getNome()
                    + "\" nao pertence ao grupo \"" + grupo.getNome() + "\".");
        }

        Map<Long, FluxogramaSintoma> associadosPorSintomaId = new LinkedHashMap<>();
        associacaoRepository.findByFluxogramaIdOrderByOrdemAscIdAsc(fluxograma.getId())
                .forEach(fs -> associadosPorSintomaId.put(fs.getSintoma().getId(), fs));

        Set<Long> solicitados = request.sintomasIds() == null ? Set.of() : request.sintomasIds();
        List<Long> desconhecidos = solicitados.stream()
                .filter(id -> !associadosPorSintomaId.containsKey(id))
                .toList();
        if (!desconhecidos.isEmpty()) {
            throw new RequisicaoInvalidaException("Os sintomas de id " + desconhecidos
                    + " nao estao associados ao fluxograma \"" + fluxograma.getNome() + "\".");
        }

        List<FluxogramaSintoma> selecionados = associadosPorSintomaId.values().stream()
                .filter(fs -> solicitados.contains(fs.getSintoma().getId()))
                .toList();

        FatoTriagem.Construtor construtor = FatoTriagem.novo()
                .grupo(grupo.getCodigo(), grupo.getNome())
                .fluxograma(fluxograma.getCodigo(), fluxograma.getNome())
                .idadeAnos(request.idadeAnos())
                .gestante(request.isGestante());
        selecionados.forEach(fs -> construtor.sintoma(fs.getSintoma().getCodigo(), fs.getSintoma().getNome()));
        FatoTriagem fato = construtor.construir();

        List<PropostaClassificacao> propostas = new ArrayList<>(executarDrl(fato, selecionados));
        executarDmn(fluxograma, fato).ifPresent(propostas::add);

        ResultadoTriagemResponse resposta = arbitrar(grupo, fluxograma, selecionados, propostas);
        return new Inferencia(grupo, fluxograma, resposta);
    }

    private List<PropostaClassificacao> executarDrl(FatoTriagem fato, List<FluxogramaSintoma> selecionados) {
        KieSession sessao = baseConhecimento.novaSessao();
        try {
            // A lista e preenchida pelas regras na ordem em que disparam, que o autor do
            // protocolo controla via salience. Essa ordem e o criterio de desempate final.
            List<PropostaClassificacao> coletadas = new ArrayList<>();
            sessao.setGlobal("propostas", coletadas);

            sessao.insert(fato);
            for (FluxogramaSintoma fs : selecionados) {
                Classificacao efetiva = fs.classificacaoEfetiva();
                sessao.insert(new SintomaObservado(fs.getSintoma().getId(), fs.getSintoma().getCodigo(),
                        fs.getSintoma().getNome(), efetiva == null ? null : efetiva.getCodigo()));
            }
            sessao.fireAllRules();
            return coletadas;
        } finally {
            sessao.dispose();
        }
    }

    private Optional<PropostaClassificacao> executarDmn(Fluxograma fluxograma, FatoTriagem fato) {
        String nomeModelo = fluxograma.getModeloDmn();
        if (nomeModelo == null || nomeModelo.isBlank()) {
            return Optional.empty();
        }
        Optional<DMNModel> modelo = baseConhecimento.modeloDmn(nomeModelo);
        if (modelo.isEmpty()) {
            log.warn("Fluxograma {} aponta para o modelo DMN \"{}\", que nao esta na base de conhecimento.",
                    fluxograma.getCodigo(), nomeModelo);
            return Optional.empty();
        }

        DMNContext contexto = baseConhecimento.dmnRuntime().newContext();
        contexto.set("Sintomas", new ArrayList<>(fato.getSintomas()));
        contexto.set("Fluxograma", fluxograma.getCodigo());

        DMNResult resultado = baseConhecimento.dmnRuntime().evaluateAll(modelo.get(), contexto);
        if (resultado.hasErrors()) {
            log.warn("Modelo DMN \"{}\" retornou erros: {}", nomeModelo, resultado.getMessages());
            return Optional.empty();
        }

        Object saida = resultado.getContext().get("Classificacao");
        if (!(saida instanceof String codigo) || codigo.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new PropostaClassificacao(codigo, "DMN: " + nomeModelo,
                "Tabela de decisao \"" + nomeModelo + "\" indicou " + codigo + ".", OrigemDecisao.DMN));
    }

    /**
     * Elege a proposta mais grave. A gravidade vem da prioridade cadastrada, nunca da regra:
     * e o cadastro que define a escala, o que permite a unidade reordena-la sem tocar em DRL.
     */
    private ResultadoTriagemResponse arbitrar(Grupo grupo,
                                              Fluxograma fluxograma,
                                              List<FluxogramaSintoma> selecionados,
                                              List<PropostaClassificacao> propostas) {

        Map<String, Classificacao> porCodigo = new HashMap<>();
        List<PropostaAvaliada> avaliadas = new ArrayList<>();
        for (PropostaClassificacao proposta : propostas) {
            Classificacao classificacao = porCodigo.computeIfAbsent(proposta.getClassificacaoCodigo(),
                    codigo -> classificacaoRepository.findByCodigo(codigo).orElse(null));
            if (classificacao == null) {
                log.warn("Regra \"{}\" propos a classificacao \"{}\", que nao existe no cadastro; proposta ignorada.",
                        proposta.getRegra(), proposta.getClassificacaoCodigo());
                continue;
            }
            avaliadas.add(new PropostaAvaliada(proposta, classificacao));
        }

        // List.sort e estavel, entao propostas empatadas em gravidade e origem mantem a ordem
        // de disparo definida pela salience das regras. Nenhum criterio arbitrario decide
        // qual regra leva o credito pela classificacao.
        avaliadas.sort(Comparator
                .comparingInt((PropostaAvaliada p) -> p.classificacao().getPrioridade())
                .thenComparingInt(p -> PESO_ORIGEM.getOrDefault(p.proposta().getOrigem(), 9)));

        PropostaAvaliada vencedora = avaliadas.isEmpty() ? null : avaliadas.getFirst();

        Classificacao classificacaoFinal;
        OrigemDecisao origem;
        String regraAplicada;
        String justificativa;

        if (vencedora != null) {
            classificacaoFinal = vencedora.classificacao();
            origem = vencedora.proposta().getOrigem();
            regraAplicada = vencedora.proposta().getRegra();
            justificativa = vencedora.proposta().getJustificativa();
        } else {
            // Nenhuma regra opinou: cai no nivel menos grave ativo, deixando explicito na origem
            // que a decisao nao veio do motor e merece conferencia do profissional.
            classificacaoFinal = classificacaoRepository.findFirstByAtivoTrueOrderByPrioridadeDesc()
                    .orElseThrow(() -> new IllegalStateException(
                            "Nenhuma classificacao ativa cadastrada; o protocolo precisa de ao menos um nivel de risco."));
            origem = OrigemDecisao.NENHUMA;
            regraAplicada = null;
            justificativa = selecionados.isEmpty()
                    ? "Nenhum sintoma foi marcado. Classificacao provisoria no menor nivel de risco; revise antes de confirmar."
                    : "Nenhuma regra do protocolo se aplicou aos sintomas marcados. Revise antes de confirmar.";
        }

        List<PropostaResponse> detalhamento = avaliadas.stream()
                .map(p -> new PropostaResponse(
                        p.classificacao().getCodigo(),
                        p.classificacao().getNome(),
                        p.classificacao().getCor(),
                        p.classificacao().getPrioridade(),
                        p.proposta().getOrigem(),
                        p.proposta().getRegra(),
                        p.proposta().getJustificativa(),
                        p == vencedora))
                .toList();

        List<ReferenciaResponse> sintomas = selecionados.stream()
                .map(fs -> Mapeadores.referencia(fs.getSintoma()))
                .toList();

        return new ResultadoTriagemResponse(
                Mapeadores.referencia(grupo),
                Mapeadores.referencia(fluxograma),
                sintomas,
                Mapeadores.paraResponse(classificacaoFinal),
                origem,
                regraAplicada,
                justificativa,
                detalhamento,
                baseConhecimento.estado().versao());
    }

    private record PropostaAvaliada(PropostaClassificacao proposta, Classificacao classificacao) {
    }

    private record Inferencia(Grupo grupo, Fluxograma fluxograma, ResultadoTriagemResponse resposta) {
    }
}
