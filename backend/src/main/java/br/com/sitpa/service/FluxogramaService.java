package br.com.sitpa.service;

import br.com.sitpa.domain.Classificacao;
import br.com.sitpa.domain.Fluxograma;
import br.com.sitpa.domain.FluxogramaSintoma;
import br.com.sitpa.domain.Sintoma;
import br.com.sitpa.dto.AssociacaoSintomaRequest;
import br.com.sitpa.dto.FluxogramaRequest;
import br.com.sitpa.dto.FluxogramaResponse;
import br.com.sitpa.dto.SintomaAssociadoResponse;
import br.com.sitpa.exception.ConflitoException;
import br.com.sitpa.exception.RecursoNaoEncontradoException;
import br.com.sitpa.repository.FluxogramaRepository;
import br.com.sitpa.repository.FluxogramaSintomaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD dos fluxogramas e gestao da associacao com sintomas. */
@Service
@Transactional(readOnly = true)
public class FluxogramaService {

    private final FluxogramaRepository repository;
    private final FluxogramaSintomaRepository associacaoRepository;
    private final GrupoService grupoService;
    private final SintomaService sintomaService;
    private final ClassificacaoService classificacaoService;

    public FluxogramaService(FluxogramaRepository repository,
                             FluxogramaSintomaRepository associacaoRepository,
                             GrupoService grupoService,
                             SintomaService sintomaService,
                             ClassificacaoService classificacaoService) {
        this.repository = repository;
        this.associacaoRepository = associacaoRepository;
        this.grupoService = grupoService;
        this.sintomaService = sintomaService;
        this.classificacaoService = classificacaoService;
    }

    public List<FluxogramaResponse> listar(Long grupoId, boolean apenasAtivos) {
        List<Fluxograma> fluxogramas;
        if (grupoId != null) {
            fluxogramas = apenasAtivos
                    ? repository.findByGrupoIdAndAtivoTrueOrderByNomeAsc(grupoId)
                    : repository.findByGrupoIdOrderByNomeAsc(grupoId);
        } else {
            fluxogramas = repository.findAllByOrderByNomeAsc();
            if (apenasAtivos) {
                fluxogramas = fluxogramas.stream().filter(Fluxograma::isAtivo).toList();
            }
        }
        return fluxogramas.stream().map(this::comContagem).toList();
    }

    public FluxogramaResponse buscar(Long id) {
        return comContagem(exigir(id));
    }

    public Fluxograma exigir(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Fluxograma", id));
    }

    @Transactional
    public FluxogramaResponse criar(FluxogramaRequest request) {
        if (repository.existsByCodigo(request.codigo())) {
            throw new ConflitoException("Ja existe um fluxograma com o codigo " + request.codigo() + ".");
        }
        Fluxograma f = new Fluxograma();
        aplicar(request, f);
        return comContagem(repository.save(f));
    }

    @Transactional
    public FluxogramaResponse atualizar(Long id, FluxogramaRequest request) {
        Fluxograma f = exigir(id);
        if (repository.existsByCodigoAndIdNot(request.codigo(), id)) {
            throw new ConflitoException("Ja existe outro fluxograma com o codigo " + request.codigo() + ".");
        }
        aplicar(request, f);
        return comContagem(repository.save(f));
    }

    @Transactional
    public void excluir(Long id) {
        Fluxograma f = exigir(id);
        List<FluxogramaSintoma> associacoes = associacaoRepository.findByFluxogramaIdOrderByOrdemAscIdAsc(id);
        if (!associacoes.isEmpty()) {
            throw new ConflitoException("O fluxograma " + f.getCodigo() + " possui " + associacoes.size()
                    + " sintoma(s) associado(s). Remova as associacoes antes de excluir, ou apenas desative-o.");
        }
        repository.delete(f);
    }

    // --- Associacao de sintomas ---

    public List<SintomaAssociadoResponse> listarSintomas(Long fluxogramaId) {
        exigir(fluxogramaId);
        return associacaoRepository.findByFluxogramaIdOrderByOrdemAscIdAsc(fluxogramaId).stream()
                .map(Mapeadores::paraResponse)
                .toList();
    }

    @Transactional
    public SintomaAssociadoResponse associarSintoma(Long fluxogramaId, AssociacaoSintomaRequest request) {
        Fluxograma fluxograma = exigir(fluxogramaId);
        Sintoma sintoma = sintomaService.exigir(request.sintomaId());
        associacaoRepository.findByFluxogramaIdAndSintomaId(fluxogramaId, request.sintomaId())
                .ifPresent(existente -> {
                    throw new ConflitoException("O sintoma " + sintoma.getCodigo()
                            + " ja esta associado a este fluxograma.");
                });

        Classificacao especifica = request.classificacaoEspecificaId() == null
                ? null
                : classificacaoService.exigir(request.classificacaoEspecificaId());

        FluxogramaSintoma associacao = new FluxogramaSintoma(fluxograma, sintoma, especifica, request.ordem());
        return Mapeadores.paraResponse(associacaoRepository.save(associacao));
    }

    @Transactional
    public SintomaAssociadoResponse atualizarAssociacao(Long fluxogramaId, Long sintomaId,
                                                        AssociacaoSintomaRequest request) {
        FluxogramaSintoma associacao = associacaoRepository
                .findByFluxogramaIdAndSintomaId(fluxogramaId, sintomaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Associacao entre fluxograma " + fluxogramaId + " e sintoma " + sintomaId + " nao encontrada."));

        associacao.setClassificacaoEspecifica(request.classificacaoEspecificaId() == null
                ? null
                : classificacaoService.exigir(request.classificacaoEspecificaId()));
        if (request.ordem() != null) {
            associacao.setOrdem(request.ordem());
        }
        return Mapeadores.paraResponse(associacaoRepository.save(associacao));
    }

    @Transactional
    public void desassociarSintoma(Long fluxogramaId, Long sintomaId) {
        FluxogramaSintoma associacao = associacaoRepository
                .findByFluxogramaIdAndSintomaId(fluxogramaId, sintomaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Associacao entre fluxograma " + fluxogramaId + " e sintoma " + sintomaId + " nao encontrada."));
        associacaoRepository.delete(associacao);
    }

    private FluxogramaResponse comContagem(Fluxograma f) {
        long quantidade = associacaoRepository.findByFluxogramaIdOrderByOrdemAscIdAsc(f.getId()).size();
        return Mapeadores.paraResponse(f, quantidade);
    }

    private void aplicar(FluxogramaRequest request, Fluxograma f) {
        f.setCodigo(request.codigo());
        f.setNome(request.nome());
        f.setDescricao(request.descricao());
        f.setGrupo(grupoService.exigir(request.grupoId()));
        f.setModeloDmn(request.modeloDmn() == null || request.modeloDmn().isBlank() ? null : request.modeloDmn());
        f.setAtivo(request.ativo() == null || request.ativo());
    }
}
