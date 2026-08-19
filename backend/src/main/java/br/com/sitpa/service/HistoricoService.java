package br.com.sitpa.service;

import br.com.sitpa.domain.ClassificacaoRealizada;
import br.com.sitpa.dto.ClassificacaoRealizadaResponse;
import br.com.sitpa.dto.EstatisticaClassificacaoResponse;
import br.com.sitpa.exception.RecursoNaoEncontradoException;
import br.com.sitpa.repository.ClassificacaoRealizadaRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Consulta do historico de classificacoes realizadas e estatisticas derivadas. */
@Service
@Transactional(readOnly = true)
public class HistoricoService {

    private final ClassificacaoRealizadaRepository repository;

    public HistoricoService(ClassificacaoRealizadaRepository repository) {
        this.repository = repository;
    }

    /**
     * Filtros combinaveis do historico. Todos opcionais; os nulos simplesmente nao restringem.
     *
     * @param de  inicio do intervalo, inclusivo
     * @param ate fim do intervalo, inclusivo
     */
    public record Filtro(Instant de,
                         Instant ate,
                         String classificacaoCodigo,
                         Long fluxogramaId,
                         Long grupoId,
                         String usuarioLogin,
                         String identificadorPaciente) {
    }

    public Page<ClassificacaoRealizadaResponse> consultar(Filtro filtro, Pageable pageable) {
        return repository.findAll(especificacao(filtro), pageable).map(Mapeadores::paraResponse);
    }

    public ClassificacaoRealizadaResponse buscar(Long id) {
        return repository.findById(id)
                .map(Mapeadores::paraResponse)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Classificacao realizada", id));
    }

    public List<EstatisticaClassificacaoResponse> estatisticas() {
        var resumos = repository.resumirPorClassificacao();
        long total = resumos.stream().mapToLong(ClassificacaoRealizadaRepository.ResumoPorClassificacao::getTotal).sum();
        return resumos.stream()
                .map(r -> new EstatisticaClassificacaoResponse(r.getCodigo(), r.getNome(), r.getCor(),
                        r.getPrioridade(), r.getTotal(),
                        total == 0 ? 0d : Math.round(r.getTotal() * 10000d / total) / 100d))
                .toList();
    }

    private Specification<ClassificacaoRealizada> especificacao(Filtro filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (filtro.de() != null) {
                predicados.add(cb.greaterThanOrEqualTo(raiz.get("realizadaEm"), filtro.de()));
            }
            if (filtro.ate() != null) {
                predicados.add(cb.lessThanOrEqualTo(raiz.get("realizadaEm"), filtro.ate()));
            }
            if (temTexto(filtro.classificacaoCodigo())) {
                predicados.add(cb.equal(raiz.get("classificacaoCodigo"), filtro.classificacaoCodigo()));
            }
            if (filtro.fluxogramaId() != null) {
                predicados.add(cb.equal(raiz.get("fluxogramaId"), filtro.fluxogramaId()));
            }
            if (filtro.grupoId() != null) {
                predicados.add(cb.equal(raiz.get("grupoId"), filtro.grupoId()));
            }
            if (temTexto(filtro.usuarioLogin())) {
                predicados.add(cb.equal(cb.lower(raiz.get("usuarioLogin")), filtro.usuarioLogin().toLowerCase()));
            }
            if (temTexto(filtro.identificadorPaciente())) {
                predicados.add(cb.like(cb.lower(raiz.get("identificadorPaciente")),
                        "%" + filtro.identificadorPaciente().toLowerCase() + "%"));
            }
            return predicados.isEmpty() ? cb.conjunction() : cb.and(predicados.toArray(Predicate[]::new));
        };
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
