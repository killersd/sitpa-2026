package br.com.sitpa.service;

import br.com.sitpa.domain.RegraProtocolo;
import br.com.sitpa.dto.RegraProtocoloRequest;
import br.com.sitpa.dto.RegraProtocoloResponse;
import br.com.sitpa.exception.ConflitoException;
import br.com.sitpa.exception.RecursoNaoEncontradoException;
import br.com.sitpa.repository.RegraProtocoloRepository;
import br.com.sitpa.rules.ArtefatoRegra;
import br.com.sitpa.rules.BaseConhecimento;
import br.com.sitpa.rules.EstadoBaseConhecimento;
import br.com.sitpa.rules.RegrasDoClasspath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Monta a base de conhecimento e mantem o cadastro de regras editaveis.
 *
 * <p>A base e a soma de duas fontes: os artefatos entregues no build
 * (<code>resources/rules</code>) e as regras cadastradas no banco. Qualquer alteracao no cadastro
 * dispara uma recompilacao completa <b>dentro da transacao</b>: se o DRL nao compilar, a excecao
 * desfaz a gravacao e o banco nunca fica com uma regra que o motor nao aceita.</p>
 */
@Service
public class RegrasService {

    private static final Logger log = LoggerFactory.getLogger(RegrasService.class);

    private final RegraProtocoloRepository repository;
    private final RegrasDoClasspath regrasDoClasspath;
    private final BaseConhecimento baseConhecimento;

    /** Assinatura das regras de banco ja publicadas, usada pela reconciliacao periodica. */
    private final AtomicReference<String> assinaturaPublicada = new AtomicReference<>("");

    public RegrasService(RegraProtocoloRepository repository,
                         RegrasDoClasspath regrasDoClasspath,
                         BaseConhecimento baseConhecimento) {
        this.repository = repository;
        this.regrasDoClasspath = regrasDoClasspath;
        this.baseConhecimento = baseConhecimento;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void carregarNaSubida() {
        try {
            recarregar();
        } catch (RuntimeException e) {
            // Uma regra invalida no banco nao pode impedir a aplicacao de subir: sem base, o
            // /triagens responde 503 e o administrador corrige a regra pela propria API.
            log.error("Nao foi possivel carregar a base de conhecimento na inicializacao.", e);
        }
    }

    /**
     * Reconciliacao periodica: detecta regras alteradas por outra instancia (ou direto no banco)
     * e republica a base. Em uma unica instancia, a recarga ja acontece na propria escrita e este
     * job apenas confirma que nada mudou.
     */
    @Scheduled(fixedDelayString = "${sitpa.regras.intervalo-reconciliacao-ms:60000}",
            initialDelayString = "${sitpa.regras.intervalo-reconciliacao-ms:60000}")
    @Transactional(readOnly = true)
    public void reconciliar() {
        try {
            List<RegraProtocolo> ativas = repository.findByAtivoTrueOrderByNomeAsc();
            String assinatura = assinatura(ativas);
            if (assinatura.equals(assinaturaPublicada.get()) && baseConhecimento.estaCarregada()) {
                return;
            }
            log.info("Regras do banco divergem da base publicada; recarregando.");
            publicar(ativas);
        } catch (RuntimeException e) {
            log.error("Reconciliacao da base de conhecimento falhou; base atual mantida.", e);
        }
    }

    @Transactional(readOnly = true)
    public EstadoBaseConhecimento recarregar() {
        return publicar(repository.findByAtivoTrueOrderByNomeAsc());
    }

    public EstadoBaseConhecimento estado() {
        return baseConhecimento.estado();
    }

    // --- CRUD das regras editaveis ---

    @Transactional(readOnly = true)
    public List<RegraProtocoloResponse> listar() {
        return repository.findAllByOrderByNomeAsc().stream().map(Mapeadores::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public RegraProtocoloResponse buscar(Long id) {
        return Mapeadores.paraResponse(exigir(id));
    }

    @Transactional
    public RegraProtocoloResponse criar(RegraProtocoloRequest request) {
        if (repository.existsByNome(request.nome())) {
            throw new ConflitoException("Ja existe uma regra com o nome " + request.nome() + ".");
        }
        RegraProtocolo r = new RegraProtocolo();
        aplicar(request, r);
        RegraProtocolo salva = repository.saveAndFlush(r);
        recarregar();
        return Mapeadores.paraResponse(salva);
    }

    @Transactional
    public RegraProtocoloResponse atualizar(Long id, RegraProtocoloRequest request) {
        RegraProtocolo r = exigir(id);
        if (repository.existsByNomeAndIdNot(request.nome(), id)) {
            throw new ConflitoException("Ja existe outra regra com o nome " + request.nome() + ".");
        }
        aplicar(request, r);
        RegraProtocolo salva = repository.saveAndFlush(r);
        recarregar();
        return Mapeadores.paraResponse(salva);
    }

    @Transactional
    public void excluir(Long id) {
        RegraProtocolo r = exigir(id);
        repository.delete(r);
        repository.flush();
        recarregar();
    }

    private RegraProtocolo exigir(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Regra", id));
    }

    private void aplicar(RegraProtocoloRequest request, RegraProtocolo r) {
        r.setNome(request.nome());
        r.setDescricao(request.descricao());
        r.setTipo(request.tipo());
        r.setConteudo(request.conteudo());
        r.setAtivo(request.ativo() == null || request.ativo());
    }

    private EstadoBaseConhecimento publicar(List<RegraProtocolo> regrasAtivas) {
        List<ArtefatoRegra> artefatos = new ArrayList<>(regrasDoClasspath.carregar());
        regrasAtivas.forEach(r -> artefatos.add(ArtefatoRegra.doBanco(r.getNome(), r.getTipo(), r.getConteudo())));

        EstadoBaseConhecimento estado = baseConhecimento.recarregar(artefatos);
        assinaturaPublicada.set(assinatura(regrasAtivas));
        return estado;
    }

    private String assinatura(List<RegraProtocolo> regras) {
        StringBuilder sb = new StringBuilder();
        regras.forEach(r -> sb.append(r.getId()).append(':').append(r.getVersao()).append(';'));
        return sb.toString();
    }
}
