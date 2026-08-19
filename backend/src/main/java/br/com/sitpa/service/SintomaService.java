package br.com.sitpa.service;

import br.com.sitpa.domain.Sintoma;
import br.com.sitpa.dto.SintomaRequest;
import br.com.sitpa.dto.SintomaResponse;
import br.com.sitpa.exception.ConflitoException;
import br.com.sitpa.exception.RecursoNaoEncontradoException;
import br.com.sitpa.repository.FluxogramaSintomaRepository;
import br.com.sitpa.repository.SintomaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD dos sintomas/discriminadores, cada um com sua gravidade padrao. */
@Service
@Transactional(readOnly = true)
public class SintomaService {

    private final SintomaRepository repository;
    private final FluxogramaSintomaRepository associacaoRepository;
    private final ClassificacaoService classificacaoService;

    public SintomaService(SintomaRepository repository,
                          FluxogramaSintomaRepository associacaoRepository,
                          ClassificacaoService classificacaoService) {
        this.repository = repository;
        this.associacaoRepository = associacaoRepository;
        this.classificacaoService = classificacaoService;
    }

    public List<SintomaResponse> listar(boolean apenasAtivos) {
        List<Sintoma> sintomas = apenasAtivos
                ? repository.findByAtivoTrueOrderByNomeAsc()
                : repository.findAllByOrderByNomeAsc();
        return sintomas.stream().map(Mapeadores::paraResponse).toList();
    }

    public SintomaResponse buscar(Long id) {
        return Mapeadores.paraResponse(exigir(id));
    }

    public Sintoma exigir(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Sintoma", id));
    }

    @Transactional
    public SintomaResponse criar(SintomaRequest request) {
        if (repository.existsByCodigo(request.codigo())) {
            throw new ConflitoException("Ja existe um sintoma com o codigo " + request.codigo() + ".");
        }
        Sintoma s = new Sintoma();
        aplicar(request, s);
        return Mapeadores.paraResponse(repository.save(s));
    }

    @Transactional
    public SintomaResponse atualizar(Long id, SintomaRequest request) {
        Sintoma s = exigir(id);
        if (repository.existsByCodigoAndIdNot(request.codigo(), id)) {
            throw new ConflitoException("Ja existe outro sintoma com o codigo " + request.codigo() + ".");
        }
        aplicar(request, s);
        return Mapeadores.paraResponse(repository.save(s));
    }

    @Transactional
    public void excluir(Long id) {
        Sintoma s = exigir(id);
        if (associacaoRepository.existsBySintomaId(id)) {
            throw new ConflitoException("O sintoma " + s.getCodigo()
                    + " esta associado a fluxogramas. Desassocie-o antes de excluir, ou apenas desative-o.");
        }
        repository.delete(s);
    }

    private void aplicar(SintomaRequest request, Sintoma s) {
        s.setCodigo(request.codigo());
        s.setNome(request.nome());
        s.setDescricao(request.descricao());
        s.setClassificacao(classificacaoService.exigir(request.classificacaoId()));
        s.setAtivo(request.ativo() == null || request.ativo());
    }
}
