package br.com.sitpa.repository;

import br.com.sitpa.domain.Classificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassificacaoRepository extends JpaRepository<Classificacao, Long> {

    Optional<Classificacao> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    boolean existsByPrioridade(Integer prioridade);

    boolean existsByPrioridadeAndIdNot(Integer prioridade, Long id);

    List<Classificacao> findAllByOrderByPrioridadeAsc();

    List<Classificacao> findByAtivoTrueOrderByPrioridadeAsc();

    /** Menos grave entre as ativas: piso usado quando nenhuma regra dispara. */
    Optional<Classificacao> findFirstByAtivoTrueOrderByPrioridadeDesc();
}
