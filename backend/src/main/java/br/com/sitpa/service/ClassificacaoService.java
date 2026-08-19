package br.com.sitpa.service;

import br.com.sitpa.domain.Classificacao;
import br.com.sitpa.dto.ClassificacaoRequest;
import br.com.sitpa.dto.ClassificacaoResponse;
import br.com.sitpa.exception.ConflitoException;
import br.com.sitpa.exception.RecursoNaoEncontradoException;
import br.com.sitpa.repository.ClassificacaoRepository;
import br.com.sitpa.repository.FluxogramaSintomaRepository;
import br.com.sitpa.repository.SintomaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD dos niveis de risco do protocolo. */
@Service
@Transactional(readOnly = true)
public class ClassificacaoService {

    private final ClassificacaoRepository repository;
    private final SintomaRepository sintomaRepository;
    private final FluxogramaSintomaRepository associacaoRepository;

    public ClassificacaoService(ClassificacaoRepository repository,
                                SintomaRepository sintomaRepository,
                                FluxogramaSintomaRepository associacaoRepository) {
        this.repository = repository;
        this.sintomaRepository = sintomaRepository;
        this.associacaoRepository = associacaoRepository;
    }

    public List<ClassificacaoResponse> listar(boolean apenasAtivas) {
        List<Classificacao> encontradas = apenasAtivas
                ? repository.findByAtivoTrueOrderByPrioridadeAsc()
                : repository.findAllByOrderByPrioridadeAsc();
        return encontradas.stream().map(Mapeadores::paraResponse).toList();
    }

    public ClassificacaoResponse buscar(Long id) {
        return Mapeadores.paraResponse(exigir(id));
    }

    public Classificacao exigir(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Classificacao", id));
    }

    public Classificacao exigirPorCodigo(String codigo) {
        return repository.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Classificacao", codigo));
    }

    @Transactional
    public ClassificacaoResponse criar(ClassificacaoRequest request) {
        if (repository.existsByCodigo(request.codigo())) {
            throw new ConflitoException("Ja existe uma classificacao com o codigo " + request.codigo() + ".");
        }
        if (repository.existsByPrioridade(request.prioridade())) {
            throw new ConflitoException("A prioridade " + request.prioridade()
                    + " ja pertence a outra classificacao. A escala de gravidade nao admite empates.");
        }
        Classificacao c = new Classificacao();
        aplicar(request, c);
        return Mapeadores.paraResponse(repository.save(c));
    }

    @Transactional
    public ClassificacaoResponse atualizar(Long id, ClassificacaoRequest request) {
        Classificacao c = exigir(id);
        if (repository.existsByCodigoAndIdNot(request.codigo(), id)) {
            throw new ConflitoException("Ja existe outra classificacao com o codigo " + request.codigo() + ".");
        }
        if (repository.existsByPrioridadeAndIdNot(request.prioridade(), id)) {
            throw new ConflitoException("A prioridade " + request.prioridade()
                    + " ja pertence a outra classificacao. A escala de gravidade nao admite empates.");
        }
        aplicar(request, c);
        return Mapeadores.paraResponse(repository.save(c));
    }

    @Transactional
    public void excluir(Long id) {
        Classificacao c = exigir(id);
        if (sintomaRepository.existsByClassificacaoId(id)) {
            throw new ConflitoException("A classificacao " + c.getCodigo()
                    + " esta atribuida a sintomas. Reatribua esses sintomas antes de excluir,"
                    + " ou apenas desative a classificacao.");
        }
        if (associacaoRepository.existsByClassificacaoEspecificaId(id)) {
            throw new ConflitoException("A classificacao " + c.getCodigo()
                    + " e usada como gravidade especifica em associacoes de fluxograma."
                    + " Remova essas sobrescritas antes de excluir.");
        }
        repository.delete(c);
    }

    private void aplicar(ClassificacaoRequest request, Classificacao c) {
        c.setCodigo(request.codigo());
        c.setNome(request.nome());
        c.setCor(request.cor());
        c.setPrioridade(request.prioridade());
        c.setTempoMaximoEsperaMinutos(request.tempoMaximoEsperaMinutos());
        c.setLocalAtendimento(request.localAtendimento());
        c.setTipoAtendimento(request.tipoAtendimento());
        c.setAtivo(request.ativo() == null || request.ativo());
    }
}
