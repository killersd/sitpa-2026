package br.com.sitpa.service;

import br.com.sitpa.domain.Grupo;
import br.com.sitpa.dto.GrupoRequest;
import br.com.sitpa.dto.GrupoResponse;
import br.com.sitpa.exception.ConflitoException;
import br.com.sitpa.exception.RecursoNaoEncontradoException;
import br.com.sitpa.repository.FluxogramaRepository;
import br.com.sitpa.repository.GrupoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD dos grupos (categorias amplas) do protocolo. */
@Service
@Transactional(readOnly = true)
public class GrupoService {

    private final GrupoRepository repository;
    private final FluxogramaRepository fluxogramaRepository;

    public GrupoService(GrupoRepository repository, FluxogramaRepository fluxogramaRepository) {
        this.repository = repository;
        this.fluxogramaRepository = fluxogramaRepository;
    }

    public List<GrupoResponse> listar(boolean apenasAtivos) {
        List<Grupo> grupos = apenasAtivos
                ? repository.findByAtivoTrueOrderByNomeAsc()
                : repository.findAllByOrderByNomeAsc();
        return grupos.stream()
                .map(g -> Mapeadores.paraResponse(g, fluxogramaRepository.findByGrupoIdOrderByNomeAsc(g.getId()).size()))
                .toList();
    }

    public GrupoResponse buscar(Long id) {
        Grupo g = exigir(id);
        return Mapeadores.paraResponse(g, fluxogramaRepository.findByGrupoIdOrderByNomeAsc(id).size());
    }

    public Grupo exigir(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Grupo", id));
    }

    @Transactional
    public GrupoResponse criar(GrupoRequest request) {
        if (repository.existsByCodigo(request.codigo())) {
            throw new ConflitoException("Ja existe um grupo com o codigo " + request.codigo() + ".");
        }
        Grupo g = new Grupo();
        aplicar(request, g);
        return Mapeadores.paraResponse(repository.save(g), 0);
    }

    @Transactional
    public GrupoResponse atualizar(Long id, GrupoRequest request) {
        Grupo g = exigir(id);
        if (repository.existsByCodigoAndIdNot(request.codigo(), id)) {
            throw new ConflitoException("Ja existe outro grupo com o codigo " + request.codigo() + ".");
        }
        aplicar(request, g);
        return Mapeadores.paraResponse(repository.save(g),
                fluxogramaRepository.findByGrupoIdOrderByNomeAsc(id).size());
    }

    @Transactional
    public void excluir(Long id) {
        Grupo g = exigir(id);
        if (fluxogramaRepository.existsByGrupoId(id)) {
            throw new ConflitoException("O grupo " + g.getCodigo()
                    + " possui fluxogramas vinculados. Exclua ou mova os fluxogramas antes.");
        }
        repository.delete(g);
    }

    private void aplicar(GrupoRequest request, Grupo g) {
        g.setCodigo(request.codigo());
        g.setNome(request.nome());
        g.setDescricao(request.descricao());
        g.setAtivo(request.ativo() == null || request.ativo());
    }
}
