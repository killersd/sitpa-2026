package br.com.sitpa.repository;

import br.com.sitpa.domain.FluxogramaSintoma;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FluxogramaSintomaRepository extends JpaRepository<FluxogramaSintoma, Long> {

    @EntityGraph(attributePaths = {"sintoma", "sintoma.classificacao", "classificacaoEspecifica"})
    List<FluxogramaSintoma> findByFluxogramaIdOrderByOrdemAscIdAsc(Long fluxogramaId);

    Optional<FluxogramaSintoma> findByFluxogramaIdAndSintomaId(Long fluxogramaId, Long sintomaId);

    boolean existsBySintomaId(Long sintomaId);

    boolean existsByClassificacaoEspecificaId(Long classificacaoId);
}
